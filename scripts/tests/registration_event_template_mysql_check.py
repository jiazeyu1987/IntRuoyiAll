"""Run the actual successor procedure against a connection-local MySQL table.

Requires REGCERT_REPAIR_DB_*; no business template or delivered message is changed.
The test procedure has an exclusive random name and is removed on cleanup.
"""
import importlib.util
import json
from pathlib import Path
import re
import unittest
import uuid

root = Path(__file__).parents[2]
spec = importlib.util.spec_from_file_location('reg_repair', root / 'scripts/repair/repair-registration-download-summary.py')
repair = importlib.util.module_from_spec(spec)
spec.loader.exec_module(repair)
MIGRATION = '20261122_dcc_registration_certificate_event_template_params.sql'
CODE = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'
PARAMS = ['productName', 'eventTitle', 'certificateNo', 'effectiveDate', 'expiryDate']


class EventTemplateMySqlCheck(unittest.TestCase):
    def setUp(self):
        self.c = repair.connect()
        self.addCleanup(self.c.close)
        self.proc = 'regcert_template_test_' + uuid.uuid4().hex
        self.addCleanup(self.drop_procedure)
        source = (root / 'IntRuoyiBackend/sql/mysql' / MIGRATION).read_text(encoding='utf-8')
        statement = re.search(r'CREATE PROCEDURE (\w+)\(\)([\s\S]*?)END \$\$', source)
        self.assertIsNotNone(statement)
        statement = statement.group(0).removesuffix(' $$').replace(statement.group(1), self.proc, 1)
        with self.c.cursor() as q:
            self.assertTrue(self.c.get_server_info().startswith('8.'))
            for table in ('system_notify_template', 'system_tenant_notify_template'):
                q.execute('SHOW CREATE TABLE ' + table)
                definition = q.fetchone()['Create Table']
                q.execute(definition.replace('CREATE TABLE', 'CREATE TEMPORARY TABLE', 1))
            q.execute(statement)
        self.c.commit()

    def drop_procedure(self):
        self.c.rollback()
        with self.c.cursor() as q:
            q.execute('DROP PROCEDURE IF EXISTS ' + self.proc)
        self.c.commit()

    def seed(self, template_id=1, code=CODE, content='国内注册证事件 {eventType}', params='["eventType"]'):
        with self.c.cursor() as q:
            q.execute('INSERT INTO system_notify_template '
                      '(id,name,code,type,nickname,content,params,status,remark,deleted) '
                      'VALUES(%s,%s,%s,8,%s,%s,%s,1,%s,0)',
                      (template_id, '旧模板', code, '自定义发送人', content, params, '保留备注'))
        self.c.commit()

    def rows(self):
        with self.c.cursor() as q:
            q.execute('SELECT * FROM system_notify_template ORDER BY id')
            return list(q.fetchall())

    def seed_tenant(self, template_id=101, tenant_id=186, code=CODE):
        with self.c.cursor() as q:
            q.execute('INSERT INTO system_tenant_notify_template '
                      '(id,tenant_id,source_id,name,code,type,nickname,content,params,status,remark,deleted) '
                      'VALUES(%s,%s,NULL,%s,%s,8,%s,%s,%s,1,%s,0)',
                      (template_id, tenant_id, '旧模板', code, '自定义发送人',
                       '国内注册证事件 {eventType}', '["eventType"]', '保留备注'))
        self.c.commit()

    def tenant_rows(self):
        with self.c.cursor() as q:
            q.execute('SELECT * FROM system_tenant_notify_template ORDER BY id')
            return list(q.fetchall())

    def apply(self):
        with self.c.cursor() as q:
            q.execute('CALL ' + self.proc + '()')
            while q.nextset():
                pass
        self.c.commit()

    def assert_contract(self, row):
        self.assertEqual('注册证业务通知', row['name'])
        self.assertEqual(PARAMS, json.loads(row['params']))
        self.assertEqual(PARAMS, re.findall(r'\{(\w+)\}', row['content']))
        self.assertNotIn('国内', row['content'])
        rendered = row['content']
        for k, v in dict(productName='CE产品', eventTitle='已入库', certificateNo='CE-001',
                         effectiveDate='证件未载明', expiryDate='2030-01-01').items():
            rendered = rendered.replace('{' + k + '}', v)
        self.assertNotIn('{', rendered)
        self.assertIn('CE产品', rendered)

    def test_legacy_template_repairs_only_content_contract_and_replay_is_idempotent(self):
        self.seed()
        self.seed(2, 'UNRELATED', '其它模板', '[]')
        self.seed_tenant()
        self.seed_tenant(102, 1)
        self.seed_tenant(103, 186, 'UNRELATED')
        before = self.rows()
        tenant_before = self.tenant_rows()
        self.apply()
        after = self.rows()
        self.assert_contract(after[0])
        self.assertEqual(before[1], after[1])
        changed_fields = ('name', 'content', 'params', 'update_time')
        self.assertEqual({k: v for k, v in before[0].items() if k not in changed_fields},
                         {k: v for k, v in after[0].items() if k not in changed_fields})
        tenant_after = self.tenant_rows()
        for old, new in zip(tenant_before[:2], tenant_after[:2]):
            self.assert_contract(new)
            self.assertEqual({k: v for k, v in old.items() if k not in changed_fields},
                             {k: v for k, v in new.items() if k not in changed_fields})
        self.assertEqual(tenant_before[2], tenant_after[2])
        self.apply()
        self.assertEqual(after, self.rows())
        self.assertEqual(tenant_after, self.tenant_rows())

    def test_missing_or_duplicate_active_template_fails_without_mutation(self):
        with self.assertRaises(repair.pymysql.MySQLError):
            self.apply()
        self.assertEqual([], self.rows())
        self.seed()
        self.seed(2)
        before = self.rows()
        with self.assertRaises(repair.pymysql.MySQLError):
            self.apply()
        self.assertEqual(before, self.rows())

    def test_missing_tenant_template_fails_before_global_change(self):
        self.seed()
        before = self.rows()
        with self.assertRaises(repair.pymysql.MySQLError):
            self.apply()
        self.assertEqual(before, self.rows())
        self.assertEqual([], self.tenant_rows())

    def test_tenant_write_failure_rolls_back_global_update(self):
        self.seed()
        self.seed_tenant()
        before = self.rows()
        tenant_before = self.tenant_rows()
        with self.c.cursor() as q:
            q.execute("ALTER TABLE system_tenant_notify_template ADD CONSTRAINT "
                      "test_reject_changed_name CHECK (name = '旧模板')")
        with self.assertRaises(repair.pymysql.MySQLError):
            self.apply()
        self.assertEqual(before, self.rows())
        self.assertEqual(tenant_before, self.tenant_rows())

    def test_already_neutral_template_with_stale_params_is_repaired_by_registered_successor(self):
        self.seed(content='产品《{productName}》的注册证{eventTitle}。', params='["eventType","tenantId"]')
        self.seed_tenant()
        self.apply()
        self.assert_contract(self.rows()[0])
        catalog = json.loads((root / 'IntRuoyiBackend/sql/mysql/release-sources.json').read_text(encoding='utf-8'))
        self.assertIn(MIGRATION, catalog['migrations'])
        source = (root / 'IntRuoyiBackend/sql/mysql' / MIGRATION).read_text(encoding='utf-8')
        self.assertIn('dependsOn=20261120_dcc_registration_certificate_neutral_event_notify_template,20261112_tenant_notification_resources;', source)


if __name__ == '__main__':
    unittest.main()
