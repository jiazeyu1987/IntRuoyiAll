"""Real Linux ownership/files with explicit Docker and SQL doubles."""
import copy,importlib.util,tempfile,unittest,subprocess
from types import SimpleNamespace
from pathlib import Path
from unittest.mock import patch
SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'
BINDING={'operationId':'owned-container-001','sourceFingerprint':'a'*64,
 'databaseUuid':'0fa6c22b-5a90-11f1-ade1-0242ac150002','minioContainerId':'b'*64}
def record(identity,name):
 return {'id':identity,'name':'/'+name,'image':'sha256:'+'d'*64,'mounts':[],
  'restartPolicy':{'Name':'no','MaximumRetryCount':0},
  'state':{'Running':True,'Paused':False,'Restarting':False,'Dead':False,'OOMKilled':False,
   'ExitCode':0,'StartedAt':'2026-10-01T00:00:00Z','FinishedAt':'0001-01-01T00:00:00Z'}}
class Containers(unittest.TestCase):
 def setUp(self):
  spec=importlib.util.spec_from_file_location('containers',SOURCE);self.m=importlib.util.module_from_spec(spec);spec.loader.exec_module(self.m)
  self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)/'claim'
  self.claim=self.m.FormalMaintenanceClaim(self.root,BINDING)
  self.claim.record('db-fence-intent',{});self.claim.record('db-fenced',{})
  self.rows={'c'*64:record('c'*64,'owned-backend'),'b'*64:record('b'*64,'owned-minio')};self.calls=[]
  self.inspect=patch.object(self.m,'inspect_formal_container',side_effect=lambda i:copy.deepcopy(self.rows[i]));self.inspect.start()
  self.action=patch.object(self.m,'run_formal_container_action',side_effect=self.mutate);self.action.start()
 def tearDown(self):self.action.stop();self.inspect.stop();self.claim.close();self.temp.cleanup()
 def bind(self):self.m.bind_claimed_container_scope(self.claim,'c'*64,'owned-backend','owned-minio')
 def mutate(self,action,identity):
  self.calls.append((action,identity));state=self.rows[identity]['state']
  state['Running']=action=='start'
  if action=='stop':state['FinishedAt']='2026-10-02T00:00:00Z'
  else:state['StartedAt']='2026-10-02T00:01:00Z'
 def test_sequential_stop_restore_same_containers(self):
  self.bind();self.m.stop_claimed_container(self.claim,'backend');self.m.stop_claimed_container(self.claim,'minio')
  self.m.restore_claimed_container(self.claim,'minio');self.claim.record('db-restore-intent',{});self.claim.record('db-restored',{})
  self.m.restore_claimed_container(self.claim,'backend')
  self.assertEqual(self.calls,[('stop','c'*64),('stop','b'*64),('start','b'*64),('start','c'*64)])
 def test_minio_cannot_stop_before_backend(self):
  self.bind()
  with self.assertRaises(self.m.FormalSnapshotError):self.m.stop_claimed_container(self.claim,'minio')
  self.assertEqual(self.calls,[])
 def test_changed_image_refused_before_stop(self):
  self.bind();self.rows['c'*64]['image']='sha256:'+'e'*64
  with self.assertRaises(self.m.FormalSnapshotError):self.m.stop_claimed_container(self.claim,'backend')
  self.assertEqual(self.calls,[])
 def test_interrupted_stop_reconciles_without_repeat(self):
  self.bind()
  def interrupted(action,identity):self.mutate(action,identity);raise RuntimeError('INTERRUPTED')
  with patch.object(self.m,'run_formal_container_action',side_effect=interrupted):
   with self.assertRaises(RuntimeError):self.m.stop_claimed_container(self.claim,'backend')
  with self.assertRaises(self.m.FormalSnapshotError):self.m.stop_claimed_container(self.claim,'backend')
  self.m.reconcile_claimed_container(self.claim,'backend','stop');self.assertEqual(len(self.calls),1)
 def test_nonzero_exit_refuses_stop_completion(self):
  self.bind()
  def failed(action,identity):self.mutate(action,identity);self.rows[identity]['state']['ExitCode']=137
  with patch.object(self.m,'run_formal_container_action',side_effect=failed):
   with self.assertRaises(self.m.FormalSnapshotError):self.m.stop_claimed_container(self.claim,'backend')
  self.assertFalse(any(e['step']=='backend-stopped' for e in self.claim.events))
 def test_backend_restore_requires_db_restore(self):
  self.bind();self.m.stop_claimed_container(self.claim,'backend');self.m.stop_claimed_container(self.claim,'minio');self.m.restore_claimed_container(self.claim,'minio')
  with self.assertRaises(self.m.FormalSnapshotError):self.m.restore_claimed_container(self.claim,'backend')
  self.assertEqual(len(self.calls),3)
 def test_interrupted_start_reconciles_without_repeat(self):
  self.bind();self.m.stop_claimed_container(self.claim,'backend');self.m.stop_claimed_container(self.claim,'minio')
  def interrupted(action,identity):self.mutate(action,identity);raise RuntimeError('INTERRUPTED')
  with patch.object(self.m,'run_formal_container_action',side_effect=interrupted):
   with self.assertRaises(RuntimeError):self.m.restore_claimed_container(self.claim,'minio')
  with self.assertRaises(self.m.FormalSnapshotError):self.m.restore_claimed_container(self.claim,'minio')
  self.m.reconcile_claimed_container(self.claim,'minio','start');self.assertEqual(len(self.calls),3)
