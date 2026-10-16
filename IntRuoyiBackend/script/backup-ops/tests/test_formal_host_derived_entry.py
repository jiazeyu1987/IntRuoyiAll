"""Normal CLI and actual Linux claim/derived reader; systemd/proc fixtures only."""
import copy
import hashlib
import json
import time
import unittest
from unittest.mock import patch

from test_formal_host_admission import DerivedClaimWiring
import formal_capture_producer as producer
import formal_snapshot_linux as snapshot


class NormalDerivedEntry(unittest.TestCase):
    def setUp(self):
        self.fixture=DerivedClaimWiring();self.fixture.setUp()
        self.addCleanup(self.fixture.tearDown)
        self.claim=self.fixture.claim
        self.request=dict(schemaVersion='release-formal-capture-request/v13',
            action='bind-host-derived-originals',claimRoot=str(self.claim.root),
            claimBinding=json.loads(self.claim._plan),totalSeconds=60,
            derivedBinding=copy.deepcopy(self.fixture.related.binding))
        self.path=self.fixture.host.root/'request.json'
    def write(self):
        raw=snapshot._canonical_bytes(self.request)
        self.path.write_bytes(raw);self.path.chmod(0o600)
        return hashlib.sha256(raw).hexdigest()
    def invoke(self,**options):
        sha=self.write()
        self.assertTrue(callable(getattr(producer,'run_approved_host_derived_binding',None)))
        with patch.object(self.fixture.m,'NativeHostAdmission',return_value=self.fixture.native):
            return getattr(producer,'run_approved_host_derived_binding')(str(self.path),sha,**options)
    def test_normal_cli_seals_same_claim_without_service_actions(self):
        sha=self.write();self.claim.close()
        with patch.object(self.fixture.m,'NativeHostAdmission',return_value=self.fixture.native):
            code=producer.main(['--request',str(self.path),'--expected-request-sha256',sha,'--bind-host-derived-originals'])
        self.assertEqual(code,0,'normal producer lacks approved pre-close derived binding route')
        self.assertEqual(self.fixture.host.calls,[])
        self.assertTrue((self.claim.root/'host-derived-originals.json').is_file())
    def test_explicit_cli_routes_before_generic_read_and_budget_reset(self):
        sha=self.write();self.claim.close()
        args=['--request',str(self.path),'--expected-request-sha256',sha,'--bind-host-derived-originals']
        with patch.object(self.fixture.m,'NativeHostAdmission',return_value=self.fixture.native),\
                patch.object(producer,'read_private',side_effect=AssertionError('generic read precedes native secure approval')):
            try:code=producer.main(args)
            except SystemExit:self.fail('explicit native binding route is missing')
        self.assertEqual(code,0)
    def test_explicit_cli_cannot_mix_owner_or_observer(self):
        sha=self.write()
        calls=[]
        args=['--request',str(self.path),'--expected-request-sha256',sha,'--bind-host-derived-originals']
        with patch.object(producer,'run_approved_host_derived_binding',side_effect=lambda *a:calls.append(a)):
            for conflict in ['--run-original-lifecycle-owner','--observe-boot-state','--observe-original-launch']:
                try:code=producer.main(args+[conflict])
                except SystemExit:self.fail('explicit route grammar is missing')
                self.assertEqual(code,1)
        self.assertEqual(calls,[])
    def test_normal_entry_missing_source_refuses_before_claim(self):
        self.request.pop('derivedBinding');sha=self.write()
        self.assertTrue(callable(getattr(producer,'run_approved_host_derived_binding',None)))
        with patch.object(producer,'FormalMaintenanceClaim') as acquire:
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'HOST_DERIVED_REQUEST_INVALID'):
                getattr(producer,'run_approved_host_derived_binding')(str(self.path),sha)
        acquire.assert_not_called()
    def test_normal_entry_wrong_sha_refuses_before_claim(self):
        self.write()
        self.assertTrue(callable(getattr(producer,'run_approved_host_derived_binding',None)))
        with patch.object(producer,'FormalMaintenanceClaim') as acquire:
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'HOST_DERIVED_REQUEST_CHANGED'):
                getattr(producer,'run_approved_host_derived_binding')(str(self.path),'0'*64)
        acquire.assert_not_called()
    def test_normal_entry_late_binding_retains_without_source_write(self):
        self.claim.record('host-crond-mask-intent',{'originalSha256':'a'*64})
        self.claim.close()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_BINDING_TOO_LATE'):self.invoke()
        self.assertFalse((self.claim.root/'host-derived-originals.json').exists())
        self.assertEqual(self.fixture.host.calls,[])
    def test_equal_bytes_request_inode_replacement_refuses_before_seal(self):
        self.claim.close();observe=self.fixture.native.observe_related_work
        def drift(*args,**kwargs):
            result=observe(*args,**kwargs)
            raw=self.path.read_bytes();self.path.rename(self.path.with_suffix('.old'))
            self.path.write_bytes(raw);self.path.chmod(0o600)
            kwargs['guard']()
            return result
        self.fixture.native.observe_related_work=drift
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'HOST_DERIVED_REQUEST_CHANGED'):self.invoke()
        self.assertFalse((self.claim.root/'host-derived-originals.json').exists())
    def test_actual_observer_return_drift_cannot_publish_completion(self):
        self.claim.close();observe=self.fixture.native.observe_related_work
        def drift(*args,**kwargs):
            result=observe(*args,**kwargs)
            raw=self.path.read_bytes();self.path.rename(self.path.with_suffix('.old'))
            self.path.write_bytes(raw);self.path.chmod(0o600)
            # No synthetic guard: actual reader's last owner callback ended.
            return result
        self.fixture.native.observe_related_work=drift
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'HOST_DERIVED_REQUEST_CHANGED'):self.invoke()
        self.assertFalse((self.claim.root/'host-derived-originals.json').exists())
        self.assertFalse(any(json.loads(p.read_bytes())['step']=='host-derived-originals-bound'
            for p in self.claim.root.glob('event-*.json')))
    def test_native_tail_request_replacement_cannot_publish_completion(self):
        self.claim.close();observe=self.fixture.native.observe_related_work_native
        def drift(*args,**kwargs):
            result=observe(*args,**kwargs)
            raw=self.path.read_bytes();self.path.rename(self.path.with_suffix('.old'))
            self.path.write_bytes(raw);self.path.chmod(0o600)
            return result
        self.fixture.native.observe_related_work_native=drift
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'HOST_DERIVED_REQUEST_CHANGED'):self.invoke()
        self.assertFalse(any(json.loads(p.read_bytes())['step']=='host-derived-originals-bound'
            for p in self.claim.root.glob('event-*.json')))
    def test_missing_actual_known_pid_is_not_bound_as_empty(self):
        self.fixture.related.member();self.claim.close()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_KNOWN_SET_CHANGED'):self.invoke()
        self.assertFalse((self.claim.root/'host-derived-originals.json').exists())
    def test_request_preparation_counts_in_total_budget(self):
        self.claim.close();read=self.fixture.m.boot._read_secure;clock=time.monotonic();expired=[]
        def slow(*args,**kwargs):
            pair=read(*args,**kwargs)
            if args[0]==self.path:expired.append(True)
            return pair
        with patch.object(self.fixture.m.boot,'_read_secure',side_effect=slow),\
                patch.object(producer.time,'monotonic',side_effect=lambda:clock+61 if expired else clock),\
                patch.object(producer,'FormalMaintenanceClaim') as acquire:
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'HOST_DERIVED_DEADLINE'):
                self.invoke()
        acquire.assert_not_called()
    def test_owner_actual_sealed_binding_accepts_completed_preparation(self):
        import inspect
        self.assertIn('deadline',inspect.signature(producer._original_lifecycle_pre_capture_stage).parameters)
        self.fixture.bind();self.fixture.host.close_all()
        self.claim.record('db-fence-intent',{'originalSha256':'a'*64})
        self.claim.record('db-fenced',{'originalSha256':'a'*64})
        with patch.object(self.fixture.m,'NativeHostAdmission',return_value=self.fixture.native):
            checks=producer._original_lifecycle_pre_capture_stage(self.claim,deadline=time.monotonic()+60)
        self.assertIsInstance(checks,self.fixture.m.ClosedHostAdmissionChecks)
    def test_owner_same_bytes_source_replacement_refuses_before_capture(self):
        import inspect
        self.assertIn('deadline',inspect.signature(producer._original_lifecycle_pre_capture_stage).parameters)
        self.fixture.bind();self.fixture.host.close_all()
        self.claim.record('db-fence-intent',{'originalSha256':'a'*64})
        self.claim.record('db-fenced',{'originalSha256':'a'*64})
        path=self.claim.root/'host-derived-originals.json';raw=path.read_bytes()
        path.rename(path.with_suffix('.old'));path.write_bytes(raw);path.chmod(0o600)
        with patch.object(self.fixture.m,'NativeHostAdmission',return_value=self.fixture.native):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_ORIGINAL_BINDING_CHANGED'):
                producer._original_lifecycle_pre_capture_stage(self.claim,deadline=time.monotonic()+60)


if __name__=='__main__':unittest.main()
