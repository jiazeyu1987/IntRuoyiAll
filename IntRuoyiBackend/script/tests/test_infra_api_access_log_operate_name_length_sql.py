from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260708_infra_api_access_log_operate_name_length.sql"
TEST_SCHEMA_PATH = REPO_ROOT / "yudao-module-infra" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"missing file: {path}"
    return path.read_text(encoding="utf-8")


def test_infra_api_access_log_operate_name_migration_has_release_metadata() -> None:
    sql = _read(SQL_PATH)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=low"
    )


def test_infra_api_access_log_operate_name_migration_expands_column() -> None:
    sql = _read(SQL_PATH)

    assert "ALTER TABLE `infra_api_access_log`" in sql
    assert "MODIFY COLUMN `operate_name` varchar(128)" in sql
    assert "COMMENT '操作名'" in sql


def test_infra_api_access_log_operate_name_test_schema_matches_runtime_contract() -> None:
    test_schema = _read(TEST_SCHEMA_PATH)

    assert "`operate_name`             varchar(128)  NOT NULL" in test_schema


def test_infra_api_access_log_operate_name_migration_is_non_destructive() -> None:
    upper_sql = _read(SQL_PATH).upper()

    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
