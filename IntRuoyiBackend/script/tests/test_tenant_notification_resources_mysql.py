"""Verify tenant notification snapshots using a task-owned MySQL without network access."""
from pathlib import Path
import re
import subprocess
import time
import unittest
import uuid

ROOT = Path(__file__).resolve().parents[2]
TABLES = ['system_mail_account', 'system_mail_template', 'system_sms_channel',
          'system_sms_template', 'system_notify_template', 'system_mail_log', 'system_sms_log', 'system_sms_code', 'system_notify_message']


class TenantNotificationResourcesMySqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = 'tenant-notification-test-' + uuid.uuid4().hex[:12]
        subprocess.run(['docker', 'run', '--pull=never', '--rm', '-d', '--network=none', '--name', cls.container,
                        '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', 'mysql:8.0.39'], check=True, capture_output=True)
        cls.addClassCleanup(lambda: subprocess.run(['docker', 'rm', '-f', cls.container], check=True, capture_output=True))
        deadline = time.monotonic() + 60
        while cls.sql('SELECT 1', check=False).returncode:
            if time.monotonic() > deadline:
                raise RuntimeError('Task-owned MySQL did not become ready')
            time.sleep(1)

    @classmethod
    def sql(cls, query, check=True):
        result = subprocess.run(['docker', 'exec', '-i', cls.container, 'mysql', '-uroot', '--protocol=TCP',
                                 '-h127.0.0.1', '-BN', '--default-character-set=utf8mb4'],
                                input=query, capture_output=True, encoding='utf-8')
        if check and result.returncode:
            raise AssertionError(result.stderr)
        return result

    def setUp(self):
        self.database = 'test_' + uuid.uuid4().hex
        self.sql(f'CREATE DATABASE {self.database};')
        self.addCleanup(lambda: self.sql(f'DROP DATABASE {self.database};'))
        self.query('CREATE TABLE system_tenant (id bigint PRIMARY KEY); INSERT INTO system_tenant VALUES (11),(22);'
                   'CREATE TABLE system_users (id bigint PRIMARY KEY,tenant_id bigint NOT NULL);'
                   'INSERT INTO system_users VALUES (1001,11),(1002,22);')
        schema = (ROOT / 'sql/mysql/ruoyi-vue-pro.sql').read_text(encoding='utf-8')
        for table in TABLES:
            ddl = re.search(r'CREATE TABLE `' + table + r'`\s*\([\s\S]*?;(?=\n)', schema).group(0)
            self.query(ddl)
        self.insert('system_mail_account', id=10, host='original.invalid')
        self.insert('system_sms_channel', id=20, code='ALIYUN')
        self.insert('system_mail_template', id=30, account_id=10, code='same-mail', params='[]')
        self.insert('system_sms_template', id=40, channel_id=20, channel_code='ALIYUN', code='same-sms', params='[]')
        self.insert('system_notify_template', id=50, code='same-notice', params='[]')
        self.insert('system_notify_message', id=300, user_id=1001, user_type=2, tenant_id=11, template_id=50)
        self.insert('system_sms_code', id=400, tenant_id=11, used=1, code='123456')
        self.insert('system_mail_log', id=100, user_id=1001, user_type=2, account_id=10, template_id=30)
        self.insert('system_sms_log', id=200, user_id=1002, user_type=2, channel_id=20, template_id=40)

    def query(self, query, check=True):
        return self.sql(f'USE {self.database};\n' + query, check)

    def insert(self, table, **values):
        columns = self.query("SELECT COLUMN_NAME,DATA_TYPE FROM information_schema.COLUMNS "
                             f"WHERE TABLE_SCHEMA='{self.database}' AND TABLE_NAME='{table}' "
                             "AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL AND EXTRA NOT LIKE '%auto_increment%';")
        fixture = {}
        for line in columns.stdout.splitlines():
            name, data_type = line.split('\t')
            fixture[name] = 0 if data_type in ('tinyint', 'int', 'bigint', 'bit') else 'fixture'
        fixture.update(values)
        literals = [str(v) if isinstance(v, int) else "'" + v.replace("'", "''") + "'" for v in fixture.values()]
        self.query(f"INSERT INTO {table} ({','.join(fixture)}) VALUES ({','.join(literals)});")

    def migrate(self, check=True):
        return self.query((ROOT / 'sql/mysql/20261112_tenant_notification_resources.sql').read_text(encoding='utf-8'), check)

    def test_independent_settings_correct_references_and_private_log_ownership(self):
        self.migrate()
        self.assertEqual('2\n', self.query('SELECT COUNT(*) FROM system_tenant_mail_account;').stdout)
        self.assertEqual('11\t11\n22\t22\n', self.query(
            'SELECT t.tenant_id,a.tenant_id FROM system_tenant_mail_template t JOIN system_tenant_mail_account a '
            'ON a.id=t.account_id ORDER BY t.tenant_id;').stdout)
        self.assertEqual('100\t11\n', self.query('SELECT id,tenant_id FROM system_tenant_mail_log;').stdout)
        self.assertEqual('200\t22\n', self.query('SELECT id,tenant_id FROM system_tenant_sms_log;').stdout)
        self.query("UPDATE system_tenant_mail_account SET host='tenant-a.invalid' WHERE tenant_id=11;")
        self.migrate()
        self.assertEqual('11\ttenant-a.invalid\n22\toriginal.invalid\n', self.query(
            'SELECT tenant_id,host FROM system_tenant_mail_account ORDER BY tenant_id;').stdout)
        self.assertEqual('original.invalid\n', self.query('SELECT host FROM system_mail_account WHERE id=10;').stdout)
        self.assertEqual('400\t11\t1\n', self.query('SELECT id,tenant_id,used+0 FROM system_tenant_sms_code;').stdout)
        self.assertEqual('300\t11\n', self.query('SELECT id,tenant_id FROM system_tenant_notify_message;').stdout)

    def test_unattributable_log_stops_migration_without_partial_snapshot(self):
        self.query('UPDATE system_mail_log SET user_id=NULL,user_type=NULL WHERE id=100;')
        result = self.migrate(check=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('TENANT_MAIL_HISTORICAL_LOG_OWNER_REQUIRED', result.stderr)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM system_tenant_mail_account;').stdout)
        self.query("INSERT INTO system_tenant_message_log_owner VALUES ('mail',100,11);")
        self.migrate()
        self.assertEqual('11\n', self.query('SELECT tenant_id FROM system_tenant_mail_log WHERE id=100;').stdout)

    def test_orphan_template_stops_copy_instead_of_dropping_data(self):
        self.query('UPDATE system_mail_template SET account_id=999 WHERE id=30;')
        result = self.migrate(check=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('TENANT_NOTIFICATION_SOURCE_REFERENCE_MISSING', result.stderr)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM system_tenant_mail_template;').stdout)

    def test_removed_historical_notify_template_retains_source_and_snapshot(self):
        self.query('DELETE FROM system_notify_template WHERE id=50;')
        self.migrate()
        self.assertEqual('300\t11\tNULL\t50\n', self.query(
            'SELECT id,tenant_id,template_id,source_template_id FROM system_tenant_notify_message;').stdout)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM system_tenant_notify_template;').stdout)
        self.assertEqual('0\n', self.query(
            'SELECT COUNT(*) FROM system_notify_message s JOIN system_tenant_notify_message t ON t.id=s.id '
            'WHERE NOT(s.template_content<=>t.template_content) OR NOT(s.template_params<=>t.template_params) '
            'OR NOT(s.create_time<=>t.create_time) OR NOT(s.read_status<=>t.read_status);').stdout)
        self.query('UPDATE system_tenant_notify_message SET read_status=1 WHERE id=300;')
        self.migrate()
        self.assertEqual('1\n', self.query('SELECT read_status+0 FROM system_tenant_notify_message WHERE id=300;').stdout)
        invalid = self.query('UPDATE system_tenant_notify_message SET source_template_id=NULL WHERE id=300;', check=False)
        self.assertNotEqual(0, invalid.returncode)
        self.assertIn('chk_notify_template_identity', invalid.stderr)

    def test_notify_message_id_collision_is_rejected(self):
        self.migrate()
        self.query("UPDATE system_tenant_notify_message SET template_content='other message' WHERE id=300;")
        conflict = self.migrate(check=False)
        self.assertNotEqual(0, conflict.returncode)
        self.assertIn('TENANT_NOTIFY_HISTORICAL_IDENTITY_CONFLICT', conflict.stderr)

    def test_source_identity_backfill_preserves_existing_history_timestamp(self):
        self.migrate()
        self.query("UPDATE system_tenant_notify_message SET source_template_id=NULL, "
                   "update_time='2001-02-03 04:05:06',read_status=1 WHERE id=300;")
        self.migrate()
        self.assertEqual('50\t2001-02-03 04:05:06\t1\n', self.query(
            'SELECT source_template_id,update_time,read_status+0 FROM system_tenant_notify_message WHERE id=300;').stdout)

    def test_confirmed_test1_orphan_sms_attribution_and_conflict(self):
        self.query("ALTER TABLE system_tenant ADD name varchar(64), ADD status int DEFAULT 0, "
                   "ADD deleted bit DEFAULT b'0'; INSERT INTO system_tenant(id,name) VALUES (186,'测试1');")
        columns = self.query("SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                             f"WHERE TABLE_SCHEMA='{self.database}' AND TABLE_NAME='system_sms_log' "
                             "ORDER BY ORDINAL_POSITION;").stdout.splitlines()
        selectors = [('n.id' if c == 'id' else '113' if c == 'user_id' else 's.' + c) for c in columns]
        ids = ' UNION ALL '.join(f'SELECT {n} AS id' for n in [*range(1659, 2054), 2280])
        self.query(f"INSERT INTO system_sms_log ({','.join(columns)}) SELECT {','.join(selectors)} "
                   f"FROM system_sms_log s CROSS JOIN ({ids}) n WHERE s.id=200;")
        self.assertIn('TENANT_SMS_HISTORICAL_LOG_OWNER_REQUIRED', self.migrate(check=False).stderr)
        repair = (ROOT / 'sql/mysql/20261112_test1_historical_sms_log_owner.sql').read_text(encoding='utf-8')
        self.query(repair)
        self.query(repair)
        self.migrate()
        self.assertEqual('396\n', self.query(
            'SELECT COUNT(*) FROM system_tenant_sms_log WHERE user_id=113 AND tenant_id=186;').stdout)
        self.assertEqual('22\n', self.query('SELECT tenant_id FROM system_tenant_sms_log WHERE id=200;').stdout)
        self.query("UPDATE system_tenant_message_log_owner SET tenant_id=11 WHERE log_kind='sms' AND log_id=1659;")
        conflict = self.query(repair, check=False)
        self.assertNotEqual(0, conflict.returncode)
        self.assertIn('TEST1_HISTORICAL_SMS_OWNER_CONFLICT', conflict.stderr)

    def test_test1_menu_repair_preserves_pqc_navigation_boundary(self):
        self.query("""
ALTER TABLE system_tenant ADD name varchar(64), ADD package_id bigint, ADD status int DEFAULT 0,
 ADD deleted bit DEFAULT b'0';
INSERT INTO system_tenant(id,name,package_id) VALUES(186,'测试1',116);
CREATE TABLE system_tenant_package(id bigint,menu_ids json,deleted bit);
INSERT INTO system_tenant_package VALUES(116,'[990200,990210]',0);
CREATE TABLE system_role(id bigint,tenant_id bigint,code varchar(100),status int,deleted bit);
INSERT INTO system_role VALUES(990819801,186,'qa',0,0),(990819803,186,'pqc_leader_permission',0,0),
 (990819819,186,'process_engineer',0,0);
CREATE TABLE system_role_menu(role_id bigint,menu_id bigint,creator varchar(64),create_time datetime,
 updater varchar(64),update_time datetime,deleted bit,tenant_id bigint);
INSERT INTO system_role_menu(role_id,menu_id,deleted,tenant_id) VALUES(990819801,990210,0,186),(990819819,990210,0,186);
CREATE TABLE system_menu(id bigint,parent_id bigint,path varchar(128),permission varchar(128),
 component varchar(255),type int,status int,deleted bit,updater varchar(64),update_time datetime);
INSERT INTO system_menu(id,parent_id,path,permission,component,type,status,deleted) VALUES
 (990200,0,'/mdm','','',1,0,0),
 (990210,991515,'/mes/md/dcc-project-code','dcc:project-code:query','dcc/controlled-file/basic-data/project-code/index',2,0,0);
CREATE TABLE system_approval_authority_revision(tenant_id bigint,revision bigint,updated_at datetime);
INSERT INTO system_approval_authority_revision VALUES(186,1,NOW());
""")
        repair = (ROOT / 'sql/mysql/20261111_test1_dcc_project_menu_initialization.sql').read_text(encoding='utf-8')
        self.query(repair)
        self.query(repair)
        self.assertEqual('2\n', self.query('SELECT COUNT(*) FROM system_role_menu WHERE menu_id=990200 AND deleted=0;').stdout)
        self.assertEqual('0\n', self.query('SELECT COUNT(*) FROM system_role_menu WHERE role_id=990819803;').stdout)
        self.assertEqual('990200\n', self.query('SELECT parent_id FROM system_menu WHERE id=990210;').stdout)


if __name__ == '__main__':
    unittest.main()
