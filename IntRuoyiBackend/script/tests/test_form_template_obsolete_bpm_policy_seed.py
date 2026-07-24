from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260721_form_template_obsolete_bpm_policy_seed.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing form template obsolete BPM policy seed migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_template_obsolete_policy_seed_declares_fail_fast_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy,20260722_form_template_obsolete_bpm_process_seed; "
        "type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_form_template_obsolete_bpm_policy" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Form template obsolete policy seed requires bpm_business_approval_policy" in sql
    assert "Form template obsolete policy seed requires ACT_RE_PROCDEF" in sql
    assert "requires active process definition form-template-obsolete-v1" in sql
    assert "Form template obsolete BPM policy conflict" in sql
    assert "Form template obsolete BPM policy duplicate" in sql


def test_form_template_obsolete_policy_seed_binds_bpm_required_bridge_policy() -> None:
    sql = read_sql()

    for literal in [
        "'FORM_CENTER'",
        "'FORM_TEMPLATE'",
        "'OBSOLETE'",
        "'DRAFT'",
        "'READY'",
        "'REJECTED'",
        "'PUBLISHED'",
        "'DISABLED'",
        "'BPM_REQUIRED'",
        "'form-template-obsolete-v1'",
        "'FORM_TEMPLATE_OBSOLETE'",
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
    assert "COALESCE(`policy`.`process_definition_key`, '') <> 'form-template-obsolete-v1'" in sql
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'FORM_TEMPLATE_OBSOLETE'" in sql
    assert "bpm_form_action_policy" not in sql
    assert "record_change_event" not in sql


def test_form_template_obsolete_policy_seed_derives_tenants_from_existing_process_definitions() -> None:
    sql = read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_form_template_obsolete_policy_source`" in sql
    assert "FROM `ACT_RE_PROCDEF` AS `proc`" in sql
    assert "`proc`.`KEY_` = 'form-template-obsolete-v1'" in sql
    assert "CAST(`proc`.`TENANT_ID_` AS UNSIGNED)" in sql
    assert "`proc`.`SUSPENSION_STATE_` = 1" in sql
    assert "`proc`.`TENANT_ID_` REGEXP '^[0-9]+$'" in sql
    assert "CREATE TEMPORARY TABLE `tmp_form_template_obsolete_states`" in sql
    assert "`object_state` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY" in sql
    assert "CROSS JOIN `tmp_form_template_obsolete_states`" in sql


def test_form_template_obsolete_policy_seed_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS ensure_form_template_obsolete_bpm_policy" in sql
    assert "WHERE NOT EXISTS (" in sql
    assert "HAVING COUNT(*) > 1" in sql
    assert "CALL ensure_form_template_obsolete_bpm_policy();" in sql
    assert "DROP TEMPORARY TABLE IF EXISTS `tmp_form_template_obsolete_policy_source`" in sql
    assert "DROP TEMPORARY TABLE IF EXISTS `tmp_form_template_obsolete_states`" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
