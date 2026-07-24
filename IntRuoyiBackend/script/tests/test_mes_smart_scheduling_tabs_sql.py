import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_mes_smart_scheduling_tabs_sql_declares_grouped_menu_contract() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260611_mes_smart_scheduling_tabs.sql"
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_mes_smart_scheduling_tabs",
        "SIGNAL SQLSTATE '45000'",
        "900120, '智能排产', 'mes:pro-smart-scheduling:query'",
        "`name` = '排产看板'",
        "`name` = '排产工单'",
        "`name` = '报工'",
        "`name` = '排程日历'",
        "`permission` = 'mes:home:query'",
        "`permission` = 'mes:pro-schedule-order:query'",
        "`permission` = 'mes:pro-feedback:query'",
        "`permission` = 'mes:pro-task:query'",
        "`path` = '/mes/home/index'",
        "`path` = '/mes/pro/schedule-order'",
        "`path` = '/mes/pro/feedback'",
        "`path` = '/mes/pro/schedule-calendar'",
        "JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5100' AS JSON), '$')",
        "JOIN `system_menu` m ON m.`id` IN (900120, 5985, 5580, 5581, 5550, 5551, 5262)",
    ]

    for snippet in required_snippets:
        assert snippet in text

    for menu_id, sort in ((5985, 0), (5580, 1), (5550, 2), (5262, 3)):
        update_pattern = re.compile(
            rf"UPDATE `system_menu`[\s\S]*?`sort` = {sort}[\s\S]*?`parent_id` = 900120[\s\S]*?WHERE `id` = {menu_id};"
        )
        assert update_pattern.search(text), f"menu {menu_id} must be moved under 900120 with sort {sort}"


def test_mes_smart_scheduling_tabs_sql_rejects_uncontrolled_visibility() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260611_mes_smart_scheduling_tabs.sql"
    text = sql_path.read_text(encoding="utf-8")

    assert re.search(
        r"`id` IN \(5985, 5580, 5550, 5262\)[\s\S]*?`permission` = ''[\s\S]*?SIGNAL SQLSTATE '45000'",
        text,
    )
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Missing MES scheduling page menus 5985/5580/5550/5262" in text
