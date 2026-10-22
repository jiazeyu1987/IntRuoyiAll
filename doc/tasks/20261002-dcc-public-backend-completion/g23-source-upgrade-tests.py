import importlib.util,sys,unittest
from pathlib import Path
from unittest.mock import Mock
HERE=Path(__file__).resolve().parent
def driver():
    p=HERE/"g23-source-upgrade-driver.py"
    if not p.exists():raise AssertionError("Source upgrade preparation driver is missing")
    spec=importlib.util.spec_from_file_location("g23_source_upgrade_driver",p);module=importlib.util.module_from_spec(spec);sys.modules[spec.name]=module;spec.loader.exec_module(module);return module
class SourceGateTest(unittest.TestCase):
    def test_missing_real_clone_blocks_prepare_without_client(self):
        d=driver();client=Mock();result=d.prepare({"clonePass":None},transport_factory=client)
        self.assertEqual("PREPARED_BLOCKED_MISSING_REAL_REHEARSAL_RECEIPT",result["status"]);client.assert_not_called()
    def test_missing_explicit_source_gate_prevents_any_connection(self):
        d=driver();client=Mock()
        with self.assertRaisesRegex(ValueError,"authorize-local-test-upgrade"):d.upgrade({},authorize_local_test_upgrade=False,transport_factory=client)
        client.assert_not_called()

