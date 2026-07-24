from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260708_system_user_table_column_config.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing user table column config migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_user_table_column_config_migration_has_release_metadata() -> None:
    sql = _read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium"
    )


def test_user_table_column_config_migration_creates_expected_table_contract() -> None:
    sql = _read_sql()

    required = [
        "CREATE TABLE IF NOT EXISTS `system_user_table_column_config`",
        "`user_id` bigint NOT NULL",
        "`table_key` varchar(160) NOT NULL",
        "`config_json` json NOT NULL",
        "`tenant_id` bigint NOT NULL DEFAULT 0",
        "UNIQUE KEY `uk_system_user_table_column_config` (`tenant_id`, `user_id`, `table_key`)",
    ]
    for snippet in required:
        assert snippet in sql


def test_user_table_column_config_migration_is_non_destructive() -> None:
    upper_sql = _read_sql().upper()

    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
