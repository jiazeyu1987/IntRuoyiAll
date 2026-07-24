import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_mes_smart_scheduling_extra_tabs_sql_declares_bottom_tabs() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260611_mes_smart_scheduling_extra_tabs.sql"
    assert sql_path.exists(), "missing smart scheduling extra tabs migration"
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_mes_smart_scheduling_extra_tabs",
        "SIGNAL SQLSTATE '45000'",
        "`name` = '生产排产'",
        "`name` = '璞慧排产'",
        "`permission` = 'mes:pro-task:query'",
        "`permission` = 'mes:pro-puhui-schedule:query'",
        "`path` = '/mes/pro/task'",
        "`path` = '/mes/pro/puhui-schedule'",
        "`component` = 'mes/pro/task/index'",
        "`component` = 'mes/pro/puhui-schedule/index'",
        "JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900120' AS JSON), '$')",
        "JOIN `system_menu` m ON m.`id` IN (900120, 5540, 5541, 900104)",
    ]

    for snippet in required_snippets:
        assert snippet in text

    for menu_id, sort in ((5540, 4), (900104, 5)):
        assert re.search(
            rf"UPDATE `system_menu`[\s\S]*?`sort` = {sort}[\s\S]*?`parent_id` = 900120[\s\S]*?WHERE `id` = {menu_id};",
            text,
        )


def test_mes_smart_scheduling_extra_tabs_sql_rejects_missing_permissions() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260611_mes_smart_scheduling_extra_tabs.sql"
    assert sql_path.exists(), "missing smart scheduling extra tabs migration"
    text = sql_path.read_text(encoding="utf-8")

    assert re.search(
        r"`id` IN \(5540, 900104\)[\s\S]*?`permission` = ''[\s\S]*?SIGNAL SQLSTATE '45000'",
        text,
    )
    assert "Missing MES smart scheduling parent menu 900120" in text
    assert "Missing MES production schedule or Puhui schedule menu" in text
