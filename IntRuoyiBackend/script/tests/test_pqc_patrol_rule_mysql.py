"""Real MySQL contract test; owns one disposable container, never uses an application DB.

Run: python IntRuoyiBackend/script/tests/test_pqc_patrol_rule_mysql.py
Requires the explicitly available local mysql:8.0.39 image and Docker; no automatic pull.
Fixture writes are migration tests, not business E2E acceptance.
"""
import re
import subprocess
import time
import unittest
import uuid
from pathlib import Path

SQL = Path(__file__).resolve().parents[2] / 'sql/mysql'
MID = '20261124_mes_pqc_patrol_rule_key_contract'


class PatrolRuleMysqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.name = 'intruoyi-patrol-contract-' + uuid.uuid4().hex[:10]
        subprocess.run(['docker', 'image', 'inspect', 'mysql:8.0.39'], check=True, capture_output=True)
        subprocess.run(['docker', 'run', '--name', cls.name, '--label', 'codex.task=pqc-patrol-contract',
                        '--network', 'none', '--tmpfs', '/var/lib/mysql:rw,size=1g',
                        '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', '-d',
                        'mysql:8.0.39', '--skip-log-bin'], check=True, capture_output=True)
        cls.addClassCleanup(lambda: subprocess.run(['docker', 'rm', '-f', cls.name], check=True, capture_output=True))
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            # The image starts and shuts down a temporary bootstrap server first.
            # Only PID 1 mysqld is the final server; bootstrap SELECT success is not readiness.
            process = subprocess.run(['docker', 'exec', cls.name, 'cat', '/proc/1/comm'], text=True, capture_output=True)
            probe = cls.query('SELECT VERSION();', database=False, check=False)
            if process.stdout.strip() == 'mysqld' and probe.returncode == 0:
                assert probe.stdout.strip().startswith('8.0.39'), probe.stdout
                print('MYSQL_VERSION', probe.stdout.strip(), flush=True)
                return
            time.sleep(1)
        logs = subprocess.run(['docker', 'logs', '--tail', '25', cls.name], text=True, capture_output=True)
        raise RuntimeError('Dedicated MySQL did not become ready in 120 seconds: ' + probe.stderr + logs.stdout + logs.stderr)

    @classmethod
    def query(cls, sql, *, database=True, check=True):
        command = ['docker', 'exec', '-i', cls.name, 'mysql', '-uroot', '--batch', '--raw',
                   '--default-character-set=utf8mb4', '--skip-column-names']
        if database:
            command.append('patrol_contract')
        result = subprocess.run(command, input=sql, text=True, encoding='utf-8', capture_output=True)
        if check and result.returncode:
            raise AssertionError(result.stderr)
        return result

    def setUp(self):
        self.query('DROP DATABASE IF EXISTS patrol_contract; CREATE DATABASE patrol_contract CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;', database=False)
        original = (SQL / '20260802_mes_pqc_inspection_task.sql').read_text(encoding='utf-8')
        table = re.search(r'CREATE TABLE IF NOT EXISTS `mes_pqc_inspection_task`.*?ENGINE=.*?;', original, re.S).group()
        self.query(table)
        self.query("ALTER TABLE mes_pqc_inspection_task ADD inspection_rule_key VARCHAR(32) NOT NULL, ADD inspection_item_plan_json JSON NULL;")
        self.seed()

    def seed(self):
        for identity, key in enumerate(('FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'), 1):
            self.query("INSERT INTO mes_pqc_inspection_task (id,active_order_id,work_order_id,route_id,route_version_id,route_process_id,process_id,regulation_version_id,inspection_type,business_date,shift_code,round_no,planned_inspection_quantity,task_status,tenant_id,deleted,inspection_rule_key,inspection_item_plan_json) VALUES "
                       f"({identity},1,2,3,4,5,6,7,'PATROL','2026-11-24','{key}',{identity},5,'SUBMITTED',{100 + identity},b'{identity % 2}','{key}',JSON_OBJECT('frozen','keep-{identity}'));" )

    def old_check(self, charset='utf8mb4'):
        source = (SQL / '20260814_mes_c015_route_dcc_qa_reconciliation_schema.sql').read_text(encoding='utf-8')
        check = re.search(r'ADD CONSTRAINT `chk_mes_pqc_inspection_rule_key`\s+CHECK.*?;', source, re.S).group()
        self.query('SET NAMES ' + charset + '; ALTER TABLE mes_pqc_inspection_task ' + check)

    def snapshot(self):
        return self.query('SELECT * FROM mes_pqc_inspection_task ORDER BY id;').stdout

    def migration(self, check=True):
        return self.query((SQL / (MID + '.sql')).read_text(encoding='utf-8'), check=check)

    def preflight(self):
        result = self.query((SQL / 'target-preflight' / (MID + '.preflight.sql')).read_text(encoding='utf-8')).stdout.strip()
        if result.startswith('TARGET_PREFLIGHT_BLOCKED:'):
            print('CHECK_DIAGNOSTIC', self.query("SELECT CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='chk_mes_pqc_inspection_rule_key';").stdout.strip(), flush=True)
        return result

    def assert_rejected(self, sql):
        result = self.query(sql, check=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('3819', result.stderr)

    def patrol_insert(self):
        return "INSERT INTO mes_pqc_inspection_task (id,active_order_id,work_order_id,route_id,route_version_id,route_process_id,process_id,regulation_version_id,inspection_type,business_date,shift_code,round_no,planned_inspection_quantity,task_status,tenant_id,inspection_rule_key) VALUES (101,1,2,3,4,5,6,7,'PATROL','2026-11-24','PATROL',1,5,'PENDING',101,'PATROL');"

    def test_old_check_reproduces_failure_then_migration_accepts_patrol_and_preserves_history(self):
        self.old_check()
        self.assert_rejected(self.patrol_insert())
        print('LEGACY_CHECK_REJECTED_PATROL_3819', flush=True)
        before = self.snapshot()
        self.assertEqual('TARGET_PREFLIGHT_PASS:' + MID, self.preflight())
        self.migration()
        self.assertEqual(before, self.snapshot())
        self.query(self.patrol_insert())
        self.assert_rejected("UPDATE mes_pqc_inspection_task SET inspection_rule_key='BOGUS' WHERE id=101;")
        self.assertEqual(before, self.query('SELECT * FROM mes_pqc_inspection_task WHERE id<100 ORDER BY id;').stdout)
        after = self.snapshot()
        definition = self.query('SHOW CREATE TABLE mes_pqc_inspection_task;').stdout
        self.migration()
        self.assertEqual(after, self.snapshot())
        self.assertEqual(definition, self.query('SHOW CREATE TABLE mes_pqc_inspection_task;').stdout)
        self.assertEqual('TARGET_PREFLIGHT_PASS:' + MID, self.preflight())

    def test_missing_check_gets_enforced_without_changing_history(self):
        before = self.snapshot()
        self.assertEqual('TARGET_PREFLIGHT_PASS:' + MID, self.preflight())
        self.migration()
        self.assertEqual(before, self.snapshot())
        self.query(self.patrol_insert())
        self.assert_rejected("UPDATE mes_pqc_inspection_task SET inspection_rule_key='INVALID' WHERE id=2;")
        self.assertEqual('YES', self.query("SELECT ENFORCED FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_inspection_task' AND CONSTRAINT_NAME='chk_mes_pqc_inspection_rule_key';").stdout.strip())
        self.migration()

    def test_latin1_legacy_check_representation_is_supported(self):
        self.old_check('latin1')
        before = self.snapshot()
        self.assertEqual('TARGET_PREFLIGHT_PASS:' + MID, self.preflight())
        self.migration()
        self.assertEqual(before, self.snapshot())
        self.query(self.patrol_insert())
        self.migration()
        self.assertEqual(before, self.query('SELECT * FROM mes_pqc_inspection_task WHERE id<100 ORDER BY id;').stdout)

    def test_invalid_history_including_deleted_rows_blocks_without_writes(self):
        self.query("UPDATE mes_pqc_inspection_task SET inspection_rule_key='UNKNOWN',deleted=b'1' WHERE id=2;")
        self.assert_blocked_and_unchanged()

    def test_null_history_blocks_without_writes(self):
        self.query('ALTER TABLE mes_pqc_inspection_task MODIFY inspection_rule_key VARCHAR(32) NULL; UPDATE mes_pqc_inspection_task SET inspection_rule_key=NULL WHERE id=2;')
        self.assert_blocked_and_unchanged()

    def test_unrecognized_constraint_blocks_without_replacement(self):
        self.query("ALTER TABLE mes_pqc_inspection_task ADD CONSTRAINT chk_mes_pqc_inspection_rule_key CHECK (inspection_rule_key <> 'BOGUS');")
        self.assert_blocked_and_unchanged()

    def test_disabled_constraint_blocks_without_replacement(self):
        self.old_check()
        self.query('ALTER TABLE mes_pqc_inspection_task ALTER CHECK chk_mes_pqc_inspection_rule_key NOT ENFORCED;')
        self.assert_blocked_and_unchanged()

    def test_wrong_column_contract_blocks_without_replacement(self):
        self.query('ALTER TABLE mes_pqc_inspection_task MODIFY inspection_rule_key VARCHAR(128) NOT NULL;')
        self.assert_blocked_and_unchanged()

    def assert_blocked_and_unchanged(self):
        before = self.snapshot()
        definition = self.query('SHOW CREATE TABLE mes_pqc_inspection_task;').stdout
        self.assertEqual('TARGET_PREFLIGHT_BLOCKED:' + MID, self.preflight())
        result = self.migration(check=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('1644', result.stderr)
        self.assertEqual(before, self.snapshot())
        self.assertEqual(definition, self.query('SHOW CREATE TABLE mes_pqc_inspection_task;').stdout)


if __name__ == '__main__':
    unittest.main(verbosity=2)
