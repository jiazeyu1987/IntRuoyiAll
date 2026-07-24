from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260611_mes_work_order_create_erp_order.sql"


def _sql_text() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_work_order_create_erp_sql_registers_button_permission_and_bindings() -> None:
    sql = _sql_text()

    required_tokens = [
        "mes:pro-work-order:create-erp",
        "创建ERP订单",
        "system_menu",
        "system_tenant_package",
        "system_role_menu",
        "测试租户",
        "aoteman",
    ]

    for token in required_tokens:
        assert token in sql


def test_work_order_create_erp_sql_adds_duplicate_protection() -> None:
    sql = _sql_text().lower()

    required_tokens = [
        "mes_kingdee_production_order_sync_record",
        "work_order_id",
        "unique",
        "deleted",
    ]

    for token in required_tokens:
        assert token in sql


def test_work_order_create_erp_sql_has_no_destructive_table_operations() -> None:
    sql = _sql_text().lower()

    forbidden_tokens = [
        "delete from",
        "truncate",
        "drop table",
        "drop database",
    ]

    for token in forbidden_tokens:
        assert token not in sql
