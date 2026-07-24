from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260612_mes_edhr_record_change_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR变更记录菜单权限 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_record_change_menu_declares_permissions() -> None:
    text = read_sql()

    for required in [
        "900235",
        "eDHR变更记录",
        "/mes/pro/feedback/edhr-change",
        "mes/pro/edhr/RecordChangePage",
        "MesProFeedbackEdhrRecordChange",
        "mes:pro-edhr-change:query",
        "mes:pro-edhr-change:void",
        "mes:pro-edhr-change:approve",
        "mes:pro-edhr-change:reopen",
        "mes:pro-edhr-change:supplement",
    ]:
        assert required in text


def test_edhr_record_change_menu_is_bound_to_packages_and_tenant_admin() -> None:
    text = read_sql()

    for required in [
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "900220",
        "ensure_mes_edhr_record_change_menus",
    ]:
        assert required in text

    for menu_id in ["900235", "900236", "900237", "900238", "900239", "900240"]:
        assert menu_id in text


def test_edhr_record_change_menu_migration_is_fail_fast_and_no_fallback() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing eDHR record change system_menu rows" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "INSERT IGNORE INTO `SYSTEM_MENU`" not in upper_text
