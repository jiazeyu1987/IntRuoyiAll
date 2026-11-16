"""Exercise the feedback-rule migration on an isolated, task-owned MySQL."""
import re
import unittest
import uuid
import test_tenant_notification_resources_mysql as harness

ROOT = harness.ROOT


class FeedbackCodeRuleMySqlTest(unittest.TestCase):
    setUpClass = classmethod(harness.TenantNotificationResourcesMySqlTest.setUpClass.__func__)
    sql = classmethod(harness.TenantNotificationResourcesMySqlTest.sql.__func__)
    query = harness.TenantNotificationResourcesMySqlTest.query

    def setUp(self):
        self.database = 'feedback_' + uuid.uuid4().hex
        self.sql(f'CREATE DATABASE {self.database};')
        self.addCleanup(lambda: self.sql(f'DROP DATABASE {self.database};'))
        self.query('CREATE TABLE system_tenant (id bigint PRIMARY KEY,deleted bit DEFAULT 0);'
                   'INSERT INTO system_tenant VALUES (1,0),(186,0),(187,1);'
                   'CREATE TABLE mes_pro_feedback (tenant_id bigint,code varchar(255));')
        schema = (ROOT/'sql/mysql/20260512_mes_base_schema.sql').read_text(encoding='utf-8')
        for table in ['mes_md_auto_code_rule', 'mes_md_auto_code_part', 'mes_md_auto_code_record']:
            self.query(re.search(r'CREATE TABLE IF NOT EXISTS `' + table + r'`[\s\S]*?;', schema).group(0))
        self.query('ALTER TABLE mes_md_auto_code_rule ADD project_id bigint NULL;')

    def migrate(self, check=True):
        return self.query((ROOT/'sql/mysql/20261112_mes_feedback_code_rule.sql').read_text(encoding='utf-8'), check)

    def test_missing_rule_initialized_and_rerun_preserves_customizations(self):
        self.migrate()
        self.assertEqual('1\n186\n', self.query('SELECT tenant_id FROM mes_md_auto_code_rule ORDER BY tenant_id').stdout)
        self.query("UPDATE mes_md_auto_code_part SET fix_character='CUSTOM-' WHERE tenant_id=1 AND sort=1")
        self.migrate()
        self.assertEqual('2\n', self.query('SELECT COUNT(*) FROM mes_md_auto_code_rule').stdout)
        self.assertEqual('CUSTOM-\n', self.query('SELECT fix_character FROM mes_md_auto_code_part WHERE tenant_id=1 AND sort=1').stdout)
        self.assertEqual('1\n', self.query('SELECT serial_start_no FROM mes_md_auto_code_part WHERE tenant_id=186 AND sort=2').stdout)

    def test_historical_numbers_including_deleted_generation_records_are_not_reused(self):
        self.query("INSERT INTO mes_pro_feedback VALUES (186,'FB-000042'),(1,'FB-999999');"
                   "INSERT INTO mes_md_auto_code_record (tenant_id,result,deleted) VALUES (186,'FB-000055',1)")
        self.migrate()
        self.assertEqual('56\n', self.query('SELECT serial_start_no FROM mes_md_auto_code_part WHERE tenant_id=186 AND sort=2').stdout)

    def test_overflow_aborts_without_partial_rules(self):
        self.query("INSERT INTO mes_pro_feedback VALUES (186,'FB-2147483647')")
        result = self.migrate(False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('FEEDBACK_CODE_SERIAL_OUT_OF_RANGE', result.stderr)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM mes_md_auto_code_rule').stdout)


if __name__ == '__main__':
    unittest.main()
