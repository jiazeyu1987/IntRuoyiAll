from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "sql" / "mysql" / "20260714_mes_batch_record_version_approval_bpm_seed.sql"


def test_batch_record_version_approval_bpm_seed_exists_and_is_tenant_scoped():
    sql = SEED.read_text(encoding="utf-8")

    assert "mes-batch-record-version-approval-v1" in sql
    assert "批记录升版审批" in sql
    assert "batchRecordVersionApprove" in sql
    assert "<flowable:candidateStrategy>10</flowable:candidateStrategy>" in sql
    assert "<flowable:candidateParam>', @batch_record_admin_role_id_tenant_1, '</flowable:candidateParam>" in sql
    assert "<flowable:candidateParam>', @batch_record_admin_role_id_tenant_122, '</flowable:candidateParam>" in sql
    assert 'flowable:assignee="${submittedBy}"' not in sql
    assert "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"" in sql
    assert "xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\"" in sql
    assert "xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\"" in sql
    assert "<bpmndi:BPMNDiagram" in sql
    assert "<bpmndi:BPMNShape" in sql
    assert "<bpmndi:BPMNEdge" in sql

    assert "edhr_batch_record_admin" in sql
    assert "批记录管理员" in sql
    assert "system_role" in sql
    assert "system_user_role" in sql
    assert "system_role_menu" in sql
    assert "1200 AS menu_id" in sql
    assert "1207 AS menu_id" in sql
    assert "1221 AS menu_id" in sql
    assert "1222 AS menu_id" in sql
    assert "u.`username` = 'admin'" in sql
    assert "@batch_record_admin_user_id_tenant_1" in sql
    assert "@batch_record_admin_user_id_tenant_122" in sql
    assert '"managerUserIds":[\', @batch_record_admin_user_id_tenant_1, \']' in sql
    assert '"managerUserIds":[\', @batch_record_admin_user_id_tenant_122, \']' in sql
    assert ',"managerUserIds":null,"sort"' not in sql

    assert "'1' AS tenant_id_text" in sql
    assert "'122' AS tenant_id_text" in sql
    assert "1 AS tenant_id" in sql
    assert "122 AS tenant_id" in sql

    assert "bpm_process_definition_info" in sql
    assert "act_re_procdef" in sql
    assert "act_ge_bytearray" in sql
    assert "BRV-" not in sql
