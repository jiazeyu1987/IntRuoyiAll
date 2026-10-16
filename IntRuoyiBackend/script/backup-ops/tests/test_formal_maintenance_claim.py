"""Real Linux flock/file journals; standard-library test runner, no substitutes."""
import importlib.util
import json
import os
import subprocess
import sys
from pathlib import Path
import tempfile
import unittest

SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'
BINDING={'operationId':'formal-owned-001','sourceFingerprint':'a'*64,
         'databaseUuid':'0fa6c22b-5a90-11f1-ade1-0242ac150002','minioContainerId':'b'*64}

class MysqlDouble:
    """Explicit SQL double; these tests do not claim a real database fence."""
    def __init__(self,read_only=0):self.ro=read_only;self.super_ro=0;self.sets=[];self.fail_after_set=False
    def query(self,sql):
        if sql.startswith('SET GLOBAL'):
            self.sets.append(sql)
            if 'super_read_only=ON' in sql:self.ro=1;self.super_ro=1
            else:self.super_ro=0;self.ro=int('read_only=ON' in sql)
            if self.fail_after_set:raise RuntimeError('INJECTED_INTERRUPTION_AFTER_REAL_EFFECT')
            return ''
        if '@@server_uuid' in sql:return BINDING['databaseUuid']+'\t'+str(self.ro)+'\t'+str(self.super_ro)+'\n'
        return str(self.ro)+'\t'+str(self.super_ro)+'\t0\n'