class ControlledContainerAction(unittest.TestCase):
 """Actual action adapter with explicit subprocess seam; no live claim proof."""
 def setUp(self):
  spec=importlib.util.spec_from_file_location('controlled_actions',SOURCE)
  self.m=importlib.util.module_from_spec(spec);spec.loader.exec_module(self.m)
 def test_stop_has_explicit_term_no_daemon_kill_and_bounded_client(self):
  identity='c'*64
  with patch.object(self.m.subprocess,'run',return_value=SimpleNamespace(returncode=0,stdout=(identity+'\n').encode())) as run:
   self.m.run_formal_container_action('stop',identity)
  args,kwargs=run.call_args
  self.assertEqual(args[0],['docker','stop','--signal','SIGTERM','--timeout','-1',identity])
  self.assertGreater(kwargs['timeout'],0);self.assertLessEqual(kwargs['timeout'],90)
  self.assertEqual(run.call_count,1)
 def test_start_is_exact_original_container_without_changed_options(self):
  identity='c'*64
  with patch.object(self.m.subprocess,'run',return_value=SimpleNamespace(returncode=0,stdout=(identity+'\n').encode())) as run:
   self.m.run_formal_container_action('start',identity)
  self.assertEqual(run.call_args.args[0],['docker','start',identity]);self.assertEqual(run.call_count,1)
 def test_timeout_retains_failure_without_retry_or_start(self):
  identity='c'*64
  with patch.object(self.m.subprocess,'run',side_effect=subprocess.TimeoutExpired('docker',90)) as run:
   with self.assertRaisesRegex(self.m.FormalSnapshotError,'FORMAL_CONTAINER_ACTION_FAILED'):
    self.m.run_formal_container_action('stop',identity)
  self.assertEqual(run.call_count,1)
 def test_expired_outer_budget_refuses_before_any_command(self):
  token=self.m.observation_deadline.set(0)
  try:
   with patch.object(self.m.subprocess,'run') as run:
    with self.assertRaises(self.m.FormalSnapshotError):self.m.run_formal_container_action('stop','c'*64)
    run.assert_not_called()
  finally:self.m.observation_deadline.reset(token)
 def test_wrong_response_or_nonzero_result_is_not_stop_success(self):
  for result in (SimpleNamespace(returncode=0,stdout=b'other\n'),SimpleNamespace(returncode=1,stdout=('c'*64+'\n').encode())):
   with self.subTest(result=result),patch.object(self.m.subprocess,'run',return_value=result) as run:
    with self.assertRaisesRegex(self.m.FormalSnapshotError,'FORMAL_CONTAINER_ACTION_FAILED'):
     self.m.run_formal_container_action('stop','c'*64)
    self.assertEqual(run.call_count,1)
if __name__=='__main__':unittest.main()
