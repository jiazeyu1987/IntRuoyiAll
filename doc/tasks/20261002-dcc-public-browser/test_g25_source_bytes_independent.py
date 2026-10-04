"""Independent memory-only G25 wrapper probes; never runs main, DB or SDK."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

ROOT_TOOL=Path('C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g25_source_bytes.py')
READER_TASK=Path('C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-public-backend-completion')
def tool():
    spec=importlib.util.spec_from_file_location('g25_independent_subject',ROOT_TOOL)
    module=importlib.util.module_from_spec(spec);sys.modules[spec.name]=module;spec.loader.exec_module(module);return module
def fixture():
    facts=[{'kind':'runtime','database':'ruoyi-vue-pro','serverUuid':'offline-test-server','tenantId':'1'}]
    fresh=[{'kind':'runtime','database':'ruoyi-vue-pro','serverUuid':'offline-test-server'},
      {'kind':'config','id':'28','storage':20,'deleted':0,'config':{'endpoint':'http://127.0.0.1:9000','bucket':'offline-fixture','region':'local','accessKey':'OFFLINE_KEY','accessSecret':'OFFLINE_SECRET','enablePathStyleAccess':True}}]
    for n in range(39):
        ident=str(9007199254740993+n)
        facts.extend([{'kind':'version','id':str(n+1),'tenantId':'1','sourceFileId':ident,'sourceSha256':'ab'*32,'deleted':0},
          {'kind':'storage','id':ident,'configId':'28','name':'fixture.docx','nameHex':'fixture.docx'.encode().hex().upper(),'size':'12','deleted':0}])
        fresh.append({'kind':'storage','id':ident,'configId':'28','nameHex':'fixture.docx'.encode().hex().upper(),'size':'12','key':'offline/key-'+ident,'deleted':0})
        fresh.append(copy.deepcopy(facts[-2]))
    return facts,fresh
class IndependentReview(unittest.TestCase):
    def setUp(self):
        self.d=tool();self.process=patch('subprocess.run',side_effect=AssertionError('NO PROCESS'));self.process.start()
        self.popen=patch('subprocess.Popen',side_effect=AssertionError('NO PROCESS'));self.popen.start()
    def tearDown(self):self.process.stop();self.popen.stop()
    def test_actual_extraction_shape_must_be_supported(self):
        extraction=json.loads((READER_TASK/'g25-readonly-source-bytes-runtime/extraction-receipt.json').read_text(encoding='utf-8'))
        self.assertIn('libs',extraction);self.assertNotIn('libraries',extraction)
        text=ROOT_TOOL.read_text(encoding='utf-8')
        self.assertNotIn("extraction['libraries']",text)
    def test_nonmatch_cannot_persist_arbitrary_error_or_hash_text(self):
        f,_=fixture();expected,_=self.d.expectations(f)
        rows=[{'id':i,'status':'MATCH','actualSha256':'ab'*32,'actualLength':12,'httpStatus':200,'errorCode':None} for i in expected]
        for field in ['errorCode','actualSha256','actualLength','httpStatus']:
            bad=copy.deepcopy(rows);bad[0].update(status='READ_ERROR',actualSha256=None,actualLength=None,httpStatus=None,errorCode='IO_ERROR');bad[0][field]='SECRET_SENTINEL_SHOULD_NEVER_PERSIST'
            with self.subTest(field=field),self.assertRaises(ValueError):self.d.verified_results(expected,bad,1)
    def test_match_wrong_actual_metrics_and_foreign_endpoint_reject(self):
        f,r=fixture();expected,identity=self.d.expectations(f)
        r[1]['config']['endpoint']='http://example.com:9000'
        with self.assertRaises(ValueError):self.d.reader_payload(expected,identity,r)
        rows=[{'id':i,'status':'MATCH','actualSha256':'cd'*32,'actualLength':12,'httpStatus':200,'errorCode':None} for i in expected]
        with self.assertRaises(ValueError):self.d.verified_results(expected,rows,0)
    def test_actual_sealed_shape_and_all_version_drift_reject(self):
        path=Path('C:/IntRuoyiBackups/20261003-dcc-integration/g23-legacy-name-facts.jsonl')
        raw=path.read_bytes();self.assertEqual('8c17e87327b31160e86be9d4c9424670c9a5e8097c1d909c8382f021819a416b',hashlib.sha256(raw).hexdigest())
        facts=[json.loads(line) for line in raw.decode('utf-8').splitlines() if line]
        expected,identity=self.d.expectations(facts)
        _,fresh=fixture();fresh=fresh[:2];fresh[0].update(identity,collectedAtUtc='2026-10-03T00:00:00Z')
        for row in facts:
            if row['kind']=='storage':fresh.append({'kind':'storage','id':row['id'],'configId':row['configId'],'nameHex':row['nameHex'],'size':row['size'],'deleted':row['deleted'],'key':'offline-key-'+row['id']})
            if row['kind']=='version':fresh.append(copy.deepcopy(row))
        self.assertEqual(80,len(fresh));self.assertEqual(39,len(self.d.reader_payload(expected,identity,fresh)['files']))
        for field in ['id','tenantId','masterId','sourceFileId','originalFileId','sourceSha256','projectId','leafId','fileNumber','versionNo','status','processInstanceId','predecessorId','deleted']:
            bad=copy.deepcopy(fresh);row=next(x for x in bad if x['kind']=='version');row[field]='OFFLINE_DIFFERENT'
            with self.subTest(field=field),self.assertRaises(ValueError):self.d.reader_payload(expected,identity,bad)
    def test_reader_manifest_actual_file_set_and_raw_hashes(self):
        manifest=json.loads((READER_TASK/'g25-readonly-source-bytes-delivery-fingerprints.json').read_text(encoding='utf-8'))
        root=READER_TASK.parents[2];runtime=READER_TASK/'g25-readonly-source-bytes-runtime';path=runtime/'extraction-receipt.json'
        self.assertEqual(manifest['libsExtractionReceiptSha256'],hashlib.sha256(path.read_bytes()).hexdigest())
        extraction=json.loads(path.read_text(encoding='utf-8'));self.assertEqual(41,len(extraction['libs']))
        self.assertEqual({r['name'] for r in extraction['libs']},{p.name for p in (runtime/'libs').iterdir()})
        self.assertEqual(17,len(manifest['temporaryCompiledClasses']))
        self.assertEqual({(root/r['path']).resolve() for r in manifest['temporaryCompiledClasses']},{p.resolve() for p in (runtime/'classes').iterdir()})
        for row in extraction['libs']:
            path=runtime/'libs'/row['name'];self.assertEqual(row['sha256'],hashlib.sha256(path.read_bytes()).hexdigest())
        for row in manifest['sourceAssets']+manifest['temporaryCompiledClasses']:
            path=(root/(row.get('file') or row['path'])).resolve();self.assertTrue(path.is_relative_to(READER_TASK.resolve()));self.assertEqual(row['sha256'],hashlib.sha256(path.read_bytes()).hexdigest())
if __name__=='__main__':unittest.main(verbosity=2)
