from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260715_mes_route_version_lifecycle.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_mes_route_version_lifecycle_migration_declares_release_metadata_without_sql_suffixes() -> None:
    sql = read_sql()
    first_line = sql.splitlines()[0]

    assert first_line.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
    )
    assert "dependsOn=20260613_mes_smart_scheduling_t1_schema,20260608_edhr_batch_execution_schema" in first_line
    assert ".sql" not in first_line
    assert "type=schema" in first_line
    assert "riskLevel=medium" in first_line


def test_mes_route_version_lifecycle_migration_is_idempotent_and_fail_fast() -> None:
    sql = read_sql()
    upper = sql.upper()

    assert "CREATE PROCEDURE ensure_mes_route_version_lifecycle_schema()" in sql
    assert "information_schema.COLUMNS" in sql
    assert "information_schema.STATISTICS" in sql
    assert "duplicate active route versions must be resolved before migration" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "TRUNCATE TABLE" not in upper
    assert "DELETE FROM" not in upper


def test_mes_route_version_lifecycle_migration_adds_route_and_edhr_freeze_columns() -> None:
    sql = read_sql()

    for column in [
        "lifecycle_status",
        "change_summary_json",
        "validation_result_json",
        "submitted_by",
        "submitted_time",
        "approval_process_instance_id",
        "published_by",
        "published_time",
        "active_unique_flag",
    ]:
        assert f"COLUMN_NAME = '{column}'" in sql
        assert f"ADD COLUMN `{column}`" in sql

    for column in ["route_version_id", "route_version_no", "route_snapshot_json"]:
        assert f"TABLE_NAME = 'mes_pro_edhr_batch_execution'" in sql
        assert f"COLUMN_NAME = '{column}'" in sql
        assert f"ADD COLUMN `{column}`" in sql


def test_mes_route_version_lifecycle_migration_creates_active_unique_and_freeze_indexes() -> None:
    sql = read_sql()

    for index_name in [
        "uk_mes_pro_route_version_active_one",
        "idx_mes_pro_edhr_batch_execution_route_version",
    ]:
        assert f"INDEX_NAME = '{index_name}'" in sql
        assert f"`{index_name}`" in sql