class SourceOfflineValidationTest(unittest.TestCase):
    def setUp(self):self.d=driver()
    def test_actual_missing_rehearsal_prepares_blocked_template_without_transport(self):
        import json
        from unittest.mock import patch
        request=json.loads((HERE/"g23-source-upgrade-request-template.json").read_text(encoding="utf-8"))
        with patch("subprocess.run") as run,patch("subprocess.Popen") as popen:
            result=self.d.prepare(request)
            run.assert_not_called();popen.assert_not_called()
        (HERE/"g23-source-upgrade-prepared-blocked.json").write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
        self.assertFalse(result['writeConnectionAllowed']);self.assertFalse(result['databaseWritesExecuted'])
    def test_source_foreign_database_refused_even_in_missing_clone_prepare(self):
        for value in ['foreign','dcc_intqms_g18_rehearsal']:
            with self.assertRaisesRegex(ValueError,'fixed local-test'):self.d.prepare({'sourceDatabase':value})
    def test_freshness_is_exact900_not_authorization_or_future_tolerance(self):
        import datetime as dt
        now=dt.datetime(2026,10,3,10,0,tzinfo=dt.timezone.utc)
        self.d.fresh('2026-10-03T09:45:00Z',now)
        for stamp in ['2026-10-03T09:44:59Z','2026-10-03T10:00:01Z']:
            with self.assertRaisesRegex(ValueError,'stale|future'):self.d.fresh(stamp,now)
        with self.assertRaisesRegex(ValueError,'900'):self.d.fresh('2026-10-03T09:59:00Z',now,3600)
    def test_writer_checks_include_db_null_connections_and_events_other_schemas(self):
        facts={'database':'ruoyi-vue-pro','serverUuid':self.d.SERVER,'mysqlVersion':'8.0.40','activeTransactions':0,'otherClientConnections':0,'enabledEvents':0}
        self.d.require_writer_free([facts],{})
        for key in ['activeTransactions','otherClientConnections','enabledEvents']:
            changed=dict(facts);changed[key]=1
            with self.assertRaisesRegex(ValueError,'writer/connection/event'):self.d.require_writer_free([changed],{})
        query=self.d.writer_query();self.assertNotIn('DB=DATABASE()',query);self.assertNotIn('EVENT_SCHEMA=DATABASE()',query)
        with self.assertRaisesRegex(ValueError,'identity'):self.d.require_writer_free([dict(facts,serverUuid='other')],{})
    def test_clone_status_string_cannot_replace_hashed_actual_artifacts(self):
        import tempfile,json,hashlib
        from unittest.mock import patch
        with tempfile.TemporaryDirectory() as tmp:
            base=Path(tmp);folder=base/'clone';folder.mkdir();journal=folder/'driver-receipt.json'
            journal.write_text(json.dumps({'status':'ISOLATED_REHEARSAL_PASS','databaseWritesExecuted':True,'sourceDatabase':'ruoyi-vue-pro','rehearsalDatabase':'dcc_intqms_g18_rehearsal'}),encoding='utf-8')
            desc={'path':str(journal),'sha256':self.d.sha(journal),'actualRuntimeReceipt':True,'testFixture':False,'driverSha256':self.d.FROZEN_G21_SHA,'artifacts':[]}
            with patch.object(self.d,'protected_descriptor',side_effect=lambda row:Path(row['path'])):
                with self.assertRaisesRegex(ValueError,'artifact manifest'):self.d.verify_clone_pass(desc,{},Mock())
            desc['testFixture']=True
            with patch.object(self.d,'protected_descriptor',side_effect=lambda row:Path(row['path'])):
                with self.assertRaisesRegex(ValueError,'synthetic'):self.d.verify_clone_pass(desc,{},Mock())
    def test_protected_step_sha_and_relative_path_escape_reject(self):
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            base=Path(tmp);payload=base/'first.txt';payload.write_text('exact',encoding='utf-8')
            self.assertEqual({'first.txt'},set(self.d.artifact_map(base,[{'relativePath':'first.txt','sha256':self.d.sha(payload)}])))
            for row in [{'relativePath':'../outside.txt','sha256':'0'*64},{'relativePath':'first.txt','sha256':'0'*64}]:
                with self.assertRaises(ValueError):self.d.artifact_map(base,[row])
    def test_frozen_clone_driver_cannot_be_changed_by_source_tool(self):
        from unittest.mock import patch
        d=self.d
        self.assertEqual(d.FROZEN_G21_SHA,d.sha(HERE/'g21-rehearsal-driver.py'))
        with patch.object(d,'sha',return_value='wrong'):
            with self.assertRaisesRegex(ValueError,'G21 implementation drift'):d.g21_module()
    def test_missing_actual_root_permission_refuses_client_after_valid_prepare(self):
        from unittest.mock import patch
        prepared={'status':'PREPARED_VERIFIED_SOURCE_UPGRADE_NOT_EXECUTED','request':{'actualUserWriteApprovalRecordedByRoot':False}}
        factory=Mock()
        with patch.object(self.d,'prepare',return_value=prepared):
            with self.assertRaisesRegex(ValueError,'Root actual'):self.d.upgrade(prepared,authorize_local_test_upgrade=True,transport_factory=factory)
        factory.assert_not_called()
    def test_source_postflight_rejects_clone_success_or_failed_or_nonjson(self):
        import tempfile,json,types
        from unittest.mock import patch
        d=self.d;g21=d.g21_module();materials=g21.read_json(HERE/'g21-rehearsal-prepared-inputs.json')
        for payload in ['not json',json.dumps({'status':'FAIL'}),json.dumps({'status':'POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE','database':'dcc_intqms_g18_rehearsal','serverUuid':d.SERVER,'errors':[]})]:
            with tempfile.TemporaryDirectory() as tmp:
                private=Path(tmp);environment=private/'env.jsonl';environment.write_text('{}',encoding='utf-8')
                def cli(command,**kwargs):Path(command[command.index('--result')+1]).write_text(payload,encoding='utf-8');return types.SimpleNamespace(returncode=0)
                with patch.object(g21,'protected_read',return_value='private schema facts'),patch('subprocess.run',side_effect=cli):
                    with self.assertRaises(ValueError):d.source_postflight(Mock(),materials,private,environment,g21)
    def test_true_source_schema_result_is_separate_from_clone_guard(self):
        import tempfile,json,types
        from unittest.mock import patch
        d=self.d;g21=d.g21_module();materials=g21.read_json(HERE/'g21-rehearsal-prepared-inputs.json')
        with tempfile.TemporaryDirectory() as tmp:
            private=Path(tmp);environment=private/'env.jsonl';environment.write_text('{}',encoding='utf-8')
            def cli(command,**kwargs):Path(command[command.index('--result')+1]).write_text(json.dumps({'status':'POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE','database':'ruoyi-vue-pro','serverUuid':d.SERVER,'errors':[]}),encoding='utf-8');return types.SimpleNamespace(returncode=0)
            with patch.object(g21,'protected_read',return_value='private schema facts'),patch('subprocess.run',side_effect=cli):
                self.assertEqual('ruoyi-vue-pro',d.source_postflight(Mock(),materials,private,environment,g21)['database'])
    def test_source_snapshot_origin_scope_and_aggregate_are_real_source_only(self):
        import tempfile,json,hashlib,datetime as dt
        from unittest.mock import patch
        d=self.d;now=dt.datetime(2026,10,3,10,0,tzinfo=dt.timezone.utc);rows={'31':'a'*64}
        shape={'columns':['id'],'rows':rows,'count':1,'aggregateSha256':hashlib.sha256(json.dumps(rows,sort_keys=True,separators=(',',':')).encode()).hexdigest()}
        envelope={'database':'ruoyi-vue-pro','serverUuid':d.SERVER,'origin':'ACTUAL_SOURCE_READ_NOT_CLONE','databaseWrites':False,'collectedAtUtc':'2026-10-03T09:59:00Z','snapshot':{'t':shape,'infra_release_migration':shape}}
        request={'maxAgeSeconds':900,'originalTables':['t']}
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/'snapshot.json';path.write_text(json.dumps(envelope),encoding='utf-8')
            with patch.object(d,'protected_descriptor',return_value=path):self.assertEqual(2,len(d.verify_source_snapshot({},request,now)))
            for key,value in [('origin','CLONE_BASELINE'),('collectedAtUtc','2026-10-03T08:00:00Z'),('serverUuid','other')]:
                wrong=dict(envelope);wrong[key]=value;path.write_text(json.dumps(wrong),encoding='utf-8')
                with patch.object(d,'protected_descriptor',return_value=path):
                    with self.assertRaises(ValueError):d.verify_source_snapshot({},request,now)
    def test_actual_fresh_source_proofs_are_revalidated_not_status_only(self):
        import tempfile,json,datetime as dt,shutil,copy
        from unittest.mock import patch
        d=self.d;g21=d.g21_module();original=g21.read_json(g21.MAIN_TASK/'g21-prerequisite-runtime-receipt.json')
        with tempfile.TemporaryDirectory() as tmp:
            folder=Path(tmp);aggregate=copy.deepcopy(original)
            for group in aggregate['groups']:
                for field in ['proof','facts','captureReceipt']:shutil.copyfile(g21.MAIN_TASK/group[field],folder/group[field])
                group['captureReceiptSha256']=d.sha(folder/group['captureReceipt'])
            path=folder/'g21-prerequisite-runtime-receipt.json';path.write_text(json.dumps(aggregate),encoding='utf-8')
            now=max(d.timestamp(x['collectedAtUtc']) for x in aggregate['groups'])+dt.timedelta(seconds=1)
            with patch.object(d,'protected_descriptor',return_value=path):self.assertEqual(25,len(d.verify_source_prerequisites({},g21,now,900)['prerequisiteIds']))
            changed=copy.deepcopy(aggregate);changed['groups'][0]['collectedAtUtc']=(now-dt.timedelta(seconds=1)).isoformat();path.write_text(json.dumps(changed),encoding='utf-8')
            with patch.object(d,'protected_descriptor',return_value=path):
                with self.assertRaisesRegex(ValueError,'actual capture timestamp'):d.verify_source_prerequisites({},g21,now,900)
            changed=copy.deepcopy(aggregate);group=changed['groups'][1]
            facts=folder/group['facts'];lines=facts.read_text(encoding='utf-8').splitlines();mutated=[]
            for line in lines:
                row=json.loads(line)
                if row.get('section')=='policies' and row.get('mode')=='DIRECT':row['executor']='WRONG_NEW_EXECUTOR'
                mutated.append(json.dumps(row,ensure_ascii=False))
            facts.write_text('\n'.join(mutated)+'\n',encoding='utf-8');group['factsSha256']=d.sha(facts)
            capture=folder/group['captureReceipt'];cap=json.loads(capture.read_text(encoding='utf-8'));cap['factsSha256']=group['factsSha256'];capture.write_text(json.dumps(cap),encoding='utf-8');group['captureReceiptSha256']=d.sha(capture)
            path.write_text(json.dumps(changed),encoding='utf-8')
            with patch.object(d,'protected_descriptor',return_value=path):
                with self.assertRaisesRegex(ValueError,'Fact mismatch'):d.verify_source_prerequisites({},g21,now,900)

    def test_new_backup_requires_own_epoch_directory_snapshot_writer_binding_and_real_scope(self):
        import tempfile,gzip,json,datetime as dt
        from unittest.mock import patch
        d=self.d;g21=d.g21_module();prepared=g21.read_json(HERE/'g21-rehearsal-prepared-inputs.json');now=dt.datetime(2026,10,3,10,0,tzinfo=dt.timezone.utc)
        source_scope=read_json_local=__import__('json').loads((g21.MAIN_TASK/'g18-backup-scope.json').read_text(encoding='utf-8-sig'))['dataBackupTables']
        request={'rehearsalPrepared':{'path':'doc/tasks/20261002-dcc-public-backend-completion/g21-rehearsal-prepared-inputs.json','sha256':d.sha(HERE/'g21-rehearsal-prepared-inputs.json')},'maxAgeSeconds':900,'originalTables':prepared['originalTables'],'sourceBaseline':{'sha256':'b'*64},'writerExclusionReceipt':{'sha256':'c'*64}}
        with tempfile.TemporaryDirectory() as tmp:
            base=Path(tmp);directory=base/'new-source-backup';directory.mkdir();artifacts=[]
            names={'schema':set(prepared['schemaTableNames']),'data':set(source_scope),'affected':set(prepared['originalTables'])|{'infra_release_migration'}}
            for kind,tables in names.items():
                raw='\n'.join(('-- Dumping data for table `'+t+'`') if kind=='data' else ('CREATE TABLE `'+t+'` ();') for t in sorted(tables)).encode();path=directory/(kind+'.gz')
                with gzip.open(path,'wb') as stream:stream.write(raw)
                artifacts.append({'kind':kind,'path':str(path),'bytes':path.stat().st_size,'uncompressedBytes':len(raw),'sha256':d.sha(path),'gzipIntegrity':'PASS','dumpExitCode':0})
            for row in artifacts:
                command=directory/(row['kind']+'-dump-receipt.json');command.write_text(json.dumps({'kind':row['kind'],'database':'ruoyi-vue-pro','serverUuid':d.SERVER,'sourceContainer':'int-ruoyi-mysql','captureId':'offline-fixture-connection','exitCode':0,'sha256':row['sha256'],'path':row['path'],'startedAtUtc':'2026-10-03T09:58:10Z','completedAtUtc':'2026-10-03T09:58:50Z','argvProfile':'mysqldump-readonly-fixed-local-no-routines-events-no-credentials-export'}),encoding='utf-8');row['dumpCommandReceipt']={'path':str(command),'sha256':d.sha(command)}
            receipt={'startedAtUtc':'2026-10-03T09:58:00Z','sourceCapture':{'database':'ruoyi-vue-pro','serverUuid':d.SERVER,'mysqlVersion':'8.0.40','captureId':'offline-fixture-connection'},'database':'ruoyi-vue-pro','databaseWrites':False,'credentialsExported':False,'sourceContainer':'int-ruoyi-mysql','serverUuid':d.SERVER,'tableCount':210,'completedAtUtc':'2026-10-03T09:59:00Z','backupDirectory':str(directory),'affectedTables':prepared['originalTables']+['infra_release_migration'],'sourceSnapshotSha256':'b'*64,'writerExclusionReceiptSha256':'c'*64,'artifacts':artifacts}
            path=directory/'receipt.json';path.write_text(json.dumps(receipt),encoding='utf-8')
            with patch.object(d,'BACKUPS',base),patch.object(d,'protected_descriptor',side_effect=lambda row:Path(row['path']) if row.get('path') else path):self.assertEqual(3,len(d.verify_new_backup({},request,g21,now)))
            for field,value in [('sourceSnapshotSha256','wrong'),('writerExclusionReceiptSha256','wrong'),('completedAtUtc','2026-10-03T08:00:00Z'),('backupDirectory',str(base))]:
                changed=dict(receipt);changed[field]=value;path.write_text(json.dumps(changed),encoding='utf-8')
                with patch.object(d,'BACKUPS',base),patch.object(d,'protected_descriptor',side_effect=lambda row:Path(row['path']) if row.get('path') else path):
                    with self.assertRaises(ValueError):d.verify_new_backup({},request,g21,now)
            path.write_text(json.dumps(receipt),encoding='utf-8');Path(artifacts[0]['path']).write_bytes(b'changed backup')
            with patch.object(d,'BACKUPS',base),patch.object(d,'protected_descriptor',side_effect=lambda row:Path(row['path']) if row.get('path') else path):
                with self.assertRaisesRegex(ValueError,'checksum'):d.verify_new_backup({},request,g21,now)

    def test_missing_fresh_source_materials_is_explicit_block_not_permission(self):
        from unittest.mock import patch
        import json
        d=self.d;request=json.loads((HERE/'g23-source-upgrade-request-template.json').read_text(encoding='utf-8'));request['clonePass']={'placeholder':'only mock in test'}
        g21=d.g21_module();prepared=g21.read_json(HERE/'g21-rehearsal-prepared-inputs.json')
        with patch.object(d,'g21_module',return_value=g21),patch.object(g21,'validate_prepared'),patch.object(d,'verify_clone_pass',return_value={'status':'test-only'}):
            result=d.prepare(request)
        self.assertEqual('PREPARED_BLOCKED_MISSING_FRESH_SOURCE_INPUTS',result['status']);self.assertFalse(result['writeConnectionAllowed'])


