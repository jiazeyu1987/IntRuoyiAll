from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260615_mes_schedule_order_usability_permission.sql"
)


def test_usability_permission_sql_registers_admission_diff_and_preflight_permissions():
    sql = SQL_PATH.read_text(encoding="utf-8")

    required_tokens = [
        "ensure_mes_schedule_order_usability_permission",
        "5584",
        "5585",
        "排产工单待同步差异",
        "排产工单排产前检查",
        "mes:pro-schedule-order:admission-diff",
        "mes:pro-schedule-order:preflight",
        "system_menu",
        "INSERT INTO `system_menu`",
    ]
    for token in required_tokens:
        assert token in sql


def test_usability_permission_sql_grants_existing_schedule_packages_and_admin_roles():
    sql = SQL_PATH.read_text(encoding="utf-8")

    required_tokens = [
        "JSON_TABLE",
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "5580",
        "5584",
        "5585",
    ]
    for token in required_tokens:
        assert token in sql
