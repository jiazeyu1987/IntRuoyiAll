"""Finite offline reviewed-FIRST resume boundary tests. Transport explicitly isolated; no actual DB."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import time
import unittest
from unittest.mock import patch

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('g39_driver_tested',HERE/'g39-driver.py');d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
ACTUAL=Path('C:/IntRuoyiBackups/20261003-dcc-integration/g39-clone-migration-run/first-postflight-captured-after-validator-stop.json')

class ReviewedResumeTest(unittest.TestCase):
    def fixture(self,defect=None):
        temp=tempfile.TemporaryDirectory(dir=HERE);self.addCleanup(temp.cleanup);folder=Path(temp.name);destination=folder/'resume'
        def save(name,value):
            p=folder/name;p.write_text(json.dumps(value),encoding='utf-8');return d.desc(p)
        original={t:{'columns':['id','payload'],'rows':{'31':'a'*64},'count':1,'aggregateSha256':hashlib.sha256(json.dumps({'31':'a'*64},sort_keys=True,separators=(',',':')).encode()).hexdigest()} for t in d.ORIGINAL_TABLES}
        current=copy.deepcopy(original);current['infra_release_migration']['rows']['32']='b'*64;current['infra_release_migration']['count']=2
        current['infra_release_migration']['aggregateSha256']=hashlib.sha256(json.dumps(current['infra_release_migration']['rows'],sort_keys=True,separators=(',',':')).encode()).hexdigest()
        before=save('original.json',original);operation='dcc-g39-offline-resume';material=d.compose_material('first',operation,d.CLONE);p=folder/'first.sql';p.write_bytes(material)
        stdout=folder/'first.stdout';stderr=folder/'first.stderr';stderr.write_bytes(b'')
        stdout.write_text(json.dumps({'g28Phase':'SESSION_IDENTITY','database':d.CLONE,'intendedDatabase':d.CLONE,'serverUuid':d.schema.UUID,'mysqlVersion':'8.0.40','pageSize':16384,'rowFormat':'dynamic'})+'\n'+json.dumps({'g28Phase':'FIRST_COMPLETE','migrationId':d.MIGRATION})+'\n',encoding='utf-8')
        first={'phase':'first','material':d.desc(p),'materialSha256':d.sha(p),'execution':{'exitCode':0,'database':d.CLONE,'sqlSha256':d.sha(p),'stdout':d.desc(stdout),'stderr':d.desc(stderr),'noRetry':True},'phaseStatus':'SQL_RETURNED_NOT_POSTFLIGHT_VERIFIED'}
        stopped={'status':'STOPPED_DDL_PARTIAL_COMMIT_POSSIBLE_REVIEW_REQUIRED','database':d.CLONE,'operationId':operation,'originalBefore':before,'migrationId':d.MIGRATION,'sqlSha256':d.SQL_SHA,'steps':[first],'databaseWriteAttempted':True,'automaticRetry':False,'implicitDdlCommitPossible':True,'errorType':'ValueError'}
        if defect=='exit':stopped['steps'][0]['execution']['exitCode']=9
        stop=save('stopped.json',stopped);auth=save('actual-authorization-fixture.json',{'offlineFixtureOnly':True})
        review=save('review.json',{'status':'ROOT_REVIEWED_FIRST_VALIDATOR_STOP_RESUME_REPEAT_ONLY','database':d.CLONE,'serverUuid':d.schema.UUID,'migrationId':d.MIGRATION,'migrationSha256':d.SQL_SHA,'stoppedJournalSha256':stop['sha256'],'authorizationSha256':auth['sha256'],'firstReplayForbidden':True,'repeatOnly':True,'originalStopReason':'unsupported CHECK token','sourceReviewReference':'isolated test review, not actual approval','capturedEpoch':time.time()})
        rows=json.loads(ACTUAL.read_text(encoding='utf-8-sig'));captured=save('captured.json',rows)
        request={'database':d.CLONE,'operationId':operation,'existingFirstResumeInputs':True,'stoppedFirstJournal':stop,'resumeReview':review,'authorization':auth,'stoppedPostflight':captured}
        for k in ['freshPreflight','freshOriginalBaseline','writerExclusion','freshBackup','prerequisiteProof']:request[k]=save(k+'.json',{'offlineFixtureOnly':True})
        old19=[{'migrationId':'isolated-prerequisite'}];proofs={'freshOriginalBaseline':{'tables':current},'prerequisiteProof':{'originalNineteenLedger':old19}}
        ledger={'releaseTag':operation,'operationId':operation,'file':f'sql/mysql/{d.MIGRATION}.sql','migrationId':d.MIGRATION,'sha256':d.SQL_SHA,'status':'APPLIED','environment':'test','deleted':0,'tenantId':'0','creator':'dcc-g28-local-task','updater':'dcc-g28-local-task','startedAt':'2026-10-04 00:00:00','finishedAt':'2026-10-04 00:00:00','errorMessage':None}
        class FakeTransport:
            def __init__(self):self.runs=[]
            def read(self,sql):
                if sql==d.writer_query():return json.dumps({'transactions':1 if defect=='writer' else 0,'otherConnections':0,'enabledEvents':0})+'\n'
                if sql==d.old19_ledger_query():return ''.join(json.dumps(r)+'\n'for r in old19)
                if sql==d.new_ledger_query():return json.dumps(ledger)
                actual=[r for r in rows if (r['kind']=='row_count')==(sql==d.schema.row_counts_sql())]
                return ''.join(json.dumps(r)+'\n'for r in actual)
            def snapshot(self,tables):
                v=copy.deepcopy(current)
                if defect=='history':v['dcc_controlled_file']['rows']['31']='c'*64
                return v
            def run(self,sql,private,phase):
                self.runs.append((phase,sql));out=private/'repeat.stdout';err=private/'repeat.stderr';err.write_bytes(b'')
                out.write_text(json.dumps({'g28Phase':'SESSION_IDENTITY','database':d.CLONE,'intendedDatabase':d.CLONE,'serverUuid':d.schema.UUID,'mysqlVersion':'8.0.40','pageSize':16384,'rowFormat':'dynamic'})+'\n'+json.dumps({'g28Phase':'REPEAT_COMPLETE','migrationId':d.MIGRATION})+'\n',encoding='utf-8')
                return {'exitCode':9 if defect=='repeat-failure' else 0,'database':d.CLONE,'sqlSha256':hashlib.sha256(sql).hexdigest(),'stdout':d.desc(out),'stderr':d.desc(err),'noRetry':True}
        return request,proofs,destination,FakeTransport()

    def invoke(self,defect=None):
        request,proofs,destination,fake=self.fixture(defect)
        with patch.object(d,'validate_request',return_value=(proofs,destination)),patch.object(d,'prior_proofs'),patch.object(d,'policy_closure'):
            if defect:
                with self.assertRaises(ValueError):d.resume_reviewed_first(request,True,lambda _:fake)
                result=None
            else:result=d.resume_reviewed_first(request,True,lambda _:fake)
        return fake,destination,result

    def test_exact_resume_executes_repeat_only_and_preserves_original_first_receipt(self):
        fake,folder,result=self.invoke();self.assertEqual(['repeat'],[p for p,_ in fake.runs]);self.assertNotIn(b'INSERT INTO infra_release_migration',fake.runs[0][1]);self.assertFalse(result['firstReplayExecuted'])
        self.assertTrue(result['steps'][0]['firstVerifiedAfterValidatorStop']);self.assertEqual('G28_OWNED_EXISTING_CLONE_FIRST_REPEAT_PASS',result['status']);d.validate_completed_journal(result,d.CLONE)
        self.assertEqual('STOPPED_DDL_PARTIAL_COMMIT_POSSIBLE_REVIEW_REQUIRED',d.read(d.verify_descriptor(result['originStoppedJournal']))['status'])

    def test_missing_flag_or_wrong_database_never_initializes_transport(self):
        calls=[]
        for request,flag in [({},False),({'database':d.SOURCE,'existingFirstResumeInputs':True},True)]:
            with self.assertRaises(ValueError):d.resume_reviewed_first(request,flag,lambda db:calls.append(db))
        self.assertEqual([],calls)

    def test_first_failed_exit_writer_or_history_drift_never_runs_repeat(self):
        for defect in ['exit','writer','history']:
            fake,_,_=self.invoke(defect);self.assertEqual([],fake.runs)

    def test_repeat_failure_is_one_attempt_never_first_retry_or_auto_restore(self):
        fake,folder,_=self.invoke('repeat-failure');self.assertEqual(['repeat'],[p for p,_ in fake.runs]);self.assertEqual('STOPPED_REVIEWED_REPEAT_PARTIAL_COMMIT_POSSIBLE_REVIEW_REQUIRED',d.read(folder/'driver-receipt.json')['status'])

    def test_forged_stopped_sha_and_material_cannot_authorize_manual_resume(self):
        request,_,_,_=self.fixture();bad=copy.deepcopy(request['stoppedFirstJournal']);bad['sha256']='0'*64
        with self.assertRaises(ValueError):d.validate_stopped_first(bad,d.CLONE,request['operationId'])
        stopped=d.read(d.verify_descriptor(request['stoppedFirstJournal']));p=d.verify_descriptor(stopped['steps'][0]['material']);p.write_bytes(d.compose_material('repeat',request['operationId'],d.CLONE));stopped['steps'][0]['material']=d.desc(p);stopped['steps'][0]['materialSha256']=d.sha(p)
        stop=d.verify_descriptor(request['stoppedFirstJournal']);stop.write_text(json.dumps(stopped));
        with self.assertRaises(ValueError):d.validate_stopped_first(d.desc(stop),d.CLONE,request['operationId'])

    def test_normal_source_first_repeat_material_and_validator_use_same_source_sql(self):
        for phase in ['first','repeat']:
            material=d.compose_material(phase,'dcc-g39-offline-source',d.SOURCE);self.assertIn(d.SQL_PATH.read_bytes(),material)
            self.assertEqual(1 if phase=='first' else 0,material.count(b'INSERT INTO infra_release_migration'))
        self.assertEqual('g39-schema.py',Path(d.schema.__file__).name)

if __name__=='__main__':unittest.main()
