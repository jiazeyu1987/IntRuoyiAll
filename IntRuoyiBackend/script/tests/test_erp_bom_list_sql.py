from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_erp_bom_list_schema_sql_declares_dedicated_table():
    sql = (ROOT / "sql" / "mysql" / "20260613_mes_kingdee_bom_list.sql").read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `mes_kingdee_bom_list`" in sql
    assert "`source_form_id` varchar(64) NOT NULL DEFAULT 'ENG_BOM'" in sql
    assert "`parent_material_code` varchar(64) NOT NULL" in sql
    assert "`child_material_code` varchar(64) NOT NULL" in sql
    assert "uk_mes_kingdee_bom_list_source" in sql


def test_erp_bom_list_menu_sql_declares_production_menu_entry():
    sql = (ROOT / "sql" / "mysql" / "20260613_erp_bom_list_menu.sql").read_text(encoding="utf-8")

    assert "6023, '物料清单'" in sql
    assert "'erp/production/bom-list/index'" in sql
    assert "'ErpBomList'" in sql
    assert "6024, '物料清单查询', 'erp:bom-list:query'" in sql
    assert "WHERE rm.`menu_id` = 6020" in sql
