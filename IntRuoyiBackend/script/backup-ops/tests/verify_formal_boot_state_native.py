"""Owned Linux files/processes only; injected I/O faults are not production proof."""
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

LINUX = Path(__file__).parents[1] / 'linux'
sys.path.insert(0, str(LINUX))
import formal_snapshot_linux as snapshot
import formal_capture_producer as producer
import formal_boot_state as boot


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


class NativeBootStateTest(unittest.TestCase):
    def setUp(self):
        self.assertEqual(os.name, 'posix')
        self.assertEqual(os.geteuid(), 0, 'OWNED_NATIVE_ROOT_FIXTURE_REQUIRED')
        self.root = Path(tempfile.mkdtemp(prefix='boot-', dir='/control'))
        self.directory = self.root / 'boot'; self.directory.mkdir(mode=0o755)
        self.target = self.directory / 'state.json'
        self.value = {'schemaVersion':'formal-maintenance-boot/v1', 'instanceId':'owned-backend',
                      'artifactSha256':'a'*64, 'state':'OPEN', 'epoch':'', 'phase':'NORMAL'}
        self.original = (json.dumps(self.value, indent=2)+'\n').encode()
        self.target.write_bytes(self.original); self.target.chmod(0o644)
        self.binding = {'operationId':'owned-boot', 'sourceFingerprint':'b'*64,
                        'databaseUuid':'00000000-0000-0000-0000-000000000001', 'minioContainerId':'c'*64}
        self.claim_path = self.root / 'claim'
        with_claim = snapshot.FormalMaintenanceClaim(self.claim_path, self.binding)
        originals={}
        for name in boot.ORIGINALS:
            raw=b'{"fixture":"retained bytes only, not live originals proof"}\n'
            with_claim._write(self.claim_path/name,raw); originals[name]=digest(raw)
        with_claim.record('maintenance-originals-bound', {'originalFileSha256':originals})
        with_claim.close()
        info = self.target.stat()
        self.closed = snapshot._canonical_bytes(dict(self.value, state='CLOSED', epoch=self.binding['operationId']))
        self.request = {'schemaVersion':'release-formal-capture-request/v6', 'action':'close-durable-boot-state',
                        'claimRoot':str(self.claim_path), 'claimBinding':self.binding, 'totalSeconds':30,
                        'hostBootId':Path('/proc/sys/kernel/random/boot_id').read_text().strip(),
                        'bootBinding':{'role':'backend', 'path':str(self.target), 'instanceId':'owned-backend',
                            'artifactSha256':'a'*64, 'originalOpenSha256':digest(self.original),
                            'closedSha256':digest(self.closed), 'epoch':self.binding['operationId'],
                            'expectedIdentity':{'device':info.st_dev, 'inode':info.st_ino}}}
        self.approval = self.root / 'request.json'; self.save_request()

    def tearDown(self):
        # Every path is beneath the explicitly task-owned private tmpfs directory.
        shutil.rmtree(self.root)

    def save_request(self):
        self.approval.write_bytes(snapshot._canonical_bytes(self.request)); self.approval.chmod(0o600)
        self.approved = digest(self.approval.read_bytes())

    def run_writer(self):
        return boot.close_approved_boot_state(str(self.approval), self.approved)

    def events(self):
        claim = snapshot.FormalMaintenanceClaim(self.claim_path, self.binding, resume=True)
        try:return [e['step'] for e in claim.events]
        finally:claim.close()

    def test_exact_saved_bytes_native_replace_and_completion(self):
        before = self.target.stat().st_ino
        result = self.run_writer()
        self.assertEqual(self.target.read_bytes(), self.closed)
        self.assertNotEqual(self.target.stat().st_ino, before)
        self.assertEqual((self.claim_path/'boot-backend-original-open.json').read_bytes(), self.original)
        self.assertEqual(self.target.stat().st_mode & 0o777, 0o644)
        self.assertEqual(self.events()[-3:], ['boot-backend-bound','boot-backend-close-intent','boot-backend-closed'])
        self.assertEqual(result['state'], 'DURABLE_CLOSED_PENDING_JVM_CLOSE')
        self.assertTrue(result['durableClosedConfirmed'])
        for name in ('liveAdmissionClosed','operatorPermissionVerified','applicationIdentityVerified',
                     'productionEvidence','releaseReady','serviceActionsPerformed','claimReleased'):
            self.assertIs(result[name], False)

    def test_real_unprivileged_uid_refuses_without_reading_or_writing(self):
        child = os.fork()
        if child == 0:
            try:
                os.setuid(65534)
                self.run_writer()
            except snapshot.FormalSnapshotError as error:
                os._exit(0 if error.code=='FORMAL_BOOT_ROOT_REQUIRED' else 11)
            except BaseException:os._exit(12)
            os._exit(13)
        _, status = os.waitpid(child, 0)
        self.assertEqual(os.waitstatus_to_exitcode(status), 0)
        self.assertEqual(self.events(), ['maintenance-originals-bound'])
        self.assertEqual(self.target.read_bytes(), self.original)

    def test_wrong_approval_refuses_before_claim_or_target_mutation(self):
        self.approved = 'd'*64
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_APPROVAL_CHANGED'):self.run_writer()
        self.assertEqual(self.events(), ['maintenance-originals-bound'])
        self.assertFalse((self.directory/'state.json.writer.lock').exists())

    def test_same_bytes_different_inode_refuses(self):
        replacement = self.directory/'replacement'; replacement.write_bytes(self.original); replacement.chmod(0o644)
        replacement.replace(self.target)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_BINDING_CHANGED'):self.run_writer()
        self.assertEqual(self.target.read_bytes(), self.original)
        self.assertEqual(self.events(), ['maintenance-originals-bound'])

    def test_symlink_target_parent_and_lock_refuse(self):
        for case in ('target','parent','lock'):
            with self.subTest(case=case):
                if case=='target':
                    replacement=self.directory/'actual'; self.target.rename(replacement); self.target.symlink_to(replacement)
                elif case=='parent':
                    actual=self.root/'actual-dir'; self.directory.rename(actual); self.directory.symlink_to(actual)
                else:
                    lock=self.directory/'state.json.writer.lock'
                    if lock.exists():lock.unlink()  # Prior refused subcase retained an owned ordinary lock.
                    lock.symlink_to(self.approval)
                with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
                if case=='target':self.target.unlink(); replacement.rename(self.target)
                elif case=='parent':self.directory.unlink(); actual.rename(self.directory)
                else:lock.unlink()
                self.assertEqual(self.target.read_bytes(), self.original)

    def test_untrusted_owner_and_writable_ancestors_refuse(self):
        for path in (self.approval,self.target,self.directory):
            with self.subTest(owner=str(path)):
                os.chown(path,65534,-1)
                try:
                    with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
                finally:os.chown(path,0,-1)
        for path,mode in ((self.root,0o777),(self.directory,0o775),(self.approval,0o644)):
            with self.subTest(mode=str(path)):
                before=path.stat().st_mode & 0o777; path.chmod(mode)
                try:
                    with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
                finally:path.chmod(before)
        self.assertEqual(self.events(), ['maintenance-originals-bound'])
        self.assertEqual(self.target.read_bytes(), self.original)

    def test_hardlinked_target_and_approval_refuse(self):
        for path in (self.approval,self.target):
            link=self.root/'extra-link'; os.link(path,link)
            try:
                with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
            finally:link.unlink()
        self.assertEqual(self.events(), ['maintenance-originals-bound'])

    def test_record_binding_and_duplicate_fields_refuse(self):
        for raw in (self.original.replace(b'owned-backend',b'other-backend'),
                    self.original.replace(b'"OPEN"',b'"CLOSED"'),
                    self.original.replace(b'"phase": "NORMAL"',b'"phase": "NORMAL", "phase": "NORMAL"')):
            with self.subTest(record=digest(raw)):
                self.target.write_bytes(raw)
                self.request['bootBinding']['originalOpenSha256']=digest(raw); self.save_request()
                with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
        self.assertEqual(self.events(), ['maintenance-originals-bound'])

    def test_wrong_epoch_unknown_action_and_extra_field_refuse(self):
        for key,value in (('epoch','other-operation'),('artifactSha256','f'*64),('closedSha256','e'*64)):
            selected=self.request['bootBinding']; previous=selected[key]; selected[key]=value; self.save_request()
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
            selected[key]=previous
        self.request['action']='open-durable-boot-state'; self.save_request()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
        self.request['action']='close-durable-boot-state'; self.request['fallback']=True; self.save_request()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
        self.assertEqual(self.events(), ['maintenance-originals-bound'])

    def test_real_competing_process_holds_fixed_target_lock(self):
        lock=self.directory/'state.json.writer.lock'; lock.write_bytes(b''); lock.chmod(0o600)
        read_ready,write_ready=os.pipe(); read_release,write_release=os.pipe()
        child=os.fork()
        if child==0:
            fd=os.open(lock,os.O_RDWR|os.O_NOFOLLOW)
            fcntl.flock(fd,fcntl.LOCK_EX); os.write(write_ready,b'1'); os.read(read_release,1); os._exit(0)
        os.close(write_ready); os.close(read_release)
        try:
            self.assertEqual(os.read(read_ready,1),b'1')
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_TARGET_BUSY'):self.run_writer()
            self.assertEqual(self.events(), ['maintenance-originals-bound'])
            self.assertEqual(self.target.read_bytes(),self.original)
        finally:
            os.write(write_release,b'1'); os.close(write_release); os.close(read_ready); os.waitpid(child,0)

    def test_request_drift_at_temp_file_sync_retains_open_and_intent(self):
        real=os.fsync
        def drift(fd):
            real(fd)
            if os.readlink('/proc/self/fd/'+str(fd)).endswith('.pending'):
                self.approval.write_bytes(b'{}\n')
        with patch.object(boot.os,'fsync',side_effect=drift):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_APPROVAL_CHANGED'):self.run_writer()
        self.assertEqual(self.target.read_bytes(),self.original)
        self.assertIn('boot-backend-close-intent',self.events())
        self.assertNotIn('boot-backend-closed',self.events())

    def test_directory_fsync_failure_keeps_closed_without_success_or_auto_open(self):
        real=os.fsync
        def failure(fd):
            if os.readlink('/proc/self/fd/'+str(fd))==str(self.directory) and self.target.read_bytes()==self.closed:
                raise OSError('injected-private-directory-sync-failure')
            return real(fd)
        with patch.object(boot.os,'fsync',side_effect=failure):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_IO_FAILED_RETAINED'):self.run_writer()
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertNotIn('boot-backend-closed',self.events())
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_TRANSITION_ALREADY_ATTEMPTED'):self.run_writer()
        self.assertEqual(self.target.read_bytes(),self.closed)

    def test_real_process_exit_after_replace_preserves_intent_and_refuses_replay(self):
        child=os.fork()
        if child==0:
            real=os.replace
            def exit_after_replace(*args,**kwargs):
                real(*args,**kwargs); os._exit(73)
            boot.os.replace=exit_after_replace
            try:self.run_writer()
            except BaseException:os._exit(74)
            os._exit(75)
        _,status=os.waitpid(child,0)
        self.assertEqual(os.waitstatus_to_exitcode(status),73)
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertIn('boot-backend-close-intent',self.events())
        self.assertNotIn('boot-backend-closed',self.events())
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_TRANSITION_ALREADY_ATTEMPTED'):self.run_writer()

    def test_successful_transition_is_not_implicitly_replayed(self):
        self.run_writer()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_TRANSITION_ALREADY_ATTEMPTED'):self.run_writer()
        self.assertEqual(self.target.read_bytes(),self.closed)

    def test_originals_drift_refuses_before_target_lock(self):
        original=self.claim_path/'shared-container-original.json'; original.write_bytes(b'{}\n')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_ORIGINALS_CHANGED'):self.run_writer()
        self.assertFalse((self.directory/'state.json.writer.lock').exists())
        self.assertEqual(self.target.read_bytes(),self.original)

    def test_real_target_lock_inode_replacement_refuses(self):
        real=os.fsync; replaced=[]
        def change(fd):
            real(fd)
            if not replaced and os.readlink('/proc/self/fd/'+str(fd)).endswith('.writer.lock'):
                other=self.directory/'new-lock'; other.write_bytes(b''); other.chmod(0o600)
                other.replace(self.directory/'state.json.writer.lock'); replaced.append(True)
        with patch.object(boot.os,'fsync',side_effect=change):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_LOCK_CHANGED'):self.run_writer()
        self.assertEqual(self.events(),['maintenance-originals-bound'])
        self.assertEqual(self.target.read_bytes(),self.original)

    def test_real_parent_directory_replacement_before_publish_refuses(self):
        real=os.fsync; changed=[]; held=self.root/'held-original-directory'
        def change(fd):
            real(fd)
            if not changed and os.readlink('/proc/self/fd/'+str(fd)).endswith('.pending'):
                self.directory.rename(held); self.directory.mkdir(mode=0o755)
                self.target.write_bytes(self.original); self.target.chmod(0o644); changed.append(True)
        with patch.object(boot.os,'fsync',side_effect=change):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_DIRECTORY_CHANGED'):self.run_writer()
        self.assertEqual(self.target.read_bytes(),self.original)
        self.assertEqual((held/'state.json').read_bytes(),self.original)
        self.assertNotIn('boot-backend-closed',self.events())

    def _completion_drift(self,case):
        real=snapshot.FormalMaintenanceClaim.record
        def drift(claim,step,details):
            result=real(claim,step,details)
            if step=='boot-backend-closed':
                if case=='open':self.target.write_bytes(self.original)
                elif case=='same-bytes-replaced':
                    other=self.directory/'replacement'; other.write_bytes(self.closed); other.chmod(0o644)
                    other.replace(self.target)
                elif case=='permissions':self.target.chmod(0o666)
            return result
        with patch.object(snapshot.FormalMaintenanceClaim,'record',new=drift):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_writer()
        self.assertIn('boot-backend-closed',self.events())

    def test_completion_journal_window_open_drift_does_not_confirm(self):
        self._completion_drift('open')

    def test_completion_journal_window_same_bytes_inode_drift_does_not_confirm(self):
        self._completion_drift('same-bytes-replaced')

    def test_completion_journal_window_permission_drift_does_not_confirm(self):
        self._completion_drift('permissions')

    def test_fifo_target_refuses_before_blocking_open(self):
        self.target.unlink(); os.mkfifo(self.target,0o644)
        info=self.target.stat(); self.request['bootBinding']['expectedIdentity']={'device':info.st_dev,'inode':info.st_ino}
        self.save_request()
        code='''import sys
sys.path.insert(0,sys.argv[1])
from formal_boot_state import close_approved_boot_state
from formal_snapshot_linux import FormalSnapshotError
try:
    close_approved_boot_state(sys.argv[2],sys.argv[3])
except FormalSnapshotError as error:
    print(error.code)
    sys.exit(0)
sys.exit(9)
'''
        result=subprocess.run([sys.executable,'-B','-E','-s','-c',code,str(LINUX),str(self.approval),self.approved],
                              capture_output=True,timeout=1)
        self.assertEqual(result.returncode,0)
        self.assertEqual(result.stdout,b'FORMAL_BOOT_FILE_UNTRUSTED\n')
        self.assertEqual(self.events(),['maintenance-originals-bound'])

    def test_maximum_approved_operation_and_target_names_complete(self):
        previous=self.claim_path
        self.binding=dict(self.binding,operationId='o'*128)
        self.claim_path=self.root/'long-claim'
        claim=snapshot.FormalMaintenanceClaim(self.claim_path,self.binding)
        try:
            originals={}
            for name in boot.ORIGINALS:
                raw=(previous/name).read_bytes(); claim._write(self.claim_path/name,raw); originals[name]=digest(raw)
            claim.record('maintenance-originals-bound',{'originalFileSha256':originals})
        finally:claim.close()
        target=self.directory/('s'*128); self.target.rename(target); self.target=target
        self.closed=snapshot._canonical_bytes(dict(self.value,state='CLOSED',epoch=self.binding['operationId']))
        self.request['claimBinding']=self.binding; self.request['claimRoot']=str(self.claim_path)
        self.request['bootBinding'].update(path=str(self.target),epoch=self.binding['operationId'],closedSha256=digest(self.closed))
        self.save_request()
        self.assertEqual(os.pathconf(self.directory,'PC_NAME_MAX'),255)
        self.assertTrue(self.run_writer()['durableClosedConfirmed'])
        self.assertEqual(self.target.read_bytes(),self.closed)

    def test_actual_standard_cli_selects_only_explicit_v6_and_reports_finite_scope(self):
        result=subprocess.run([sys.executable,str(LINUX/'backup_ops_linux.py'),'--mode','formal-capture',
                               '--request',str(self.approval),'--expected-request-sha256',self.approved],
                              capture_output=True,timeout=10)
        self.assertEqual(result.returncode,0,result.stdout.decode())
        self.assertEqual(result.stderr,b'')
        receipt=json.loads(result.stdout)
        self.assertEqual(receipt['state'],'DURABLE_CLOSED_PENDING_JVM_CLOSE')
        self.assertFalse(receipt['releaseReady']); self.assertFalse(receipt['liveAdmissionClosed'])
        self.assertEqual(self.target.read_bytes(),self.closed)


if __name__=='__main__':unittest.main(verbosity=2)
