import copy, importlib.util, sys, unittest
from pathlib import Path
HERE=Path(__file__).resolve().parent
def module():
 p=HERE/'g28_config26.py'
 if not p.exists():raise AssertionError('G28 exact26 generator missing')
 spec=importlib.util.spec_from_file_location('g28_test_target',p);m=importlib.util.module_from_spec(spec);sys.modules[spec.name]=m;spec.loader.exec_module(m);return m
class Config26Tests(unittest.TestCase):
 def setUp(self):
  self.m=module();self.p=self.m.package();self.publish={'tenant_id':1,'operation_id':'dcc.controlled-file.publish','policy_version':'2026-09-approved-01','source_locator':self.p['preservedPublish']['observedSourceLocator'],'active':1,'deleted':0,'applicability':'GXP'}
  self.a={'approved':True,'tenantId':1,'policyVersion':self.p['policyVersion'],'policyHash':self.p['sources']['policy']['sha256'],'coverageReportHash':self.p['formalCoverage']['reportSha256'],'approvedBy':'7','approvedAt':'2026-10-03 12:00:00','approvalReference':'OFFLINE-EXTERNAL-QA-RECORD','qualityRoleBasis':'OFFLINE genuine role proof','qualitySignatureEvidenceReference':'OFFLINE real signature reference','qualityRoleId':'9','timeZone':'Asia/Shanghai'}
 def test_missing_real_permission_and_empty_quality_never_validate(self):
  with self.assertRaises(ValueError):self.m.plan([self.publish],self.p,self.a,write_authorized=False)
  for change in [{'approved':False},{'approvedBy':'admin'},{'approvedAt':'2026-99-99 99:99:99'},{'qualityRoleId':None},{'qualitySignatureEvidenceReference':None},{'approvalReference':self.p['preservedOldPolicyApproval']['approvalReference']},{'policyHash':'0'*64},{'coverageReportHash':'1'*64}]:
   with self.subTest(change=change),self.assertRaises(ValueError):self.m.plan([self.publish],self.p,{**self.a,**change},write_authorized=True)
 def test_exact26_first_partial_repeat_preserve_all_inputs(self):
  rows=[self.publish,{'tenant_id':2,'operation_id':'dcc.unrelated','active':1}];old=copy.deepcopy(rows);r=self.m.plan(rows,self.p,self.a,write_authorized=True);self.assertEqual(26,r['insertCount']);self.assertEqual(rows,old)
  self.assertEqual(0,self.m.plan(rows+r['insertRows'],self.p,self.a,write_authorized=True)['insertCount'])
  self.assertEqual(18,self.m.plan(rows+r['insertRows'][:8],self.p,self.a,write_authorized=True)['insertCount'])
 def test_each14fields_and_rowtypes_conflict_abort_batch(self):
  full=self.m.plan([self.publish],self.p,self.a,write_authorized=True)['insertRows']
  for key in self.m.base.DB_FIELDS:
   wrong=copy.deepcopy(full);wrong[0][key]=wrong[0][key]+' '
   with self.subTest(key=key),self.assertRaises(ValueError):self.m.plan([self.publish]+wrong,self.p,self.a,write_authorized=True)
  for key,value in [('active',False),('deleted','0'),('tenant_id',True)]:
   wrong=copy.deepcopy(full);wrong[0][key]=value
   with self.subTest(key=key),self.assertRaises(ValueError):self.m.plan([self.publish]+wrong,self.p,self.a,write_authorized=True)
 def test_other_version_state_duplicate_and_old25scope_reject(self):
  exact=self.m.plan([self.publish],self.p,self.a,write_authorized=True)['insertRows'][0]
  for change in [{'policy_version':'old'},{'active':0},{'deleted':1},{'operation_id':exact['operation_id'].upper()}]:
   with self.subTest(change=change),self.assertRaises(ValueError):self.m.plan([self.publish,{**exact,**change}],self.p,self.a,write_authorized=True)
  with self.assertRaises(ValueError):self.m.plan([self.publish,exact,exact],self.p,self.a,write_authorized=True)
  changed=copy.deepcopy(self.p);changed['operations'].pop()
  with self.assertRaises(ValueError):self.m.plan([self.publish],changed,self.a,write_authorized=True)
 def test_sql_exact26_and_separate_qualitymax1_safeguards(self):
  s=self.m.operation_sql(self.p);q=self.m.quality_sql(self.p)
  self.assertEqual(26,s.count('-- exact-operation:'));self.assertEqual(1,s.count('INSERT INTO `gxp_audit_policy_operation`'));self.assertNotIn('INSERT INTO `gxp_audit_policy_version`',s)
  self.assertEqual(1,q.count('INSERT INTO `gxp_audit_policy_version`'));self.assertNotIn('INSERT INTO `gxp_audit_policy_operation`',q)
  for text in [s,q]:
   self.assertNotRegex(text,r'(?i)(INSERT IGNORE|REPLACE INTO|ON DUPLICATE KEY UPDATE|DROP PROCEDURE IF EXISTS)');self.assertIn('RESIGNAL',text);self.assertIn('ROLLBACK',text);self.assertIn("utf8mb4_unicode_ci",text);self.assertIn('STRICT_',text);self.assertIn('@@session.time_zone',text);self.assertIn('system_user_role',text);self.assertIn('@dcc_gxp26_quality_signature_evidence',text)
  self.assertIn('@dcc_gxp26_tenant1_write_authorized',s);self.assertNotIn('@dcc_gxp_tenant1_write_authorized',s);self.assertIn('changed_rows>26',s);self.assertIn('candidate scope not26',s)
  for text in [s,q]:self.assertIn('CHAR_LENGTH(quality_reference)>256',text);self.assertNotIn('CHAR_LENGTH(quality_reference)>266',text)
  for text in [s,q]:self.assertIn('@dcc_gxp26_quality_role_basis',text);self.assertIn("LOWER(@dcc_gxp26_quality_signature_evidence) LIKE '%pending%'",text);self.assertIn("@dcc_gxp26_quality_signature_evidence LIKE '%尚未%'",text)
 def test_quality_existingexact0_and_conflict_no_overwrite(self):
  with self.assertRaises(ValueError):self.m.quality_plan([],self.p,self.a,write_authorized=False)
  r=self.m.quality_plan([],self.p,self.a,write_authorized=True);self.assertEqual(1,len(r['insertRows']));self.assertEqual([],self.m.quality_plan(r['insertRows'],self.p,self.a,write_authorized=True)['insertRows'])
  with self.assertRaises(ValueError):self.m.quality_plan([{**r['insertRows'][0],'approved_by':8}],self.p,self.a,write_authorized=True)
 def test_original_g22_all_bytes_remain_immutable_and_scopehasnew1(self):
  self.m.verify_originals();self.assertEqual(26,len(self.p['operations']));self.assertIn('dcc.controlled-file.legacy-name-occupancy.activate',{x['operationId'] for x in self.p['operations']});self.assertFalse(self.p['executionAuthorized']);self.assertFalse(self.p['qualityRegistration']['executionAuthorized'])
 def test_postflight_checks_all_existing_metadata_and_exact_count_namespace(self):
  before=[{**self.publish,'id':1,'create_time':'actual old','update_time':'actual old'}]
  new=self.m.plan(before,self.p,self.a,write_authorized=True)['insertRows'];after=before+[{**r,'id':i+2} for i,r in enumerate(new)]
  version={**self.m.quality_plan([],self.p,self.a,write_authorized=True)['insertRows'][0],'id':1}
  actual=self.m.postflight(before,after,[],[version],self.p,self.a,expected_operation_inserts=26,expected_version_inserts=1);self.assertTrue(actual['originalRowsUnchanged'])
  changed=copy.deepcopy(after);changed[0]['update_time']='changed'
  with self.assertRaises(ValueError):self.m.postflight(before,changed,[],[version],self.p,self.a,expected_operation_inserts=26,expected_version_inserts=1)
  with self.assertRaises(ValueError):self.m.postflight(before,after+[{'id':99,'tenant_id':2,'operation_id':'unrelated'}],[],[version],self.p,self.a,expected_operation_inserts=26,expected_version_inserts=1)
  with self.assertRaises(ValueError):self.m.postflight(after,after,[version],[version,{'id':2,'tenant_id':2,'policy_version':'other','policy_hash':'a'*64}],self.p,self.a,expected_operation_inserts=0,expected_version_inserts=1)
if __name__=='__main__':unittest.main()
