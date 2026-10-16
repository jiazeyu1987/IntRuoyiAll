"""Claimed capture boundary tests: real private files, explicit SQL/Docker doubles."""
import hashlib,importlib.util,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'
class Capture(unittest.TestCase):
 def setUp(self):
  spec=importlib.util.spec_from_file_location('capture',SOURCE);self.m=importlib.util.module_from_spec(spec);spec.loader.exec_module(self.m)
  self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name);self.source=self.root/'source';self.source.mkdir()
  (self.source/'xl.meta').write_bytes(b'ACTUAL_PRIVATE_FILE')
  self.claim=self.m.FormalMaintenanceClaim(self.root/'claim',{'operationId':'owned-capture','sourceFingerprint':'a'*64,
    'databaseUuid':'0fa6c22b-5a90-11f1-ade1-0242ac150002','minioContainerId':'b'*64})
  self.mysql=object();self.guards=[]
 def tearDown(self):self.claim.close();self.temp.cleanup()
 def prepare(self):
  for step in ('db-fence-intent','db-fenced','backend-stop-intent','backend-stopped','minio-stop-intent','minio-stopped'):
   self.claim.record(step,{})
 def guard(self,claim,mysql,*,dump_in_progress=False):
  self.assertIs(type(dump_in_progress),bool);self.guards.append('observed');claim.assert_owned()
 def fake_capture(self,mysql,database,source,destination,identity,*,barrier_guard,native_guard):
  self.assertTrue(callable(native_guard));native_guard()
  barrier_guard();Path(destination).mkdir();(Path(destination)/'040-payload.json').write_bytes(b'ACTUAL_PARTIAL_RECEIPT')
  barrier_guard();return {'state':'CAPTURED_PENDING_FULL_BARRIER_PROOF','hostWritersVerified':False}
 def test_capture_binds_claim_payload_and_forbids_repeat(self):
  self.prepare()
  with patch.object(self.m,'observe_claimed_capture_barrier',side_effect=self.guard),patch.object(self.m,'capture_formal_payload',side_effect=self.fake_capture):
   result=self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source)
   self.assertFalse(result['hostWritersVerified']);self.assertGreaterEqual(len(self.guards),3)
   event=self.claim.events[-1];self.assertEqual(event['step'],'captured')
   raw=(self.claim.root/'payload/040-payload.json').read_bytes()
   self.assertEqual(event['details']['payloadReceiptSha256'],hashlib.sha256(raw).hexdigest())
   with self.assertRaises(self.m.FormalSnapshotError):self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source)
 def test_missing_stop_gate_refuses_before_capture(self):
  with patch.object(self.m,'capture_formal_payload') as capture:
   with self.assertRaises(self.m.FormalSnapshotError):self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source)
   capture.assert_not_called()
  self.assertFalse((self.claim.root/'payload').exists())
 def test_failed_capture_retains_intent_without_completion(self):
  self.prepare()
  with patch.object(self.m,'observe_claimed_capture_barrier',side_effect=self.guard),patch.object(self.m,'capture_formal_payload',side_effect=self.m.FormalSnapshotError('INJECTED')):
   with self.assertRaises(self.m.FormalSnapshotError):self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source)
  self.assertTrue(any(e['step']=='capture-intent' for e in self.claim.events));self.assertFalse(any(e['step']=='captured' for e in self.claim.events))
 def test_restore_started_refuses_new_capture(self):
  self.prepare();self.claim.record('minio-restore-intent',{})
  with patch.object(self.m,'capture_formal_payload') as capture:
   with self.assertRaises(self.m.FormalSnapshotError):self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source)
   capture.assert_not_called()
 def test_changed_claim_during_capture_refuses_completion(self):
  self.prepare()
  def changed(*args,**kwargs):
   result=self.fake_capture(*args,**kwargs);self.claim.close();return result
  with patch.object(self.m,'observe_claimed_capture_barrier',side_effect=self.guard),patch.object(self.m,'capture_formal_payload',side_effect=changed):
   with self.assertRaises(self.m.FormalSnapshotError):self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source)
  self.assertFalse(any(e['step']=='captured' for e in self.claim.events))
 def test_explicit_native_scope_binds_operation_and_dispatches_existing_claim(self):
  self.prepare()
  fence={'mount':str(self.root),'binding':{'operationId':'owned-capture','bootId':'owned-boot','mount':{}},'seconds':3}
  seen=[]
  def capture(*args,**kwargs):
   self.assertEqual(kwargs.pop('filesystem_fence'),fence);seen.append('native-scope')
   return self.fake_capture(*args,**kwargs)
  with patch.object(self.m,'observe_claimed_capture_barrier',side_effect=self.guard),patch.object(self.m,'capture_formal_payload',side_effect=capture),patch.object(self.m,'_require_native_physical_runtime',return_value=None):
   self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source,filesystem_fence=fence)
  self.assertEqual(seen,['native-scope'])
 def test_unsupported_native_runtime_refuses_before_claim_intent(self):
  self.prepare();before=list(self.claim.events)
  fence={'mount':str(self.root),'binding':{'operationId':'owned-capture'},'seconds':3}
  with patch.object(self.m,'_require_native_physical_runtime',side_effect=self.m.FormalSnapshotError('WATCHDOG_RUNTIME_UNSUPPORTED')),patch.object(self.m,'capture_formal_payload') as capture,patch.object(self.m,'observe_claimed_capture_barrier') as observe:
   with self.assertRaises(self.m.FormalSnapshotError) as error:
    self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source,filesystem_fence=fence)
   self.assertEqual(error.exception.code,'WATCHDOG_RUNTIME_UNSUPPORTED')
   capture.assert_not_called();observe.assert_not_called()
  self.assertEqual(before,self.claim.events);self.assertFalse((self.claim.root/'payload').exists())
 def test_native_scope_other_operation_refuses_before_intent(self):
  self.prepare()
  fence={'mount':str(self.root),'binding':{'operationId':'other-operation','bootId':'owned-boot','mount':{}},'seconds':3}
  with patch.object(self.m,'capture_formal_payload') as capture:
   with self.assertRaises(self.m.FormalSnapshotError) as error:
    self.m.capture_claimed_formal_payload(self.claim,self.mysql,'ruoyi',self.source,filesystem_fence=fence)
   self.assertEqual(error.exception.code,'FORMAL_CAPTURE_FILESYSTEM_OPERATION_CHANGED')
   capture.assert_not_called()
  self.assertFalse(any(e['step']=='capture-intent' for e in self.claim.events))
if __name__=='__main__':unittest.main()
