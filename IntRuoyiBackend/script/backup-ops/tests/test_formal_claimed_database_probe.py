"""Real Linux retained claim files, explicit SQL/runtime observation doubles."""
import hashlib,json,unittest
from datetime import datetime,timezone,timedelta
from unittest.mock import patch
from test_formal_claimed_capture import Capture

class Probe(unittest.TestCase):
 tearDown=Capture.tearDown
 prepare=Capture.prepare
 guard=Capture.guard
 def setUp(self):
  Capture.setUp(self);self.prepare();self.calls=0
  class Mysql:
   def probe_rejected_zero_row_update(inner,database,table,column):
    self.calls+=1
    sql='UPDATE `'+database+'`.`'+table+'` SET `'+column+'`=`'+column+'` WHERE 1=0;'
    return {'result':'REJECTED','mysqlErrorNumber':1290,'statementSha256':hashlib.sha256(sql.encode()).hexdigest(),'observedAt':datetime.now(timezone.utc).isoformat()}
  self.mysql=Mysql()
 def run_probe(self,phase='before'):
  with patch.object(self.m,'observe_claimed_capture_barrier',side_effect=self.guard):
   return self.m.probe_claimed_database_rejection(self.claim,self.mysql,'ruoyi','owned_table','id',phase=phase)
 def test_before_and_after_bind_actual_receipt_sha(self):
  before=self.run_probe();self.assertEqual(before['phase'],'before')
  self.claim.record('capture-intent',{})
  payload=self.claim.root/'payload';payload.mkdir();raw=b'ACTUAL_PARTIAL_PAYLOAD_RECEIPT'
  (payload/'040-payload.json').write_bytes(raw)
  self.claim.record('captured',{'payloadReceiptSha256':hashlib.sha256(raw).hexdigest()})
  after=self.run_probe('after');self.assertEqual(after['payloadReceiptSha256'],hashlib.sha256(raw).hexdigest())
  saved=(self.claim.root/'database-rejection-after.json').read_bytes()
  self.assertEqual(self.claim.events[-1]['details']['receiptSha256'],hashlib.sha256(saved).hexdigest())
  self.assertEqual(self.calls,2)
 def test_failed_probe_retains_intent_and_refuses_repeat(self):
  with patch.object(self.mysql,'probe_rejected_zero_row_update',side_effect=self.m.FormalSnapshotError('INJECTED')):
   with self.assertRaises(self.m.FormalSnapshotError):self.run_probe()
  self.assertEqual(self.claim.events[-1]['step'],'db-probe-before-intent')
  with self.assertRaises(self.m.FormalSnapshotError):self.run_probe()
  self.assertEqual(self.calls,0)
 def test_after_before_capture_refuses_without_sql(self):
  with self.assertRaises(self.m.FormalSnapshotError):self.run_probe('after')
  self.assertEqual(self.calls,0)
 def test_changed_before_receipt_refuses_after_without_sql(self):
  self.run_probe();(self.claim.root/'database-rejection-before.json').write_text('{}')
  self.claim.record('capture-intent',{});self.claim.record('captured',{'payloadReceiptSha256':'d'*64})
  with self.assertRaises(self.m.FormalSnapshotError):self.run_probe('after')
  self.assertEqual(self.calls,1)
 def test_lost_owner_after_sql_never_completes(self):
  original=self.mysql.probe_rejected_zero_row_update
  def lose(*args):
   result=original(*args);self.claim.close();return result
  with patch.object(self.mysql,'probe_rejected_zero_row_update',side_effect=lose):
   with self.assertRaises(self.m.FormalSnapshotError):self.run_probe()
  self.assertFalse((self.claim.root/'database-rejection-before.json').exists())
 def test_wrong_statement_digest_never_completes(self):
  original=self.mysql.probe_rejected_zero_row_update
  def wrong(*args):
   result=original(*args);result['statementSha256']='c'*64;return result
  with patch.object(self.mysql,'probe_rejected_zero_row_update',side_effect=wrong):
   with self.assertRaises(self.m.FormalSnapshotError):self.run_probe()
  self.assertEqual(self.claim.events[-1]['step'],'db-probe-before-intent')
  self.assertFalse((self.claim.root/'database-rejection-before.json').exists())
 def test_stale_future_naive_and_invalid_times_never_complete(self):
  original=self.mysql.probe_rejected_zero_row_update
  for stamp in ('invalid','2026-01-01T00:00:00',
    (datetime.now(timezone.utc)-timedelta(days=1)).isoformat(),
    (datetime.now(timezone.utc)+timedelta(days=1)).isoformat()):
   with self.subTest(stamp=stamp):
    # Every rejected attempt retains intent; a fresh owned test claim is required.
    if any(e['step']=='db-probe-before-intent' for e in self.claim.events):
     self.tearDown();self.setUp();original=self.mysql.probe_rejected_zero_row_update
    def wrong(*args):
     result=original(*args);result['observedAt']=stamp;return result
    with patch.object(self.mysql,'probe_rejected_zero_row_update',side_effect=wrong):
     with self.assertRaises(self.m.FormalSnapshotError):self.run_probe()
    self.assertFalse((self.claim.root/'database-rejection-before.json').exists())

if __name__=='__main__':unittest.main()
