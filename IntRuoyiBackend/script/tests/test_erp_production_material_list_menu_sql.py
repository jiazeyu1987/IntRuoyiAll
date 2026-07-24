from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260613_erp_production_material_list_menu.sql"


def test_erp_production_material_list_menu_sql_declares_entry():
    sql = SQL.read_text(encoding="utf-8")

    assert "6020, '生产管理'" in sql
    assert "6021, '生产用料清单'" in sql
    assert "'erp/production/material-list/index'" in sql
    assert "'ErpProductionMaterialList'" in sql
    assert "6022, '生产用料清单查询', 'erp:production-material-list:query'" in sql


def test_erp_production_material_list_menu_sql_grants_existing_erp_roles():
    sql = SQL.read_text(encoding="utf-8")

    assert "INSERT INTO `system_role_menu`" in sql
    assert "SELECT DISTINCT rm.`role_id`, menu_ids.`menu_id`" in sql
    assert "WHERE rm.`menu_id` = 2563" in sql
    assert "AND exists_rm.`menu_id` = menu_ids.`menu_id`" in sql
