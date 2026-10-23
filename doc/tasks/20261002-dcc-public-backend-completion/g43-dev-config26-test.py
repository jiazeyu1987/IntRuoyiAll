import copy
import importlib.util
import sys
import unittest
from pathlib import Path

HERE=Path(__file__).resolve().parent

def load(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);sys.modules[name]=m;spec.loader.exec_module(m);return m

class DevConfigurationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.old=load('g43_old_qa_generator',HERE/'g28_config26.py');cls.p=cls.old.package()
        cls.dev={'status':'DEVELOPMENT_TEST_ONLY','tenantId':'1','policyVersion':cls.p['policyVersion'],'policyHash':cls.old.POLICY_SHA,'coverageReportHash':cls.old.COVERAGE_SHA}
        cls.publish={'id':1,'tenant_id':1,'operation_id':cls.old.base.PUBLISH,'policy_version':'2026-09-approved-01','source_locator':cls.p['preservedPublish']['observedSourceLocator'],'active':1,'deleted':0,'applicability':'GXP','create_time':'old','update_time':'old'}
        cls.tool=load('g43_dev_target',HERE/'g43-dev-config26.py') if (HERE/'g43-dev-config26.py').exists() else None

    def plan(self,rows):
        if self.tool:return self.tool.plan(rows,self.p,self.dev,self.tool.fixed_environment(),write_authorized=True)
        return self.old.plan(rows,self.p,self.dev,write_authorized=True)

    def test_development26_configuration_requires_no_fabricated_quality_approval(self):
        result=self.plan([self.publish]);self.assertEqual(26,result['insertCount']);self.assertEqual(0,result['qualityVersionInsertCount'])

    def test_partial_target_namespace_is_rejected_not_silently_completed(self):
        if not self.tool:self.fail('new development-only planner required')
        rows=self.plan([self.publish])['insertRows']
        with self.assertRaises(ValueError):self.plan([self.publish]+rows[:1])

    def test_wrong_environment_extra_policy_payload_or_rowtypes_abort_before_execution(self):
        if not self.tool:self.fail('new development-only planner required')
        full=self.plan([self.publish])['insertRows']
        for field in self.old.base.DB_FIELDS:
            bad=copy.deepcopy(full);bad[0][field]+=' '
            with self.subTest(field=field),self.assertRaises(ValueError):self.plan([self.publish]+bad)
        for field,value in [('tenant_id',True),('active',False),('deleted','0')]:
            bad=copy.deepcopy(full);bad[0][field]=value
            with self.assertRaises(ValueError):self.plan([self.publish]+bad)
        for field,value in [('database','foreign'),('serverUuid','foreign'),('connectionHost','remote'),('connectionPort',3306),('timeZone','UTC'),('tenantId',2),('sourceSchemaReady',False)]:
            with self.assertRaises(ValueError):self.tool.plan([self.publish],self.p,self.dev,{**self.tool.fixed_environment(),field:value},write_authorized=True)
        bad=copy.deepcopy(self.p);bad['operations'].append(copy.deepcopy(bad['operations'][0]))
        with self.assertRaises(ValueError):self.tool.plan([self.publish],bad,self.dev,self.tool.fixed_environment(),write_authorized=True)

    def test_exact_first_and_replay_preserve_publish_and_all_quality_records(self):
        rows=[self.publish,{'id':100,'tenant_id':2,'operation_id':'other.operation','payload':'original'}];before=copy.deepcopy(rows)
        result=self.plan(rows);self.assertEqual(rows,before);inserted=[{**r,'id':i+101}for i,r in enumerate(result['insertRows'])];after=rows+inserted
        self.assertEqual(0,self.plan(after)['insertCount'])
        versions=[{'id':1,'tenant_id':1,'policy_version':'old-approved','approval_reference':'original quality fact'}]
        receipt=self.tool.postflight(rows,after,versions,copy.deepcopy(versions),self.p,self.dev,self.tool.fixed_environment(),expected_inserts=26)
        self.assertEqual(0,receipt['qualityVersionInsertCount']);self.assertTrue(receipt['originalOperationsUnchanged'])
        self.tool.postflight(after,after,versions,versions,self.p,self.dev,self.tool.fixed_environment(),expected_inserts=0)
        changed=copy.deepcopy(after);changed[0]['update_time']='changed'
        with self.assertRaises(ValueError):self.tool.postflight(rows,changed,versions,versions,self.p,self.dev,self.tool.fixed_environment(),expected_inserts=26)
        with self.assertRaises(ValueError):self.tool.postflight(rows,after,versions,versions+[{'id':2,'policy_version':'fabricated'}],self.p,self.dev,self.tool.fixed_environment(),expected_inserts=26)

    def test_extra_namespace_case_accent_space_other_version_or_duplicate_rejects(self):
        full=self.plan([self.publish])['insertRows']
        for change in [{'operation_id':full[0]['operation_id'].upper()},{'operation_id':full[0]['operation_id']+' '},{'operation_id':full[0]['operation_id'].replace('activate','activaté')},{'policy_version':'other'},{'active':0},{'deleted':1}]:
            bad=copy.deepcopy(full);bad[0].update(change)
            with self.assertRaises(ValueError):self.plan([self.publish]+bad)
        with self.assertRaises(ValueError):self.plan([self.publish]+full+[full[0]])
        with self.assertRaises(ValueError):self.tool.plan([self.publish],self.p,{**self.dev,'approvedBy':'admin'},self.tool.fixed_environment(),write_authorized=True)
        with self.assertRaises(ValueError):self.tool.plan([self.publish],self.p,self.dev,self.tool.fixed_environment(),write_authorized=False)

    def test_sql_is_only_exact26insert_and_failfast_transaction_not_quality_approval(self):
        sql=self.tool.sql(self.p)
        self.assertEqual(26,sql.count('-- exact-operation:'));self.assertEqual(1,sql.count('INSERT INTO gxp_audit_policy_operation'))
        self.assertNotIn('INSERT INTO gxp_audit_policy_version',sql);self.assertNotIn('approved_by',sql);self.assertNotIn('quality_approver',sql)
        statements='\n'.join(line for line in sql.splitlines()if not line.lstrip().startswith('--'))
        self.assertNotRegex(statements,r'(?i)(INSERT IGNORE|REPLACE INTO|ON DUPLICATE KEY UPDATE|DROP PROCEDURE IF EXISTS|UPDATE\s+gxp_|DELETE FROM)')
        self.assertIn('ROLLBACK;',sql);self.assertIn('RESIGNAL;',sql);self.assertIn('START TRANSACTION;',sql);self.assertIn('SET changed_rows=ROW_COUNT();',sql)
        self.assertIn('existing_rows NOT IN (0,26)',sql);self.assertIn('changed_rows<>26',sql);self.assertIn('uk_gxp_audit_policy_operation',sql)
        for column in self.old.base.DB_FIELDS:self.assertIn('BINARY existing.`'+column+'` <=> BINARY candidate.`'+column+'`',sql)
        self.assertLess(sql.index("SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='post-insert exact26 payload failed'"),sql.index('  COMMIT;'))
        self.assertIn('@@server_uuid',sql);self.assertIn("@@session.time_zone<>'+08:00'",sql);self.assertIn('STRICT_TRANS_TABLES',sql)

    def test_authoritative_candidate_and_frozen_g22_g28_inputs_are_unchanged(self):
        self.old.verify_originals();self.assertEqual(self.old.POLICY_SHA,self.old.base.sha(self.old.base.POLICY));self.assertEqual(26,len(self.p['operations']))
        self.assertEqual('2026-10-dcc-integration-01',self.p['policyVersion']);self.assertEqual(self.old.COVERAGE_SHA,self.p['formalCoverage']['reportSha256'])

if __name__=='__main__':unittest.main()
