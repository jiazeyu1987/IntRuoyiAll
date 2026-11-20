"""Exercise the forward repair against a real, isolated MySQL server."""
import subprocess
import time
import unittest
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MID = '20261120_mes_schedule_order_menu_forward'


class ScheduleOrderMenuForwardTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = 'menu-forward-' + uuid.uuid4().hex[:12]
        subprocess.run(['docker', 'run', '--pull=never', '--rm', '-d', '--network=none', '--tmpfs', '/var/lib/mysql',
                        '--name', cls.container, '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes',
                        'mysql:8.0.39'], check=True, capture_output=True)
        cls.addClassCleanup(lambda: subprocess.run(['docker', 'rm', '-f', cls.container],
                                                   check=True, capture_output=True))
        deadline = time.monotonic() + 180
        while (subprocess.run(['docker', 'exec', cls.container, 'cat', '/proc/1/comm'],
                              capture_output=True, text=True).stdout.strip() != 'mysqld'
               or cls.sql('SELECT 1', False).returncode):
            if time.monotonic() > deadline:
                raise RuntimeError('Isolated MySQL readiness timed out')
            time.sleep(1)

    @classmethod
    def sql(cls, query, check=True):
        result = subprocess.run(['docker', 'exec', '-i', cls.container, 'mysql', '-uroot',
                               '--default-character-set=utf8mb4', '--batch', '--skip-column-names'],
                              input=query, text=True, encoding='utf-8', capture_output=True)
        if check and result.returncode:
            raise RuntimeError(result.stderr)
        return result

    def query(self, sql, check=True):
        return self.sql('USE menu_test;\n' + sql, check)

    def setUp(self):
        self.sql("DROP DATABASE IF EXISTS menu_test; CREATE DATABASE menu_test CHARACTER SET utf8mb4;")
        self.query("""CREATE TABLE system_menu (
          id BIGINT PRIMARY KEY, name VARCHAR(50),permission VARCHAR(100),type TINYINT,sort INT,
          parent_id BIGINT,path VARCHAR(200),icon VARCHAR(100),component VARCHAR(255),
          component_name VARCHAR(255),status TINYINT,visible BIT,keep_alive BIT,always_show BIT,
          updater VARCHAR(64),update_time DATETIME,deleted BIT) ENGINE=InnoDB;
          INSERT INTO system_menu VALUES
          (900120,'智能排产','mes:pro-smart-scheduling:query',1,60,0,'/mes/smart-scheduling','#',NULL,NULL,0,1,1,1,'old','2026-10-29',0),
          (5580,'排产工单池','',2,2,5700,'schedule-order','ep:operation','mes/pro/scheduleorder/index','MesProScheduleOrder',0,1,1,1,'old','2026-07-09',0);
          CREATE TABLE system_role_menu (id BIGINT,menu_id BIGINT,role_id BIGINT);
          INSERT INTO system_role_menu VALUES (1,5580,10),(2,900120,10);
          CREATE TABLE system_tenant_package (id BIGINT,menu_ids LONGTEXT);
          INSERT INTO system_tenant_package VALUES (1,'[5580,900120]'),(2,'[900120]');""")

    def migrate(self, check=True):
        return self.query((ROOT / 'sql/mysql' / (MID + '.sql')).read_text(encoding='utf-8'), check)

    def preflight(self):
        return self.query((ROOT / 'sql/mysql/target-preflight' / (MID + '.preflight.sql')).read_text(encoding='utf-8')).stdout

    def snapshot(self):
        return self.query('SELECT * FROM system_menu ORDER BY id; SELECT * FROM system_role_menu; SELECT * FROM system_tenant_package;').stdout

    def test_old_state_repaired_idempotently_without_grant_changes(self):
        before = self.query('SELECT * FROM system_menu WHERE id=900120; SELECT * FROM system_role_menu; SELECT * FROM system_tenant_package;').stdout
        self.assertIn('TARGET_PREFLIGHT_PASS:', self.preflight())
        self.migrate()
        self.assertEqual('排产工单\t900120\t/mes/pro/schedule-order\tmes:pro-schedule-order:query\t1\n',
                         self.query('SELECT name,parent_id,path,permission,sort FROM system_menu WHERE id=5580;').stdout)
        after = self.snapshot()
        self.migrate()
        self.assertEqual(after, self.snapshot())
        self.assertIn('TARGET_PREFLIGHT_PASS:', self.preflight())
        self.assertEqual(before, self.query('SELECT * FROM system_menu WHERE id=900120; SELECT * FROM system_role_menu; SELECT * FROM system_tenant_package;').stdout)

    def test_unknown_identity_parent_and_route_conflicts_fail_closed(self):
        cases = ["UPDATE system_menu SET component='other/page' WHERE id=5580",
                 "UPDATE system_menu SET parent_id=999 WHERE id=5580",
                 "DELETE FROM system_menu WHERE id=900120",
                 "UPDATE system_menu SET path='/other' WHERE id=900120",
                 "INSERT INTO system_menu SELECT 777,name,permission,type,sort,parent_id,'/mes/pro/schedule-order',icon,component,component_name,status,visible,keep_alive,always_show,updater,update_time,deleted FROM system_menu WHERE id=5580"]
        for mutation in cases:
            with self.subTest(mutation=mutation):
                self.setUp()
                self.query(mutation)
                before = self.snapshot()
                self.assertIn('TARGET_PREFLIGHT_BLOCKED:', self.preflight())
                result = self.migrate(False)
                self.assertNotEqual(0, result.returncode)
                self.assertIn('MENU_FORWARD_BASELINE_CONFLICT', result.stderr)
                self.assertEqual(before, self.snapshot())

    def test_write_failure_is_not_reported_as_success(self):
        self.query("CREATE TRIGGER reject_repair BEFORE UPDATE ON system_menu FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test injected write failure';")
        before = self.snapshot()
        result = self.migrate(False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('test injected write failure', result.stderr)
        self.assertEqual(before, self.snapshot())

    def test_fresh_notification_chain_keeps_history_and_applies_successor(self):
        self.query("""CREATE TABLE system_notify_template (
          id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100),code VARCHAR(100),type INT,
          nickname VARCHAR(100),content TEXT,params TEXT,status INT,remark TEXT,
          creator VARCHAR(64),create_time DATETIME,updater VARCHAR(64),update_time DATETIME,deleted BIT);
        """)
        for name in ('20260901_dcc_registration_certificate_business_event_notify_template',
                     '20261120_dcc_registration_certificate_neutral_event_notify_template'):
            self.query((ROOT/'sql/mysql'/(name+'.sql')).read_text(encoding='utf-8'))
        content = self.query('SELECT name,content FROM system_notify_template;').stdout
        self.assertIn('注册证业务通知', content)
        self.assertNotIn('国内', content)


if __name__ == '__main__':
    unittest.main()
