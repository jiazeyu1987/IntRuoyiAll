from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "sql" / "mysql" / "20260722_form_template_obsolete_bpm_process_seed.sql"


def read_sql() -> str:
    assert SEED.exists(), "missing form template obsolete BPM process seed"
    return SEED.read_text(encoding="utf-8")


def test_form_template_obsolete_bpm_process_seed_declares_dedicated_process() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy; type=seed; riskLevel=low"
    )
    assert "form-template-obsolete-v1" in sql
    assert "表单模板作废审批" in sql
    assert '<process id="form-template-obsolete-v1"' in sql
    assert '<userTask id="formTemplateObsoleteApprove" name="表单模板作废审核">' in sql
    assert "<flowable:candidateStrategy>10</flowable:candidateStrategy>" in sql
    assert "<bpmndi:BPMNDiagram" in sql
    assert "<bpmndi:BPMNShape" in sql
    assert "<bpmndi:BPMNEdge" in sql


def test_form_template_obsolete_bpm_process_seed_wires_runtime_tables() -> None:
    sql = read_sql()

    for token in [
        "act_re_deployment",
        "act_ge_bytearray",
        "act_re_model",
        "act_re_procdef",
        "bpm_process_definition_info",
        "'FORM_TEMPLATE'",
        "'form-template-obsolete-v1'",
        "'form_template_obsolete_approver'",
        "'form-template-obsolete-v1:1:form-template-admin'",
        "'form-template-obsolete-v1:1:form-template-test'",
        "formCustomCreatePath",
        "formCustomViewPath",
        "/mdm/form-center/template",
    ]:
        assert token in sql

    assert "bpm_business_approval_policy" not in sql
    assert "bpm_form_action_policy" not in sql
    assert "record_change_event" not in sql


def test_form_template_obsolete_bpm_process_seed_is_tenant_scoped_and_non_destructive() -> None:
    sql = read_sql()

    assert "SELECT 1 AS `tenant_id`" in sql
    assert "SELECT 122 AS `tenant_id`" in sql
    assert "'1' AS `tenant_id_text`" in sql
    assert "'122' AS `tenant_id_text`" in sql
    assert "(`user`.`tenant_id` = 1 AND `user`.`username` = 'admin')" in sql
    assert "(`user`.`tenant_id` = 122 AND `user`.`username` = 'aoteman')" in sql
    assert "@form_template_obsolete_role_id_tenant_1" in sql
    assert "@form_template_obsolete_role_id_tenant_122" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Form template obsolete process seed requires approver principals" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
