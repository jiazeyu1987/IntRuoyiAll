"""Real MySQL regression checks. Requires Docker and the local mysql:8.0.39 image.

Run: python -m unittest discover -s script/tests -p test_kingdee_warehouse_tenant_active_unique_mysql.py -v
Uses a task-owned container without networking, ports or persistent volumes.
"""
from pathlib import Path
import subprocess
import time
import unittest
import uuid


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql/mysql/20261112_kingdee_warehouse_tenant_active_unique.sql"
BASE = ROOT / "sql/mysql/20260513_kingdee_multi_sync.sql"
TABLE = "erp_kingdee_warehouse_sync_record"
OLD_INDEX = "uk_erp_kingdee_warehouse_source"


class KingdeeWarehouseTenantActiveUniqueMySqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = "kingdee-warehouse-migration-test-" + uuid.uuid4().hex[:12]
        subprocess.run([
            "docker", "run", "--pull=never", "--rm", "--detach", "--network=none",
            "--name", cls.container, "--env", "MYSQL_ALLOW_EMPTY_PASSWORD=yes", "mysql:8.0.39",
        ], check=True, capture_output=True, text=True)
        cls.addClassCleanup(lambda: subprocess.run(
            ["docker", "rm", "--force", cls.container], check=True, capture_output=True, text=True))
        deadline = time.monotonic() + 60
        while True:
            ready = cls.sql("SELECT 1", check=False)
            if ready.returncode == 0:
                break
            if time.monotonic() >= deadline:
                raise RuntimeError("Isolated MySQL did not become ready: " + ready.stderr)
            time.sleep(1)

    @classmethod
    def sql(cls, sql, check=True):
        result = subprocess.run([
            "docker", "exec", "-i", cls.container, "mysql", "-uroot",
            "--protocol=TCP", "--host=127.0.0.1",
            "--batch", "--raw", "--skip-column-names", "--default-character-set=utf8mb4",
        ], input=sql, encoding="utf-8", capture_output=True)
        if check and result.returncode:
            raise RuntimeError(result.stderr)
        return result

    def setUp(self):
        self.database = "test_" + uuid.uuid4().hex
        self.sql(f"CREATE DATABASE `{self.database}`;")
        self.addCleanup(lambda: self.sql(f"DROP DATABASE `{self.database}`;"))
        base = BASE.read_text(encoding="utf-8")
        start = base.index(f"CREATE TABLE IF NOT EXISTS `{TABLE}`")
        end = base.index(";", start) + 1
        self.query(base[start:end])

    def query(self, sql, check=True):
        return self.sql(f"USE `{self.database}`;\n" + sql, check=check)

    def migrate(self, check=True):
        return self.query(MIGRATION.read_text(encoding="utf-8"), check=check)

    def insert(self, tenant, warehouse=193, check=True):
        return self.query(
            f"INSERT INTO `{TABLE}` (tenant_id,source_stock_org_number,source_warehouse_number,warehouse_id) "
            f"VALUES ({tenant},'881','010.01',{warehouse});", check=check)

    def rows(self):
        return self.query(
            f"SELECT id,tenant_id,deleted+0,source_stock_org_number,source_warehouse_number,warehouse_id "
            f"FROM `{TABLE}` ORDER BY id;").stdout

    def test_existing_mapping_preserved_and_other_tenant_can_sync(self):
        self.insert(1)
        before = self.rows()
        # Reproduce the reported failure using the original schema.
        failure = self.insert(186, 400, check=False)
        self.assertNotEqual(0, failure.returncode)
        self.assertIn(OLD_INDEX, failure.stderr)
        self.migrate()
        self.assertEqual(before, self.rows())
        self.insert(186, 400)
        self.assertEqual("1\t193\n186\t400\n", self.query(
            f"SELECT tenant_id,warehouse_id FROM `{TABLE}` ORDER BY tenant_id;").stdout)

    def test_same_tenant_active_duplicate_remains_an_error(self):
        self.migrate()
        self.insert(186)
        failure = self.insert(186, 999, check=False)
        self.assertNotEqual(0, failure.returncode)
        self.assertIn("uk_erp_kingdee_warehouse_tenant_active_source", failure.stderr)
        self.assertEqual("193\n", self.query(f"SELECT warehouse_id FROM `{TABLE}`;").stdout)

    def test_repeated_delete_and_resync_preserves_history(self):
        self.insert(186)
        self.query(f"UPDATE `{TABLE}` SET deleted=1;")
        before = self.rows()
        self.migrate()
        self.assertEqual(before, self.rows())
        for warehouse in (400, 401):
            self.insert(186, warehouse)
            self.query(f"UPDATE `{TABLE}` SET deleted=1 WHERE deleted=0;")
        self.insert(186, 402)
        self.assertEqual("3\t1\n", self.query(
            f"SELECT SUM(deleted=1),SUM(deleted=0) FROM `{TABLE}`;").stdout)

    def test_migration_can_be_repeated_without_changing_records(self):
        self.insert(1)
        self.migrate()
        self.insert(186, 400)
        before = self.rows()
        self.migrate()
        self.assertEqual(before, self.rows())
        self.assertEqual("tenant_id,source_stock_org_number,source_warehouse_number,active_source_flag\n",
                         self.query(
                             "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) "
                             "FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() "
                             f"AND TABLE_NAME='{TABLE}' AND INDEX_NAME="
                             "'uk_erp_kingdee_warehouse_tenant_active_source';").stdout)

    def test_missing_table_fails_clearly(self):
        self.query(f"DROP TABLE `{TABLE}`;")
        failure = self.migrate(check=False)
        self.assertNotEqual(0, failure.returncode)
        self.assertIn("KINGDEE_WAREHOUSE_MAPPING_TABLE_MISSING", failure.stderr)

    def test_existing_active_duplicates_fail_without_rewriting_data(self):
        self.query(f"ALTER TABLE `{TABLE}` DROP INDEX `{OLD_INDEX}`;")
        self.insert(186)
        self.insert(186, 400)
        before = self.rows()
        failure = self.migrate(check=False)
        self.assertNotEqual(0, failure.returncode)
        self.assertIn("KINGDEE_WAREHOUSE_ACTIVE_DUPLICATES_REQUIRE_REVIEW", failure.stderr)
        self.assertEqual(before, self.rows())


if __name__ == "__main__":
    unittest.main()
