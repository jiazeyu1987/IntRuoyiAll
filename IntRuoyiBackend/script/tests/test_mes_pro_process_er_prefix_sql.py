from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260707_mes_pro_process_er_prefix.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "MES process ER prefix migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_mes_process_er_prefix_migration_has_collision_guard() -> None:
    sql = read_sql()

    for token in [
        "@collision_count",
        "WHERE @collision_count = 0",
        "AND source.tenant_id = target.tenant_id",
        "CONCAT('ER', SUBSTRING(source.code, 11)) = target.code",
    ]:
        assert token in sql, f"migration must include collision guard token: {token}"


def test_mes_process_er_prefix_migration_has_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium"
    )


def test_mes_process_er_prefix_migration_only_rewrites_old_prefix() -> None:
    sql = read_sql()

    assert "SET code = CONCAT('ER', SUBSTRING(code, 11))" in sql
    assert "code LIKE 'EDHR\\\\_PROC\\\\_%'" in sql
    assert "deleted = b'0'" in sql


def test_mes_process_er_prefix_migration_is_non_destructive() -> None:
    upper = read_sql().upper()

    for forbidden in [
        "DELETE FROM MES_PRO_PROCESS",
        "TRUNCATE TABLE MES_PRO_PROCESS",
        "DROP TABLE MES_PRO_PROCESS",
        "ALTER TABLE MES_PRO_PROCESS",
    ]:
        assert forbidden not in upper
