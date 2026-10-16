"""Actual private Linux claim/FD tests; not real backup/NAS acceptance."""
import hashlib
import inspect
import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import time
import unittest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import formal_snapshot_linux as snapshot
import formal_capture_budget as budgets

class NormalOwnerTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(prefix='normal-interface-',dir='/root')
        self.addCleanup(self.tmp.cleanup);self.parent=Path(self.tmp.name)
        self.claim=snapshot.FormalMaintenanceClaim(self.parent/'claim',dict(operationId='owned-normal',sourceFingerprint='1'*64,
            databaseUuid='11111111-1111-1111-1111-111111111111',minioContainerId='2'*64))
        self.addCleanup(self.claim.close)
    def budget(self):
        policy=dict(schemaVersion='release-formal-capture-output-policy/v1',componentBytes={k:1048576 for k in budgets.COMPONENTS},
            totalBytes=9437184,maxFiles=1100,maxDirectories=100,maxPathBytes=1024)
        raw=snapshot._canonical_bytes(policy)
        return budgets.CaptureBudget(self.claim,raw,hashlib.sha256(raw).hexdigest(),attempt_id='a'*64,
            source_context_sha256='b'*64,guard=self.claim.assert_owned,deadline=time.monotonic()+15)
    def test_contract_signatures(self):
        import formal_capture_producer as module
        names={'run_production_a_normal_owner':('inputs','control','guard','deadline'),
            'prepare_production_a_capture_intent':('prepared','policy_raw','policy_sha256','source_context_raw','allocation_sources','guard','deadline'),
            'verify_production_a_precapture_admission':('intent_raw','nas_admission_raw','admission_transport','local_admission','guard','deadline')}
        for name,expected in names.items():
            sig=inspect.signature(getattr(module,name));self.assertEqual(tuple(sig.parameters),('claim',)+expected)
            for key in expected:self.assertEqual(sig.parameters[key].kind,inspect.Parameter.KEYWORD_ONLY)
    def test_additional_native_root_is_bounded_without_touching_payload(self):
        budget=self.budget();control=self.parent/'control';control.mkdir(mode=0o700)
        budget.register_original_root(control,guard=self.claim.assert_owned)
        with budget.open(control/'manifest','control') as out:out.write(b'bounded')
        self.assertEqual((control/'manifest').read_bytes(),b'bounded')
        self.assertFalse((self.claim.root/'manifest').exists())
        self.assertEqual(budget.receipt()['writtenBytes'],7)
    def test_complete_admission_rejects_caller_true(self):
        budget=self.budget()
        with self.assertRaises(snapshot.FormalSnapshotError):budget.bind_full_capture_admission(True)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'ADMISSION_REQUIRED'):budget.require_full_capture_admission()
    def test_original_owner_identity_survives_legal_journal_growth(self):
        import formal_normal_owner as module
        held=module.OriginalOwner(self.claim,guard=self.claim.assert_owned,deadline=time.monotonic()+10)
        self.claim.record('host-originals-bound',{'value':1});held.check()
        errors=[]
        def foreign():
            try:held.check()
            except snapshot.FormalSnapshotError as e:errors.append(e.code)
        thread=threading.Thread(target=foreign);thread.start();thread.join()
        self.assertEqual(errors,['BACKUP_INTERFACE_AUTHOR_LOST'])
    def test_duplicate_json_and_boolean_budget_reject(self):
        import formal_normal_owner as module
        for raw in (b'{"x":1,"x":2}',b'{"x":NaN}',b'\xef\xbb\xbf{}'):
            with self.assertRaises(snapshot.FormalSnapshotError):module.strict_document(raw)
        with self.assertRaises(snapshot.FormalSnapshotError):module.remaining_deadline(True,time.monotonic(),time.monotonic(),time.monotonic()+10)

    def test_allocation_formula_includes_rounding_metadata_and_failed_peak(self):
        import formal_normal_owner as module
        rule=dict(allocationUnitBytes=4096,fileMetadataUpperBytes=8192,directoryMetadataUpperBytes=4096,
            pathByteUpperBytes=2,transactionPeakBytes=32768)
        # Coexisting/retained files are counted, not only final payload bytes.
        self.assertEqual(module.allocated_peak(9000,3,2,100,rule),9000+3*4095+3*8192+2*4096+100*2+32768)
        with self.assertRaises(snapshot.FormalSnapshotError):module.allocated_peak(9000,True,2,100,rule)

    def test_pipe_sequence_large_fixed_manifest_and_permanent_switch(self):
        import formal_normal_owner as module
        owner=module.OriginalOwner(self.claim,guard=self.claim.assert_owned,deadline=time.monotonic()+10)
        control=self.parent/'control';control.mkdir(mode=0o700);budget=self.budget()
        budget.register_original_root(control,guard=self.claim.assert_owned)
        entries={'x'+str(i):'file|1|'+'f'*64 for i in range(1000)}
        manifest=module.FixedManifest(owner,entries,budget,control/'manifest')
        first=manifest.chunk(0);self.assertEqual(len(__import__('base64').b64decode(first['bytesBase64'])),32768)
        with self.assertRaises(snapshot.FormalSnapshotError):manifest.chunk(0)
        offset=32768
        while not manifest.finished:
            chunk=manifest.chunk(offset);offset+=len(__import__('base64').b64decode(chunk['bytesBase64']))
        self.assertEqual(offset,manifest.reference['bytes']);self.assertIs(manifest.entries,entries)
        entries['replacement']='directory'
        with self.assertRaises(snapshot.FormalSnapshotError):manifest.check()

    def test_request_dispatch_rejects_sequence_replay_before_effects(self):
        import formal_normal_owner as module
        b={k:'a'*64 for k in module.BINDING_FIELDS};b['releaseTag']='normal'
        gate=module.ControlSequence(b);ref={'path':'/actual/ref','bytes':1,'sha256':'c'*64}
        request=dict(schemaVersion=module.CONTROL_SCHEMA,binding=b,sequence=1,action='prepare',payloadReference=ref,
            budget=dict(remainingMilliseconds=1000,observationReference=ref))
        gate.accept(module.canonical(request),'BOUND_READONLY')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'SEQUENCE_INVALID'):gate.accept(module.canonical(request),'PREPARED')

    def test_bounded_pipe_io_uses_original_fds_and_refuses_bulk_input(self):
        import formal_normal_owner as module
        owner=module.OriginalOwner(self.claim,guard=self.claim.assert_owned,deadline=time.monotonic()+5)
        read,write=os.pipe();reply_read,reply_write=os.pipe()
        for fd in (read,write,reply_read,reply_write):self.addCleanup(os.close,fd)
        io=module.NativeControl(read,reply_write,owner)
        os.write(write,b'{"actual":true}\n')
        self.assertEqual(io.receive(),b'{"actual":true}')
        io.send({'actual':True});self.assertEqual(json.loads(os.read(reply_read,100)),{'actual':True})
        worker=threading.Thread(target=lambda:os.write(write,b'x'*8193),daemon=True);worker.start()
        with self.assertRaises(snapshot.FormalSnapshotError):io.receive()
        worker.join(timeout=1)

    def test_unknown_allocation_cannot_author_intent(self):
        import formal_normal_owner as module
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'NATIVE_PREREQUISITE_REQUIRED'):
            module.prepare_intent(self.claim,prepared=None,policy_raw=b'{}',policy_sha256='a'*64,
                source_context_raw=b'{}',allocation_sources={'allocationBoundVerified':True},guard=self.claim.assert_owned,
                deadline=time.monotonic()+5)

if __name__=='__main__':unittest.main(verbosity=2)
