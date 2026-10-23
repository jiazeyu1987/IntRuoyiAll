import importlib.util
import tempfile
from pathlib import Path
import unittest
import copy
import json
import hashlib
from unittest.mock import patch

HERE=Path(__file__).resolve().parent
class DriverTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        spec=importlib.util.spec_from_file_location('g28driver',HERE/'g28-driver.py');cls.d=importlib.util.module_from_spec(spec);spec.loader.exec_module(cls.d)
    def test_materials_only_new_exact_sql_and_max_one_plain_ledger(self):
        first=self.d.compose_material('first','dcc-g28-unit',self.d.SOURCE)
        repeat=self.d.compose_material('repeat','dcc-g28-unit',self.d.SOURCE)
        source=self.d.SQL_PATH.read_bytes();self.assertIn(source,first);self.assertIn(source,repeat)
        self.assertEqual(1,first.count(b'INSERT INTO infra_release_migration'));self.assertNotIn(b'INSERT INTO infra_release_migration',repeat)
        self.assertNotIn(b'UPDATE infra_release_migration',first);self.assertNotIn(b'--force',first)
    def test_absent_flag_rejects_before_transport_factory(self):
        called=[]
        with self.assertRaises(ValueError):self.d.execute({},False,lambda db:called.append(db))
        self.assertEqual([],called)
    def test_boolean_authorization_is_not_actual_user_evidence(self):
        request={'specificNewMigrationAuthorized':True}
        with self.assertRaises(ValueError):self.d.validate_request(request)
    def test_wrong_database_never_creates_transport(self):
        with self.assertRaises(ValueError):self.d.compose_material('first','dcc-g28-unit','other')
    def test_partial_existing_schema_blocks_execution_without_ledger_rewrite(self):
        c=self.d.schema.prepare_contract();facts=self.d.schema.fixture(c,absent=True)+[self.d.schema.fixture(c)[1]]
        with self.assertRaises(ValueError):self.d.schema.validate_facts(c,facts,'pre')
    def test_full_policy_closure_only_is_verified_not_replayed(self):
        plan=self.d.plan();self.assertEqual(8,plan['policyClosureCount']);self.assertEqual(1,plan['executionCount']);self.assertEqual(7,len(plan['prerequisitesFactOnly']));self.assertFalse(plan['writeAuthorizationGranted'])
    def test_source19_and_readiness_files_are_recomputed(self):
        proof=self.d.prior_proofs();self.assertEqual('SOURCE19_PASS_NEW_ONE_NOT_AUTHORIZED',proof['status']);self.assertFalse(proof['actualNewMigrationExecuted'])
    def test_execution_failure_stops_before_repeat_and_never_auto_restores(self):
        self.assertFalse(self.d.EXECUTION_RULES['automaticRetry']);self.assertFalse(self.d.EXECUTION_RULES['automaticRestore']);self.assertTrue(self.d.EXECUTION_RULES['implicitDdlCommitPossible'])
    def fake_pipeline(self,*,failure=None):
        d=self.d;contract=d.schema.prepare_contract();before={name:{'columns':['id','payload'],'rows':{'31':'a'*64},'count':1,'aggregateSha256':hashlib.sha256(json.dumps({'31':'a'*64},sort_keys=True,separators=(',',':')).encode()).hexdigest()} for name in d.ORIGINAL_TABLES}
        temp=tempfile.TemporaryDirectory(dir=HERE);self.addCleanup(temp.cleanup);directory=Path(temp.name)/'run'
        request={'database':d.SOURCE,'operationId':'dcc-g28-unit'};proofs={'freshOriginalBaseline':{'tables':before}}
        class Fake:
            def __init__(self):self.writes=[];self.applied=False;self.current=copy.deepcopy(before)
            def read(self,sql):
                if sql==d.writer_query():return json.dumps({'transactions':1 if failure=='writer' else 0,'otherConnections':0,'enabledEvents':0})+'\n'
                if sql==d.new_ledger_query():return json.dumps({'releaseTag':'dcc-g28-unit','operationId':'dcc-g28-unit','file':f'sql/mysql/{d.MIGRATION}.sql','migrationId':d.MIGRATION,'sha256':d.SQL_SHA,'status':'APPLIED','environment':'test','deleted':0,'tenantId':'0','creator':'dcc-g28-local-task','updater':'dcc-g28-local-task','startedAt':'2026-10-03 20:00:00','finishedAt':'2026-10-03 20:00:00','errorMessage':None})
                rows=d.schema.fixture(contract,absent=not self.applied)
                if failure=='postflight' and self.applied:next(x for x in rows if x.get('kind')=='column')['type']='wrong'
                if sql==d.schema.row_counts_sql():rows=[r for r in rows if r['kind']=='row_count']
                else:rows=[r for r in rows if r['kind']!='row_count']
                return ''.join(json.dumps(r)+'\n' for r in rows)
            def snapshot(self,tables):return copy.deepcopy(self.current)
            def compare(self,old,new,allowed):
                for name in old:
                    for key,value in old[name]['rows'].items():d.require(new[name]['rows'].get(key)==value,'old payload drift')
                    d.require(len(set(new[name]['rows'])-set(old[name]['rows']))==allowed.get(name,0),'unexpected additions')
            def run(self,sql,private,phase):
                self.writes.append(phase);self.applied=True;self.current['infra_release_migration']['rows']['32']='b'*64;self.current['infra_release_migration']['count']=2
                if failure=='history':self.current['dcc_controlled_file']['rows']['31']='c'*64
                out=private/(phase+'.stdout');error=private/(phase+'.stderr');error.write_bytes(b'')
                out.write_text(json.dumps({'g28Phase':'SESSION_IDENTITY','database':d.SOURCE,'intendedDatabase':d.SOURCE,'serverUuid':d.schema.UUID,'mysqlVersion':'8.0.40','pageSize':16384,'rowFormat':'dynamic'})+'\n'+json.dumps({'g28Phase':phase.upper()+'_COMPLETE','migrationId':d.MIGRATION})+'\n',encoding='utf-8')
                return {'exitCode':9 if failure=='write' else 0,'database':d.SOURCE,'sqlSha256':'0'*64 if failure=='wrongSQL' else hashlib.sha256(sql).hexdigest(),'stdout':d.desc(out),'stderr':d.desc(error)}
        fake=Fake()
        with patch.object(d,'validate_request',return_value=(proofs,directory)):
            if failure:
                with self.assertRaises(ValueError):d.execute(request,True,lambda db:fake)
            else:
                result=d.execute(request,True,lambda db:fake);self.assertEqual('G28_SOURCE_ONE_MIGRATION_FIRST_REPEAT_PASS_NOT_APPLICATION_READINESS',result['status']);d.validate_completed_journal(result,d.SOURCE)
        return fake,directory
    def test_actual_fake_first_repeat_pipeline_preserves_original_rows(self):
        fake,directory=self.fake_pipeline();self.assertEqual(['first','repeat'],fake.writes);self.assertTrue((directory/'driver-receipt.json').exists())
    def test_partial_first_write_failure_never_reaches_repeat(self):
        fake,directory=self.fake_pipeline(failure='write');self.assertEqual(['first'],fake.writes);self.assertEqual('STOPPED_DDL_PARTIAL_COMMIT_POSSIBLE_REVIEW_REQUIRED',json.loads((directory/'driver-receipt.json').read_text())['status'])
    def test_actual_postflight_or_history_or_sql_hash_error_stops_first(self):
        for reason in ['postflight','history','wrongSQL']:
            fake,_=self.fake_pipeline(failure=reason);self.assertEqual(['first'],fake.writes)
    def test_actual_fresh_writer_drift_blocks_before_any_sql(self):
        fake,directory=self.fake_pipeline(failure='writer');self.assertEqual([],fake.writes)
        journal=json.loads((directory/'driver-receipt.json').read_text());self.assertFalse(journal['databaseWriteAttempted']);self.assertEqual('GUARD_FAILED_NO_DDL_REACHED',journal['status'])
    def test_frozen_root_support_and_unified_signature_are_actual_formal_assets(self):
        self.assertIn('system_electronic_signature',self.d.ORIGINAL_TABLES);self.assertNotIn('signature_record',self.d.ORIGINAL_TABLES)
        self.assertEqual(self.d.SUPPORT_SHA,self.d.sha(self.d.MAIN/'g21_mysql_support.py'))
    def test_bad_clone_pass_label_cannot_replace_complete_first_repeat_evidence(self):
        with self.assertRaises(ValueError):self.d.validate_completed_journal({'status':'G28_OWNED_EXISTING_CLONE_FIRST_REPEAT_PASS','database':self.d.CLONE,'migrationId':self.d.MIGRATION,'sqlSha256':self.d.SQL_SHA},self.d.CLONE)
    def fake_backup(self,empty=False,missing_row=False):
        import gzip
        d=self.d;temp=tempfile.TemporaryDirectory(dir=HERE);self.addCleanup(temp.cleanup);folder=Path(temp.name)
        baseline={'capturedEpoch':2,'tables':{t:{'count':1} for t in d.ORIGINAL_TABLES}}
        backup={'status':'FRESH_G28_ORIGINAL_BACKUP_COMPLETE','protectedTables':d.ORIGINAL_TABLES,'baselineAggregateSha256':hashlib.sha256(json.dumps(baseline['tables'],sort_keys=True,separators=(',',':')).encode()).hexdigest(),'capturedEpoch':5,'artifacts':[]}
        for kind in ['schema','protected-original-rows']:
            payload=''
            if not empty:
                for table in d.ORIGINAL_TABLES:
                    payload+=f'CREATE TABLE `{table}` (id bigint);\n' if kind=='schema' else f'-- Dumping data for table `{table}`\n'+('' if missing_row else f'INSERT INTO `{table}` VALUES (1);\n')
                payload+='-- Dump completed on 2026-10-03 00:00:00\n'
            path=folder/(kind+'.gz');path.write_bytes(gzip.compress(payload.encode()))
            command={'profile':'G28_SCHEMA_SINGLE_TRANSACTION_NO_DATA' if kind=='schema' else 'G28_PROTECTED_ROWS_SINGLE_TRANSACTION_SINGLE_ROW_INSERTS','database':d.SOURCE,'serverUuid':d.schema.UUID,'container':'int-ruoyi-mysql','exitCode':0,'databaseWrites':False,'credentialsPersisted':False,'artifactSha256':d.sha(path),'artifactBytes':path.stat().st_size,'startedEpoch':3,'finishedEpoch':4,'uncompressedBytes':len(payload.encode()),'tables':d.ORIGINAL_TABLES,'singleRowInsertCounts':{t:1 for t in d.ORIGINAL_TABLES}}
            receipt=folder/(kind+'-command.json');receipt.write_text(json.dumps(command));backup['artifacts'].append({'kind':kind,**d.desc(path),'dumpExitCode':0,'dumpCommandReceipt':d.desc(receipt)})
        return backup,baseline
    def test_exact_complete_capture_dump_coverage_is_accepted(self):
        backup,baseline=self.fake_backup();self.d.validate_backup(backup,baseline,self.d.SOURCE,{'capturedEpoch':1})
    def test_empty_or_missing_original_row_gzip_cannot_claim_restorable_backup(self):
        for empty,missing in [(True,False),(False,True)]:
            backup,baseline=self.fake_backup(empty,missing)
            with self.assertRaises(ValueError):self.d.validate_backup(backup,baseline,self.d.SOURCE,{'capturedEpoch':1})

if __name__=='__main__':unittest.main()
