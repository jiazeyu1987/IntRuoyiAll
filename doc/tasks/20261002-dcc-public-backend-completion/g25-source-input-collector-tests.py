import importlib.util,sys,unittest
from pathlib import Path
from unittest.mock import Mock
HERE=Path(__file__).resolve().parent
def collector():
    path=HERE/"g25-source-input-collector.py"
    if not path.exists():raise AssertionError("Fresh actual-source collector preparation is missing")
    spec=importlib.util.spec_from_file_location("g25_source_collector",path);m=importlib.util.module_from_spec(spec);sys.modules[spec.name]=m;spec.loader.exec_module(m);return m
class CollectorGateTest(unittest.TestCase):
    def test_offline_plan_builds_readonly_queries_without_client(self):
        m=collector();client=Mock();plan=m.plan(transport_factory=client)
        self.assertEqual("PREPARED_READONLY_SOURCE_COLLECTION_NOT_EXECUTED",plan["status"]);client.assert_not_called()
    def test_missing_root_writer_attestation_rejects_before_client(self):
        m=collector();client=Mock()
        with self.assertRaisesRegex(ValueError,"writer.*attestation"):m.collect({},transport_factory=client)
        client.assert_not_called()

class CollectorOfflineTest(unittest.TestCase):
    def test_plan_actual_frozen_helpers_all_queries_select_and_no_upgrade_method(self):
        from unittest.mock import patch
        m=collector()
        with patch('subprocess.run') as run,patch('subprocess.Popen') as popen:plan=m.plan();run.assert_not_called();popen.assert_not_called()
        self.assertEqual(6,len(plan['queries']));self.assertEqual(19,len(plan['materials']['entries']))
        self.assertFalse(plan['databaseWritesExecuted'])
    def test_authorization_receipt_requires_actual_approved_source_scope(self):
        import tempfile,json
        from unittest.mock import patch
        m=collector()
        self.assertTrue(m.verify_authorization(m.MAIN/'g25-user-authorization.json').is_file())
        with tempfile.TemporaryDirectory() as tmp:
            base=Path(tmp);p=base/'g25-user-authorization.json';p.write_text(json.dumps({'source':'fake-test','localDatabaseAuthorization':{'isolatedFirstRepeatRehearsal':True,'upgradeOriginalTestDatabaseAfterSuccessfulRehearsal':False}}),encoding='utf-8')
            with patch.object(m,'MAIN',base):
                with self.assertRaisesRegex(ValueError,'Actual local user'):m.verify_authorization(p)
    def test_root_attestation_not_inferred_from_mysql_zero(self):
        import tempfile,json
        m=collector()
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/'attest.json'
            for payload in [{'database':'ruoyi-vue-pro','serverUuid':m.UUID,'allKnownWritersExcluded':True,'attestedBy':'not-root','exclusionReference':'test'},{'database':'dcc_intqms_g18_rehearsal','serverUuid':m.UUID,'allKnownWritersExcluded':True,'attestedBy':'Root','exclusionReference':'test'}]:
                p.write_text(json.dumps(payload),encoding='utf-8')
                with self.assertRaisesRegex(ValueError,'Root actual writer'):m.validate_attestation({'writerAttestation':str(p)})
    def test_actual_clone_journal_can_be_readonly_verified_without_client(self):
        from unittest.mock import patch
        m=collector();source,g21,_=m.tools();prepared=g21.read_json(m.HERE/'g21-rehearsal-prepared-inputs.json')
        with patch('subprocess.run') as run,patch('subprocess.Popen') as popen:
            desc=m.clone_descriptor(m.BACKUPS/'g25-rehearsal-run/driver-receipt.json',source,g21,prepared);run.assert_not_called();popen.assert_not_called()
        self.assertTrue(desc['actualRuntimeReceipt']);self.assertGreater(len(desc['artifacts']),50)
    def test_capture_live_source_query_preserves_receipt_and_refuses_errors_or_foreign_db(self):
        import tempfile,json
        m=collector();mysql=Mock()
        header={'kind':'capture','database':'ruoyi-vue-pro','serverUuid':m.UUID,'version':'8.0.40','collectedAtUtc':'2026-10-03 10:00:00.000000'}
        mysql.read.return_value=json.dumps(header)+'\n'+json.dumps({'actual':'readonly-facts'})+'\n'
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp);q=p/'query.sql';q.write_text('SELECT 1;',encoding='utf-8')
            receipt=m.collect_query(mysql,q,p/'facts.jsonl',identity={});self.assertEqual(1,receipt['selectCount']);self.assertEqual(m.sha(p/'facts.jsonl'),receipt['factsSha256'])
            with self.assertRaisesRegex(ValueError,'overwrite'):m.collect_query(mysql,q,p/'facts.jsonl',identity={})
            mysql.read.return_value=json.dumps(dict(header,database='foreign'))+'\n{}\n'
            with self.assertRaisesRegex(ValueError,'identity mismatch'):m.collect_query(mysql,q,p/'foreign.jsonl',identity={})
            for sql in ['UPDATE t SET x=1;','SELECT 1 INTO OUTFILE \'bad\';','SELECT SLEEP(1);']:
                q.write_text(sql,encoding='utf-8')
                with self.assertRaisesRegex(ValueError,'SELECT'):m.collect_query(mysql,q,p/'unsafe.jsonl',identity={})
    def test_capture_private_mysql_error_never_goes_into_public_receipt(self):
        import tempfile
        m=collector();mysql=Mock()
        class PrivateError(RuntimeError):
            def __init__(self):self.private_error=b'ERROR private nonUTF8\xff';super().__init__('generic error')
        mysql.read.side_effect=PrivateError()
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp);q=p/'query.sql';q.write_text('SELECT 1;',encoding='utf-8')
            with self.assertRaises(PrivateError):m.collect_query(mysql,q,p/'facts.jsonl',identity={})
            self.assertEqual(b'ERROR private nonUTF8\xff',(p/'facts.jsonl.error.txt').read_bytes())
            self.assertFalse((p/'facts.jsonl').exists())


    def test_dump_runner_streams_new_gzip_and_seals_command_without_credentials(self):
        import tempfile,json,io,gzip
        from unittest.mock import patch
        m=collector();captured=[]
        class Process:
            def __init__(self,args,**kwargs):captured.extend(args);self.stdout=io.BytesIO(b'CREATE TABLE `offline_test` (id bigint);\n')
            def wait(self):return 0
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)
            with patch('shutil.which',return_value='docker.exe'),patch('subprocess.Popen',side_effect=Process):result=m.dump_backup(p,'schema',['--no-data'],{'captureId':'test-epoch'})
            self.assertEqual(b'CREATE TABLE `offline_test` (id bigint);\n',gzip.open(result['path'],'rb').read())
            receipt=json.loads(Path(result['dumpCommandReceipt']['path']).read_text(encoding='utf-8'));self.assertEqual('test-epoch',receipt['captureId']);self.assertEqual(result['sha256'],receipt['sha256'])
            self.assertIn('int-ruoyi-mysql',captured);self.assertIn('ruoyi-vue-pro',captured);self.assertFalse(any(x.startswith('-p') or 'accessSecret' in x for x in captured))
            with self.assertRaisesRegex(ValueError,'overwrite'):m.dump_backup(p,'schema',['--no-data'],{'captureId':'test-epoch'})
    def test_dump_scope_rejects_foreign_database_shell_or_write_directive(self):
        m=collector()
        for args in [['--databases'],['foreign-db; DROP DATABASE source'],['--result-file=outside']]:
            with self.assertRaisesRegex(ValueError,'dump argument'):m.dump_backup(HERE,'schema',args,{'captureId':'unit'})

    def test_dump_runner_failed_exit_preserves_private_stderr_no_receipt_pass(self):
        import tempfile,io
        from unittest.mock import patch
        m=collector()
        class Process:
            def __init__(self,args,**kwargs):self.stdout=io.BytesIO(b'partial metadata');kwargs['stderr'].write(b'PRIVATE\xffdumpfailure')
            def wait(self):return 1
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)
            with patch('shutil.which',return_value='docker.exe'),patch('subprocess.Popen',side_effect=Process):
                with self.assertRaisesRegex(ValueError,'backup failed'):m.dump_backup(p,'data',[],{'captureId':'test-epoch'})
            self.assertEqual(b'PRIVATE\xffdumpfailure',(p/'source-data.stderr.txt').read_bytes());self.assertFalse((p/'data-dump-command-receipt.json').exists())

