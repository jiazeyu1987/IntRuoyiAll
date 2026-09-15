from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260812_mes_route_version_snapshot_identity_enforce.sql"
MIGRATION_ID = "20260812_mes_route_version_snapshot_identity_enforce"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"missing migration SQL: {MIGRATION_ID}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_snapshot_identity_enforce_declares_release_contract() -> None:
    sql = read_sql()
    first_line = sql.splitlines()[0]

    assert first_line.startswith("-- release-migration: allowedEnvironments=test,backup,prod; ")
    assert "dependsOn=20260812_mes_route_version_snapshot_identity" in first_line
    assert "type=schema" in first_line
    assert "riskLevel=high" in first_line
    assert "approvedHook=route-snapshot-identity" in first_line
    assert MIGRATION_ID in str(SQL_PATH)


def test_snapshot_identity_enforce_fails_before_not_null_without_ready_identity() -> None:
    sql = read_sql()

    assert "route snapshot identity nullable migration is missing" in sql
    assert "route snapshot identity blockers must be zero before enforcement" in sql
    assert "`route_snapshot_sha256` IS NULL" in sql
    assert "TRIM(`route_snapshot_sha256`) = ''" in sql
    assert "`route_snapshot_sha256` NOT REGEXP '^[0-9a-f]{64}$'" in sql
    assert "`route_snapshot_format_version` <> 'MES_ROUTE_SNAPSHOT_CANONICAL_V1'" in sql
    assert "IF blocker_count <> 0 THEN" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql


def test_snapshot_identity_enforce_only_tightens_existing_columns() -> None:
    sql = read_sql()
    upper = sql.upper()

    assert "ALTER TABLE `mes_pro_route_version`" in sql
    assert "MODIFY COLUMN `route_snapshot_sha256` varchar(64) NOT NULL" in sql
    assert "MODIFY COLUMN `route_snapshot_format_version` varchar(32) NOT NULL" in sql

    for forbidden in ("INSERT INTO", "UPDATE `", "DELETE FROM", "TRUNCATE TABLE"):
        assert forbidden not in upper
