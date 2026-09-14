from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260727_system_profile_workbench_task_visibility.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing profile workbench task visibility migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_profile_workbench_task_visibility_migration_has_release_metadata() -> None:
    sql = _read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260708_system_user_table_column_config; type=schema; riskLevel=medium"
    )


def test_profile_workbench_task_visibility_migration_creates_expected_table_contract() -> None:
    sql = _read_sql()

    required = [
        "CREATE TABLE IF NOT EXISTS `system_profile_workbench_task_visibility`",
        "`user_id` bigint NOT NULL",
        "`task_key` varchar(160) NOT NULL",
        "`task_type` varchar(64) NOT NULL",
        "`source` varchar(64) NOT NULL",
        "`business_id` varchar(80) DEFAULT NULL",
        "`detail` varchar(500) DEFAULT NULL",
        "`hidden_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP",
        "`tenant_id` bigint NOT NULL DEFAULT 0",
        "UNIQUE KEY `uk_system_profile_workbench_task_visibility` (`tenant_id`, `user_id`, `task_key`, `deleted`)",
    ]
    for snippet in required:
        assert snippet in sql


def test_profile_workbench_task_visibility_migration_is_non_destructive() -> None:
    upper_sql = _read_sql().upper()

    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