class Claims(unittest.TestCase):
    def setUp(self):
        spec=importlib.util.spec_from_file_location('formal_claim',SOURCE)
        self.m=importlib.util.module_from_spec(spec);spec.loader.exec_module(self.m)
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)/'claim'
    def tearDown(self):self.temp.cleanup()
    def test_exclusive_claim_and_explicit_resume_preserve_chain(self):
        claim=self.m.FormalMaintenanceClaim(self.root,BINDING)
        claim.record('db-fence-intent',{'originalReadOnly':0})
        with self.assertRaises(self.m.FormalSnapshotError) as error:
            self.m.FormalMaintenanceClaim(self.root,BINDING,resume=True)
        self.assertEqual(error.exception.code,'FORMAL_MAINTENANCE_BUSY')
        claim.close()
        resumed=self.m.FormalMaintenanceClaim(self.root,BINDING,resume=True)
        self.assertEqual(resumed.events[-1]['step'],'db-fence-intent')
        self.assertFalse(resumed.restorationAutomaticallyPerformed)
        resumed.record('db-fenced',{'readOnly':1,'superReadOnly':1})
        resumed.close()
        self.assertEqual(len(list(self.root.glob('event-*.json'))),2)
    def test_existing_requires_explicit_resume(self):
        c=self.m.FormalMaintenanceClaim(self.root,BINDING);c.close()
        with self.assertRaises(self.m.FormalSnapshotError):self.m.FormalMaintenanceClaim(self.root,BINDING)
    def test_changed_binding_refused(self):
        c=self.m.FormalMaintenanceClaim(self.root,BINDING);c.close()
        changed=dict(BINDING);changed['operationId']='other'
        with self.assertRaises(self.m.FormalSnapshotError):self.m.FormalMaintenanceClaim(self.root,changed,resume=True)
    def test_changed_event_refused_on_resume(self):
        c=self.m.FormalMaintenanceClaim(self.root,BINDING);c.record('db-fence-intent',{});c.close()
        p=self.root/'event-000001.json';value=json.loads(p.read_text());value['previousDigest']='c'*64
        p.write_text(json.dumps(value))
        with self.assertRaises(self.m.FormalSnapshotError):self.m.FormalMaintenanceClaim(self.root,BINDING,resume=True)
    def test_replaced_lock_rejected_before_record(self):
        c=self.m.FormalMaintenanceClaim(self.root,BINDING)
        p=self.root/'ownership.lock';p.rename(self.root/'displaced-lock');p.write_bytes(b'')
        with self.assertRaises(self.m.FormalSnapshotError):c.record('db-fence-intent',{})
        c.close();self.assertFalse(list(self.root.glob('event-*.json')))
    def test_repeat_step_refused_without_overwrite(self):
        c=self.m.FormalMaintenanceClaim(self.root,BINDING);c.record('db-fence-intent',{})
        saved=(self.root/'event-000001.json').read_bytes()
        with self.assertRaises(self.m.FormalSnapshotError):c.record('db-fence-intent',{})
        c.close();self.assertEqual((self.root/'event-000001.json').read_bytes(),saved)
    def test_invalid_binding_creates_no_directory(self):
        changed=dict(BINDING);changed['sourceFingerprint']='invalid'
        with self.assertRaises(self.m.FormalSnapshotError):self.m.FormalMaintenanceClaim(self.root,changed)
        self.assertFalse(self.root.exists())
    def test_fork_cannot_record_as_parent_owner(self):
        c=self.m.FormalMaintenanceClaim(self.root,BINDING)
        child=os.fork()
        if child==0:
            try:c.record('db-fence-intent',{})
            except self.m.FormalSnapshotError:os._exit(0)
            os._exit(2)
        _,status=os.waitpid(child,0);c.close()
        self.assertEqual(os.waitstatus_to_exitcode(status),0)
        self.assertFalse(list(self.root.glob('event-*.json')))
    def test_killed_process_releases_lock_but_retains_intent(self):
        code='''import importlib.util,json,sys
from pathlib import Path
spec=importlib.util.spec_from_file_location('claim',sys.argv[1])
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
c=m.FormalMaintenanceClaim(Path(sys.argv[2]),json.loads(sys.argv[3]))
c.record('db-fence-intent',{'originalReadOnly':0})
print('INTENT_DURABLE',flush=True)
sys.stdin.read()
'''
        child=subprocess.Popen([sys.executable,'-I','-S','-c',code,str(SOURCE),str(self.root),json.dumps(BINDING)],
                               stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        try:
            self.assertEqual(child.stdout.readline(),b'INTENT_DURABLE\n')
            child.kill();child.wait(timeout=5)
            resumed=self.m.FormalMaintenanceClaim(self.root,BINDING,resume=True)
            self.assertEqual(resumed.events[0]['step'],'db-fence-intent')
            self.assertFalse(resumed.restorationAutomaticallyPerformed);resumed.close()
        finally:
            if child.poll() is None:child.kill();child.wait(timeout=5)
            child.stdin.close();child.stdout.close();child.stderr.close()
    def test_claimed_db_acquire_resume_restore_original_read_only(self):
        for original in (0,1):
            root=self.root/str(original);self.root.mkdir(exist_ok=True)
            mysql=MysqlDouble(original);claim=self.m.FormalMaintenanceClaim(root,BINDING)
            self.m.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
            self.assertEqual((mysql.ro,mysql.super_ro),(1,1));claim.close()
            resumed=self.m.FormalMaintenanceClaim(root,BINDING,resume=True)
            self.m.restore_claimed_database_fence(resumed,mysql)
            self.assertEqual((mysql.ro,mysql.super_ro),(original,0))
            self.assertEqual(resumed.events[-1]['step'],'db-restored');resumed.close()
    def test_interrupted_acquisition_requires_readonly_reconciliation(self):
        mysql=MysqlDouble();mysql.fail_after_set=True
        claim=self.m.FormalMaintenanceClaim(self.root,BINDING)
        with self.assertRaises(RuntimeError):self.m.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
        claim.close();mysql.fail_after_set=False;before=len(mysql.sets)
        claim=self.m.FormalMaintenanceClaim(self.root,BINDING,resume=True)
        with self.assertRaises(self.m.FormalSnapshotError):self.m.restore_claimed_database_fence(claim,mysql)
        with self.assertRaises(self.m.FormalSnapshotError):self.m.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
        self.assertEqual(len(mysql.sets),before)
        self.m.reconcile_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
        self.assertEqual(len(mysql.sets),before)
        self.m.restore_claimed_database_fence(claim,mysql);claim.close()
        self.assertEqual((mysql.ro,mysql.super_ro),(0,0))
    def test_interrupted_restore_requires_readonly_reconciliation(self):
        mysql=MysqlDouble();claim=self.m.FormalMaintenanceClaim(self.root,BINDING)
        self.m.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned);mysql.fail_after_set=True
        with self.assertRaises(RuntimeError):self.m.restore_claimed_database_fence(claim,mysql)
        claim.close();mysql.fail_after_set=False;before=len(mysql.sets)
        claim=self.m.FormalMaintenanceClaim(self.root,BINDING,resume=True)
        with self.assertRaises(self.m.FormalSnapshotError):self.m.restore_claimed_database_fence(claim,mysql)
        self.assertEqual(len(mysql.sets),before)
        self.m.reconcile_claimed_database_restore(claim,mysql)
        self.assertEqual(len(mysql.sets),before);self.assertEqual(claim.events[-1]['step'],'db-restored');claim.close()
    def test_post_set_guard_failure_cannot_complete_via_unguarded_reconciliation(self):
        mysql=MysqlDouble();claim=self.m.FormalMaintenanceClaim(self.root,BINDING)
        def guard():
            if mysql.sets:raise self.m.FormalSnapshotError('OWNED_GUARD_REJECTED')
        with self.assertRaisesRegex(self.m.FormalSnapshotError,'^OWNED_GUARD_REJECTED$'):
            self.m.acquire_claimed_database_fence(claim,mysql,guard=guard)
        claim.close();claim=self.m.FormalMaintenanceClaim(self.root,BINDING,resume=True)
        before={p.name:p.read_bytes() for p in self.root.iterdir()};sets=list(mysql.sets)
        for check in (None,guard):
            expected='FORMAL_DATABASE_MUTATION_GUARD_REQUIRED' if check is None else 'OWNED_GUARD_REJECTED'
            with self.assertRaisesRegex(self.m.FormalSnapshotError,'^'+expected+'$'):
                self.m.reconcile_claimed_database_fence(claim,mysql,guard=check)
            self.assertEqual(before,{p.name:p.read_bytes() for p in self.root.iterdir()})
            self.assertEqual(mysql.sets,sets)
        self.m.reconcile_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
        self.assertTrue(claim.events[-1]['details']['reconciled']);self.assertEqual(mysql.sets,sets)
        claim.close()
    def test_changed_original_intent_prevents_restoration(self):
        mysql=MysqlDouble();claim=self.m.FormalMaintenanceClaim(self.root,BINDING)
        self.m.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned);before=len(mysql.sets)
        p=self.root/'database-original.json';value=json.loads(p.read_text());value['readOnly']=1
        p.write_bytes(self.m._canonical_bytes(value))
        with self.assertRaises(self.m.FormalSnapshotError):self.m.restore_claimed_database_fence(claim,mysql)
        self.assertEqual(len(mysql.sets),before);claim.close()
    def test_closed_claim_prevents_database_queries_or_mutation(self):
        mysql=MysqlDouble();claim=self.m.FormalMaintenanceClaim(self.root,BINDING);claim.close()
        with self.assertRaises(self.m.FormalSnapshotError):self.m.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
        self.assertEqual(mysql.sets,[])

if __name__=='__main__':unittest.main()
