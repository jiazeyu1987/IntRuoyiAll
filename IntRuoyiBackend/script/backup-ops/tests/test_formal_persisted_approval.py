"""Actual Linux private files/flock and v8 peer; SQL/Docker fault boundaries.

This suite is not genuine JVM authorization or production drain evidence.
The independent real-MySQL runner exercises the selected SQL separately.
"""
import copy
import contextlib
import hashlib
import importlib
import io
import json
import os
from pathlib import Path
import signal
import time
import unittest
from unittest.mock import patch

import test_formal_live_control as live_fixture
import formal_snapshot_linux as snapshot
sha=live_fixture.sha


class PersistedApprovalTest(unittest.TestCase):
    def setUp(self):
        self.f=live_fixture.NativeLiveControlTest('test_explicit_observe_keeps_all_close_original_bytes')
        self.f.setUp(); self.m=importlib.import_module('formal_persisted_work')
        self.original={'id':'f'*64,'name':'/owned-fixture-mysql','image':'sha256:'+'a'*64,
            'mounts':[], 'restartPolicy':{},'state':{'Running':True,'Paused':False,'Restarting':False,
            'Dead':False,'OOMKilled':False,'ExitCode':0,'StartedAt':'2026-10-06T00:00:00Z',
            'FinishedAt':'0001-01-01T00:00:00Z'}}
        c=snapshot.FormalMaintenanceClaim(self.f.claimpath,self.f.binding,resume=True)
        raw=snapshot._canonical_bytes({'mysql':self.original})
        (self.f.claimpath/'shared-container-original.json').write_bytes(raw)
        # Fixture schema original binding is explicit; no actual Docker identity proof.
        event=c.events[0];event['details']['originalFileSha256']['shared-container-original.json']=sha(raw)
        event_raw=snapshot._canonical_bytes(event)
        (self.f.claimpath/'event-000001.json').write_bytes(event_raw)
        # Rechain the existing durable boot records after changing the fixture original.
        previous=event_raw
        for i in range(2,len(c.events)+1):
            e=c.events[i-1];e['previousDigest']=sha(previous);previous=snapshot._canonical_bytes(e)
            (self.f.claimpath/('event-%06d.json'%i)).write_bytes(previous)
        c.close()
        c=snapshot.FormalMaintenanceClaim(self.f.claimpath,self.f.binding,resume=True)
        c.record('shared-container-scope',{'scopeSha256':sha(raw)});c.close()
        response=self.f.response
        def zero(*args):
            value=response(*args);value['outstandingWork']=0;value['completedAcceptedRoots']=1;return value
        self.f.response=zero
        self.f.start_server();self.f.run_client()
        self.f.request['action']='observe-live-admission';self.f.save_request()
        self.credential=self.f.root/'mysql-password';self.credential.write_text('private-fixture-password\n');self.credential.chmod(0o600)
        self.request={'schemaVersion':'release-formal-capture-request/v9','action':'observe-selected-persisted-work',
            'claimRoot':str(self.f.claimpath),'claimBinding':self.f.binding,'totalSeconds':20,
            'database':'owned_probe','mysqlContainerId':self.original['id'],'mysqlCredentialFile':str(self.credential),
            'liveObservationRequestFile':str(self.f.approval),'liveObservationRequestSha256':self.f.approved}
        self.approval=self.f.root/'v9.json';self.save()

    def tearDown(self): self.f.tearDown()
    def save(self):
        self.approval.write_bytes(snapshot._canonical_bytes(self.request));self.approval.chmod(0o600)
        self.digest=sha(self.approval.read_bytes())
    def run_client(self, observer=None, inspect=None, cli=False):
        def observe(*args,**kwargs):
            kwargs['guard']()
            return {'selectedTablesTerminalConfirmed':True,'fullDrain':False,'releaseReady':False}
        with patch.object(snapshot,'inspect_formal_container',side_effect=inspect or (lambda _:copy.deepcopy(self.original))), \
             patch.object(self.m,'observe_selected_work',side_effect=observer or observe):
            if cli:
                capture=importlib.import_module('formal_capture_producer');out=io.StringIO()
                with contextlib.redirect_stdout(out):
                    status=capture.main(['--request',str(self.approval),'--expected-request-sha256',self.digest])
                self.assertNotIn('private-fixture-password',out.getvalue())
                self.assertNotIn(self.f.grant['token'],out.getvalue())
                return status,json.loads(out.getvalue())
            return self.m.run_approved_persisted_observation(str(self.approval),self.digest)
    def retained(self): return {p.name:p.read_bytes() for p in self.f.claimpath.iterdir() if p.is_file()}

    def test_actual_private_claim_and_pre_post_v8_observe(self):
        before=self.retained();result=self.run_client()
        self.assertEqual(result['state'],'SELECTED_PERSISTED_WORK_OBSERVED_ONLY')
        self.assertEqual(before,self.retained())
        for name in ('fullDrain','releaseReady','productionEvidence','hostWritersVerified','claimReleased','serviceActionsPerformed'):
            self.assertIs(result[name],False)
    def test_actual_capture_cli_v9_keeps_private_originals(self):
        before=self.retained();status,value=self.run_client(cli=True)
        self.assertEqual(status,0);self.assertEqual(value['state'],'SELECTED_PERSISTED_WORK_OBSERVED_ONLY')
        self.assertEqual(before,self.retained());self.assertIs(value['releaseReady'],False)
    def test_actual_capture_cli_failure_fixed_and_redacted(self):
        self.credential.chmod(0o644);before=self.retained();status,value=self.run_client(cli=True)
        self.assertEqual(status,1);self.assertEqual(value['state'],'FAILED_RETAINED')
        self.assertFalse(value['productionEvidence']);self.assertEqual(before,self.retained())

    def test_missing_private_request(self):
        self.approval.unlink()
        with self.assertRaises((snapshot.FormalSnapshotError,OSError)):self.run_client()
    def test_request_symlink(self):
        saved=self.f.root/'retained-v9';self.approval.rename(saved);self.approval.symlink_to(saved)
        with self.assertRaises((snapshot.FormalSnapshotError,OSError)):self.run_client()
    def test_request_mode(self):
        self.approval.chmod(0o644)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_request_digest(self):
        self.digest='0'*64
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_close_action_not_replayed(self):
        self.f.request['action']='close-live-admission';self.f.save_request()
        self.request['liveObservationRequestSha256']=self.f.approved;self.save()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_live_approval_drift(self):
        self.f.approval.write_bytes(b'{}')
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_foreign_claim(self):
        self.request['claimBinding']=dict(self.f.binding,operationId='foreign');self.save()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_foreign_container(self):
        self.request['mysqlContainerId']='1'*64;self.save()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_credential_symlink(self):
        saved=self.f.root/'retained-password';self.credential.rename(saved);self.credential.symlink_to(saved)
        with self.assertRaises((snapshot.FormalSnapshotError,OSError)):self.run_client()
    def test_final_credential_drift(self):
        def observe(*args,**kwargs):
            self.credential.write_bytes(b'changed');return {'selectedTablesTerminalConfirmed':True}
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=observe)
    def test_final_container_drift(self):
        def observe(*args,**kwargs):
            self.original['state']['StartedAt']='2026-10-06T01:00:00Z';return {}
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=observe)
    def test_last_handoff_journal_append_refused(self):
        live=importlib.import_module('formal_live_control');real=live.run_approved_live_control;calls=[]
        def changed(*args):
            result=real(*args);calls.append(1)
            if len(calls)==2:
                c=snapshot.FormalMaintenanceClaim(self.f.claimpath,self.f.binding,resume=True)
                c.record('failed-retained',{'fixture':'handoff-drift'});c.close()
            return result
        with patch.object(live,'run_approved_live_control',side_effect=changed):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_PERSISTED_CLAIM_CHANGED'):self.run_client()
    def test_last_handoff_lock_replacement_refused(self):
        live=importlib.import_module('formal_live_control');real=live.run_approved_live_control;calls=[]
        def changed(*args):
            result=real(*args);calls.append(1)
            if len(calls)==2:
                lock=self.f.claimpath/'ownership.lock';lock.rename(self.f.claimpath/'retained.lock')
                lock.write_bytes(b'');lock.chmod(0o600)
            return result
        with patch.object(live,'run_approved_live_control',side_effect=changed):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_PERSISTED_CLAIM_CHANGED'):self.run_client()
    def test_first_handoff_lock_replacement_refused(self):
        live=importlib.import_module('formal_live_control');real=live.run_approved_live_control;calls=[]
        def changed(*args):
            result=real(*args);calls.append(1)
            if len(calls)==1:
                lock=self.f.claimpath/'ownership.lock';lock.rename(self.f.claimpath/'retained.lock')
                lock.write_bytes(b'');lock.chmod(0o600)
            return result
        with patch.object(live,'run_approved_live_control',side_effect=changed):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_PERSISTED_CLAIM_CHANGED'):self.run_client()
    def test_first_handoff_journal_append_refused(self):
        live=importlib.import_module('formal_live_control');real=live.run_approved_live_control;calls=[]
        def changed(*args):
            result=real(*args);calls.append(1)
            if len(calls)==1:
                c=snapshot.FormalMaintenanceClaim(self.f.claimpath,self.f.binding,resume=True)
                c.record('external-health-scope',{'fixture':'first-handoff-drift'});c.close()
            return result
        with patch.object(live,'run_approved_live_control',side_effect=changed):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_PERSISTED_CLAIM_CHANGED'):self.run_client()
    def test_first_handoff_original_replaced_with_equal_bytes(self):
        live=importlib.import_module('formal_live_control');real=live.run_approved_live_control;calls=[]
        def changed(*args):
            result=real(*args);calls.append(1)
            if len(calls)==1:
                target=self.f.claimpath/'external-callers-original.json';raw=target.read_bytes()
                target.rename(self.f.claimpath/'retained-original.json');target.write_bytes(raw);target.chmod(0o600)
            return result
        with patch.object(live,'run_approved_live_control',side_effect=changed):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_PERSISTED_ORIGINALS_CHANGED'):self.run_client()
    def test_final_private_read_callback_journal_drift(self):
        boot=importlib.import_module('formal_boot_state');real=boot._read_secure
        at_final=[False];done=[False]
        live=importlib.import_module('formal_live_control');actual=live.run_approved_live_control;calls=[]
        def finished(*args):
            result=actual(*args);calls.append(1)
            if len(calls)==2:at_final[0]=True
            return result
        reads=[]
        def read(path,*args):
            result=real(path,*args)
            if at_final[0] and Path(path)==self.credential:
                reads.append(1)
                # final inputs(), after guard's initial/final input callbacks.
                if len(reads)==4 and not done[0]:
                    done[0]=True
                    target=self.f.claimpath/'event-000001.json'
                    target.write_bytes(b'{}')
            return result
        with patch.object(live,'run_approved_live_control',side_effect=finished),patch.object(boot,'_read_secure',side_effect=read):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertTrue(done[0])
    def test_final_private_read_deadline_exhausted(self):
        boot=importlib.import_module('formal_boot_state');read=boot._read_secure
        live=importlib.import_module('formal_live_control');actual=live.run_approved_live_control
        calls=[];reads=[];done=[]
        def finished(*args):
            result=actual(*args);calls.append(1);return result
        def expired(path,*args):
            result=read(path,*args)
            if len(calls)==2 and Path(path)==self.credential:
                reads.append(1)
                if len(reads)==4:
                    snapshot.observation_deadline.set(time.monotonic()-1);done.append(1)
            return result
        with patch.object(live,'run_approved_live_control',side_effect=finished),patch.object(boot,'_read_secure',side_effect=expired):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_DATA_DEADLINE'):self.run_client()
        self.assertEqual(done,[1])
    def test_selected_query_budget_exhausted(self):
        before=self.retained()
        def expired(*args,**kwargs):
            snapshot.observation_deadline.set(time.monotonic()-1);return {}
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_DATA_DEADLINE'):self.run_client(observer=expired)
        self.assertEqual(before,self.retained())
    def test_final_inspect_callback_changes_other_original(self):
        live=importlib.import_module('formal_live_control');real=live.run_approved_live_control;calls=[];done=[]
        def finished(*args):
            result=real(*args);calls.append(1);return result
        def inspect(_):
            if len(calls)==2 and not done:
                (self.f.claimpath/'external-callers-original.json').write_bytes(b'{}');done.append(1)
            return copy.deepcopy(self.original)
        with patch.object(live,'run_approved_live_control',side_effect=finished):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(inspect=inspect)
        self.assertEqual(done,[1])
    def test_fence_stage_refused_before_sql(self):
        c=snapshot.FormalMaintenanceClaim(self.f.claimpath,self.f.binding,resume=True)
        c.record('db-fence-intent',{});c.close();calls=[]
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_PERSISTED_STAGE_INVALID'):
            self.run_client(observer=lambda *a,**k:calls.append(1))
        self.assertEqual(calls,[])
    def test_actual_unprivileged_root_refusal(self):
        child=os.fork()
        if child==0:
            try:
                os.setgroups([]);os.setgid(65534);os.setuid(65534)
                self.m.run_approved_persisted_observation(str(self.approval),self.digest)
            except snapshot.FormalSnapshotError as e:os._exit(0 if e.code=='FORMAL_PERSISTED_ROOT_REQUIRED' else 80)
            except BaseException:os._exit(81)
            os._exit(82)
        _,status=os.waitpid(child,0);self.assertEqual(os.waitstatus_to_exitcode(status),0)


if __name__=='__main__':unittest.main()
