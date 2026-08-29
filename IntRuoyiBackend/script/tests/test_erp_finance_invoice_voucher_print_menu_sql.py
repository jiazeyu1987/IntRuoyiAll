from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql/mysql/20260828_erp_finance_invoice_voucher_print_menu.sql"
WRONG_MIGRATION = ROOT / "sql/mysql/20260828_erp_finance_fenbeitong_voucher_menu.sql"


def test_invoice_voucher_print_menu_migration_targets_print_assistant_entry():
    assert MIGRATION.exists(), f"required migration missing: {MIGRATION}"
    sql = MIGRATION.read_text(encoding="utf-8")

    assert "SET NAMES utf8mb4;" in sql
    assert "ERP 系统 / 财务管理 / 发票凭证打印" in sql
    assert "6034, '发票凭证打印', 'erp:invoice-voucher-print:query'" in sql
    assert "2, 90, 2645, 'invoice-voucher-print', 'ep:printer'" in sql
    assert "'erp/finance/invoice-voucher-print/index'" in sql
    assert "'ErpInvoiceVoucherPrint'" in sql
    assert "分贝通凭证" not in sql
    assert "fenbeitong-voucher" not in sql


def test_invoice_voucher_print_menu_does_not_broadly_propagate_from_finance_parent():
    sql = MIGRATION.read_text(encoding="utf-8")

    assert "WHERE rm.`menu_id` = 2645" not in sql
    assert "SELECT DISTINCT rm.`role_id`, menu_ids.`menu_id`" not in sql
    assert "exists_rm.`menu_id` = menu_ids.`menu_id`" not in sql


def test_wrong_fenbeitong_menu_migration_is_not_task_owned_output():
    assert not WRONG_MIGRATION.exists(), f"wrong menu migration should be removed: {WRONG_MIGRATION}"
