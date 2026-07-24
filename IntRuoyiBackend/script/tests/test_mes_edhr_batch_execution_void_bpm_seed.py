from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "sql" / "mysql" / "20260714_mes_edhr_batch_execution_void_bpm_seed.sql"


def test_edhr_batch_execution_void_bpm_seed_exists_and_is_row_context_scoped():
    sql = SEED.read_text(encoding="utf-8")

    assert "SET NAMES utf8mb4" in sql
    assert "mes-edhr-batch-execution-void-v1" in sql
    assert "eDHR批次执行作废" in sql
    assert "batchExecutionVoidApprove" in sql
    assert "batchExecutionCode" in sql
    assert "workOrderCode" in sql
    assert "reasonCategory" in sql
    assert "<flowable:candidateStrategy>10</flowable:candidateStrategy>" in sql
    assert "<flowable:candidateParam>', @edhr_batch_void_admin_role_id_tenant_1, '</flowable:candidateParam>" in sql
    assert "<flowable:candidateParam>', @edhr_batch_void_admin_role_id_tenant_122, '</flowable:candidateParam>" in sql
    assert "flowable:assignee=\"${submittedBy}\"" not in sql

    assert "'1' AS tenant_id_text" in sql
    assert "'122' AS tenant_id_text" in sql
    assert "bpm_process_definition_info" in sql
    assert "act_re_procdef" in sql
    assert "act_ge_bytearray" in sql
