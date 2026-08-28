from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260829_erp_finance_invoice_voucher_print_role_permission.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing finance invoice voucher print role permission migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_role_permission_sql_declares_release_contract_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260828_erp_finance_invoice_voucher_print_menu,20260707_system_role_category_management; "
        "type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "START TRANSACTION;" in text
    assert "ensure_erp_invoice_print_role_20260829" in text
    assert "SIGNAL SQLSTATE '45000'" in text

    for required_guard in [
        "Invalid system_tenant_package.menu_ids JSON; cannot assign finance invoice voucher print role",
        "Missing enabled ERP system menu 2563",
        "Missing enabled ERP finance menu 2645",
        "Missing enabled invoice voucher print menu 6034",
        "Invoice voucher print menu 6034 route/component mismatch",
        "Missing finance role category for invoice voucher print role tenant",
        "Duplicate finance invoice voucher print role code in target tenant",
        "Missing tenant 1 admin user for invoice voucher print role binding",
    ]:
        assert required_guard in text


def test_role_permission_sql_creates_finance_category_and_role() -> None:
    text = read_sql()

    assert "INSERT INTO `system_role_category`" in text
    assert "'财务'" in text
    assert "'finance'" in text
    assert "财务权限角色" in text
    assert "INSERT INTO `system_role`" in text
    assert "'财务发票打印'" in text
    assert "'finance_invoice_voucher_print'" in text
    assert "`category`.`code` = 'finance'" in text
    assert "`role`.`category_id` = `category`.`id`" in text


def test_role_permission_sql_grants_only_required_menu_chain_to_finance_print_role() -> None:
    text = read_sql()

    assert "tmp_erp_finance_invoice_voucher_print_role_permission_menu" in text
    assert "INSERT INTO `system_role_menu`" in text
    for menu_id in [2563, 2645, 6034]:
        assert f"SELECT {menu_id} AS `menu_id`" in text

    assert "erp:invoice-voucher-print:query" in text
    assert "Invoice voucher print role menu permission grant incomplete" in text
    assert "Invoice voucher print role menu permission grant has duplicate active bindings" in text


def test_role_permission_sql_assigns_admin_and_restricts_other_roles_without_destructive_sql() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "`user`.`username` = 'admin'" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "Admin user is not assigned invoice voucher print role" in text
    assert "SET `role_menu`.`deleted` = b'1'" in text
    assert "`role_menu`.`menu_id` = 6034" in text
    assert "`role`.`code` <> 'finance_invoice_voucher_print'" in text
    assert "Active invoice voucher print menu is still granted to a non-finance print role" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM SYSTEM_ROLE_MENU",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "DELETE FROM SYSTEM_USER_ROLE",
        "TRUNCATE TABLE",
        "MAX(`EXISTING_ROLE`.`ID`)",
        "MAX(ID)",
        "LAST_INSERT_ID()",
    ]:
        assert forbidden not in upper_text
