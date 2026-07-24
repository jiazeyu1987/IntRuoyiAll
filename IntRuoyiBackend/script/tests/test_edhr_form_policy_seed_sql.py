from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260720_edhr_release_void_form_policy_seed.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR release/void form policy seed migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_form_policy_seed_declares_release_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260717_bpm_form_center; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_edhr_release_void_form_policy" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "EDHR form policy requires bpm_form_action_policy" in sql
    assert "EDHR form policy requires ACT_RE_PROCDEF" in sql
    assert "EDHR form policy requires release and void process definitions" in sql
    assert "EDHR release form policy conflict" in sql
    assert "EDHR batch void form policy conflict" in sql
    assert "EDHR form policy duplicate" in sql


def test_edhr_form_policy_seed_binds_release_and_void_effect_contracts() -> None:
    sql = read_sql()

    for literal in [
        "'MES'",
        "'EDHR_BATCH_EXECUTION'",
        "'RELEASE'",
        "'PRECHECK_PASSED'",
        "'EDHR_RELEASE'",
        "'VOID'",
        "'CLOSED'",
        "'EDHR_BATCH_VOID'",
        "'PUBLISHED'",
        "'[]'",
        "'eDHR release approval through form center'",
        "'eDHR batch void approval through form center'",
    ]:
        assert literal in sql

    assert "mes-edhr-approval-v1" in sql
    assert "mes-edhr-batch-execution-void-v1" in sql
    assert re.search(
        r"INSERT\s+INTO\s+`bpm_form_action_policy`[\s\S]+"
        r"`data_domain`[\s\S]+`system_code`[\s\S]+`object_type`[\s\S]+"
        r"`action_code`[\s\S]+`object_state`[\s\S]+`policy_type`[\s\S]+"
        r"`bpm_process_key`[\s\S]+`effect_executor_code`",
        sql,
        re.I,
    )
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'EDHR_RELEASE'" in sql
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'EDHR_BATCH_VOID'" in sql
    assert "COALESCE(`policy`.`policy_type`, '') <> 'NONE'" in sql
    assert "COALESCE(`policy`.`slots_json`, '[]') <> '[]'" in sql


def test_edhr_form_policy_seed_derives_tenants_from_existing_bpm_process_definitions() -> None:
    sql = read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_edhr_policy_source`" in sql
    assert "`proc`.`TENANT_ID_`" in sql
    assert "`proc`.`KEY_` IN ('mes-edhr-approval-v1', 'mes-edhr-batch-execution-void-v1')" in sql
    assert "CAST(`proc`.`TENANT_ID_` AS UNSIGNED)" in sql
    assert "`proc`.`SUSPENSION_STATE_` = 1" in sql
    assert "`proc`.`TENANT_ID_` REGEXP '^[0-9]+$'" in sql
    assert "GROUP BY `proc`.`TENANT_ID_`" in sql
    assert "FROM `tmp_edhr_policy_source` AS `source`" in sql
    assert "`source`.`tenant_id`" in sql
    assert "`source`.`release_process_key`" in sql
    assert "`source`.`void_process_key`" in sql

    assert "flowable:assignee" not in sql
    assert "smokeappr" not in sql
    assert "smokeplan" not in sql


def test_edhr_form_policy_seed_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS ensure_edhr_release_void_form_policy" in sql
    assert "WHERE NOT EXISTS (" in sql
    assert "HAVING COUNT(*) > 1" in sql
    assert "CALL ensure_edhr_release_void_form_policy();" in sql
    assert "DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_policy_source`" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
