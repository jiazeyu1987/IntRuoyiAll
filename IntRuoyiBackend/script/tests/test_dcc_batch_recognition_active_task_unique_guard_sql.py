from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260710_dcc_batch_recognition_active_task_unique_guard.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"


def test_active_task_unique_guard_migration_is_idempotent_and_fail_fast():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260710_dcc_file_category_batch_task; "
        "type=schema; riskLevel=medium"
    )
    assert "active_recognition_type" in sql
    assert "uk_dcc_batch_recognition_task_active_type" in sql
    assert "information_schema.COLUMNS" in sql
    assert "information_schema.STATISTICS" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "WAITING" in sql
    assert "RUNNING" in sql
    assert "tenant_id" in sql
    assert "DROP TABLE" not in sql.upper()
    assert "DROP COLUMN" not in sql.upper()


def test_active_task_unique_guard_exists_in_test_schema():
    sql = TEST_SCHEMA_SQL.read_text(encoding="utf-8")

    assert "`active_recognition_type` varchar(32) GENERATED ALWAYS AS" in sql
    assert "uk_dcc_batch_recognition_task_active_type" in sql
    assert (
        "UNIQUE KEY `uk_dcc_batch_recognition_task_active_type` "
        "(`tenant_id`, `active_recognition_type`)"
    ) in sql
