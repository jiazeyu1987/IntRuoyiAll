from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RESTORE_SQL = ROOT / "sql" / "mysql" / "20260615_mes_pro_work_order_menu_route_restore.sql"


def read_text(path: Path) -> str:
    assert path.exists(), f"missing required file: {path}"
    return path.read_text(encoding="utf-8")


def test_corrective_sql_restores_production_work_order_menu_route():
    sql = read_text(RESTORE_SQL)

    assert "`id` = 5530" in sql
    assert "`type` = 2" in sql
    assert "`deleted` = b'0'" in sql
    assert "`parent_id` = 5700" in sql
    assert "`path` = 'work-order'" in sql
    assert "`component` = 'mes/pro/workorder/index'" in sql
    assert "`component_name` = 'MesProWorkOrder'" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "ON DUPLICATE KEY UPDATE" not in sql
    assert "`parent_id` = 5101" not in sql