class SourcePipelineTest(unittest.TestCase):
    def run_case(self,*,writer=False,table_exists=False,ledger_exists=False,drift=False,fail_write=False,fail_postflight=False,wrong_seed=False,omitted_columns=False):
        import copy,tempfile,json,hashlib,types
        from unittest.mock import patch
        d=driver();g21=d.g21_module();materials=g21.read_json(HERE/'g21-rehearsal-prepared-inputs.json')
        modules=g21.load_support(Path(materials['mainTask']),g21.read_json(materials['contractPath']));real_support=modules['g21_mysql_support']
        fields=g21.copied_business_columns(materials);sources=[x for x in g21.read_json(HERE/'g21-bpm-policy-contract.json')['sections']['procdefs'] if x['version']==3 and x['key'] in {'dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete'}]
        copied={table:{'columns':cols,'sources':{x['id']:hashlib.sha256((table+x['id']).encode()).hexdigest() for x in sources}} for table,cols in fields.items()}
        expected=g21.expected_seed_facts(materials,copied)
        before={table:{'columns':['ID_' if table.startswith('act_') else 'id','payload'],'rows':{'31':'a'*64},'count':1,'aggregateSha256':hashlib.sha256(json.dumps({'31':'a'*64},sort_keys=True,separators=(',',':')).encode()).hexdigest()} for table in materials['originalTables']+['infra_release_migration']}
        after=copy.deepcopy(before);actual={}
        for table,rows in expected.items():
            actual[table]=[dict(x,id=table+str(n)) for n,x in enumerate(rows)]
            for row in actual[table]:after[table]['rows'][row['id'].encode().hex().upper()]='b'*64
            after[table]['count']=len(after[table]['rows'])
        ledger=[]
        for n,entry in enumerate(materials['entries']):
            row={'id':str(100+n),'migrationId':entry['migrationId'],'sha256':entry['sha256'],'fileName':entry['file'],'environment':'test','status':'APPLIED','operationId':materials['operationId'],'releaseTag':materials['operationId'],'deleted':0,'tenant':'0'};ledger.append(row);after['infra_release_migration']['rows'][row['id'].encode().hex().upper()]='b'*64
        after['infra_release_migration']['count']=20
        if wrong_seed:actual['bpm_process_definition_info'][0]['copiedBusinessHash']='0'*64
        env={'kind':'environment','database':'ruoyi-vue-pro','serverUuid':d.SERVER,'databaseCharset':'utf8mb4','databaseCollation':'utf8mb4_general_ci','utf8mb4DefaultCollation':'utf8mb4_0900_ai_ci','defaultStorageEngine':'InnoDB'}
        req={'sourceDatabase':'ruoyi-vue-pro','actualUserWriteApprovalRecordedByRoot':True,'rootAuthorizationReference':'OFFLINE UNIT EXPLICIT FAKE AUTHORIZATION - NOT REAL USER APPROVAL'}
        prepared={'status':'PREPARED_VERIFIED_SOURCE_UPGRADE_NOT_EXECUTED','request':req,'materials':materials,'sourceBaseline':copy.deepcopy(before),'sourceEnvironment':env}
        if omitted_columns:prepared['sourceBaseline']['dcc_controlled_file']['columns']=['id']
        state={'written':False,'calls':[],'factories':[]}
        class Mysql:
            database='ruoyi-vue-pro'
            def read(self,sql):
                state['calls'].append('read')
                if 'activeTransactions' in sql:return json.dumps({'database':'ruoyi-vue-pro','serverUuid':d.SERVER,'mysqlVersion':'8.0.40','activeTransactions':1 if writer else 0,'otherClientConnections':0,'enabledEvents':0})
                if sql.startswith('SELECT TABLE_NAME'):return materials['newTables'][0] if table_exists else ''
                if 'FROM infra_release_migration' in sql:return '\n'.join(json.dumps(x) for x in ledger) if state['written'] or ledger_exists else ''
                if "'kind','environment'" in sql:return json.dumps(env)
                if sql.startswith('SELECT COUNT(*) FROM `'):return '0'
                for table in actual:
                    if 'FROM `'+table+'`' in sql:return '\n'.join(json.dumps(x) for x in actual[table])
                raise AssertionError('unexpected source test read')
            def run_authorized_sql(self,sql,*,private_directory,stem):
                state['calls'].append('write')
                expected_bytes=Path(materials['postflight']['environmentQuery']).read_bytes()+b'\n'+Path(materials['scripts']['first']).read_bytes()
                self_outer.assertEqual(expected_bytes,sql);self_outer.assertNotIn(b'CREATE DATABASE',sql)
                out=private_directory/(stem+'.stdout.txt');error=private_directory/(stem+'.stderr.txt')
                out.write_text(json.dumps(env)+'\n'+'\n'.join(json.dumps({'taskPhase':phase,'migrationId':entry['migrationId']}) for entry in materials['entries'] for phase in ['sql_begin','sql_complete']),encoding='utf-8')
                error.write_bytes(b'PRIVATE SQL FAILURE' if fail_write else b'')
                state['written']=True
                if fail_write:raise RuntimeError('private failure')
                return {'database':'ruoyi-vue-pro','sqlSha256':hashlib.sha256(sql).hexdigest(),'exitCode':0,'stdout':str(out),'stderr':str(error)}
        self_outer=self
        def factory(db):state['factories'].append(db);return Mysql()
        def snapshot(*args,**kwargs):
            if not state['written']:self.assertEqual(2,len(args),"Source before snapshot must discover complete actual columns, not reuse caller list")
            value=copy.deepcopy(after if state['written'] else before)
            if drift and not state['written']:value['dcc_controlled_file']['rows']['31']='c'*64
            return value
        support=types.SimpleNamespace(LocalMysql=factory,snapshot_original_rows=snapshot,compare_original_rows=real_support.compare_original_rows)
        def postflight(*args):
            state['calls'].append('postflight')
            if fail_postflight:raise ValueError('source schema fails')
            return {'status':'POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE','database':'ruoyi-vue-pro','serverUuid':d.SERVER,'errors':[]}
        with tempfile.TemporaryDirectory() as tmp:
            base=Path(tmp);private=base/'source-run'
            with patch.object(d,'BACKUPS',base),patch.object(d,'prepare',return_value=prepared),patch.object(d,'g21_module',return_value=g21),patch.object(g21,'load_support',return_value={'g21_mysql_support':support}),patch.object(g21,'capture_copied_baseline',return_value=copied),patch.object(d,'source_postflight',side_effect=postflight):
                try:result=d.upgrade(prepared,authorize_local_test_upgrade=True,transport_factory=factory,private_directory=private)
                except ValueError:result=json.loads((private/'source-driver-receipt.json').read_text(encoding='utf-8'))
            result['__testProtectedFiles']=[x.name for x in private.iterdir()]
            return result,state
    def test_source_fake_pipeline_writes_only_nineteen_original_bytes_and_checks_all(self):
        result,state=self.run_case();self.assertEqual('LOCAL_TEST_SOURCE_NINETEEN_UPGRADE_PASS_NOT_APPLICATION_READINESS',result['status']);self.assertEqual(['ruoyi-vue-pro'],state['factories'])
        self.assertEqual(1,state['calls'].count('write'));self.assertEqual(33,result['exactConfigAdditions']);self.assertEqual(19,result['exactLedgerAdditions']);self.assertFalse(result['actualUserApprovalProvenByFlag'])
    def test_any_writer_prevents_source_write(self):
        result,state=self.run_case(writer=True);self.assertFalse(state['written']);self.assertEqual('FAILED_SOURCE_UPGRADE_STOPPED_NO_AUTOMATIC_RECOVERY',result['status'])
    def test_existing_new_table_prevents_source_write(self):
        result,state=self.run_case(table_exists=True);self.assertFalse(state['written'])
    def test_existing_candidate_ledger_prevents_source_write(self):
        result,state=self.run_case(ledger_exists=True);self.assertFalse(state['written'])
    def test_current_source_changed_after_backup_prevents_write(self):
        result,state=self.run_case(drift=True);self.assertFalse(state['written'])
    def test_omitted_original_source_column_list_cannot_hide_payload(self):
        result,state=self.run_case(omitted_columns=True);self.assertFalse(state['written']);self.assertEqual('FAILED_SOURCE_UPGRADE_STOPPED_NO_AUTOMATIC_RECOVERY',result['status'])

    def test_source_sql_failure_retains_partial_ddl_risk_and_does_not_recover(self):
        result,state=self.run_case(fail_write=True);self.assertEqual('FAILED_SOURCE_UPGRADE_STOPPED_NO_AUTOMATIC_RECOVERY',result['status'])
        self.assertTrue(result['databaseWriteAttempted']);self.assertTrue(result['partialDdlCommitPossible']);self.assertNotIn('postflight',state['calls']);self.assertIn('source-nineteen-first.stderr.txt',result['__testProtectedFiles'])
    def test_wrong_copied_seed_field_after_write_cannot_be_success(self):
        result,state=self.run_case(wrong_seed=True);self.assertTrue(state['written']);self.assertEqual('FAILED_SOURCE_UPGRADE_STOPPED_NO_AUTOMATIC_RECOVERY',result['status']);self.assertNotIn('postflight',state['calls'])
    def test_source_schema_failure_keeps_error_evidence_no_auto_recovery(self):
        result,state=self.run_case(fail_postflight=True);self.assertEqual('FAILED_SOURCE_UPGRADE_STOPPED_NO_AUTOMATIC_RECOVERY',result['status']);self.assertIn('source-driver-error.txt',result['__testProtectedFiles'])


