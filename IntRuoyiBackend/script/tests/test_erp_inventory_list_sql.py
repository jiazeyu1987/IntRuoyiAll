from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_erp_inventory_list_schema_sql_declares_dedicated_table():
    sql = (ROOT / "sql" / "mysql" / "20260613_erp_kingdee_inventory_list.sql").read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `erp_kingdee_inventory_list`" in sql
    assert "`source_form_id` varchar(64) NOT NULL DEFAULT 'STK_Inventory'" in sql
    assert "`material_number` varchar(64) NOT NULL" in sql
    assert "`warehouse_number` varchar(64) NOT NULL" in sql
    assert "uk_erp_kingdee_inventory_list_source" in sql


def test_erp_inventory_list_menu_sql_declares_production_menu_entry():
    sql = (ROOT / "sql" / "mysql" / "20260613_erp_inventory_list_menu.sql").read_text(encoding="utf-8")

    assert "6025, '即时库存'" in sql
    assert "'erp/production/inventory-list/index'" in sql
    assert "'ErpInventoryList'" in sql
    assert "6026, '即时库存查询', 'erp:inventory-list:query'" in sql
    assert "WHERE rm.`menu_id` = 6020" in sql
