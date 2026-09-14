from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260723_unify_form_action_policy_into_business_approval_policy.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing unified business approval policy migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_unified_policy_migration_declares_release_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260722_form_center_policy_menu_hide; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "unify_form_action_policy_into_business_approval_policy" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Unified policy migration requires bpm_form_action_policy" in sql
    assert "Unified policy migration requires bpm_business_approval_policy" in sql
    assert "Unified policy migration requires bpm_form_action_instance" in sql
    assert "ADD COLUMN `form_policy_type`" in sql
    assert "ADD COLUMN `form_slots_json`" in sql
    assert "Unsupported form action approval mode for unified business approval policy" in sql
    assert "BPM_REQUIRED form action policy requires bpm_process_key before unified migration" in sql
    assert "Form action policy requires effect_executor_code before unified migration" in sql


def test_unified_policy_migration_copies_policy_rows_to_business_approval_table() -> None:
    sql = read_sql()

    assert re.search(
        r"INSERT\s+INTO\s+`bpm_business_approval_policy`[\s\S]+"
        r"SELECT[\s\S]+`form_policy`.`tenant_id`[\s\S]+"
        r"`form_policy`.`data_domain`[\s\S]+`form_policy`.`system_code`[\s\S]+"
        r"`form_policy`.`object_type`[\s\S]+`form_policy`.`action_code`[\s\S]+"
        r"`form_policy`.`object_state`[\s\S]+`form_policy`.`approval_mode`[\s\S]+"
        r"`form_policy`.`bpm_process_key`[\s\S]+`form_policy`.`effect_executor_code`[\s\S]+"
        r"`form_policy`.`policy_type`[\s\S]+`form_policy`.`slots_json`",
        sql,
        re.I,
    )
    assert "FROM `bpm_form_action_policy` AS `form_policy`" in sql
    assert "AND NOT EXISTS (" in sql
    assert "`business_policy`.`policy_mode` = `form_policy`.`approval_mode`" in sql
    assert "`business_policy`.`process_definition_key`" in sql
    assert "`business_policy`.`effect_executor_code` = `form_policy`.`effect_executor_code`" in sql


def test_unified_policy_migration_updates_existing_form_action_instances_to_business_policy_ids() -> None:
    sql = read_sql()

    assert "UPDATE `bpm_form_action_instance` AS `instance`" in sql
    assert "JOIN `bpm_form_action_policy` AS `form_policy`" in sql
    assert "JOIN `bpm_business_approval_policy` AS `business_policy`" in sql
    assert "SET `instance`.`policy_id` = `business_policy`.`id`" in sql
    assert "`instance`.`policy_id` = `form_policy`.`id`" in sql
    assert "Form action instance policy migration requires exactly one business approval policy" in sql


def test_unified_policy_migration_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS unify_form_action_policy_into_business_approval_policy" in sql
    assert "CALL unify_form_action_policy_into_business_approval_policy();" in sql
    assert "DROP PROCEDURE IF EXISTS unify_form_action_policy_into_business_approval_policy" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
