from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260719_dcc_obsolete_form_policy_seed.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing DCC obsolete form policy seed migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_dcc_obsolete_form_policy_seed_declares_release_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260717_bpm_form_center; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_dcc_obsolete_form_policy" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "DCC obsolete form policy requires bpm_form_action_policy" in sql
    assert "DCC obsolete form policy conflict" in sql
    assert "DCC obsolete form policy duplicate" in sql


def test_dcc_obsolete_form_policy_seed_binds_form_center_obsolete_effect_contract() -> None:
    sql = read_sql()

    for literal in [
        "'DCC'",
        "'CONTROLLED_FILE'",
        "'OBSOLETE'",
        "'ACTIVE'",
        "'DCC_OBSOLETE'",
        "'PUBLISHED'",
        "'[]'",
        "'DCC obsolete approval through form center'",
    ]:
        assert literal in sql

    assert re.search(
        r"INSERT\s+INTO\s+`bpm_form_action_policy`[\s\S]+"
        r"`data_domain`[\s\S]+`system_code`[\s\S]+`object_type`[\s\S]+"
        r"`action_code`[\s\S]+`object_state`[\s\S]+`policy_type`[\s\S]+"
        r"`bpm_process_key`[\s\S]+`effect_executor_code`",
        sql,
        re.I,
    )
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'DCC_OBSOLETE'" in sql
    assert "COALESCE(`policy`.`policy_type`, '') <> 'NONE'" in sql
    assert "COALESCE(`policy`.`slots_json`, '[]') <> '[]'" in sql


def test_dcc_obsolete_form_policy_seed_derives_tenant_process_from_published_upload_policy() -> None:
    sql = read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_dcc_obsolete_policy_source`" in sql
    assert "`upload`.`tenant_id`" in sql
    assert "`upload`.`bpm_process_key`" in sql
    assert "`action_code` = 'UPLOAD'" in sql
    assert "`object_state` = 'DRAFT'" in sql
    assert "`status` = 'PUBLISHED'" in sql
    assert "MAX(`id`) AS `id`" in sql
    assert "GROUP BY `tenant_id`" in sql
    assert "COALESCE(`bpm_process_key`, '') <> ''" in sql
    assert "FROM `tmp_dcc_obsolete_policy_source` AS `source`" in sql
    assert "`source`.`tenant_id`" in sql
    assert "`source`.`bpm_process_key`" in sql

    assert "mes-route-version-approval-v1" not in sql
    assert "flowable:assignee" not in sql


def test_dcc_obsolete_form_policy_seed_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS ensure_dcc_obsolete_form_policy" in sql
    assert "WHERE NOT EXISTS (" in sql
    assert "HAVING COUNT(*) > 1" in sql
    assert "CALL ensure_dcc_obsolete_form_policy();" in sql
    assert "DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_obsolete_policy_source`" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
