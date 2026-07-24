from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL = (REPO_ROOT / "sql/mysql/20260606_mes_puhui_schedule_menu.sql").read_text(encoding="utf-8")


def test_puhui_schedule_menu_preserves_current_legal_parent_when_replayed() -> None:
    assert "DECLARE v_puhui_parent_menu_id BIGINT DEFAULT NULL;" in SQL
    assert "DECLARE v_puhui_permission VARCHAR(255) DEFAULT '';" in SQL
    assert "DECLARE v_puhui_path VARCHAR(255) DEFAULT 'puhui-schedule';" in SQL
    assert "SELECT `parent_id`" in SQL
    assert "`id` IN (5700, 900120)" in SQL
    assert "IF v_puhui_parent_menu_id = 900120 THEN" in SQL
    assert "SET v_puhui_permission = 'mes:pro-puhui-schedule:query';" in SQL
    assert "SET v_puhui_path = '/mes/pro/puhui-schedule';" in SQL
    assert "`path` IN ('puhui-schedule', '/mes/pro/puhui-schedule')" in SQL
    assert "AND `existing_menu`.`menu_id` IN (5700, 900120, 5540)" in SQL
    assert "`parent_id` = v_puhui_parent_menu_id" in SQL
    assert "`permission` = v_puhui_permission" in SQL
    assert "`path` = v_puhui_path" in SQL
    assert "Missing MES production schedule menu 5540 under 5700" not in SQL
