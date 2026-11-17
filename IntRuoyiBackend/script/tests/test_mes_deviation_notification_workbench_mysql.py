"""Exercise deviation migrations in disposable schemas, never application databases."""
import json
import subprocess
import unittest
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql/mysql"

class DeviationNotificationWorkbenchMysqlTest(unittest.TestCase):
    def setUp(self):
        self.schema = "codex_deviation_fix_" + uuid.uuid4().hex
        self.run_sql("CREATE DATABASE `" + self.schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
    def tearDown(self):
        self.run_sql("DROP DATABASE `" + self.schema + "`;")
    def run_sql(self, sql, select_schema=False, expect_error=False):
        if select_schema:
            sql = "USE `" + self.schema + "`;\n" + sql
        result = subprocess.run(["docker", "exec", "-i", "int-ruoyi-mysql", "sh", "-c",
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root --default-character-set=utf8mb4 --batch --raw --skip-column-names'],
            input=sql, encoding="utf-8", capture_output=True, timeout=30)
        if expect_error:
            self.assertNotEqual(0,result.returncode)
            return result.stderr
        self.assertEqual(0,result.returncode,result.stderr)
        return result.stdout.strip()
    def test_category_preserves_hidden_history_and_is_idempotent(self):
        ddl = """
CREATE TABLE mes_pro_edhr_work_task(id BIGINT,tenant_id BIGINT,task_type VARCHAR(64)) ENGINE=InnoDB;
CREATE TABLE system_workbench_visibility_context(tenant_id BIGINT,user_id BIGINT,visibility_version BIGINT,PRIMARY KEY(tenant_id,user_id)) ENGINE=InnoDB;
CREATE TABLE system_profile_workbench_task_visibility(
 id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,task_key VARCHAR(160),task_type VARCHAR(64),source VARCHAR(64),business_id VARCHAR(80),detail VARCHAR(500),hidden_at DATETIME,
 creator VARCHAR(64),create_time DATETIME,updater VARCHAR(64),update_time DATETIME,deleted BIT,tenant_id BIGINT,
 UNIQUE KEY identity_key(tenant_id,user_id,task_key,deleted)) ENGINE=InnoDB;
INSERT INTO mes_pro_edhr_work_task VALUES(13,7,'DEVIATION_VERIFY'),(14,7,'FILL'),(13,8,'FILL');
INSERT INTO system_workbench_visibility_context VALUES(7,42,1),(8,42,1);
INSERT INTO system_profile_workbench_task_visibility(user_id,task_key,task_type,source,business_id,detail,hidden_at,creator,create_time,updater,update_time,deleted,tenant_id) VALUES
(42,'EDHR_WORK_TASK:13','批记录','eDHR工作任务','13','原始隐藏快照','2026-11-01','tester','2026-11-01','tester','2026-11-01',0,7),
(42,'EDHR_WORK_TASK:14','批记录','eDHR工作任务','14','普通任务','2026-11-01','tester','2026-11-01','tester','2026-11-01',0,7),
(42,'EDHR_WORK_TASK:13','批记录','eDHR工作任务','13','其他租户普通任务','2026-11-01','tester','2026-11-01','tester','2026-11-01',0,8);
"""
        self.run_sql(ddl,True)
        migration=(SQL/'20261117_mes_edhr_deviation_workbench_category.sql').read_text(encoding='utf-8')
        self.run_sql(migration,True)
        self.run_sql(migration,True)
        rows=self.run_sql("SELECT task_key,task_type,source,detail,DATE(hidden_at),user_id,tenant_id FROM system_profile_workbench_task_visibility WHERE task_key='EDHR_DEVIATION_TASK:13';",True)
        self.assertEqual('EDHR_DEVIATION_TASK:13\t偏差\teDHR偏差任务\t原始隐藏快照\t2026-11-01\t42\t7',rows)
        self.assertEqual('4',self.run_sql('SELECT COUNT(*) FROM system_profile_workbench_task_visibility;',True))
        self.assertEqual('1',self.run_sql('SELECT visibility_version FROM system_workbench_visibility_context WHERE tenant_id=8;',True))
    def test_readable_templates_fail_on_missing_dependencies_then_update_all_duties(self):
        self.run_sql("""CREATE TABLE system_notify_template(id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(128),code VARCHAR(128),type INT,nickname VARCHAR(128),content VARCHAR(2000),params JSON,status INT,remark VARCHAR(1000),creator VARCHAR(64),create_time DATETIME,updater VARCHAR(64),update_time DATETIME,deleted BIT);
INSERT INTO system_notify_template(code,content,deleted) VALUES('MES_EDHR_DEVIATION_RECEIVE_ASSIGNED','old URL',0),('MES_EDHR_DEVIATION_CORRECT_ASSIGNED','old URL',0);""",True)
        migration=(SQL/'20261117_mes_edhr_deviation_task_readable_notifications.sql').read_text(encoding='utf-8')
        error=self.run_sql(migration,True,True)
        self.assertIn('禁止执行可读历史通知迁移',error)
        self.assertEqual('2',self.run_sql("SELECT COUNT(*) FROM system_notify_template WHERE content='old URL';",True))
        self.run_sql("INSERT INTO system_notify_template(code,content,deleted) VALUES('MES_EDHR_DEVIATION_REVIEW_ASSIGNED','old URL',0);",True)
        self.run_sql(migration,True)
        self.run_sql(migration,True)
        self.assertEqual('4',self.run_sql('SELECT COUNT(*) FROM system_notify_template;',True))
        self.assertEqual('0',self.run_sql("SELECT COUNT(*) FROM system_notify_template WHERE content LIKE '%actionUrl%' OR NOT JSON_CONTAINS(params,JSON_QUOTE('taskTitle'));",True))

if __name__ == '__main__':
    unittest.main(verbosity=2)
