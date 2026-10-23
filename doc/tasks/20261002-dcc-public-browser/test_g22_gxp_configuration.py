"""Offline contract tests only; never imports a DB driver or executes SQL."""
import copy
import hashlib
import importlib.util
import sys
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent
def module():
    path = HERE / 'g22_gxp_configuration.py'
    if not path.exists(): raise AssertionError('exact25 GxP configuration generator/validator missing')
    spec = importlib.util.spec_from_file_location('g22_preparation', path)
    result = importlib.util.module_from_spec(spec);sys.modules[spec.name] = result;spec.loader.exec_module(result)
    return result

class GxpPreparationTests(unittest.TestCase):
    def setUp(self):
        self.module = module()
        self.package = self.module.prepare_package()
        self.publish = {'tenant_id': 1, 'operation_id': 'dcc.controlled-file.publish', 'policy_version': '2026-09-approved-01',
            'source_locator': self.package['preservedPublish']['observedSourceLocator'], 'active': 1, 'deleted': 0, 'applicability': 'GXP'}
        self.other = {'tenant_id': 2, 'operation_id': self.package['operations'][0]['operationId'], 'policy_version': 'another', 'active': 1, 'deleted': 0}
        self.approval = {'approved': True, 'tenantId': 1, 'policyVersion': self.package['policyVersion'], 'policyHash': self.package['sources']['policy']['sha256'],
            'approvalReference': 'ACTUAL-QUALITY-RECORD-FIXTURE', 'approvedBy': '7', 'approvedAt': '2026-10-03 10:00:00', 'coverageReportHash': self.package['formalCoverage']['reportSha256']}

    def test_exact_26_parser_payload_and_25_scope_keep_publish_pending(self):
        self.assertEqual(26, self.package['candidateDccOperationCount']);self.assertEqual(25, len(self.package['operations']))
        self.assertNotIn('dcc.controlled-file.publish', [row['operationId'] for row in self.package['operations']])
        self.assertIsNone(self.package['qualityApproval']['approvalReference']);self.assertFalse(self.package['executionAuthorized'])
        self.assertTrue(all(row['sourceLocatorVerified'] for row in self.package['operations']))

    def test_first_and_repeated_plan_preserve_publish_other_tenant_and_inputs(self):
        rows = [self.publish, self.other];before = copy.deepcopy(rows)
        plan = self.module.plan(rows, self.package, 1, self.approval)
        self.assertEqual(25, len(plan['insertRows']));self.assertEqual(0, plan['updateCount']);self.assertEqual(rows, before)
        again = self.module.plan(rows + plan['insertRows'], self.package, 1, self.approval)
        self.assertEqual([], again['insertRows']);self.assertEqual(0, again['updateCount']);self.assertEqual(0, again['deleteCount'])

    def test_each_exact_registered_payload_field_conflict_fails_all(self):
        rows = [self.publish] + self.module.plan([self.publish], self.package, 1, self.approval)['insertRows']
        for field in self.module.DB_FIELDS:
            corrupt = copy.deepcopy(rows)
            corrupt[1][field] = corrupt[1][field].upper() if field=='operation_id' else corrupt[1][field]+'-CORRUPTED'
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, 'conflict'):
                self.module.plan(corrupt, self.package, 1, self.approval)

    def test_existing_other_version_disabled_deleted_or_duplicate_never_overwritten(self):
        exact = self.module.plan([self.publish], self.package, 1, self.approval)['insertRows'][0]
        for change in [{'policy_version': 'old'}, {'active': 0}, {'deleted': 1}, {'operation_id': exact['operation_id'].upper()}, {'operation_id':exact['operation_id']+' '}]:
            with self.subTest(change=change), self.assertRaises(ValueError):
                self.module.plan([self.publish, {**exact, **change}], self.package, 1, self.approval)
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            self.module.plan([self.publish, exact, exact], self.package, 1, self.approval)

    def test_wrong_tenant_missing_pending_or_hash_drift_approval_blocks(self):
        with self.assertRaisesRegex(ValueError, 'tenant'): self.module.plan([self.publish], self.package, 2, self.approval)
        for change in [None, {**self.approval, 'approved': False}, {**self.approval, 'approvalReference': 'PENDING-REVIEW'},
            {**self.approval, 'policyHash': 'b'*64}, {**self.approval, 'coverageReportHash': ''}, {**self.approval, 'approvedBy': '0'}]:
            with self.assertRaises(ValueError): self.module.plan([self.publish], self.package, 1, change)

    def test_missing_or_changed_publish_and_package_tamper_block(self):
        with self.assertRaisesRegex(ValueError, 'publish'):self.module.plan([], self.package, 1, self.approval)
        with self.assertRaisesRegex(ValueError, 'publish'):self.module.plan([{**self.publish,'active':0}], self.package, 1, self.approval)
        corrupt=copy.deepcopy(self.package);corrupt['operations'][0]['reasonPolicy']='OPTIONAL'
        with self.assertRaisesRegex(ValueError,'payload'):self.module.plan([self.publish], corrupt, 1, self.approval)

    def test_sql_is_operation_only_guarded_insert_with_no_quality_fabrication(self):
        sql = self.module.render_sql(self.package)
        self.assertIn('quality approval record required', sql);self.assertIn('payload conflict', sql)
        self.assertIn('START TRANSACTION',sql);self.assertIn('ROLLBACK',sql);self.assertIn('GET_LOCK',sql)
        self.assertEqual(1, sql.count('INSERT INTO `gxp_audit_policy_operation`'))
        self.assertNotRegex(sql,r'(?i)(UPDATE|DELETE FROM|REPLACE INTO|INSERT IGNORE)\s+`?gxp_')
        self.assertNotIn('ON DUPLICATE KEY UPDATE',sql);self.assertNotIn('INSERT INTO `gxp_audit_policy_version`',sql)
        self.assertIn('BINARY existing.`source_locator`',sql);self.assertIn('existing.tenant_id=1',sql)
        self.assertIn('@dcc_gxp_quality_approval_reference',sql);self.assertNotIn('PENDING-REVIEW-20261001\'',sql)
        self.assertEqual(25, sql.count('-- exact-operation:'))

    def test_separate_quality_registration_requires_real_confirmed_authorization_and_does_not_copy_old_reference(self):
        self.assertFalse(self.package['qualityRegistration']['executionAuthorized'])
        self.assertIsNone(self.package['qualityRegistration']['approvedBy'])
        with self.assertRaisesRegex(ValueError,'old approval'):
            self.module.validate_approval(self.package,1,{**self.approval,'approvalReference':self.package['preservedOldPolicyApproval']['approvalReference']})
        with self.assertRaisesRegex(ValueError,'quality role'):
            self.module.quality_registration_plan([],self.package,self.approval,quality_role_confirmed=False,write_authorized=True)
        plan=self.module.quality_registration_plan([],self.package,self.approval,quality_role_confirmed=True,write_authorized=True)
        self.assertEqual(1,len(plan['insertRows']));self.assertEqual(0,plan['updateCount'])
        repeated=self.module.quality_registration_plan(plan['insertRows'],self.package,self.approval,quality_role_confirmed=True,write_authorized=True)
        self.assertEqual([],repeated['insertRows'])
        changed=[{**plan['insertRows'][0],'coverage_report_hash':'b'*64}]
        with self.assertRaisesRegex(ValueError,'conflict'):
            self.module.quality_registration_plan(changed,self.package,self.approval,quality_role_confirmed=True,write_authorized=True)
        trailing_version=[{**plan['insertRows'][0],'policy_version':self.package['policyVersion']+' '}]
        with self.assertRaisesRegex(ValueError,'conflict'):
            self.module.quality_registration_plan(trailing_version,self.package,self.approval,quality_role_confirmed=True,write_authorized=True)

    def test_version_template_has_only_single_registration_insert_and_exact_prepared_hashes(self):
        sql=self.module.render_quality_registration_sql(self.package)
        self.assertEqual(1,sql.count('INSERT INTO `gxp_audit_policy_version`'))
        self.assertNotIn('INSERT INTO `gxp_audit_policy_operation`',sql)
        self.assertNotRegex(sql,r'(?i)(UPDATE|DELETE FROM|REPLACE INTO|INSERT IGNORE)\s+`?gxp_')
        self.assertIn(self.package['sources']['policy']['sha256'],sql)
        self.assertIn(self.package['formalCoverage']['reportSha256'],sql)
        self.assertIn('@dcc_gxp_quality_approved_at',sql)
        self.assertIn('quality-role identity confirmed',sql)

    def test_other_non_target_rows_are_untouched_and_partial_exact_replay_inserts_only_missing(self):
        full=self.module.plan([self.publish],self.package,1,self.approval)['insertRows']
        foreign={'tenant_id':1,'operation_id':'dcc.unrelated.existing','policy_version':'legacy','active':1,'deleted':0}
        source=[self.publish,foreign,self.other]+full[:8];copy_before=copy.deepcopy(source)
        result=self.module.plan(source,self.package,1,self.approval)
        self.assertEqual(17,len(result['insertRows']));self.assertEqual(copy_before,source)
        self.assertEqual(0,result['updateCount']);self.assertEqual(0,result['deleteCount'])

    def test_sql_requires_transactional_targets_and_cleans_only_owned_temporary_table(self):
        sql=self.module.render_sql(self.package)
        self.assertIn('DECLARE temporary_owned int DEFAULT 0',sql)
        self.assertIn('IF temporary_owned=1 THEN DROP TEMPORARY TABLE',sql)
        self.assertLess(sql.index('CREATE TEMPORARY TABLE'),sql.index('SET temporary_owned=1'))
        self.assertLess(sql.index('SET temporary_owned=1'),sql.index('INSERT INTO `tmp_dcc_gxp25_tenant1`'))
        self.assertIn("ENGINE='InnoDB'",sql)
        self.assertLess(sql.index('transactional audit table engines required'),sql.index('START TRANSACTION'))
        version=self.module.render_quality_registration_sql(self.package)
        self.assertIn("ENGINE='InnoDB'",version)
        self.assertLess(version.index('transactional quality table engine required'),version.index('START TRANSACTION'))

    def test_generated_durable_report_bytes_match_bound_quality_coverage_hash(self):
        self.module.main()
        actual=(HERE/'g22-formal-policy-source-coverage.txt').read_bytes()
        self.assertEqual(self.package['formalCoverage']['reportSha256'],hashlib.sha256(actual).hexdigest())
        self.assertNotIn(b'\r',actual)

if __name__ == '__main__':unittest.main()
