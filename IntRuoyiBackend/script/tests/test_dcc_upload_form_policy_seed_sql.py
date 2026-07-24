from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260719_dcc_upload_form_policy_seed.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing DCC upload form policy seed migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_dcc_upload_form_policy_seed_declares_release_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260717_bpm_form_center; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_dcc_upload_form_policy" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "DCC upload form policy requires bpm_form_action_policy" in sql
    assert "DCC upload form policy conflict" in sql
    assert "DCC upload form policy duplicate" in sql


def test_dcc_upload_form_policy_seed_binds_form_center_upload_effect_contract() -> None:
    sql = read_sql()

    for literal in [
        "'DCC'",
        "'CONTROLLED_FILE'",
        "'UPLOAD'",
        "'DRAFT'",
        "'DCC_UPLOAD'",
        "'PUBLISHED'",
        "'[]'",
        "'DCC upload approval through form center'",
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
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'DCC_UPLOAD'" in sql
    assert "COALESCE(`policy`.`policy_type`, '') <> 'NONE'" in sql
    assert "COALESCE(`policy`.`slots_json`, '[]') <> '[]'" in sql


def test_dcc_upload_form_policy_seed_reuses_existing_controlled_file_process_and_tenant_scope() -> None:
    sql = read_sql()

    assert "dcc-controlled-file-approval" in sql
    assert "1 AS tenant_id" in sql
    assert "122 AS tenant_id" in sql
    assert "bpm_process_definition_info" in sql
    assert "act_re_procdef" in sql
    assert "DCC upload form policy requires dcc-controlled-file-approval process" in sql

    assert "mes-route-version-approval-v1" not in sql
    assert "form-template-upgrade-v1" not in sql
    assert "DCC受控文件上传审批" not in sql
    assert "<flowable:candidateStrategy>10</flowable:candidateStrategy>" not in sql
    assert "dccControlledFileUploadApprove" not in sql
    assert "flowable:assignee" not in sql


def test_dcc_upload_form_policy_seed_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS ensure_dcc_upload_form_policy" in sql
    assert "WHERE NOT EXISTS (" in sql
    assert "HAVING COUNT(*) > 1" in sql
    assert "CALL ensure_dcc_upload_form_policy();" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