class CollectorPipelineTest(unittest.TestCase):
    def run_case(self,*,writer=False,dump_fail=False,changed_rows=False,prepare_fail=False):
        import tempfile,copy,json,types,datetime as dt
        from unittest.mock import patch
        m=collector();source,g21,modules=m.tools();realplan=m.plan();materials=realplan['materials']
        before={table:{'columns':['id'],'rows':{'31':'a'*64},'count':1,'aggregateSha256':'a'*64} for table in materials['originalTables']+['infra_release_migration']}
        state={'queries':0,'snapshots':0,'dumps':[],'sourceUpgradeCalls':0}
        class Mysql:
            database='ruoyi-vue-pro'
            def read(self,sql):
                state['queries']+=1
                if "'kind','capture'" in sql:
                    header={'kind':'capture','database':'ruoyi-vue-pro','serverUuid':m.UUID,'version':'8.0.40','collectedAtUtc':m.utc()}
                    return json.dumps(header)+'\n'+json.dumps({'actualFreshFakeFacts':'not-runtime-proof'})+'\n'
                if 'activeTransactions' in sql:return json.dumps({'database':'ruoyi-vue-pro','serverUuid':m.UUID,'mysqlVersion':'8.0.40','activeTransactions':1 if writer else 0,'otherClientConnections':0,'enabledEvents':0})
                if "'captureId'" in sql:return json.dumps({'database':'ruoyi-vue-pro','serverUuid':m.UUID,'mysqlVersion':'8.0.40','captureId':'offline-fixture-epoch','collectedAtUtc':m.utc()})
                if sql.startswith('SELECT TABLE_NAME'):return '\n'.join(materials['schemaTableNames'])
                if 'FROM infra_release_migration' in sql:return ''
                if "'kind','environment'" in sql:return json.dumps({'kind':'environment','database':'ruoyi-vue-pro','serverUuid':m.UUID,'databaseCharset':'utf8mb4','databaseCollation':'utf8mb4_general_ci','utf8mb4DefaultCollation':'utf8mb4_0900_ai_ci','defaultStorageEngine':'InnoDB'})
                raise AssertionError('unexpected query')
        def snapshot(*args,**kwargs):
            state['snapshots']+=1;rows=copy.deepcopy(before)
            if changed_rows and state['snapshots']>1:rows['dcc_controlled_file']['rows']['31']='b'*64
            return rows
        fake_modules={'g21_mysql_support':types.SimpleNamespace(snapshot_original_rows=snapshot,LocalMysql=Mysql)}
        def fake_validate(group,dest):save=dest/group['proof'];save.write_text(json.dumps({'status':'TEST_FAKE_ONLY'}),encoding='utf-8');return {}
        def backup(directory,kind,args,identity):
            state['dumps'].append(kind)
            if dump_fail and kind=='data':raise ValueError('private test dump failure')
            return {'kind':kind,'path':str(directory/(kind+'.gz')),'sha256':'a'*64}
        with tempfile.TemporaryDirectory() as tmp:
            base=Path(tmp);attest=base/'attestation.json';attest.write_text(json.dumps({'database':'ruoyi-vue-pro','serverUuid':m.UUID,'allKnownWritersExcluded':True,'attestedBy':'Root','exclusionReference':'OFFLINE_FAKE_ONLY'}),encoding='utf-8')
            dest=base/'fresh';options={'destination':str(dest),'writerAttestation':str(attest),'authorization':str(m.MAIN/'g25-user-authorization.json'),'cloneJournal':'UNUSED_FAKE_ONLY'}
            with patch.object(m,'BACKUPS',base),patch.object(m,'plan',return_value=realplan),patch.object(m,'tools',return_value=(source,g21,fake_modules)),patch.object(m,'clone_descriptor',return_value={'explicitFake':'not_actual_receipt'}),patch.object(m,'validate_group',side_effect=fake_validate),patch.object(source,'verify_source_prerequisites',return_value={}),patch.object(source,'prepare',side_effect=ValueError('strict source rejects') if prepare_fail else None,return_value={'status':'PREPARED_VERIFIED_SOURCE_UPGRADE_NOT_EXECUTED'}),patch.object(source,'upgrade',side_effect=AssertionError('collector must never upgrade')):
                try:result=m.collect(options,transport_factory=lambda db:Mysql(),backup_runner=backup)
                except ValueError:result=json.loads((dest/'collection-receipt.json').read_text(encoding='utf-8'))
            return result,state
    def test_fake_full_collector_seals_fresh_inputs_without_invoking_upgrade(self):
        result,state=self.run_case();self.assertEqual('FRESH_SOURCE_INPUTS_VERIFIED_NOT_UPGRADED',result['status']);self.assertFalse(result['databaseWritesExecuted']);self.assertFalse(result['upgradeInvoked']);self.assertEqual(['schema','data','affected'],state['dumps'])
    def test_writer_failure_stops_before_snapshot_or_backup(self):
        result,state=self.run_case(writer=True);self.assertEqual('COLLECTION_FAILED_STOPPED_NO_SOURCE_UPGRADE',result['status']);self.assertFalse(state['dumps']);self.assertEqual(0,state['snapshots'])
    def test_backup_failure_stops_affected_and_prepare(self):
        result,state=self.run_case(dump_fail=True);self.assertEqual('COLLECTION_FAILED_STOPPED_NO_SOURCE_UPGRADE',result['status']);self.assertEqual(['schema','data'],state['dumps'])
    def test_source_changed_during_backup_rejects_input(self):
        result,state=self.run_case(changed_rows=True);self.assertEqual('COLLECTION_FAILED_STOPPED_NO_SOURCE_UPGRADE',result['status'])
    def test_strict_source_prepare_rejection_is_not_collection_success(self):
        result,state=self.run_case(prepare_fail=True);self.assertEqual('COLLECTION_FAILED_STOPPED_NO_SOURCE_UPGRADE',result['status']);self.assertFalse(result['upgradeInvoked'])

if __name__=="__main__":unittest.main(verbosity=2)
