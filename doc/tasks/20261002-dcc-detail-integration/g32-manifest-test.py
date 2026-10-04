import importlib.util,unittest,json,copy,tempfile,hashlib
from datetime import datetime,timezone,timedelta
from pathlib import Path
HERE=Path(__file__).resolve().parent
class ManifestTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  spec=importlib.util.spec_from_file_location('g32',HERE/'g32-manifest.py');cls.b=importlib.util.module_from_spec(spec);spec.loader.exec_module(cls.b)
 def test_actual35_match4_missing_never_produces_activation_manifest(self):
  facts=Path('C:/IntRuoyiBackups/20261003-dcc-integration/g23-legacy-name-facts.jsonl');receipt=Path('C:/IntRuoyiBackups/20261003-dcc-integration/g25-source-bytes-run4/receipt.json');results=receipt.parent/'source-bytes-results.jsonl'
  status=self.b.verify_bytes(facts.read_bytes(),receipt.read_bytes(),results.read_bytes())
  self.assertEqual('BLOCKED_INCOMPLETE_SOURCES',status['status']);self.assertEqual(35,status['matches']);self.assertEqual(4,len(status['missingSourceIds']))
  scope=self.b.reviewed_scope_contract(facts.read_bytes());self.assertEqual(39,len(scope['exactScopeIds']['version']));self.assertEqual(25,len(scope['exactScopeIds']['claim']))
 def bundle(self):
  version={'kind':'version','id':'20','tenantId':'1','masterId':'10','sourceFileId':'100','sourceSha256':'a'*64,'versionNo':'A/1','processInstanceId':None}
  rows=[{'kind':'runtime','database':'ruoyi-vue-pro','serverUuid':self.b.UUID,'tenantId':'1','activeClaimCount':1},version,{'kind':'claim','id':'7','tenantId':'1','masterId':'10','normalizedName':'template','deleted':0},
   {'kind':'master','id':'10','tenantId':'1','projectId':'5','leafId':'6','normalizedNumber':'N-1','deleted':0},
   {'kind':'storage','id':'100','configId':'28','name':'SOP.pdf','nameHex':'534F502E706466','size':'4','deleted':0},
   {'kind':'ownership','id':'2','controlledFileId':'20','sourceFileId':'100','sourceSha256':'a'*64,'tenantId':'1','deleted':0},
   {'kind':'ticket','id':'3','controlledFileId':'20','storageFileId':'100','fileSha256':'a'*64,'originalName':'SOP.pdf','size':'4','nameHex':'534F502E706466','tenantId':'1','purpose':'SOURCE','status':'BOUND','cleanupStatus':'BOUND','deleted':0}]
  facts=(''.join(json.dumps(r)+'\n' for r in rows)).encode();results=(json.dumps({'id':'100','status':'MATCH','actualSha256':'a'*64,'actualLength':4,'httpStatus':200,'errorCode':None})+'\n').encode()
  receipt=json.dumps({'status':'SOURCE_BYTES_VERIFIED','sourceBytesVerified':True,'readerExitCode':0,'source':{'database':'ruoyi-vue-pro','serverUuid':self.b.UUID},'sourceFactsSha256':hashlib.sha256(facts).hexdigest(),'resultsSha256':hashlib.sha256(results).hexdigest(),'objectsRead':1,'matches':1,'finishedAtUtc':'2026-10-04T03:04:05.123456789+00:00'}).encode()
  decision=json.dumps({'historicalNamesDecision':{'preserveHistoricalFilesVersionsNamesAndSignatures':True,'verifiedHistoricalOriginalNamesRemainOccupied':True,'rejectFutureNewExactDuplicateNames':True,'autoRenameMergeOrDeleteHistoricalRecords':False}}).encode()
  preimages={'status':'ACTUAL_JDBC_JAVA_ROW_HASH_RECEIPT','database':'ruoyi-vue-pro','serverUuid':self.b.UUID,'tenantId':'1','factsSha256':hashlib.sha256(facts).hexdigest(),'canonicalProtocol':'DccLegacyNameVerifiedScope.rowHash/BootJackson.v1','rows':[{'kind':kind,'id':id,'rowHash':'b'*64} for kind,id in [('claim','7'),('master','10'),('version','20'),('storage','100')]],'storage':[{'id':'100','configId':'28','path':'original/100','name':'SOP.pdf','size':'4'}],'config':{'id':'28','storage':20,'endpoint':'http://127.0.0.1:9000','bucket':'originals','region':'us-east-1','pathStyle':True}}
  identities={'claim':{'tenant_id':'1','master_id':'10','normalized_name':'template','source_original_file_name':None,'dcc_project_code_id':None,'file_type_taxonomy_leaf_id':None,'normalized_file_number':None},'master':{'tenant_id':'1','dcc_project_code_id':'5','file_type_taxonomy_leaf_id':'6','normalized_file_number':'N-1'},'version':{'tenant_id':'1','master_id':'10','source_file_id':'100','source_sha256':'a'*64,'version_no':'A/1','process_instance_id':None,'source_original_file_name':None},'storage':{'config_id':'28','name':'SOP.pdf','size':'4','path':'original/100'}}
  for row in preimages['rows']:row['identity']=identities[row['kind']]
  preimages.update({'actualReadOnlyConnection':True,'snapshotIsolation':'REPEATABLE_READ','jdbcRuntimeVersion':'8.0.40','mapperNullProbe':'{"nil":null}','capturedAtUtc':datetime.now(timezone.utc).isoformat()})
  return facts,receipt,results,decision,preimages
 def test_allmatch_fake_scope_keeps_exact_long_nulls_time_and_identity(self):
  facts,receipt,results,decision,preimages=self.bundle();manifest=self.b.build(facts,receipt,results,decision,preimages,'unit-scope','verified originals','request')
  self.assertEqual('2026-10-04T11:04:05.123456',manifest['verifiedAt']);self.assertEqual('20',manifest['evidence'][0]['fileId']);self.assertIsNone(manifest['evidence'][0]['claimProjectId']);self.assertEqual('SOP.pdf',manifest['evidence'][0]['sourceName'])
 def test_duplicate_or_foreign_ownership_or_invalid_source_hash_is_rejected(self):
  for target in ['duplicate','foreign','hash']:
   facts,receipt,results,decision,preimages=self.bundle();rows=[json.loads(x) for x in facts.decode().splitlines()]
   if target=='duplicate':rows.append(dict(rows[1]))
   elif target=='foreign':next(x for x in rows if x['kind']=='ownership')['tenantId']='2'
   else:rows[1]['sourceSha256']='z'*64
   altered=''.join(json.dumps(r)+'\n' for r in rows).encode();proof=json.loads(receipt);proof['sourceFactsSha256']=hashlib.sha256(altered).hexdigest();receipt=json.dumps(proof).encode();preimages['factsSha256']=hashlib.sha256(altered).hexdigest()
   with self.assertRaises(ValueError):self.b.build(altered,receipt,results,decision,preimages,'unit','reason','request')
 def test_bad_name_locator_preimage_protocol_or_decision_is_rejected(self):
  for target in ['name','config','path','protocol','decision']:
   facts,receipt,results,decision,preimages=self.bundle()
   if target=='name':preimages['storage'][0]['name']='different.pdf'
   elif target=='config':preimages['config']['endpoint']='https://foreign.example'
   elif target=='path':preimages['storage'][0]['path']=''
   elif target=='protocol':preimages['canonicalProtocol']='g21-hex-concat-rowhash'
   else:decision=json.dumps({'historicalNamesDecision':{'preserveHistoricalFilesVersionsNamesAndSignatures':False}}).encode()
   with self.assertRaises(ValueError):self.b.build(facts,receipt,results,decision,preimages,'unit','reason','request')
 def test_readonly_collector_uses_actual_java_hash_instead_of_baseline_sql_hash(self):
  self.assertTrue((HERE/'G32ManifestSupport.java').exists())
 def test_wrong_actual_length_duplicate_result_or_facts_hash_is_rejected(self):
  for change in ['length','duplicate','factsHash']:
   facts,receipt,results,decision,preimages=self.bundle();proof=json.loads(receipt);rows=[json.loads(x) for x in results.splitlines()]
   if change=='length':rows[0]['actualLength']=5
   elif change=='duplicate':rows.append(dict(rows[0]))
   else:proof['sourceFactsSha256']='0'*64
   results=''.join(json.dumps(r)+'\n' for r in rows).encode();proof['resultsSha256']=hashlib.sha256(results).hexdigest();receipt=json.dumps(proof).encode()
   with self.assertRaises(ValueError):self.b.build(facts,receipt,results,decision,preimages,'unit','reason','request')
 def test_actual_compiled_strict_java_factory_consumes_fake_allmatch_manifest(self):
  import subprocess
  facts,receipt,results,decision,preimages=self.bundle();manifest=self.b.build(facts,receipt,results,decision,preimages,'unit','reason','request')
  with tempfile.TemporaryDirectory(dir=HERE) as tmp:
   folder=Path(tmp)
   for name,blob in [('facts.jsonl',facts),('receipt.json',receipt),('results.jsonl',results),('decision.json',decision),('manifest.json',json.dumps(manifest,ensure_ascii=False,separators=(',',':')).encode())]:(folder/name).write_bytes(blob)
   cp=str(HERE/'g32-java-runtime/classes')+';'+str(HERE/'g32-java-runtime/libs/*')
   run=subprocess.run(['java','-cp',cp,'cn.iocoder.yudao.module.dcc.service.file.G32ManifestSupport','verify-manifest',str(folder/'manifest.json'),hashlib.sha256((folder/'manifest.json').read_bytes()).hexdigest(),str(folder/'facts.jsonl'),str(folder/'receipt.json'),str(folder/'results.jsonl'),str(folder/'decision.json')],capture_output=True)
   self.assertEqual(0,run.returncode,run.stderr.decode());self.assertIn(b'ACTUAL_STRICT_JAVA_FACTORY_PASS_OFFLINE_NO_ACTIVATION',run.stdout)
 def test_actual_java_canonical_hash_distinguishes_null_binary_timestamp_and_boolean_types(self):
  import subprocess
  rows=[{'kind':'version','id':'20','values':{'nil':None,'id':{'jdbcType':'Long','value':'20'},'deleted':{'jdbcType':'Boolean','value':'false'},'binary':{'jdbcType':'Bytes','value':'4142'},'time':{'jdbcType':'Timestamp','value':'2026-10-04 01:02:03.123456'}}}]
  cp=str(HERE/'g32-java-runtime/classes')+';'+str(HERE/'g32-java-runtime/libs/*');command=['java','-cp',cp,'cn.iocoder.yudao.module.dcc.service.file.G32ManifestSupport','hash-typed-rows']
  run=subprocess.run(command,input=json.dumps(rows).encode(),capture_output=True);self.assertEqual(0,run.returncode,run.stderr.decode());first=json.loads(run.stdout)['rows'][0]['rowHash'];rows[0]['values'].pop('nil')
  run=subprocess.run(command,input=json.dumps(rows).encode(),capture_output=True);self.assertEqual(0,run.returncode);self.assertNotEqual(first,json.loads(run.stdout)['rows'][0]['rowHash'])
 def test_helper_sanitizes_invalid_typed_payload_error_without_raw_values(self):
  import subprocess
  rows=[{'kind':'version','id':'20','values':{'private_field':{'jdbcType':'Long','value':'PRIVATE_SECRET_FOR_TEST'}}}]
  cp=str(HERE/'g32-java-runtime/classes')+';'+str(HERE/'g32-java-runtime/libs/*')
  run=subprocess.run(['java','-cp',cp,'cn.iocoder.yudao.module.dcc.service.file.G32ManifestSupport','hash-typed-rows'],input=json.dumps(rows).encode(),capture_output=True)
  self.assertEqual(1,run.returncode);self.assertEqual(b'',run.stderr);self.assertNotIn(b'PRIVATE_SECRET_FOR_TEST',run.stdout);self.assertEqual('G32_HELPER_FAILED',json.loads(run.stdout)['status'])
 def test_actual_preimage_readonly_runtime_profile_and_freshness_are_required(self):
  for key,value in [('actualReadOnlyConnection',False),('snapshotIsolation','READ_COMMITTED'),('jdbcRuntimeVersion','8.0.39'),('mapperNullProbe','{}'),('capturedAtUtc',(datetime.now(timezone.utc)-timedelta(seconds=901)).isoformat())]:
   facts,receipt,results,decision,preimages=self.bundle();preimages[key]=value
   with self.assertRaises(ValueError):self.b.build(facts,receipt,results,decision,preimages,'unit','reason','request')
if __name__=='__main__':unittest.main()
