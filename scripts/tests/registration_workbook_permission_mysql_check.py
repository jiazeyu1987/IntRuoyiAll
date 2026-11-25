"""Execute the migration in a disposable MySQL schema, never against business tables.

REGCERT_TEST_CONFIG must explicitly name a local Spring YAML config. Credentials
are read in memory. Only the randomly named test schema is written and removed.
"""
import json
import os
from pathlib import Path
import re
import unittest
import uuid

import pymysql
import yaml

ROOT = Path(__file__).resolve().parents[2]
MIGRATION = '20261124_registration_certificate_workbook_import_permission.sql'
TABLES = ('system_menu', 'system_tenant_package', 'system_tenant', 'system_role', 'system_role_menu')
IMPORT_ID = 2026112401


class WorkbookPermissionMySqlCheck(unittest.TestCase):
    def setUp(self):
        configs = list(yaml.safe_load_all(Path(os.environ['REGCERT_TEST_CONFIG']).read_text(encoding='utf-8')))
        ds = next(x['spring']['datasource']['dynamic']['datasource']['master']
                  for x in configs if x and 'datasource' in x.get('spring', {}))
        match = re.fullmatch(r'jdbc:mysql://(127\.0\.0\.1|localhost):(\d+)/([\w-]+)\?.*', ds['url'])
        if not match:
            raise ValueError('An explicit localhost MySQL configuration is required')
        self.c = pymysql.connect(host=match[1], port=int(match[2]), database=match[3],
                                 user=ds['username'], password=str(ds['password']),
                                 charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)
        self.addCleanup(self.c.close)
        self.schema = 'regcert_permission_test_' + uuid.uuid4().hex
        with self.c.cursor() as q:
            q.execute('CREATE DATABASE `' + self.schema + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
        self.addCleanup(self.drop_schema)
        with self.c.cursor() as q:
            for table in TABLES:
                q.execute(f'CREATE TABLE `{self.schema}`.`{table}` LIKE `{match[3]}`.`{table}`')
            q.execute('USE `' + self.schema + '`')
            source = (ROOT / 'IntRuoyiBackend/sql/mysql' / MIGRATION).read_text(encoding='utf-8')
            procedure = re.search(r'CREATE PROCEDURE [\s\S]+?END\$\$', source).group().removesuffix('$$')
            q.execute(procedure)
            q.execute("INSERT INTO system_menu(id,name,type,parent_id,path,component,permission,status,deleted) VALUES"
                      "(42,'注册证',2,5,'/mdm/registration-certificate','dcc/registration-certificate/index/index',"
                      "'dcc:registration-certificate:query-current',0,0),"
                      "(43,'上传审批',3,42,'','','dcc:registration-certificate:upload:approve',0,0),"
                      "(605071321,'旧历史导入',2,5,'old','old','dcc:registration-certificate:historical-import',1,1)")
            q.execute("INSERT INTO system_tenant_package(id,name,menu_ids,status) VALUES(7,'未选择导入','[42,43]',0)")
            q.execute("INSERT INTO system_tenant(id,name,contact_name,package_id,expire_time,account_count,status) "
                      "VALUES(70,'无关租户','测试',7,'2099-01-01',10,0)")
            q.execute("INSERT INTO system_role(id,name,code,sort,status,type,tenant_id) "
                      "VALUES(700,'管理员','tenant_admin',1,0,2,70)")
        self.c.commit()

    def drop_schema(self):
        self.c.rollback()
        with self.c.cursor() as q:
            q.execute('DROP DATABASE `' + self.schema + '`')

    def rows(self, table):
        with self.c.cursor() as q:
            q.execute('SELECT * FROM ' + table + ' ORDER BY id')
            return q.fetchall()

    def apply(self):
        with self.c.cursor() as q:
            q.execute('CALL ensure_regcert_workbook_permission_20261124()')
            while q.nextset():
                pass

    def test_restore_button_without_granting_unrelated_tenants_and_replay(self):
        before = {t: self.rows(t) for t in TABLES}
        self.apply()
        menus = self.rows('system_menu')
        button = next(r for r in menus if r['id'] == IMPORT_ID)
        self.assertEqual((3, 42, 0, b'\0'), (button['type'], button['parent_id'], button['status'], button['deleted']))
        self.assertEqual(before['system_menu'], [r for r in menus if r['id'] != IMPORT_ID])
        for t in TABLES[1:]:
            self.assertEqual(before[t], self.rows(t))
        after = {t: self.rows(t) for t in TABLES}
        self.apply()
        self.assertEqual(after, {t: self.rows(t) for t in TABLES})

    def test_sync_only_explicit_package_tenant_admin(self):
        with self.c.cursor() as q:
            q.execute('INSERT INTO system_tenant_package(id,name,menu_ids,status) VALUES(8,%s,%s,0)',
                      ('专用套餐', json.dumps([43, IMPORT_ID])))
            q.execute("INSERT INTO system_tenant(id,name,contact_name,package_id,expire_time,account_count,status) "
                      "VALUES(80,'独立租户','测试',8,'2099-01-01',10,0)")
            q.execute("INSERT INTO system_role(id,name,code,sort,status,type,tenant_id) VALUES"
                      "(800,'管理员','tenant_admin',1,0,2,80),(801,'普通用户','normal',1,0,2,80)")
        self.c.commit()
        self.apply()
        self.assertEqual([(800, IMPORT_ID, 80)],
                         [(r['role_id'], r['menu_id'], r['tenant_id']) for r in self.rows('system_role_menu')])
        packages = self.rows('system_tenant_package')
        self.assertEqual([42, 43], json.loads(packages[0]['menu_ids']))
        self.assertEqual({42, 43, IMPORT_ID}, set(json.loads(packages[1]['menu_ids'])))
        before = {t: self.rows(t) for t in TABLES}
        self.apply()
        self.assertEqual(before, {t: self.rows(t) for t in TABLES})

    def test_missing_approval_or_permission_conflict_fails_without_mutation(self):
        for setup in (
            "UPDATE system_menu SET status=1 WHERE id=43",
            "INSERT INTO system_menu(id,name,type,parent_id,permission,status) VALUES"
            "(99,'冲突',3,42,'dcc:registration-certificate:historical-import',0)",
        ):
            with self.subTest(setup=setup), self.c.cursor() as q:
                q.execute(setup)
                self.c.commit()
                before = {t: self.rows(t) for t in TABLES}
                with self.assertRaises(pymysql.MySQLError):
                    self.apply()
                self.assertEqual(before, {t: self.rows(t) for t in TABLES})
                q.execute('UPDATE system_menu SET status=0 WHERE id=43')
                q.execute('DELETE FROM system_menu WHERE id=99')
                self.c.commit()

    def test_opt_in_without_approval_is_rejected_without_mutation(self):
        with self.c.cursor() as q:
            q.execute('UPDATE system_tenant_package SET menu_ids=%s WHERE id=7', (json.dumps([42, IMPORT_ID]),))
        self.c.commit()
        before = {t: self.rows(t) for t in TABLES}
        with self.assertRaises(pymysql.MySQLError):
            self.apply()
        self.assertEqual(before, {t: self.rows(t) for t in TABLES})


if __name__ == '__main__':
    unittest.main()
