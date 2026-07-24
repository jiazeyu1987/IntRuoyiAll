from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
HISTORICAL_SQL_PATH = ROOT / "sql" / "mysql" / "20260720_mes_schedule_replan_form_policy_seed.sql"
SQL_PATH = ROOT / "sql" / "mysql" / "20260721_mes_schedule_replan_approval_retire.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES schedule replan approval retirement migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_mes_schedule_replan_historical_approval_seed_is_kept_for_retirement_context() -> None:
    assert HISTORICAL_SQL_PATH.exists(), "historical MES schedule replan approval seed must remain auditable"
    sql = HISTORICAL_SQL_PATH.read_text(encoding="utf-8")

    assert "'MES_SCHEDULE_REPLAN'" in sql
    assert "'SCHEDULE_REPLAN_SCOPE'" in sql
    assert "'mes-schedule-replan-approval-v1'" in sql


def test_mes_schedule_replan_approval_retire_declares_release_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260720_mes_schedule_replan_form_policy_seed; "
        "type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_mes_schedule_replan_approval_retired" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "bpm_form_action_policy" in sql

    upper_sql = sql.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql


def test_mes_schedule_replan_approval_retire_disables_published_policy_without_deleting_history() -> None:
    sql = read_sql()

    assert "'MES'" in sql
    assert "'SCHEDULE_REPLAN_SCOPE'" in sql
    assert "'REPLAN'" in sql
    assert "'READY'" in sql
    assert "'MES_SCHEDULE_REPLAN'" in sql
    assert "`status` = 'RETIRED'" in sql
    assert "`status` = 'PUBLISHED'" in sql
    assert "Manual replan is not approval-backed" in sql


def test_mes_schedule_replan_approval_retire_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS ensure_mes_schedule_replan_approval_retired" in sql
    assert "CALL ensure_mes_schedule_replan_approval_retired();" in sql
    assert "WHERE `data_domain` = 'MES'" in sql
    assert "WHERE `system_code` = 'MES'" in sql
