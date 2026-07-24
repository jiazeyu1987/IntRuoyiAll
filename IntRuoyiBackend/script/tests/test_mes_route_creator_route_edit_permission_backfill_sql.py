from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = (
    REPO_ROOT
    / "sql"
    / "mysql"
    / "20260722_mes_route_creator_route_edit_permission_backfill.sql"
)


def read_sql() -> str:
    assert SQL_PATH.exists(), f"required file missing: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_creator_permission_backfill_declares_release_contract() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260615_mes_edhr_tail_four_goals; "
        "type=data; riskLevel=low"
    )
    assert "CREATE PROCEDURE ensure_mes_route_creator_route_edit_permission_backfill()" in sql
    assert "CALL ensure_mes_route_creator_route_edit_permission_backfill();" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_mes_route_creator_route_edit_permission_backfill;" in sql


def test_route_creator_permission_backfill_is_idempotent_and_complete() -> None:
    sql = read_sql()

    assert "`route`.`creator` REGEXP '^[0-9]+$'" in sql
    assert "CAST(`route`.`creator` AS UNSIGNED)" in sql
    assert "`scope`.`tenant_id` = `route`.`tenant_id`" in sql
    assert "'VIEW' AS `ability`" in sql
    assert "UNION ALL SELECT 'ROUTE_EDIT'" in sql
    assert "UNION ALL SELECT 'PERMISSION_ADMIN'" in sql
    assert sql.count("NOT EXISTS") >= 3


def test_route_creator_permission_backfill_is_non_destructive_and_fail_fast() -> None:
    sql = read_sql()
    upper = sql.upper()

    for destructive in ("DELETE FROM", "TRUNCATE TABLE", "DROP TABLE"):
        assert destructive not in upper

    assert "`route`.`id` = 922119" in sql
    assert "Route 922119 creator permission scope backfill failed" in sql
    assert "Route 922119 creator ROUTE_EDIT rule backfill failed" in sql
    assert sql.count("SIGNAL SQLSTATE '45000'") == 2