class SourceCloneFullReceiptTest(unittest.TestCase):
    def fixture(self,base):
        import copy,json,hashlib
        d=driver();g21=d.g21_module();prepared=g21.read_json(HERE/'g21-rehearsal-prepared-inputs.json');directory=base/'clone';directory.mkdir()
        def save(name,value):path=directory/name;path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');return path
        env={'kind':'environment','database':'dcc_intqms_g18_rehearsal','serverUuid':d.SERVER,'databaseCharset':'utf8mb4','databaseCollation':'utf8mb4_general_ci','utf8mb4DefaultCollation':'utf8mb4_0900_ai_ci','defaultStorageEngine':'InnoDB'}
        (directory/'frozen-environment.jsonl').write_text(json.dumps(env)+'\n',encoding='utf-8')
        (directory/'restored-tables.facts.txt').write_text('\n'.join(prepared['schemaTableNames']),encoding='utf-8')
        sources=[x for x in g21.read_json(HERE/'g21-bpm-policy-contract.json')['sections']['procdefs'] if x['version']==3 and x['key'] in {'dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete'}]
        copied={table:{'columns':cols,'sources':{x['id']:hashlib.sha256((table+x['id']).encode()).hexdigest() for x in sources}} for table,cols in g21.copied_business_columns(prepared).items()};save('baseline-v3-complete-copied-business.json',copied)
        expected=g21.expected_seed_facts(prepared,copied)
        before={table:{'columns':['ID_' if table.startswith('act_') else 'id','payload'],'rows':{'31':'a'*64},'count':1,'aggregateSha256':'a'*64} for table in prepared['originalTables']+['infra_release_migration']};save('baseline-original-rows.json',before)
        after=copy.deepcopy(before);actual={}
        for table,rows in expected.items():
            actual[table]=[dict(x,id=table+str(n)) for n,x in enumerate(rows)]
            after[table]['rows'].update({x['id'].encode().hex().upper():'b'*64 for x in actual[table]});after[table]['count']=len(after[table]['rows'])
        ledger=[]
        for n,entry in enumerate(prepared['entries']):ledger.append({'id':str(100+n),'migrationId':entry['migrationId'],'sha256':entry['sha256'],'fileName':entry['file'],'environment':'test','status':'APPLIED','operationId':prepared['operationId'],'releaseTag':prepared['operationId'],'deleted':0,'tenant':'0'})
        after['infra_release_migration']['rows'].update({x['id'].encode().hex().upper():'b'*64 for x in ledger});after['infra_release_migration']['count']=20
        modules=g21.load_support(Path(prepared['mainTask']),g21.read_json(prepared['contractPath']));support=modules['g21_mysql_support'];allow=dict(prepared['permittedAdditions']);allow['infra_release_migration']=19
        validator_path=Path(prepared['postflight']['validator']);spec=importlib.util.spec_from_file_location('source_test_schema',validator_path);v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)
        contract=g21.read_json(prepared['postflight']['contract']);environment={k:val for k,val in env.items() if k!='kind'};facts=v.positive_fixture(contract,environment)
        fact_rows=[{'kind':'runtime','database':environment['database'],'serverUuid':d.SERVER,'version':'8.0.40'}]
        for table,shape in facts['tables'].items():fact_rows.append({'kind':'table','table':table,**shape})
        for table,columns in facts['columns'].items():
            for column,shape in columns.items():
                extra=' '.join(filter(None,['auto_increment' if shape['extra']['autoIncrement'] else '',shape['extra']['generated'].upper()+' GENERATED' if shape['extra']['generated'] else '', 'on update CURRENT_TIMESTAMP' if shape['extra']['onUpdate'] else '']))
                fact_rows.append({'kind':'column','table':table,'column':column,**shape,'extra':extra})
        for table,indexes in facts['indexes'].items():
            for name,shape in indexes.items():
                for sequence,col in enumerate(shape['columns'],1):fact_rows.append({'kind':'index','table':table,'name':name,'type':shape['type'],'unique':int(shape['unique']),'sequence':sequence,**col})
        for mid,shape in facts['ledger'].items():fact_rows.append({'kind':'ledger','migrationId':mid,**shape})
        journal={'status':'ISOLATED_REHEARSAL_PASS','databaseWritesExecuted':True,'sourceDatabase':'ruoyi-vue-pro','rehearsalDatabase':'dcc_intqms_g18_rehearsal','steps':[]}
        for stem,field in [('create-fixed-clone',None),('restore-schema','schemaDump'),('restore-data','dataDump')]:
            out=directory/(stem+'.stdout.txt');err=directory/(stem+'.stderr.txt');out.write_text('',encoding='utf-8');err.write_text('',encoding='utf-8')
            if field:journal['steps'].append({'database':'dcc_intqms_g18_rehearsal','exitCode':0,'dumpSha256':d.sha(prepared[field]),'stdout':str(out),'stderr':str(err)})
        previous=None
        for phase in ['first','repeat']:
            save(phase+'-original-rows.json',after);save(phase+'-history-proof.json',support.compare_original_rows(before,after,allow));save(phase+'-seed33-proof.json',g21.validate_seed_rows(expected,actual,before,after));save(phase+'-ledger19-proof.json',{'oldLedgerUnchanged':True,'newCount':19,'exactNewRows':ledger});save(phase+'-new17-empty.json',{x:0 for x in prepared['newTables']})
            for table,rows in actual.items():(directory/(phase+'-seed33-'+table+'.facts.txt')).write_text('\n'.join(json.dumps(x) for x in rows),encoding='utf-8')
            (directory/(phase+'-ledger19.facts.txt')).write_text('\n'.join(json.dumps(x) for x in ledger),encoding='utf-8')
            for table in prepared['newTables']:(directory/(phase+'-new-table-count-'+table+'.facts.txt')).write_text('0',encoding='utf-8')
            (directory/(phase+'-all-table-identities.facts.txt')).write_text('\n'.join(prepared['schemaTableNames']+prepared['newTables']),encoding='utf-8')
            schema_path=directory/(phase+'-schema.jsonl');schema_path.write_text('\n'.join(json.dumps(x) for x in fact_rows),encoding='utf-8');proof=v.validate(contract,v.read_facts(schema_path),environment,previous);self.assertEqual('POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE',proof['status']);save(phase+'-schema-result.json',proof);previous=proof
            out=directory/(phase+'-nineteen.stdout.txt');err=directory/(phase+'-nineteen.stderr.txt');out.write_text(json.dumps(env)+'\n'+'\n'.join(json.dumps({'taskPhase':a,'migrationId':x['migrationId']}) for x in prepared['entries'] for a in ['sql_begin','sql_complete']),encoding='utf-8');err.write_bytes(b'')
            material=Path(prepared['scripts'][phase]).read_bytes();prefix=Path(prepared['postflight']['environmentQuery']).read_bytes()+b'\n'
            journal['steps'].append({'database':'dcc_intqms_g18_rehearsal','exitCode':0,'stdout':str(out),'stderr':str(err),'materialSqlSha256':hashlib.sha256(material).hexdigest(),'sessionEnvelopeQuerySha256':hashlib.sha256(prefix[:-1]).hexdigest(),'sqlSha256':hashlib.sha256(prefix+material).hexdigest()})
            journal['steps'].append({'phase':phase,'oldRowsUnchanged':True,'exactConfigAdditionCount':33,'exactLedgerAdditionCount':19,'newTablesEmpty':17})
        journal_path=save('driver-receipt.json',journal)
        def sealed():return {'path':str(journal_path),'sha256':d.sha(journal_path),'actualRuntimeReceipt':True,'testFixture':False,'driverSha256':d.FROZEN_G21_SHA,'artifacts':[{'relativePath':x.relative_to(directory).as_posix(),'sha256':d.sha(x)} for x in directory.iterdir() if x!=journal_path]}
        return d,g21,prepared,directory,sealed
    def test_complete_offline_clone_receipt_verifies_actual_hash_chain_not_just_label(self):
        import tempfile
        from unittest.mock import patch
        with tempfile.TemporaryDirectory() as tmp:
            d,g21,prepared,directory,sealed=self.fixture(Path(tmp))
            with patch.object(d,'protected_descriptor',side_effect=lambda row:Path(row['path'])):
                self.assertEqual('REAL_CLONE_FIRST_REPEAT_VERIFIED',d.verify_clone_pass(sealed(),prepared,g21)['status'])
    def test_resealed_raw_ledger_or_empty_count_or_final_table_tampering_rejects(self):
        import tempfile,json
        from unittest.mock import patch
        for name,content in [('first-ledger19.facts.txt','{}'),('repeat-new-table-count-dcc_project_reviewer_config.facts.txt','1'),('first-all-table-identities.facts.txt','other-table')]:
            with tempfile.TemporaryDirectory() as tmp:
                d,g21,prepared,directory,sealed=self.fixture(Path(tmp));(directory/name).write_text(content,encoding='utf-8')
                with patch.object(d,'protected_descriptor',side_effect=lambda row:Path(row['path'])):
                    with self.assertRaises(ValueError):d.verify_clone_pass(sealed(),prepared,g21)

if __name__=="__main__":unittest.main(verbosity=2)
