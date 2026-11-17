"""Real MySQL migration regression; writes only randomly named disposable schemas."""
import json
from pathlib import Path
import subprocess
import unittest
import uuid

SQL = (Path(__file__).resolve().parents[2] / 'sql/mysql/20261117_dcc_file_type_taxonomy_menu_restore.sql').read_text(encoding='utf-8')
TABLES = ('system_menu', 'system_role', 'system_role_menu', 'system_tenant', 'system_tenant_package')
COMPONENT = 'dcc/controlled-file/basic-data/file-type-taxonomy/index'


def mysql(sql, schema=None, success=True):
    prefix = f'USE `{schema}`;\n' if schema else ''
    result = subprocess.run(['docker', 'exec', '-i', 'int-ruoyi-mysql', 'sh', '-c',
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --raw --skip-column-names'],
        input=prefix + sql, encoding='utf-8', capture_output=True, timeout=60)
    if success and result.returncode:
        raise AssertionError(result.stderr)
    return result


class TaxonomyMigrationMySQLTest(unittest.TestCase):
    def setUp(self):
        self.schema = 'codex_taxonomy_' + uuid.uuid4().hex
        mysql(f'CREATE DATABASE `{self.schema}`;')
        self.addCleanup(lambda: mysql(f'DROP DATABASE `{self.schema}`;'))
        mysql(''.join(f'CREATE TABLE `{t}` LIKE `ruoyi-vue-pro`.`{t}`;' for t in TABLES), self.schema)
        mysql("""
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,component,status) VALUES
(990200,'mdm','',1,1,0,'/mdm','',0),(6803,'manage','dcc:controlled-file:category:manage',2,1,0,'old','old',0),
(990230,'certificate','cert',2,1,990200,'certificate','cert',0);
INSERT INTO system_role(id,name,code,sort,status,type,tenant_id) VALUES(1,'qualified','qualified',1,0,2,1),(2,'unqualified','unqualified',1,0,2,2);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id) VALUES(1,6803,1);
INSERT INTO system_tenant_package(id,name,menu_ids) VALUES(1,'shared','[]'),(2,'independent','[]');
INSERT INTO system_tenant(id,name,contact_name,package_id,expire_time,account_count) VALUES
(1,'one','test',1,'2030-01-01',10),(2,'two','test',1,'2030-01-01',10),(3,'three','test',2,'2030-01-01',10);
""", self.schema)

    def query(self, sql):
        return mysql(sql, self.schema).stdout.strip()

    def snapshot(self):
        return {t: self.query(f'SELECT * FROM `{t}` ORDER BY id;') for t in TABLES}

    def rejected_unchanged(self, message):
        before = self.snapshot()
        result = mysql(SQL, self.schema, success=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(message, result.stderr)
        self.assertEqual(before, self.snapshot(), 'Migration failure changed a business table')

    def test_restore_idempotent_and_authorization_scope(self):
        mysql(SQL, self.schema)
        self.assertEqual(self.query('SELECT component FROM system_menu WHERE id=990230'), 'cert')
        self.assertEqual(self.query('SELECT COUNT(*) FROM system_role_menu WHERE role_id=2'), '0')
        self.assertEqual(self.query('SELECT COUNT(*) FROM system_role_menu WHERE role_id=1'), '3')
        self.assertEqual(json.loads(self.query('SELECT menu_ids FROM system_tenant_package WHERE id=1')), [990200, 605072160])
        self.assertEqual(json.loads(self.query('SELECT menu_ids FROM system_tenant_package WHERE id=2')), [])
        # Tenant 2 shares package 1 but receives no role grant.
        self.assertEqual(self.query('SELECT package_id FROM system_tenant WHERE id=2'), '1')
        before = self.snapshot()
        mysql(SQL, self.schema)
        # update_time may legitimately refresh; compare business values explicitly.
        self.assertEqual(self.query('SELECT COUNT(*) FROM system_role_menu WHERE role_id=1'), '3')
        self.assertEqual(self.query(f"SELECT COUNT(*) FROM system_menu WHERE component='{COMPONENT}'"), '1')
        self.assertEqual(before['system_role_menu'], self.snapshot()['system_role_menu'])

    def test_existing_semantic_menu(self):
        mysql(f"INSERT INTO system_menu(id,name,type,sort,parent_id,path,component,status,visible) VALUES(777,'existing',2,1,0,'old-taxonomy','{COMPONENT}',1,b'0');", self.schema)
        mysql(SQL, self.schema)
        self.assertEqual(self.query('SELECT CONCAT(parent_id,\':\',path,\':\',status,\':\',visible+0) FROM system_menu WHERE id=777'), '990200:file-type-taxonomy:0:1')
        self.assertEqual(self.query('SELECT COUNT(*) FROM system_menu WHERE id=605072160'), '0')
        self.assertEqual(self.query('SELECT COUNT(*) FROM system_role_menu WHERE menu_id=777'), '1')

    def test_id_conflict(self):
        mysql("INSERT INTO system_menu(id,name,type,sort,parent_id,path,status) VALUES(605072160,'other',2,1,0,'other',0);", self.schema)
        self.rejected_unchanged('Reserved taxonomy menu ID')

    def test_route_conflict(self):
        mysql("INSERT INTO system_menu(id,name,type,sort,parent_id,path,status) VALUES(778,'other',2,1,990200,'file-type-taxonomy',0);", self.schema)
        self.rejected_unchanged('Taxonomy route occupied')

    def test_duplicate_component(self):
        mysql(f"INSERT INTO system_menu(id,name,type,sort,parent_id,path,component,status) VALUES(777,'a',2,1,0,'a','{COMPONENT}',0),(778,'b',2,1,0,'b','{COMPONENT}',0);", self.schema)
        self.rejected_unchanged('Duplicate taxonomy menu components')

    def test_no_qualification(self):
        mysql('DELETE FROM system_role_menu;', self.schema)
        self.rejected_unchanged('No existing active category-manage role')

    def test_invalid_package(self):
        mysql("UPDATE system_tenant_package SET menu_ids='{}' WHERE id=1;", self.schema)
        self.rejected_unchanged('Qualified tenant package missing')

    def test_missing_package(self):
        mysql('DELETE FROM system_tenant_package WHERE id=1;', self.schema)
        self.rejected_unchanged('Qualified tenant package missing')


if __name__ == '__main__':
    unittest.main(verbosity=2)
