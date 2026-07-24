from pathlib import Path
import re


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260621_srm_phase3_purchase_order.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"missing Phase 3 SQL: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def normalized_sql() -> str:
    return " ".join(read_sql().split())


def test_phase3_release_migration_metadata_uses_manifest_contract() -> None:
    first_line = read_sql().splitlines()[0]
    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260621_srm_phase1_supplier_portal; type=schema; riskLevel=medium"
    )


def test_phase3_purchase_order_tables_and_change_tables_are_declared() -> None:
    sql = read_sql()
    for table_name in [
        "srm_purchase_order",
        "srm_purchase_order_line",
        "srm_purchase_order_change",
        "srm_purchase_order_change_line",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for snippet in [
        "`pending_changed_quantity` decimal(24,6) DEFAULT NULL",
        "`pending_changed_delivery_date` date DEFAULT NULL",
        "`pending_changed_remark` varchar(500) DEFAULT NULL",
        "`change_no` varchar(64) NOT NULL",
        "`change_status` varchar(32) NOT NULL",
        "`change_reason` varchar(500) NOT NULL",
        "`reject_remark` varchar(500) DEFAULT NULL",
        "`withdraw_remark` varchar(500) DEFAULT NULL",
        "`before_quantity` decimal(24,6) NOT NULL",
        "`changed_quantity` decimal(24,6) NOT NULL",
        "`changed_delivery_date` date NOT NULL",
    ]:
        assert snippet in sql


def test_phase3_incremental_columns_use_information_schema_guard_instead_of_if_not_exists() -> None:
    sql = read_sql()
    assert "information_schema.COLUMNS" in sql
    assert "TABLE_NAME = 'srm_purchase_order_line'" in sql
    assert "COLUMN_NAME = 'pending_changed_quantity'" in sql
    assert "COLUMN_NAME = 'pending_changed_delivery_date'" in sql
    assert "COLUMN_NAME = 'pending_changed_remark'" in sql
    assert "ADD COLUMN IF NOT EXISTS" not in sql


def test_phase3_menu_package_and_role_merge_is_fail_fast_and_structural() -> None:
    sql = read_sql()
    normalized = normalized_sql()

    for snippet in [
        "采购订单协同",
        "供应商确认台",
        "srm/purchase-order/index",
        "srm/purchase-order/my",
        "srm:purchase-order:query",
        "srm:purchase-order:create",
        "JSON_VALID(`package`.`menu_ids`)",
        "JSON_TABLE(",
        "Invalid system_tenant_package.menu_ids JSON",
        "Missing SRM purchase-order route menu for get-permission-info",
        "Missing SRM purchase-order supplier route menu for get-permission-info",
        "tmp_srm_phase3_package_menu_ids",
        "`system_role_menu`",
    ]:
        assert snippet in sql

    assert "INSERT IGNORE" not in normalized
    assert "ON DUPLICATE KEY UPDATE" not in normalized
    assert re.search(
        r"INSERT\s+INTO\s+`system_role_menu`[\s\S]+NOT\s+EXISTS",
        sql,
        re.IGNORECASE,
    ), "Phase 3 role-menu merge must remain idempotent"


def test_phase3_code_rules_include_purchase_order_change() -> None:
    sql = read_sql()
    for snippet in [
        "'SRM_PURCHASE_ORDER'",
        "'PURCHASE_ORDER'",
        "'SRM_PURCHASE_ORDER_LINE'",
        "'PURCHASE_ORDER_LINE'",
        "'SRM_PURCHASE_ORDER_CHANGE'",
        "'PURCHASE_ORDER_CHANGE'",
    ]:
        assert snippet in sql
