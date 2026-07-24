from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260611_mes_edhr_multi_signature_approval.sql"
BPM_SEED_PATH = REPO_ROOT / "sql" / "mysql" / "20260610_mes_admin_edhr_approval_v1_seed.sql"
TEST_SCHEMA_PATH = REPO_ROOT / "yudao-module-mes" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR 多人并行签核 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_multi_signature_schema_extends_work_tasks_and_signatures() -> None:
    text = read_sql()

    for required in [
        "`mes_pro_edhr_work_task`",
        "`signature_cell_key` varchar(128) NOT NULL DEFAULT ''",
        "`signature_row_index` int DEFAULT NULL",
        "`signature_column_index` int DEFAULT NULL",
        "`review_source_type` varchar(16) DEFAULT NULL",
        "`review_source_id` bigint DEFAULT NULL",
        "`review_source_name` varchar(128) DEFAULT NULL",
        "`bpm_task_id` varchar(64) DEFAULT NULL",
        "`mes_pro_batch_record_execution_signature`",
        "`signature_cell_key` varchar(128) DEFAULT NULL",
        "`review_source_type` varchar(16) DEFAULT NULL",
        "`review_source_id` bigint DEFAULT NULL",
        "`review_source_name` varchar(128) DEFAULT NULL",
    ]:
        assert required in text


def test_multi_signature_schema_allows_parallel_review_tasks_without_losing_single_fill_guard() -> None:
    text = read_sql()

    assert "DROP INDEX `uk_mes_pro_edhr_work_task_active`" in text
    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_work_task_active_cell` "
        "(`tenant_id`, `batch_task_id`, `task_type`, `status`, `signature_cell_key`, `deleted`)"
    ) in text
    assert "idx_mes_pro_edhr_work_task_signature_cell" in text
    assert "SIGNAL SQLSTATE '45000'" in text


def test_edhr_bpm_seed_declares_start_user_select_parallel_approve_node() -> None:
    seed = BPM_SEED_PATH.read_text(encoding="utf-8")

    assert "flowable:candidateStrategy=\"35\"" in seed
    assert "<multiInstanceLoopCharacteristics isSequential=\"false\"" in seed
    assert "id=\"approveNode\"" in seed
    assert "targetRef=\"approveNode\"" in seed
    assert "flowable:collection=\"${coll_userList}\"" in seed
    assert "nrOfCompletedInstances == nrOfInstances" in seed
    assert "WHERE procdef.KEY_ = 'mes-edhr-approval-v1'" in seed


def test_mes_test_schema_allows_parallel_review_work_tasks() -> None:
    schema = TEST_SCHEMA_PATH.read_text(encoding="utf-8")

    assert '"signature_cell_key" varchar(128) NOT NULL DEFAULT \'\'' in schema
    assert '"bpm_task_id" varchar(64) DEFAULT NULL' in schema
    assert '"uk_mes_pro_edhr_work_task_active_cell" UNIQUE ("tenant_id", "batch_task_id", "task_type", "status", "signature_cell_key", "deleted")' in schema
