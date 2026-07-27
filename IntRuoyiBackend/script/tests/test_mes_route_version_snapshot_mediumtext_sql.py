from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260727_mes_route_version_snapshot_mediumtext.sql"


def read_migration_sql() -> str:
    assert MIGRATION.exists(), (
        "missing route version snapshot capacity migration: "
        "sql/mysql/20260727_mes_route_version_snapshot_mediumtext.sql"
    )
    return MIGRATION.read_text(encoding="utf-8")


def test_route_version_snapshot_mediumtext_migration_metadata() -> None:
    first_line = read_migration_sql().splitlines()[0]

    assert first_line.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
    )
    assert "dependsOn=20260715_mes_route_version_lifecycle" in first_line
    assert "type=schema" in first_line
    assert "riskLevel=low" in first_line
    assert ".sql" not in first_line


def test_route_version_snapshot_mediumtext_migration_is_idempotent_and_fail_fast() -> None:
    sql = read_migration_sql()
    upper = sql.upper()

    assert "CREATE PROCEDURE ensure_mes_route_version_snapshot_mediumtext()" in sql
    assert "information_schema.TABLES" in sql
    assert "information_schema.COLUMNS" in sql
    assert "mes_pro_route_version is missing" in sql
    assert "route_snapshot_json is missing" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "TRUNCATE TABLE" not in upper
    assert "DELETE FROM" not in upper


def test_route_version_snapshot_column_expands_beyond_text_limit() -> None:
    sql = read_migration_sql()

    assert "DATA_TYPE NOT IN ('mediumtext', 'longtext', 'json')" in sql
    assert "MODIFY COLUMN `route_snapshot_json` MEDIUMTEXT" in sql
    assert "CHARACTER SET utf8mb4" in sql
    assert "COLLATE utf8mb4_unicode_ci" in sql
