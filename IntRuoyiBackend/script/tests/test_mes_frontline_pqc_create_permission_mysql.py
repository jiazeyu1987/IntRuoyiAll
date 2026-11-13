"""Execute the permission migration in a disposable, network-isolated MySQL."""
import pathlib
import re
import subprocess
import time
import unittest
import uuid


class FrontlinePqcCreatePermissionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        root = pathlib.Path(__file__).resolve().parents[2]
        cls.migration = (root / 'sql/mysql/20261112_mes_frontline_pqc_create_permission.sql').read_text(encoding='utf-8')
        cls.container = 'pqc-create-test-' + uuid.uuid4().hex[:10]
        subprocess.run(['docker', 'run', '-d', '--pull=never', '--network=none', '--name', cls.container,
                        '--tmpfs', '/var/lib/mysql', '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', 'mysql:8.0.39'],
                       check=True, capture_output=True)
        cls.addClassCleanup(lambda: subprocess.run(['docker', 'rm', '-f', cls.container], check=True, capture_output=True))
        for _ in range(60):
            if subprocess.run(['docker', 'exec', cls.container, 'mysqladmin', '--protocol=tcp', '-h127.0.0.1',
                               'ping', '--silent'], capture_output=True).returncode == 0:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError('Isolated MySQL did not become ready')
        schema = (root / 'sql/mysql/ruoyi-vue-pro.sql').read_text(encoding='utf-8')
        cls.ddl = '\n'.join(re.search(r'CREATE TABLE `' + table + r'`\s*\([\s\S]*?\) ENGINE[^;]*;', schema).group(0)
                            for table in ['system_menu', 'system_role', 'system_role_menu', 'system_tenant', 'system_tenant_package'])

    @classmethod
    def sql(cls, source, database=None, check=True):
        command = ['docker', 'exec', '-i', cls.container, 'mysql', '--protocol=tcp', '-h127.0.0.1',
                   '--default-character-set=utf8mb4', '--batch', '--skip-column-names', '-uroot']
        if database:
            command.append(database)
        result = subprocess.run(command, input=source, encoding='utf-8', capture_output=True)
        if check and result.returncode:
            raise AssertionError(result.stderr)
        return result

    def setUp(self):
        self.database = 'pqc_' + uuid.uuid4().hex[:10]
        self.sql(f'CREATE DATABASE {self.database} CHARACTER SET utf8mb4;')
        self.sql(self.ddl + """
CREATE TABLE system_approval_authority_revision(tenant_id BIGINT PRIMARY KEY,revision BIGINT,updated_at DATETIME);
INSERT INTO system_tenant(id,name,package_id,status,contact_name,expire_time,account_count) VALUES
 (1,'Main',0,0,'Test','2099-01-01',100),(2,'Test',20,0,'Test','2099-01-01',100),(3,'Other',30,0,'Test','2099-01-01',100);
INSERT INTO system_tenant_package(id,name,menu_ids,status) VALUES (20,'Test','[900438,888]',0),(30,'Other','[888]',0);
INSERT INTO system_approval_authority_revision VALUES (1,10,NOW()),(2,20,NOW()),(3,30,NOW());
INSERT INTO system_menu(id,name,type,parent_id,component,permission,status) VALUES
 (900438,'Frontline PQC',2,1,'mes/pro/edhr-batch/BatchPqcFillPage','',0),
 (888,'Unrelated',3,1,'','other:create',0);
INSERT INTO system_role(id,name,code,sort,tenant_id,status,type) VALUES
 (10,'Inspector','pqc_permission',0,1,0,2),(20,'Leader','pqc_leader_permission',0,2,0,2),
 (30,'No page','pqc_leader_permission',0,3,0,2),(40,'Other','other',0,2,0,2),
 (50,'Disabled','pqc_permission',0,2,1,2);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,deleted) VALUES
 (10,900438,1,0),(20,900438,2,0),(40,900438,2,0),(50,900438,2,0),(10,888,1,0);
""", self.database)

    def query(self, source):
        return self.sql(source, self.database).stdout.strip()

    def test_scoped_grants_package_navigation_and_idempotence(self):
        self.sql(self.migration, self.database)
        menu_id = self.query("SELECT id FROM system_menu WHERE parent_id=900438 AND permission='mes:pro-feedback:create';")
        self.assertEqual(self.query(f'SELECT GROUP_CONCAT(role_id ORDER BY role_id) FROM system_role_menu WHERE menu_id={menu_id} AND deleted=0;'), '10,20')
        self.assertEqual(self.query(f'SELECT COUNT(*) FROM system_tenant_package WHERE id=20 AND JSON_CONTAINS(menu_ids,CAST({menu_id} AS JSON));'), '1')
        self.assertEqual(self.query('SELECT menu_ids FROM system_tenant_package WHERE id=30;'), '[888]')
        self.assertEqual(self.query('SELECT COUNT(*) FROM system_role_menu WHERE menu_id=888 AND deleted=0;'), '1')
        self.assertEqual(self.query('SELECT GROUP_CONCAT(revision ORDER BY tenant_id) FROM system_approval_authority_revision;'), '11,21,30')
        self.assertEqual(self.query(f'SELECT CONCAT(type,":",visible+0) FROM system_menu WHERE id={menu_id};'), '3:0')
        count = self.query('SELECT COUNT(*) FROM system_role_menu;')
        packages = self.query('SELECT menu_ids FROM system_tenant_package ORDER BY id;')
        self.sql(self.migration, self.database)
        self.assertEqual(self.query('SELECT COUNT(*) FROM system_role_menu;'), count)
        self.assertEqual(self.query('SELECT menu_ids FROM system_tenant_package ORDER BY id;'), packages)

    def test_invalid_package_fails_without_grant(self):
        self.sql("UPDATE system_tenant_package SET menu_ids='[888]' WHERE id=20;", self.database)
        result = self.sql(self.migration, self.database, check=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('valid frontline PQC tenant package', result.stderr)
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_menu WHERE permission='mes:pro-feedback:create';"), '0')
        self.assertEqual(self.query('SELECT revision FROM system_approval_authority_revision WHERE tenant_id=1;'), '10')

    def test_disabled_existing_permission_rolls_back(self):
        self.sql("INSERT INTO system_menu(id,name,type,parent_id,permission,status) VALUES(999,'Disabled',3,900438,'mes:pro-feedback:create',1);", self.database)
        result = self.sql(self.migration, self.database, check=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('disabled or invalid', result.stderr)
        self.assertEqual(self.query('SELECT COUNT(*) FROM system_role_menu WHERE menu_id=999;'), '0')
        self.assertEqual(self.query('SELECT revision FROM system_approval_authority_revision WHERE tenant_id=1;'), '10')


if __name__ == '__main__':
    unittest.main(verbosity=2)
