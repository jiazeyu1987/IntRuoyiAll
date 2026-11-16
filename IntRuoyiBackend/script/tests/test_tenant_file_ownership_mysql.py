"""Verified ownership migration against task-owned MySQL, without touching live data."""
import re
import unittest
import uuid
import test_tenant_notification_resources_mysql as harness
ROOT = harness.ROOT

class TenantFileOwnershipMySqlTest(unittest.TestCase):
    setUpClass = classmethod(harness.TenantNotificationResourcesMySqlTest.setUpClass.__func__)
    sql = classmethod(harness.TenantNotificationResourcesMySqlTest.sql.__func__)
    query = harness.TenantNotificationResourcesMySqlTest.query

    def setUp(self):
        self.database = 'file_owner_' + uuid.uuid4().hex
        self.sql(f'CREATE DATABASE {self.database};')
        self.addCleanup(lambda: self.sql(f'DROP DATABASE {self.database};'))
        self.query('CREATE TABLE system_tenant(id bigint PRIMARY KEY); INSERT INTO system_tenant VALUES(11),(22);'
                   'CREATE TABLE infra_tenant_file_config(id bigint,tenant_id bigint,PRIMARY KEY(id,tenant_id));'
                   'INSERT INTO infra_tenant_file_config VALUES(10,11),(10,22);')
        schema = (ROOT/'sql/mysql/ruoyi-vue-pro.sql').read_text(encoding='utf-8')
        for table in ['infra_file', 'infra_file_content']:
            self.query(re.search(r'CREATE TABLE `' + table + r'`\s*\([\s\S]*?;(?=\n)',schema).group(0))
        self.query("INSERT INTO infra_file(id,config_id,path,url,size) VALUES(1,10,'a.txt','http://files/a.txt',1),"
                   "(2,10,'b.txt','http://files/b.txt',1);"
                   "INSERT INTO infra_file_content(id,config_id,path,content) VALUES(1,10,'a.txt',X'41'),(2,10,'b.txt',X'42');")

    def migrate(self, check=True):
        return self.query((ROOT/'sql/mysql/20261112_tenant_file_ownership.sql').read_text(encoding='utf-8'),check)

    def owners(self):
        self.migrate(False)  # Creates the manifest table but refuses unknown ownership.
        self.query("INSERT INTO infra_file_tenant_owner VALUES(1,11,'verified business A'),(2,22,'verified business B');")

    def test_missing_owner_fails_without_assigning_files(self):
        self.assertIn('TENANT_FILE_VERIFIED_OWNER_REQUIRED',self.migrate(False).stderr)
        self.assertEqual('0\n',self.query('SELECT COUNT(*) FROM infra_file_tenant_migration').stdout)
        self.assertEqual('2\n',self.query('SELECT COUNT(*) FROM infra_file').stdout)

    def test_verified_ownership_preserves_ids_bytes_and_does_not_reapply(self):
        self.owners()
        self.migrate()
        self.assertEqual('1\t11\n2\t22\n',self.query('SELECT id,tenant_id FROM infra_file ORDER BY id').stdout)
        self.assertEqual('11\t41\n22\t42\n',self.query('SELECT tenant_id,HEX(content) FROM infra_file_content ORDER BY id').stdout)
        self.query("INSERT INTO infra_file(id,config_id,path,url,size,tenant_id) VALUES(3,10,'tenants/22/c.txt','http://files/c.txt',1,22);")
        self.migrate()
        self.assertEqual('3\n',self.query('SELECT COUNT(*) FROM infra_file').stdout)
        self.assertNotEqual(0,self.query("INSERT INTO infra_file(config_id,path,url,size) VALUES(10,'d','d',1)",False).returncode)

    def test_shared_object_or_orphan_content_is_rejected(self):
        self.owners()
        self.query("UPDATE infra_file SET path='a.txt' WHERE id=2")
        self.assertIn('TENANT_FILE_SHARED_OBJECT_REQUIRES_COPY',self.migrate(False).stderr)
        self.query("UPDATE infra_file SET path='b.txt' WHERE id=2; UPDATE infra_file_content SET path='orphan' WHERE id=2")
        self.assertIn('TENANT_FILE_CONTENT_OWNER_REQUIRED',self.migrate(False).stderr)
        self.assertEqual('0\n',self.query('SELECT COUNT(*) FROM infra_file_tenant_migration').stdout)

    def test_only_the_declared_platform_retention_storage_can_omit_tenant_config(self):
        self.owners()
        self.query('UPDATE infra_file SET config_id=NULL WHERE id=1')
        self.assertIn('TENANT_FILE_CONFIG_REQUIRED',self.migrate(False).stderr)
        self.query('UPDATE infra_file SET config_id=-123 WHERE id=1')
        self.assertIn('TENANT_FILE_CONFIG_REQUIRED',self.migrate(False).stderr)
        self.query('UPDATE infra_file SET config_id=-1040750314 WHERE id=1; UPDATE infra_file_content SET config_id=-1040750314 WHERE id=1')
        self.migrate()
        self.assertEqual('11\n',self.query('SELECT tenant_id FROM infra_file WHERE id=1').stdout)

if __name__ == '__main__':
    unittest.main()
