import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260809_mes_team_leader_employee_scope_backfill.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing production employee leader-scope backfill migration"
    return SQL_PATH.read_text(encoding="utf-8")


def compact(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).strip()


def test_migration_metadata_transaction_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260805_mes_process_pool_production_personnel; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_mes_pp_employee_scope_backfill_20260809" in sql
    assert "START TRANSACTION;" in sql
    assert "DECLARE EXIT HANDLER FOR SQLEXCEPTION" in sql
    assert "ROLLBACK;" in sql
    assert "RESIGNAL;" in sql
    assert "COMMIT;" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Missing mes_pro_process_pool_team_employee_profile" in sql
    assert "Missing mes_pro_process_pool_team_leader_scope" in sql
    assert "Duplicate production employee profile leader identity" in sql
    assert "Duplicate active production employee leader scope" in sql
    assert "Production employee profile and leader scope enabled status mismatch" in sql
    assert "Production employee leader scope backfill verification failed" in sql


def test_backfill_uses_the_same_employee_identity_as_runtime_scope_sync() -> None:
    sql = read_sql()
    flat = compact(sql)

    assert "mes_pro_process_pool_team_employee_profile" in sql
    assert "mes_pro_process_pool_team_leader_scope" in sql
    assert "COALESCE(`profile`.`system_user_id`, `profile`.`id`)" in sql
    assert "'PRODUCTION'" in sql
    assert "'EMPLOYEE'" in sql
    assert "`profile`.`deleted` = b'0'" in sql
    assert "`existing`.`deleted` = b'0'" in sql
    assert "`existing`.`tenant_id` = `profile`.`tenant_id`" in sql
    assert "`existing`.`leader_user_id` = `profile`.`leader_user_id`" in sql
    assert "`existing`.`employee_user_id` = COALESCE(`profile`.`system_user_id`, `profile`.`id`)" in sql
    assert "`profile`.`enabled`" in sql
    assert "AND NOT EXISTS" in flat


def test_backfill_is_idempotent_and_does_not_mutate_existing_scope_rows() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    assert "INSERT INTO `mes_pro_process_pool_team_leader_scope`" in sql
    assert "20260809-team-leader-scope-backfill" in sql
    for forbidden in [
        "UPDATE `MES_PRO_PROCESS_POOL_TEAM_LEADER_SCOPE`",
        "DELETE FROM `MES_PRO_PROCESS_POOL_TEAM_LEADER_SCOPE`",
        "TRUNCATE TABLE `MES_PRO_PROCESS_POOL_TEAM_LEADER_SCOPE`",
        "DROP TABLE `MES_PRO_PROCESS_POOL_TEAM_LEADER_SCOPE`",
    ]:
        assert forbidden not in upper_sql
