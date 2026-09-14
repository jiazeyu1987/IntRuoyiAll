from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "sql" / "mysql" / "20260721_form_template_upgrade_bpm_seed.sql"


def read_sql() -> str:
    assert SEED.exists(), "missing form template upgrade BPM seed"
    return SEED.read_text(encoding="utf-8")


def test_form_template_upgrade_bpm_seed_declares_dedicated_process() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy; type=seed; riskLevel=low"
    )
    assert "form-template-upgrade-v1" in sql
    assert "表单模板升版审批" in sql
    assert '<process id="form-template-upgrade-v1"' in sql
    assert '<userTask id="formTemplateUpgradeApprove" name="表单模板升版审核">' in sql
    assert "<flowable:candidateStrategy>10</flowable:candidateStrategy>" in sql
    assert "<bpmndi:BPMNDiagram" in sql
    assert "<bpmndi:BPMNShape" in sql
    assert "<bpmndi:BPMNEdge" in sql


def test_form_template_upgrade_bpm_seed_wires_process_and_business_policy() -> None:
    sql = read_sql()

    for token in [
        "act_re_deployment",
        "act_ge_bytearray",
        "act_re_model",
        "act_re_procdef",
        "bpm_process_definition_info",
        "bpm_business_approval_policy",
        "'FORM_CENTER'",
        "'FORM_TEMPLATE'",
        "'UPGRADE'",
        "'DRAFT'",
        "'BPM_REQUIRED'",
        "'FORM_TEMPLATE_UPGRADE'",
        "'PUBLISHED'",
    ]:
        assert token in sql

    assert re.search(
        r"INSERT\s+INTO\s+`bpm_business_approval_policy`[\s\S]+"
        r"`data_domain`[\s\S]+`system_code`[\s\S]+`object_type`[\s\S]+"
        r"`action_code`[\s\S]+`object_state`[\s\S]+`policy_mode`[\s\S]+"
        r"`process_definition_key`[\s\S]+`effect_executor_code`",
        sql,
        re.I,
    )
    assert "COALESCE(`policy`.`effect_executor_code`, '') <> 'FORM_TEMPLATE_UPGRADE'" in sql
    assert "COALESCE(`policy`.`process_definition_key`, '') <> 'form-template-upgrade-v1'" in sql
    assert "UPDATE `bpm_business_approval_policy` AS `policy`" in sql
    assert "SET `policy`.`policy_mode` = 'BPM_REQUIRED'" not in sql
    assert "COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'" not in sql
    assert "`policy`.`policy_mode` = 'BPM_REQUIRED'" in sql
    assert "WHEN `policy`.`policy_mode` = 'BPM_REQUIRED' THEN 'form-template-upgrade-v1'" in sql
    assert "`policy`.`effect_executor_code` = 'FORM_TEMPLATE_UPGRADE'" in sql


def test_form_template_upgrade_bpm_seed_is_tenant_scoped_and_non_destructive() -> None:
    sql = read_sql()

    assert "'1' AS tenant_id_text" in sql
    assert "'122' AS tenant_id_text" in sql
    assert "1 AS tenant_id" in sql
    assert "122 AS tenant_id" in sql
    assert "form-template-upgrade-v1:1:form-template-admin" in sql
    assert "form-template-upgrade-v1:1:form-template-test" in sql
    assert "(`user`.`tenant_id` = 1 AND `user`.`username` = 'admin')" in sql
    assert "(`user`.`tenant_id` = 122 AND `user`.`username` = 'aoteman')" in sql
    assert "@form_template_admin_role_id_tenant_1" in sql
    assert "@form_template_admin_role_id_tenant_122" in sql

    assert "mes-route-version-approval-v1" not in sql
    assert "dcc-controlled-file-approval" not in sql
    assert "dcc-controlled-file-obsolete-approval" not in sql
    assert not re.search(r"\b(TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert "fallback" not in sql.lower()
