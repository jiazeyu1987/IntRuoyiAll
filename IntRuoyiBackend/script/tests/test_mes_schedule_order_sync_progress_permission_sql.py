from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260613_mes_schedule_order_sync_progress_permission.sql"
)


def test_sync_progress_permission_sql_registers_menu_permission():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "mes:pro-schedule-order:update" in sql
    assert "排产工单同步进度" in sql
    assert "system_menu" in sql
    assert "INSERT INTO `system_menu`" in sql
    assert "5583" in sql


def test_sync_progress_permission_sql_grants_existing_schedule_packages_and_admin_roles():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "JSON_TABLE" in sql
    assert "system_tenant_package" in sql
    assert "system_role_menu" in sql
    assert "tenant_admin" in sql
    assert "5580" in sql
    assert "5583" in sql
