import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260612_mes_process_use_route_tabs.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES process use route tabs migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_process_use_route_tabs_sql_declares_menus_and_permissions() -> None:
    text = read_sql()

    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_mes_process_use_route_tabs",
        "SIGNAL SQLSTATE '45000'",
        "900121, '工艺排产路线', 'mes:pro-schedule-route:query'",
        "900122, '工艺排产路线配置', 'mes:pro-schedule-route:update'",
        "900221, '工艺批记录路线', 'mes:pro-batch-record-route:query'",
        "900222, '工艺批记录路线配置', 'mes:pro-batch-record-route:update'",
        "`path` = '/mes/pro/schedule-route'",
        "`component` = 'mes/pro/schedule-route/index'",
        "`path` = '/mes/pro/feedback/edhr-batch-route'",
        "`component` = 'mes/pro/edhr-batch-route/index'",
        "`permission` = 'mes:pro-edhr-batch-processing:query'",
        "`path` = 'edhr-batch-processing'",
        "JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900120' AS JSON), '$')",
        "JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900220' AS JSON), '$')",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert re.search(
        r"JOIN `system_menu` m ON m\.`id` IN \(900120, 900121, 900122\)",
        text,
    )
    assert re.search(
        r"JOIN `system_menu` m ON m\.`id` IN \(900220, 900221, 900222\)",
        text,
    )


def test_process_use_route_tabs_sql_reorders_visible_tabs() -> None:
    text = read_sql()

    expected_smart_order = (
        (5985, 0),
        (5590, 1),
        (5580, 2),
        (900121, 3),
        (5550, 4),
        (5262, 5),
        (5540, 6),
        (900104, 7),
    )
    for menu_id, sort in expected_smart_order:
        assert re.search(
            rf"UPDATE `system_menu`[\s\S]*?`sort` = {sort}[\s\S]*?`parent_id` = 900120[\s\S]*?WHERE `id` = {menu_id};",
            text,
        ), f"smart scheduling child {menu_id} must use sort {sort}"

    expected_edhr_order = (
        (900002, 0),
        (900221, 1),
        (900024, 2),
        (900025, 3),
        (900026, 4),
        (900033, 5),
    )
    for menu_id, sort in expected_edhr_order:
        assert re.search(
            rf"UPDATE `system_menu`[\s\S]*?`sort` = {sort}[\s\S]*?`parent_id` = 900220[\s\S]*?WHERE `id` = {menu_id};",
            text,
        ), f"eDHR batch child {menu_id} must use sort {sort}"


def test_process_use_route_tabs_sql_rejects_conflicts_and_missing_parents() -> None:
    text = read_sql()

    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Missing MES smart scheduling parent menu 900120" in text
    assert "Missing eDHR batch processing parent menu 900220" in text
    assert "Menu id 900121 is already used by another menu" in text
    assert "Menu id 900221 is already used by another menu" in text
    assert re.search(
        r"`id` IN \(900121, 900221\)[\s\S]*?`permission` = ''[\s\S]*?SIGNAL SQLSTATE '45000'",
        text,
    )
