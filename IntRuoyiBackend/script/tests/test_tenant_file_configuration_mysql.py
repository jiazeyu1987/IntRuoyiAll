"""Tenant file configuration migration on an isolated local MySQL container."""
import re
import unittest
import uuid
import test_tenant_notification_resources_mysql as harness
ROOT=harness.ROOT

class TenantFileConfigurationMySqlTest(unittest.TestCase):
    setUpClass=classmethod(harness.TenantNotificationResourcesMySqlTest.setUpClass.__func__)
    sql=classmethod(harness.TenantNotificationResourcesMySqlTest.sql.__func__)
    query=harness.TenantNotificationResourcesMySqlTest.query
    def setUp(self):
        self.database='file_config_'+uuid.uuid4().hex
        self.sql(f'CREATE DATABASE {self.database};')
        self.addCleanup(lambda:self.sql(f'DROP DATABASE {self.database};'))
        self.query('CREATE TABLE system_tenant(id bigint PRIMARY KEY); INSERT INTO system_tenant VALUES (11),(22);')
        schema=(ROOT/'sql/mysql/ruoyi-vue-pro.sql').read_text(encoding='utf-8')
        self.query(re.search(r'CREATE TABLE `infra_file_config`\s*\([\s\S]*?;(?=\n)',schema).group(0))
        self.query("INSERT INTO infra_file_config(id,name,storage,master,config) VALUES(10,'original',10,1,'{}'),(20,'other',10,0,'{}')")
    def migrate(self,check=True):
        return self.query((ROOT/'sql/mysql/20261112_tenant_file_configuration.sql').read_text(encoding='utf-8'),check)
    def test_ids_remain_stable_and_modifications_are_independent_and_not_overwritten(self):
        self.migrate()
        self.query("UPDATE infra_tenant_file_config SET name='tenant-a',master=0 WHERE tenant_id=11 AND id=10; UPDATE infra_tenant_file_config SET master=1 WHERE tenant_id=11 AND id=20;")
        self.migrate()
        self.assertEqual('11\t20\n22\t10\n',self.query('SELECT tenant_id,id FROM infra_tenant_file_config WHERE master=1 ORDER BY tenant_id').stdout)
        self.assertEqual('original\n',self.query('SELECT name FROM infra_file_config WHERE id=10').stdout)
        self.query('DELETE FROM infra_tenant_file_config WHERE tenant_id=11;')
        self.migrate()
        self.assertEqual('0\n',self.query('SELECT COUNT(*) FROM infra_tenant_file_config WHERE tenant_id=11').stdout)
        self.query("INSERT INTO infra_tenant_file_config(tenant_id,name,storage,master,config) VALUES(22,'new',10,0,'{}')")
        self.assertEqual('1\n',self.query("SELECT id>20 FROM infra_tenant_file_config WHERE name='new'").stdout)
    def test_missing_or_ambiguous_default_fails_before_copying(self):
        self.query('UPDATE infra_file_config SET master=0')
        self.assertIn('TENANT_FILE_DEFAULT_CONFIG_REQUIRED',self.migrate(False).stderr)
        self.assertEqual('0\n',self.query('SELECT COUNT(*) FROM infra_tenant_file_config').stdout)
        self.query('UPDATE infra_file_config SET master=1')
        self.assertIn('TENANT_FILE_DEFAULT_CONFIG_REQUIRED',self.migrate(False).stderr)

if __name__=='__main__': unittest.main()
