"""Independent offline adversarial tests; no production transport or database initialization."""
import copy,hashlib,importlib.util,json,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
HERE=Path(__file__).resolve().parent
OTHER=HERE.parent/'20261002-dcc-detail-integration'
spec=importlib.util.spec_from_file_location('g28_independent_target',OTHER/'g28-driver.py');d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
class IndependentDriverReview(unittest.TestCase):
 def test_missing_or_false_authorization_never_initializes_actual_transport(self):
  def forbidden(*a,**k):raise AssertionError('actual transport must never initialize')
  with patch.object(d,'validate_request',side_effect=AssertionError('no proof validation on missing technical flag')):
   for flag in [False,None,0,'true']:
    with self.subTest(flag=flag),self.assertRaises(ValueError):d.execute({},flag,forbidden)
  for request in [{},{'version':'G28-EXEC-1','database':d.SOURCE,'specificNewMigrationAuthorized':False},{'version':'G28-EXEC-1','database':d.SOURCE,'specificNewMigrationAuthorized':True}]:
   with self.subTest(request=request),self.assertRaises((ValueError,TypeError)):d.execute(request,True,forbidden)
 def test_duplicate_checks_index_prefix_columnordinal_and_literal_case_rejected(self):
  contract=d.schema.prepare_contract();base=d.schema.fixture(contract)
  modifications=[]
  row=copy.deepcopy(base);checks=[x for x in row if x['kind']=='check'];checks[-1]['expression']=checks[0]['expression'];modifications.append(row)
  row=copy.deepcopy(base);next(x for x in row if x['kind']=='index')['prefix']=1;modifications.append(row)
  row=copy.deepcopy(base);next(x for x in row if x['kind']=='column')['ordinal']=99;modifications.append(row)
  row=copy.deepcopy(base);c=next(x for x in row if x['kind']=='check' and "'MATCH'" in x['expression']);c['expression']=c['expression'].replace("'MATCH'","'match'");modifications.append(row)
  for rows in modifications:
   with self.subTest(rows=rows[1]['table']),self.assertRaises(ValueError):d.schema.validate_facts(contract,rows,'post')
 def test_plain_forged_clone_pass_status_rejected_without_real_phase_artifacts(self):
  forged={'status':'G28_OWNED_EXISTING_CLONE_FIRST_REPEAT_PASS','database':d.CLONE,'migrationId':d.MIGRATION,'sqlSha256':d.SQL_SHA,'databaseWriteAttempted':True,'steps':[{'phase':'first'},{'phase':'repeat'}]}
  with self.assertRaises(ValueError):d.validate_completed_journal(forged,d.CLONE)
 def test_check_introducer_normalization_never_strips_literal_underscore_payload(self):
  self.assertNotEqual(d.schema.check_ast("reservation_kind='LEGACY_GROUP'"),d.schema.check_ast("reservation_kind='LEGACY'"))
  self.assertNotEqual(d.schema.check_ast("status='PRE_PARED'"),d.schema.check_ast("status='PRE'"))
  self.assertEqual(d.schema.check_ast("reservation_kind='LEGACY_GROUP'"),d.schema.check_ast("reservation_kind=_utf8mb4'LEGACY_GROUP'"))
 def test_self_consistent_but_wrong_material_and_missing_baseline_cannot_be_clone_proof(self):
  # This fake journal has self-hashed phase artifacts but contains no authorized DDL or true baseline proof.
  # It must never be accepted as a completed rehearsal before SOURCE writes.
  with tempfile.TemporaryDirectory(dir=HERE) as temp:
   directory=Path(temp);contract=d.schema.prepare_contract();rows=d.schema.fixture(contract);rows[0]['database']=d.CLONE
   journal={'status':'G28_OWNED_EXISTING_CLONE_FIRST_REPEAT_PASS','database':d.CLONE,'migrationId':d.MIGRATION,'sqlSha256':d.SQL_SHA,'databaseWriteAttempted':True,'steps':[]}
   for phase in ['first','repeat']:
    material=directory/(phase+'.sql');material.write_bytes(b'SELECT 1; -- unrelated material, no DDL or ledger\n')
    out=directory/(phase+'.out');out.write_text(json.dumps({'g28Phase':'SESSION_IDENTITY','database':d.CLONE,'intendedDatabase':d.CLONE,'serverUuid':d.schema.UUID,'mysqlVersion':'8.0.40','pageSize':16384,'rowFormat':'dynamic'})+'\n'+json.dumps({'g28Phase':phase.upper()+'_COMPLETE','migrationId':d.MIGRATION})+'\n',encoding='utf-8')
    error=directory/(phase+'.err');error.write_bytes(b'');post=directory/(phase+'-post.json');post.write_text(json.dumps(rows),encoding='utf-8');history=directory/(phase+'-history.json');history.write_text(json.dumps({table:{} for table in d.ORIGINAL_TABLES}),encoding='utf-8')
    journal['steps'].append({'phase':phase,'phaseStatus':'POSTFLIGHT_AND_ORIGINAL_ROWS_PASS','material':d.desc(material),'materialSha256':d.sha(material),'execution':{'exitCode':0,'database':d.CLONE,'sqlSha256':d.sha(material),'stdout':d.desc(out),'stderr':d.desc(error)},'postflight':d.desc(post),'originalAfter':d.desc(history)})
   with patch.object(d,'PROTECTED',directory):
    with self.assertRaises(ValueError):d.validate_completed_journal(journal,d.CLONE)
if __name__=='__main__':unittest.main()
