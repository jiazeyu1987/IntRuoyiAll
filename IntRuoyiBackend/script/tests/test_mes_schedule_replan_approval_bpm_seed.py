from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "sql" / "mysql" / "20260720_mes_schedule_replan_approval_bpm_seed.sql"


def read_sql() -> str:
    assert SEED.exists(), "missing MES schedule replan approval BPM seed"
    return SEED.read_text(encoding="utf-8")


def test_schedule_replan_approval_bpm_seed_exists_and_is_tenant_scoped() -> None:
    sql = read_sql()

    assert "SET NAMES utf8mb4" in sql
    assert "mes-schedule-replan-approval-v1" in sql
    assert "排产重排审批" in sql
    assert "scheduleReplanApprove" in sql
    assert "<flowable:candidateStrategy>10</flowable:candidateStrategy>" in sql
    assert "<flowable:candidateParam>', @schedule_replan_approver_role_id_tenant_1, '</flowable:candidateParam>" in sql
    assert "<flowable:candidateParam>', @schedule_replan_approver_role_id_tenant_122, '</flowable:candidateParam>" in sql
    assert 'flowable:assignee="${submittedBy}"' not in sql
    assert "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"" in sql
    assert "<bpmndi:BPMNDiagram" in sql
    assert "<bpmndi:BPMNShape" in sql
    assert "<bpmndi:BPMNEdge" in sql

    assert "mes_schedule_replan_approver" in sql
    assert "排产重排审批人" in sql
    assert "system_role" in sql
    assert "system_user_role" in sql
    assert "u.`username` = 'admin'" in sql
    assert "u.`username` = 'smokeappr1'" in sql
    assert "@schedule_replan_approver_user_id_tenant_1" in sql
    assert "@schedule_replan_approver_user_id_tenant_122" in sql
    assert '"managerUserIds":[\', @schedule_replan_approver_user_id_tenant_1, \']' in sql
    assert '"managerUserIds":[\', @schedule_replan_approver_user_id_tenant_122, \']' in sql

    assert "'1' AS tenant_id_text" in sql
    assert "'122' AS tenant_id_text" in sql
    assert "1 AS tenant_id" in sql
    assert "122 AS tenant_id" in sql

    assert "bpm_process_definition_info" in sql
    assert "act_re_procdef" in sql
    assert "act_ge_bytearray" in sql
    assert "DROP TABLE" not in sql.upper()
    assert "TRUNCATE TABLE" not in sql.upper()
    assert "DELETE FROM" not in sql.upper()
