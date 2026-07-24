from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260713_mes_edhr_form_fill_log_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR form fill log menu SQL must exist."
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_fill_log_menu_declares_readonly_page_and_permission() -> None:
    text = read_sql()

    for required in [
        "release-migration: allowedEnvironments=test,backup,prod",
        "900432",
        "900433",
        "表单日志",
        "/mes/pro/feedback/edhr-form-fill-log",
        "mes/pro/edhr/FormFillLogPage",
        "MesProEdhrFormFillLogPage",
        "mes:pro-edhr-form-fill-log:query",
    ]:
        assert required in text

    forbidden_permissions = [
        "mes:pro-edhr-form-fill-log:create",
        "mes:pro-edhr-form-fill-log:update",
        "mes:pro-edhr-form-fill-log:delete",
        "mes:pro-edhr-form-fill-log:export",
        "reasonText",
        "reasonCategory",
    ]
    for forbidden in forbidden_permissions:
        assert forbidden not in text

    assert "900432, '表单日志', 'mes:pro-edhr-form-fill-log:query', 2, 6, 900220" in text
    assert "900433, '表单日志查询', 'mes:pro-edhr-form-fill-log:query', 3" in text
    assert "SET `name` = '表单日志'" in text
    assert "`sort` = 6" in text
    assert "WHERE `id` = 900303" in text
    assert "`sort` <= 6" in text


def test_form_fill_log_menu_is_bound_to_packages_and_tenant_admin() -> None:
    text = read_sql()

    for required in [
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "ensure_mes_edhr_form_fill_log_menus",
        "tmp_mes_edhr_form_fill_log_target_packages",
        "tmp_mes_edhr_form_fill_log_menu_ids",
    ]:
        assert required in text


def test_form_fill_log_menu_migration_is_fail_fast_without_system_menu_ignore() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Missing eDHR form fill log system_menu rows" in text
    assert "INSERT IGNORE INTO `SYSTEM_MENU`" not in upper_text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
