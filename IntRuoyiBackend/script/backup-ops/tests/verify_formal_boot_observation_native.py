"""Finite native read-only observation; no production originals or disk durability proof."""
import json
import os
from pathlib import Path
import subprocess
import shutil
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import verify_formal_boot_state_native as writer_fixture
import formal_snapshot_linux as snapshot
import formal_boot_state as boot
import formal_boot_observation as observation


class NativeBootObservationTest(unittest.TestCase):
    setUp=writer_fixture.NativeBootStateTest.setUp
    tearDown=writer_fixture.NativeBootStateTest.tearDown
    save_request=writer_fixture.NativeBootStateTest.save_request
    run_writer=writer_fixture.NativeBootStateTest.run_writer

    def observe(self):
        return observation.observe_approved_boot_state(str(self.approval),self.approved)

    def filesystem(self):
        # The assertion's directory traversal must itself preserve atime.
        # Path.rglob/scandir(path) can update relatime again on coarse-clock tmpfs.
        def walk(path):
            yield path
            if path.is_dir() and not path.is_symlink():
                fd=os.open(path,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW|os.O_NOATIME)
                try:names=sorted(os.listdir(fd))
                finally:os.close(fd)
                for name in names:yield from walk(path/name)
        paths=list(walk(self.root))
        return {str(p):tuple(getattr(p.lstat(),n) for n in ('st_dev','st_ino','st_mode','st_uid',
            'st_gid','st_nlink','st_size','st_atime_ns','st_mtime_ns','st_ctime_ns')) for p in paths}

    def old_atime(self):
        for name in self.filesystem():
            info=Path(name).stat()
            os.utime(name,ns=(1,info.st_mtime_ns),follow_symlinks=False)

    def assert_same_filesystem(self,before):
        after=self.filesystem()
        changed={name:(before.get(name),after.get(name)) for name in before.keys()|after.keys()
                 if before.get(name)!=after.get(name)}
        self.assertFalse(changed,changed)

    def assert_finite(self,result):
        self.assertEqual(result['state'],'BOOT_STATE_OBSERVED_ONLY')
        for key in ('durableClosedConfirmed','liveAdmissionClosed','operatorPermissionVerified',
                    'applicationIdentityVerified','productionEvidence','releaseReady','serviceActionsPerformed',
                    'claimReleased','writerExitConfirmed','ownershipAcquired','lockHeldObserved'):
            self.assertIs(result[key],False)

    def test_unattempted_is_readonly_and_does_not_create_locks(self):
        self.old_atime()
        before=self.filesystem()
        result=self.observe()
        self.assert_same_filesystem(before)
        self.assertEqual(result['transition'],'NOT_RECORDED')
        self.assertEqual(result['target']['matches'],'ORIGINAL_OPEN')
        self.assertFalse(result['savedOriginal']['exists'])
        self.assert_finite(result)

    def test_completed_is_observation_not_durable_or_live_confirmation(self):
        self.run_writer(); self.old_atime(); before=self.filesystem()
        result=self.observe()
        self.assert_same_filesystem(before)
        self.assertEqual(result['transition'],'CLOSED_RECORDED')
        self.assertEqual(result['target']['matches'],'EXPECTED_CLOSED')
        self.assertTrue(result['recordedIdentityMatchesCurrent'])
        self.assertTrue(result['hostBootIdMatches'])
        self.assert_finite(result)

    def test_saved_original_before_event_is_retained_without_completion(self):
        saved=self.claim_path/'boot-backend-original-open.json'
        saved.write_bytes(self.original); saved.chmod(0o600)
        result=self.observe()
        self.assertEqual(result['transition'],'NOT_RECORDED')
        self.assertTrue(result['savedOriginal']['exists'])
        self.assert_finite(result)

    def test_each_partial_journal_prefix_retains_actual_target(self):
        self.run_writer()
        (self.claim_path/'event-000004.json').unlink()
        result=self.observe()
        self.assertEqual(result['transition'],'INTENT_RECORDED')
        self.assertEqual(result['target']['matches'],'EXPECTED_CLOSED')
        self.assertIsNone(result['recordedIdentityMatchesCurrent'])
        (self.claim_path/'event-000003.json').unlink()
        self.assertEqual(self.observe()['transition'],'BOUND_RECORDED')

    def test_pending_bytes_are_reported_without_cleanup_or_replay(self):
        self.run_writer(); (self.claim_path/'event-000004.json').unlink()
        pending=self.directory/('.boot-close-'+self.approved+'.pending')
        pending.write_bytes(b'partial'); pending.chmod(0o644)
        result=self.observe()
        self.assertTrue(result['pending']['exists'])
        self.assertFalse(result['pending']['matchesExpectedClosed'])
        self.assertEqual(pending.read_bytes(),b'partial')
        self.assert_finite(result)

    def test_closed_record_does_not_hide_open_or_replaced_current_target(self):
        self.run_writer()
        for raw,match in ((self.original,'ORIGINAL_OPEN'),(self.closed,'EXPECTED_CLOSED'),(b'unknown','OTHER')):
            with self.subTest(match=match):
                replacement=self.directory/'replacement'
                replacement.write_bytes(raw); replacement.chmod(0o644); replacement.replace(self.target)
                result=self.observe()
                self.assertEqual(result['target']['matches'],match)
                self.assertFalse(result['recordedIdentityMatchesCurrent'])
                self.assert_finite(result)

    def test_missing_target_is_a_fact_not_success_or_an_open_action(self):
        self.run_writer(); self.target.unlink()
        result=self.observe()
        self.assertFalse(result['target']['exists'])
        self.assertFalse(result['recordedIdentityMatchesCurrent'])
        self.assert_finite(result)

    def test_other_host_boot_reports_mismatch(self):
        self.request['hostBootId']='00000000-0000-0000-0000-000000000099';self.save_request()
        self.assertFalse(self.observe()['hostBootIdMatches'])

    def test_wrong_approval_and_wrong_schema_refuse(self):
        approved=self.approved;self.approved='d'*64
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_APPROVAL_CHANGED'):self.observe()
        self.approved=approved;self.request['schemaVersion']='release-formal-capture-request/v5';self.save_request()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_REQUEST_INVALID'):self.observe()

    def test_originals_drift_refuses(self):
        (self.claim_path/'shared-container-original.json').write_bytes(b'changed')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_ORIGINALS_CHANGED'):self.observe()

    def test_saved_original_drift_refuses(self):
        self.run_writer(); (self.claim_path/'boot-backend-original-open.json').write_bytes(b'changed')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_SAVED_ORIGINAL_INVALID'):self.observe()

    def test_truncated_journal_and_out_of_order_role_events_refuse(self):
        self.run_writer(); path=self.claim_path/'event-000004.json'; original=path.read_bytes()
        path.write_bytes(original[:-3])
        with self.assertRaises(snapshot.FormalSnapshotError):self.observe()
        event=json.loads(original);event['step']='boot-test-backend-closed'
        path.write_bytes(snapshot._canonical_bytes(event))
        # Selected role now only has bound + intent; another role cannot stand in for its completion.
        self.assertEqual(self.observe()['transition'],'INTENT_RECORDED')
        event=json.loads((self.claim_path/'event-000003.json').read_bytes())
        event['step']='boot-backend-closed'; event['details']['publishedIdentity']={'device':1,'inode':2}
        (self.claim_path/'event-000003.json').write_bytes(snapshot._canonical_bytes(event));path.unlink()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_EVENT_ORDER_INVALID'):self.observe()

    def test_sampling_change_refuses_without_retry(self):
        original=observation._sample;calls=[]
        def changed(*args,**kwargs):
            result=original(*args,**kwargs);calls.append(1)
            if len(calls)==1:self.target.write_bytes(b'changed')
            return result
        with patch.object(observation,'_sample',side_effect=changed):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_OBSERVATION_CHANGED'):self.observe()
        self.assertEqual(len(calls),2)

    def test_symlink_hardlink_and_fifo_refuse_without_blocking(self):
        for kind in ('symlink','hardlink','fifo'):
            with self.subTest(kind=kind):
                self.target.unlink()
                if kind=='symlink':self.target.symlink_to(self.approval)
                elif kind=='hardlink':os.link(self.approval,self.target)
                else:os.mkfifo(self.target,0o644)
                command=['python3','-B','-c','import sys;sys.path.insert(0,"/workspace/linux");'
                         'import formal_boot_observation as o;o.observe_approved_boot_state(sys.argv[1],sys.argv[2])',
                         str(self.approval),self.approved]
                result=subprocess.run(command,capture_output=True,timeout=3)
                self.assertNotEqual(result.returncode,0)
                self.assertIn(b'FORMAL_BOOT_',result.stderr)

    def test_real_cli_never_dispatches_the_writer(self):
        result=subprocess.run(['python3','-B','/workspace/linux/backup_ops_linux.py','--mode','formal-capture',
            '--observe-boot-state','--request',str(self.approval),'--expected-request-sha256',self.approved],
            capture_output=True,timeout=5)
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        self.assert_finite(json.loads(result.stdout))
        self.assertEqual(self.target.read_bytes(),self.original)
        self.assertEqual(len(list(self.claim_path.glob('event-*'))),1)

    def test_unprivileged_uid_refuses(self):
        child=os.fork()
        if child==0:
            try:os.setuid(65534);self.observe()
            except snapshot.FormalSnapshotError as error:os._exit(0 if error.code=='FORMAL_BOOT_ROOT_REQUIRED' else 11)
            except BaseException:os._exit(12)
            os._exit(13)
        _,status=os.waitpid(child,0)
        self.assertEqual(os.waitstatus_to_exitcode(status),0)

    def test_snapshot_measurement_itself_preserves_old_atime(self):
        self.run_writer();self.old_atime()
        before=self.filesystem();self.assert_same_filesystem(before)
        self.assertTrue(all(value[7]==1 for value in before.values()))

    def test_writable_cli_copy_without_B_does_not_write_bytecode(self):
        tools=self.root/'tools'
        shutil.copytree(Path('/workspace/linux'),tools,ignore=shutil.ignore_patterns('__pycache__','*.pyc'))
        result=subprocess.run(['python3',str(tools/'backup_ops_linux.py'),'--mode','formal-capture',
            '--observe-boot-state','--request',str(self.approval),'--expected-request-sha256',self.approved],
            capture_output=True,timeout=5)
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        self.assertFalse(list(tools.rglob('*.pyc')))
        self.assertFalse(list(tools.rglob('__pycache__')))

    def test_large_claim_directory_is_bounded_before_materialization(self):
        for index in range(257):(self.claim_path/('extra-'+str(index))).touch()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_DIRECTORY_LIMIT'):self.observe()

    def test_accepted_abbreviation_never_authors_bytecode_in_either_cli(self):
        for script,arguments in (('backup_ops_linux.py',['--mode','formal-capture','--observe-boot']),
                                  ('formal_capture_producer.py',['--observe-boot'])):
            with self.subTest(script=script):
                tools=self.root/script.replace('.py','-copy')
                shutil.copytree(Path('/workspace/linux'),tools,ignore=shutil.ignore_patterns('__pycache__','*.pyc'))
                result=subprocess.run(['python3',str(tools/script),*arguments,'--request',str(self.approval),
                    '--expected-request-sha256',self.approved],capture_output=True,timeout=5)
                self.assertEqual(result.returncode,0,result.stdout+result.stderr)
                self.assert_finite(json.loads(result.stdout))
                self.assertFalse(list(tools.rglob('*.pyc')))

    def test_deep_request_real_cli_has_fixed_error_without_traceback(self):
        raw=b'{"nested":'+b'['*7000+b'0'+b']'*7000+b'}'
        self.approval.write_bytes(raw);self.approved=boot._sha(raw)
        result=subprocess.run(['python3','-B','/workspace/linux/backup_ops_linux.py','--mode','formal-capture',
            '--observe-boot-state','--request',str(self.approval),'--expected-request-sha256',self.approved],
            capture_output=True,timeout=5)
        self.assertEqual(result.returncode,1)
        self.assertEqual(result.stderr,b'')
        value=json.loads(result.stdout)
        self.assertEqual(value['code'],'FORMAL_BOOT_REQUEST_INVALID')
        self.assertEqual(value['state'],'FAILED_RETAINED')
        self.assertNotIn('nested',result.stdout.decode())

    def test_deep_journal_refuses_as_fixed_error(self):
        path=self.claim_path/'event-000001.json'
        plan=snapshot._canonical_bytes(self.binding)
        raw=b'{"details":{"nested":'+b'['*7000+b'0'+b']'*7000+b'},"previousDigest":"'+boot._sha(plan).encode()+b'","sequence":1,"step":"maintenance-originals-bound"}\n'
        path.write_bytes(raw)
        # This pinned native Python can serialize 7000 levels; validation still
        # rejects the wrong originals shape. Portable parser recursion is tested separately.
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_BOOT_ORIGINALS_REQUIRED'):self.observe()


if __name__=='__main__':unittest.main(verbosity=2)
