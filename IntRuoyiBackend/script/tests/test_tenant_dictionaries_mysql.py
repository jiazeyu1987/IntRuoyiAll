"""Run dictionary migration against a task-owned MySQL; never uses application databases."""
from pathlib import Path
import re
import unittest
import uuid
import test_tenant_notification_resources_mysql as harness
ROOT = harness.ROOT

class TenantDictionariesMySqlTest(unittest.TestCase):
    setUpClass = classmethod(harness.TenantNotificationResourcesMySqlTest.setUpClass.__func__)
    sql = classmethod(harness.TenantNotificationResourcesMySqlTest.sql.__func__)
    query = harness.TenantNotificationResourcesMySqlTest.query

    def setUp(self):
        self.database = 'dict_' + uuid.uuid4().hex
        self.sql(f'CREATE DATABASE {self.database};')
        self.addCleanup(lambda: self.sql(f'DROP DATABASE {self.database};'))
        self.query('CREATE TABLE system_tenant (id bigint PRIMARY KEY); INSERT INTO system_tenant VALUES (11),(22);')
        schema = (ROOT / 'sql/mysql/ruoyi-vue-pro.sql').read_text(encoding='utf-8')
        for table in ['system_dict_type','system_dict_data']:
            self.query(re.search(r'CREATE TABLE `' + table + r'`\s*\([\s\S]*?;(?=\n)',schema).group(0))
        self.query("INSERT INTO system_dict_type (id,name,type) VALUES (10,'Mode','mode'); INSERT INTO system_dict_data (id,dict_type,value,label) VALUES (20,'mode','1','Production');")

    def migrate(self, check=True):
        return self.query((ROOT/'sql/mysql/20261112_tenant_dictionaries.sql').read_text(encoding='utf-8'), check)

    def test_snapshots_are_private_and_rerun_does_not_refill_deleted_or_modified_data(self):
        self.migrate()
        self.query("UPDATE system_tenant_dict_data SET label='Test' WHERE tenant_id=22;")
        self.migrate()
        self.assertEqual('11\tProduction\n22\tTest\n', self.query('SELECT tenant_id,label FROM system_tenant_dict_data ORDER BY tenant_id').stdout)
        self.query('DELETE FROM system_tenant_dict_data WHERE tenant_id=22; DELETE FROM system_tenant_dict_type WHERE tenant_id=22;')
        self.migrate()
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM system_tenant_dict_data WHERE tenant_id=22').stdout)
        self.assertEqual('Production\n', self.query('SELECT label FROM system_dict_data WHERE id=20').stdout)

    def test_orphan_source_aborts_without_partial_copy(self):
        self.query("UPDATE system_dict_data SET dict_type='missing'")
        result=self.migrate(False)
        self.assertNotEqual(0,result.returncode)
        self.assertIn('TENANT_DICTIONARY_SOURCE_TYPE_MISSING',result.stderr)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM system_tenant_dict_type').stdout)

    def test_duplicate_values_abort_and_uniqueness_is_per_tenant(self):
        self.query("INSERT INTO system_dict_data (dict_type,value,label) VALUES ('mode','1','duplicate')")
        self.assertNotEqual(0,self.migrate(False).returncode)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM system_tenant_dict_type').stdout)
        self.query('DELETE FROM system_dict_data WHERE id<>20')
        self.migrate()
        self.assertNotEqual(0,self.query("INSERT INTO system_tenant_dict_data (tenant_id,dict_type,value,label) VALUES (11,'mode','1','duplicate')",False).returncode)

if __name__ == '__main__':
    unittest.main()
