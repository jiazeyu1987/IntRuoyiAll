from pathlib import Path


SQL_PATH = Path("sql/mysql/20260708_mes_schedule_order_export_permission.sql")


def test_schedule_order_export_permission_sql_registers_idempotent_menu_permission():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "release-migration:" in sql
    assert "ensure_mes_schedule_order_export_permission" in sql
    assert "INSERT INTO `system_menu`" in sql
    assert "排产工单导出" in sql
    assert "mes:pro-schedule-order:export" in sql
    assert "5589" in sql
    assert "5580" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "system_menu id 5589 is already used by another menu" in sql


def test_schedule_order_export_permission_sql_is_scoped_and_non_destructive():
    sql = SQL_PATH.read_text(encoding="utf-8")
    upper_sql = sql.upper()

    assert "`parent_id` = 5580" in sql
    assert "`id` = 5589" in sql
    assert "DELETE FROM" not in upper_sql
    assert "TRUNCATE" not in upper_sql
    assert "DROP TABLE" not in upper_sql


def test_schedule_order_export_permission_sql_syncs_packages_and_roles():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "system_tenant_package" in sql
    assert "JSON_VALID(`menu_ids`)" in sql
    assert "JSON_TABLE" in sql
    assert "JSON_ARRAYAGG(`menu_id`)" in sql
    assert "system_role_menu" in sql
    assert "tenant_admin" in sql
    assert "super_admin" in sql
    assert "mes_scheduler" in sql
    assert "`menu_id` = 5589" in sql
