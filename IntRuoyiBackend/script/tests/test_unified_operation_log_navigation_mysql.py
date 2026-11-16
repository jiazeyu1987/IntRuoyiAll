"""Verify the menu migration in a disposable, network-isolated MySQL container."""
import pathlib
import re
import subprocess
import time
import unittest
import uuid


class UnifiedOperationLogMigrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root = pathlib.Path(__file__).resolve().parents[2]
        cls.migration = (cls.root / "sql/mysql/20261112_unified_operation_log_navigation.sql").read_text(encoding="utf-8")
        cls.container = "unified-operation-log-test-" + uuid.uuid4().hex[:10]
        subprocess.run(["docker", "image", "inspect", "mysql:8.0.39"], check=True, capture_output=True)
        subprocess.run([
            "docker", "run", "--detach", "--pull=never", "--network=none", "--name", cls.container,
            "--tmpfs", "/var/lib/mysql", "--env", "MYSQL_ALLOW_EMPTY_PASSWORD=yes", "mysql:8.0.39"
        ], check=True, capture_output=True)
        cls.addClassCleanup(lambda: subprocess.run(["docker", "rm", "--force", cls.container], check=True, capture_output=True))
        for _ in range(60):
            ready = subprocess.run(["docker", "exec", cls.container, "mysqladmin", "--protocol=tcp", "-h127.0.0.1", "ping", "--silent"], capture_output=True)
            if ready.returncode == 0:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError("Isolated MySQL did not become ready")
        schema = (cls.root / "sql/mysql/ruoyi-vue-pro.sql").read_text(encoding="utf-8")
        cls.ddl = "\n".join(re.search(r"CREATE TABLE `" + table + r"`\s*\([\s\S]*?\) ENGINE[^;]*;", schema).group(0)
                            for table in ["system_menu", "system_role", "system_role_menu", "system_tenant_package"])

    @classmethod
    def sql(cls, source, database=None, check=True):
        command = ["docker", "exec", "-i", cls.container, "mysql", "--protocol=tcp", "-h127.0.0.1", "--default-character-set=utf8mb4", "--batch", "--skip-column-names", "-uroot"]
        if database:
            command.append(database)
        result = subprocess.run(command, input=source, encoding="utf-8", capture_output=True)
        if check and result.returncode:
            raise AssertionError(result.stderr)
        return result

    def setUp(self):
        self.database = "log_test_" + uuid.uuid4().hex[:10]
        self.sql(f"CREATE DATABASE {self.database} CHARACTER SET utf8mb4;")
        self.sql(self.ddl + """
INSERT INTO system_menu(id,name,type,parent_id,component,permission) VALUES
 (1,'System',1,0,NULL,''),(108,'Audit',1,1,NULL,''),
 (500,'Operation',2,108,'system/operatelog/index',''),
 (501,'Query',3,500,NULL,'system:operate-log:query'),
 (600,'CRM',3,0,NULL,'crm:customer:query'),
 (700,'DCC',2,0,'dcc/controlled-file/logs/index',''),
 (701,'DCC query',3,700,NULL,'dcc:controlled-file:log:query'),
 (702,'DCC export',3,700,NULL,'dcc:controlled-file:log:export'),
 (800,'Unrelated',3,0,NULL,'system:user:query'),
 (900,'API logs',2,0,NULL,''),
 (901,'Access',2,900,'infra/apiAccessLog/index','infra:api-access-log:query'),
 (902,'Error',2,900,'infra/apiErrorLog/index','infra:api-error-log:query'),
 (1100,'Mixed',2,0,NULL,''),
 (1101,'Access',2,1100,'infra/apiAccessLog/index','infra:api-access-log:query'),
 (1102,'Business',2,1100,'business/index','');
INSERT INTO system_role(id,name,code,sort,tenant_id,status,type) VALUES
 (10,'CRM','crm',0,1,0,2),(20,'DCC','dcc',0,2,0,2),
 (30,'Other','other',0,1,0,2),(40,'Disabled','disabled',0,1,1,2),
 (50,'Deleted grant','deleted-grant',0,1,0,2);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,deleted) VALUES
 (10,600,1,0),(20,700,2,0),(20,701,2,0),(20,702,2,0),
 (30,800,1,0),(40,600,1,0),(50,600,1,1);
INSERT INTO system_tenant_package(id,name,menu_ids) VALUES
 (10,'CRM','[600]'),(20,'DCC','[700,701,702]'),(30,'Other','[800]');
""", self.database)

    def query(self, source):
        return self.sql(source, self.database).stdout.strip()

    def test_preserves_grants_and_history_navigation_without_cross_tenant_or_data_permission_grants(self):
        self.sql(self.migration, self.database)
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_role_menu WHERE deleted=0 AND role_id IN(10,20) AND menu_id IN(1,108,500);"), "6")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_role_menu WHERE deleted=0 AND menu_id=501;"), "0")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_role_menu WHERE deleted=0 AND role_id IN(30,40,50) AND menu_id IN(1,108,500);"), "0")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_role_menu WHERE role_id=20 AND tenant_id=1;"), "0")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_role_menu WHERE role_id=20 AND tenant_id=2 AND menu_id IN(700,701,702) AND deleted=0;"), "3")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_menu WHERE id=700 AND type=3 AND visible=0 AND parent_id=500 AND component='';"), "1")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_menu WHERE id IN(701,702) AND type=3 AND parent_id=500 AND permission<>'';"), "2")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_menu WHERE id IN(900,901,902) AND type=3 AND visible=0 AND parent_id=500 AND component='';"), "3")
        self.assertEqual(self.query("SELECT type FROM system_menu WHERE id=1100;"), "2")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_tenant_package WHERE id IN(10,20) AND JSON_CONTAINS(menu_ids,'[1,108,500]');"), "2")
        self.assertEqual(self.query("SELECT menu_ids FROM system_tenant_package WHERE id=30;"), "[800]")
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_tenant_package WHERE JSON_CONTAINS(menu_ids,'501');"), "0")
        count = self.query("SELECT COUNT(*) FROM system_role_menu;")
        package_ids = self.query("SELECT menu_ids FROM system_tenant_package ORDER BY id;")
        self.sql(self.migration, self.database)
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_role_menu;"), count)
        self.assertEqual(self.query("SELECT menu_ids FROM system_tenant_package ORDER BY id;"), package_ids)

    def test_invalid_navigation_fails_before_any_grant_or_retirement(self):
        self.sql("UPDATE system_menu SET permission='system:user:query' WHERE id=108;", self.database)
        count = self.query("SELECT COUNT(*) FROM system_role_menu;")
        result = self.sql(self.migration, self.database, check=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("ancestor is missing, disabled or permission-bearing", result.stderr)
        self.assertEqual(self.query("SELECT COUNT(*) FROM system_role_menu;"), count)
        self.assertEqual(self.query("SELECT type FROM system_menu WHERE id=700;"), "2")


if __name__ == "__main__":
    unittest.main(verbosity=2)
