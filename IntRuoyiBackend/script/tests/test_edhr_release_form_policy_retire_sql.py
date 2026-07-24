from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
HISTORICAL_SQL_PATH = ROOT / "sql" / "mysql" / "20260720_edhr_release_void_form_policy_seed.sql"
SQL_PATH = ROOT / "sql" / "mysql" / "20260722_edhr_release_form_policy_retire.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR release form policy retirement migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_release_historical_form_policy_seed_remains_auditable() -> None:
    assert HISTORICAL_SQL_PATH.exists(), "historical eDHR release form policy seed must remain auditable"
    sql = HISTORICAL_SQL_PATH.read_text(encoding="utf-8")

    assert "'EDHR_BATCH_EXECUTION'" in sql
    assert "'RELEASE'" in sql
    assert "'PRECHECK_PASSED'" in sql
    assert "'EDHR_RELEASE'" in sql


def test_edhr_release_form_policy_retire_declares_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260720_edhr_release_void_form_policy_seed; "
        "type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_edhr_release_form_policy_retired" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "bpm_form_action_policy" in sql
    assert "EDHR release policy retirement requires bpm_form_action_policy" in sql


def test_edhr_release_form_policy_retire_disables_published_release_policy_without_deleting_history() -> None:
    sql = read_sql()

    assert "'MES'" in sql
    assert "'EDHR_BATCH_EXECUTION'" in sql
    assert "'RELEASE'" in sql
    assert "'PRECHECK_PASSED'" in sql
    assert "'EDHR_RELEASE'" in sql
    assert "`status` = 'RETIRED'" in sql
    assert "`status` = 'PUBLISHED'" in sql
    assert "owner electronic signature submit" in sql
    assert "EDHR release must not keep a published form-center approval policy" in sql


def test_edhr_release_form_policy_retire_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    assert "DROP PROCEDURE IF EXISTS ensure_edhr_release_form_policy_retired" in sql
    assert "CALL ensure_edhr_release_form_policy_retired();" in sql
    assert "WHERE `data_domain` = 'MES'" in sql
    assert "AND `system_code` = 'MES'" in sql
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert "fallback" not in sql.lower()
