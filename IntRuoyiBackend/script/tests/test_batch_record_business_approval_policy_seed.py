from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260721_batch_record_bpm_policy_seed.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing batch record BPM business approval policy seed migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_batch_record_policy_seed_declares_fail_fast_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_batch_record_bpm_policy" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Batch record BPM policy seed requires bpm_business_approval_policy" in sql
    assert "Batch record BPM policy seed requires ACT_RE_PROCDEF" in sql
    assert "Batch record BPM policy requires version and execution process definitions" in sql
    assert "Batch record version BPM policy conflict" in sql
    assert "Batch record execution BPM policy conflict" in sql
    assert "Batch record BPM policy duplicate" in sql


def test_batch_record_policy_seed_binds_two_platform_policy_keys() -> None:
    sql = read_sql()

    for literal in [
        "'MES'",
        "'BATCH_RECORD_VERSION'",
        "'PUBLISH'",
        "'PRECHECK_PASSED'",
        "'EDHR_BATCH_EXECUTION'",
        "'SUBMIT_REVIEW'",
        "'DRAFT'",
        "'BPM_REQUIRED'",
        "'PUBLISHED'",
        "'MES_BATCH_RECORD_VERSION_PUBLISH'",
        "'EDHR_BATCH_EXECUTION_SUBMIT_REVIEW'",
        "'mes-batch-record-version-approval-v1'",
        "'mes-edhr-approval-v1'",
    ]:
        assert literal in sql

    assert re.search(
        r"INSERT\s+INTO\s+`bpm_business_approval_policy`[\s\S]+"
        r"`data_domain`[\s\S]+`system_code`[\s\S]+`object_type`[\s\S]+"
        r"`action_code`[\s\S]+`object_state`[\s\S]+`policy_mode`[\s\S]+"
        r"`process_definition_key`[\s\S]+`effect_executor_code`",
        sql,
        re.I,
    )
    assert "COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'" in sql
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'MES_BATCH_RECORD_VERSION_PUBLISH'" in sql
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'EDHR_BATCH_EXECUTION_SUBMIT_REVIEW'" in sql


def test_batch_record_policy_seed_derives_tenants_from_existing_process_definitions() -> None:
    sql = read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_batch_record_policy_source`" in sql
    assert "`proc`.`TENANT_ID_`" in sql
    assert "`proc`.`KEY_` IN ('mes-batch-record-version-approval-v1', 'mes-edhr-approval-v1')" in sql
    assert "CAST(`proc`.`TENANT_ID_` AS UNSIGNED)" in sql
    assert "`proc`.`SUSPENSION_STATE_` = 1" in sql
    assert "`proc`.`TENANT_ID_` REGEXP '^[0-9]+$'" in sql
    assert "GROUP BY `proc`.`TENANT_ID_`" in sql
    assert "`source`.`version_process_key`" in sql
    assert "`source`.`execution_process_key`" in sql


def test_batch_record_policy_seed_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS ensure_batch_record_bpm_policy" in sql
    assert "WHERE NOT EXISTS (" in sql
    assert "HAVING COUNT(*) > 1" in sql
    assert "CALL ensure_batch_record_bpm_policy();" in sql
    assert "DROP TEMPORARY TABLE IF EXISTS `tmp_batch_record_policy_source`" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
