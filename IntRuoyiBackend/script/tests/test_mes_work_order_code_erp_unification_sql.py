from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260609_mes_work_order_code_erp_unification.sql"


def _sql_text() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_work_order_code_erp_unification_sql_is_tenant_scoped_and_auditable() -> None:
    sql = _sql_text()

    required_tokens = [
        "SET @mes_work_order_code_erp_target_tenant_id := 1",
        "tmp_mes_work_order_code_erp_fix",
        "wo.tenant_id = @mes_work_order_code_erp_target_tenant_id",
        "wo.temporary_frozen = b'0'",
        "wo.status <> 3",
        "wo.code LIKE 'KDMO-%'",
        "wo.order_source_code REGEXP '^[0-9A-Za-z_-]*MO[0-9A-Za-z_-]*$'",
        "SELECT id, tenant_id, old_code, erp_code",
    ]

    for token in required_tokens:
        assert token in sql


def test_work_order_code_erp_unification_sql_syncs_snapshots_and_clears_duplicate_source() -> None:
    sql = _sql_text()

    required_tokens = [
        "UPDATE mes_pro_batch_record_execution execution",
        "SET execution.work_order_code = fix.erp_code",
        "UPDATE mes_pro_edhr_batch_execution batch_execution",
        "SET batch_execution.work_order_code = fix.erp_code",
        "UPDATE mes_pro_work_order wo",
        "SET wo.code = fix.erp_code",
        "wo.order_source_code = NULL",
        "remaining_selectable_local_auto_code_with_erp_source_count",
        "duplicated_order_source_code_count",
    ]

    for token in required_tokens:
        assert token in sql


def test_work_order_code_erp_unification_sql_has_no_destructive_table_operations() -> None:
    sql = _sql_text().lower()

    forbidden_tokens = [
        "delete from",
        "truncate table",
        "drop table mes_pro_work_order",
        "drop table mes_pro_batch_record_execution",
        "drop table mes_pro_edhr_batch_execution",
    ]

    for token in forbidden_tokens:
        assert token not in sql
