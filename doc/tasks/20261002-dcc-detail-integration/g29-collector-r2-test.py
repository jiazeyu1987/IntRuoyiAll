import importlib.util
import tempfile
from pathlib import Path
import unittest, json, time, copy, gzip, hashlib
from unittest.mock import Mock
HERE=Path(__file__).resolve().parent
class CollectorTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  spec=importlib.util.spec_from_file_location('g29',HERE/'g29-collector-r2.py');cls.c=importlib.util.module_from_spec(spec);spec.loader.exec_module(cls.c)
 def db(self,rows):
  db=Mock();db.read=Mock(side_effect=rows);return db
 def test_plan_never_invokes_transport(self):
  from unittest.mock import patch
  with patch('sys.argv',['g29-collector.py','--plan']),patch.object(self.c,'RecordingAdapter') as adapter:
   self.c.main();adapter.assert_not_called()
 def test_wrong_database_is_rejected_before_adapter(self):
  from unittest.mock import patch
  with patch.object(self.c,'RecordingAdapter') as adapter:
   with self.assertRaises(ValueError):self.c.collect_fresh_inputs('other',self.c.PROTECTED/'never',{},adapter)
   adapter.assert_not_called()
 def test_direct_sql_read_boundary_rejects_side_effect_functions(self):
  for sql in ['SELECT SLEEP(1);','SELECT LOAD_FILE("private");','SELECT 1 INTO OUTFILE "x";']:
   adapter=self.c.RecordingAdapter.__new__(self.c.RecordingAdapter)
   with self.assertRaises(ValueError):adapter.read(sql)
 def pipeline(self,fail=None,windows_capture=False):
  c=self.c;temp=tempfile.TemporaryDirectory(dir=c.PROTECTED);self.addCleanup(temp.cleanup);root=Path(temp.name);output=root/'new'
  baseline={t:{'columns':['id','payload','generated'],'rows':{'31':'a'*64},'count':1,'aggregateSha256':hashlib.sha256(json.dumps({'31':'a'*64},sort_keys=True,separators=(',',':')).encode()).hexdigest()} for t in c.TABLES}
  class Adapter:
   explicitOfflineTestAdapter=not windows_capture
   capture_receipts=[]
   def __init__(self,source,directory):
    self.source=source;self.dir=directory;self.reads=0;self.directory=directory;self.database=source;self.calls=0;self.capture_receipts=[];self.mysql=Mock();self.mysql.command.return_value=['mock-readonly-local']
   def read(self,sql):
    self.reads+=1
    if windows_capture and sql in [(HERE/'g21-postflight-schema-queries.sql').read_bytes().decode('utf-8'),(HERE/'g21-postflight-environment-query.sql').read_bytes().decode('utf-8')]:return c.RecordingAdapter.read(self,sql)
    if sql==c.runtime_sql():return json.dumps({'kind':'runtime','database':c.SOURCE,'serverUuid':'wrong' if fail=='identity' else c.UUID,'mysqlVersion':'8.0.40'})+'\n'
    if sql==c.writer_sql():return json.dumps({'transactions':1 if fail=='writers' else 0,'otherConnections':0,'enabledEvents':0})+'\n'
    if sql==c.g28.old19_ledger_query():return ''.join(json.dumps({'migrationId':k,'sha256':v,'status':'APPLIED','environment':'test','deleted':0})+'\n' for k,v in c.g28.expected_old19().items())
    return '{}\n'
   def snapshot(self):
    result=copy.deepcopy(baseline)
    if fail=='history' and self.reads>=6:result[c.TABLES[0]]['rows']['31']='b'*64
    return result
   def backup(self,kind,directory):
    content=''
    for table in c.TABLES:content+=f'CREATE TABLE `{table}` (`id` bigint, `payload` varchar(256), `generated` varchar(256) GENERATED ALWAYS AS (`payload`) STORED);\n' if kind=='schema' else f'-- Dumping data for table `{table}`\nINSERT INTO `{table}` (`id`,`payload`) VALUES (1,\'text\');\n'
    content+='-- Dump completed on test\n'
    if fail=='emptybackup':content=''
    path=directory/(kind+'.gz');path.write_bytes(gzip.compress(content.encode()));now=time.time()
    command={'profile':'G28_SCHEMA_SINGLE_TRANSACTION_NO_DATA' if kind=='schema' else 'G28_PROTECTED_ROWS_SINGLE_TRANSACTION_SINGLE_ROW_INSERTS','database':c.SOURCE,'serverUuid':c.UUID,'container':'int-ruoyi-mysql','exitCode':0,'databaseWrites':False,'credentialsPersisted':False,'artifactSha256':c.sha(path),'artifactBytes':path.stat().st_size,'startedEpoch':now,'finishedEpoch':now,'uncompressedBytes':len(content.encode()),'tables':c.TABLES,'singleRowInsertCounts':{t:1 for t in c.TABLES}}
    receipt=directory/(kind+'-command.json');receipt.write_text(json.dumps(command));return {'kind':kind,**c.descriptor(path),'dumpExitCode':0,'dumpCommandReceipt':c.descriptor(receipt)}
  from unittest.mock import patch
  attestation={'database':c.SOURCE,'serverUuid':c.UUID,'allApplicationWritersExcluded':True,'capturedEpoch':time.time()}
  expected_validation={'status':'POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE'}
  envelope={'kind':'runtime','database':c.SOURCE,'serverUuid':c.UUID,'mysqlVersion':'8.0.40'}
  process=Mock(returncode=0,stdout=(json.dumps(envelope)+'\n{"kind":"offline_schema_port"}\n').encode('utf-8'),stderr=b'')
  with patch.object(c,'PROTECTED',root),patch.object(c.g28,'collect_schema',return_value=c.g28.schema.fixture(c.g28.schema.prepare_contract(),absent=True)),patch.object(c.g28.schema.base,'read_facts',return_value={}),patch.object(c.g28.schema.base,'read_environment',return_value={}),patch.object(c.g28.schema.base,'validate',return_value=expected_validation),patch.object(c.subprocess,'run',return_value=process):
   if fail:
    with self.assertRaises(ValueError):c.collect_fresh_inputs(c.SOURCE,output,attestation,Adapter)
    return output
   result=c.collect_fresh_inputs(c.SOURCE,output,attestation,Adapter);return result
 def test_full_fresh_collection_generates_strict_g28_request_without_authorization(self):
  result=self.pipeline();self.assertFalse(result['databaseWrites']);self.assertFalse(result['specificNewMigrationAuthorized'])
  request=json.loads(Path(result['request']['path']).read_text());self.assertIsNone(request['authorization']);self.assertFalse(request['specificNewMigrationAuthorized'])
  for key in ['freshPreflight','freshOriginalBaseline','writerExclusion','freshBackup','prerequisiteProof']:self.assertTrue(Path(request[key]['path']).is_file())
 def test_empty_backup_or_live_writer_drift_cannot_produce_ready_execution_request(self):
  for fail in ['emptybackup','writers','identity','history']:
   out=self.pipeline(fail);self.assertFalse((out/'g28-execution-request.json').exists())
   self.assertTrue((out/'g29-collection-failure.json').is_file())
 def test_snapshot_rows_are_not_truncated_for_large_original_file_scope(self):
  rows={format(i,'x'):'a'*64 for i in range(60000)};snapshot={'columns':['id','payload'],'rows':rows,'count':len(rows),'aggregateSha256':hashlib.sha256(json.dumps(rows,sort_keys=True,separators=(',',':')).encode()).hexdigest()}
  self.c.g28.compare_snapshots({'dcc_controlled_file':snapshot},{'dcc_controlled_file':copy.deepcopy(snapshot)},{});self.assertEqual(60000,len(snapshot['rows']))
 def test_dump_profile_data_stream_counts_rows_and_end_marker(self):
  import io
  c=self.c
  with tempfile.TemporaryDirectory(dir=c.PROTECTED) as tmp:
   folder=Path(tmp);adapter=c.RecordingAdapter.__new__(c.RecordingAdapter);adapter.database=c.SOURCE;adapter.mysql=Mock(docker='docker.exe');adapter.dump_baseline={t:{'columns':['id','payload']} for t in c.TABLES};(folder/'schema.sql.gz').write_bytes(gzip.compress(''.join(f'CREATE TABLE `{t}` (`id` bigint, `payload` varchar(256));\n' for t in c.TABLES).encode()))
   fake=Mock();fake.stdout=io.BytesIO(b'-- Dumping data for table `dcc_controlled_file`\nINSERT INTO `dcc_controlled_file` VALUES (1,\'text\');\n-- Dump completed on test\n');fake.wait.return_value=0;fake.poll.return_value=0
   from unittest.mock import patch
   with patch.object(c.subprocess,'Popen',return_value=fake):artifact=adapter.backup('protected-original-rows',folder)
   command=json.loads(Path(artifact['dumpCommandReceipt']['path']).read_text());self.assertEqual(1,command['singleRowInsertCounts']['dcc_controlled_file']);self.assertEqual(0,command['exitCode']);self.assertFalse(command['credentialsPersisted'])
 def test_rejected_existing_capture_directory_remains_byte_identical(self):
  c=self.c
  with tempfile.TemporaryDirectory(dir=c.PROTECTED) as tmp:
   output=Path(tmp);failure=output/'g29-collection-failure.json';failure.write_bytes(b'old-sealed-failure');sentinel=output/'sentinel';sentinel.write_bytes(b'earlier-facts')
   before={p.name:p.read_bytes() for p in output.iterdir()}
   with self.assertRaises(ValueError):c.collect_fresh_inputs(c.SOURCE,output,{},Mock())
   self.assertEqual(before,{p.name:p.read_bytes() for p in output.iterdir()})
 def test_actual_read_envelope_is_verified_before_unprefixed_payload_is_returned(self):
  c=self.c
  with tempfile.TemporaryDirectory(dir=c.PROTECTED) as tmp:
   adapter=c.RecordingAdapter.__new__(c.RecordingAdapter);adapter.mysql=Mock();adapter.mysql.command.return_value=['docker','readonly'];adapter.database=c.SOURCE;adapter.directory=Path(tmp);adapter.calls=0;adapter.capture_receipts=[]
   envelope={'kind':'runtime','database':c.SOURCE,'serverUuid':c.UUID,'mysqlVersion':'8.0.40'};result=Mock(returncode=0,stdout=(json.dumps(envelope)+'\n31\taaaaa\n').encode(),stderr=b'')
   from unittest.mock import patch
   with patch.object(c.subprocess,'run',return_value=result):body=adapter.read('SELECT id FROM table_name;')
   self.assertEqual('31\taaaaa\n',body);receipt=json.loads(Path(adapter.capture_receipts[0]['path']).read_text());self.assertEqual(envelope,receipt['capture'])
   result.stdout=(json.dumps({**envelope,'serverUuid':'wrong'})+'\n31\taaaaa\n').encode()
   with patch.object(c.subprocess,'run',return_value=result),self.assertRaises(ValueError):adapter.read('SELECT id FROM table_name;')
 def test_formal_post19_query_text_hash_preserves_raw_crlf(self):
  path=HERE/'g21-postflight-schema-queries.sql';payload=path.read_bytes();text=payload.decode('utf-8')
  self.assertEqual(self.c.sha(path),hashlib.sha256(text.encode('utf-8')).hexdigest())
 def test_windows_capture_raw_lf_facts_match_actual_read_receipt_without_offline_bypass(self):
  result=self.pipeline(windows_capture=True);request=json.loads(Path(result['request']['path']).read_text());folder=Path(result['request']['path']).parent
  expected=b'{"kind":"offline_schema_port"}\n'
  self.assertEqual(expected,(folder/'post19-schema-facts.jsonl').read_bytes());self.assertEqual(expected,(folder/'post19-environment.jsonl').read_bytes())
  proof=json.loads(Path(request['prerequisiteProof']['path']).read_text());capture=json.loads(Path(proof['schemaCaptureReceipt']['path']).read_text());actual=json.loads(Path(capture['actualReadReceipt']['path']).read_text())
  self.assertFalse(capture['offlineFixtureOnly']);self.assertEqual(capture['factsSha256'],actual['factsSha256']);self.assertEqual(self.c.sha(HERE/'g21-postflight-schema-queries.sql'),actual['sqlSha256'])
 def test_dump_profile_is_fixed_local_readonly_and_never_exposes_password(self):
  command=self.c.dump_command(self.c.SOURCE,'protected-original-rows','docker.exe')
  self.assertIn('--skip-extended-insert',command);self.assertIn('--no-create-info',command);self.assertNotIn('--password',str(command))
  self.assertNotIn('CREATE DATABASE',str(command));self.assertEqual(self.c.TABLES,command[-7:])
 def test_large_snapshot_is_bounded_by_rows_not_count_label(self):
  text=Path(HERE/'g29-collector-r2.py').read_text(encoding='utf-8');self.assertIn("TABLES=['dcc_controlled_file'",text);self.assertIn("'system_electronic_signature'",text)
if __name__=='__main__':unittest.main()
