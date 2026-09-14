from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260910_erp_finance_fenbeitong_assistant_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.is_file(), f"required migration missing: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_has_release_contract_and_finance_parent_guard() -> None:
    sql = read_sql()
    first_line = sql.splitlines()[0]

    assert "allowedEnvironments=test,backup,prod" in first_line
    assert "type=menu" in first_line
    assert "riskLevel=low" in first_line
    assert "Missing enabled ERP finance menu 2645" in sql
    assert "`parent_id`" in sql
    assert "2645" in sql


def test_migration_defines_page_and_granular_permissions() -> None:
    sql = read_sql()

    assert "991200" in sql
    assert "'分贝通凭证'" in sql
    assert "'fenbeitong-voucher'" in sql
    assert "'erp/finance/fenbeitong-voucher/index'" in sql
    assert "'ErpFenbeitongVoucher'" in sql
    assert "'erp:fenbeitong-voucher:query'" in sql
    assert "'erp:fenbeitong-voucher:config'" in sql
    assert "'erp:fenbeitong-voucher:save'" in sql
    assert "ON DUPLICATE KEY UPDATE" in sql


def test_migration_does_not_grant_users_or_roles() -> None:
    sql = read_sql().upper()

    assert "SYSTEM_USER_ROLE" not in sql
    assert "SYSTEM_ROLE_MENU" not in sql
    assert "INSERT INTO `SYSTEM_ROLE`" not in sql
