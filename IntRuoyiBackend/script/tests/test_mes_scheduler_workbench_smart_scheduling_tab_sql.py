import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_mes_scheduler_workbench_smart_scheduling_tab_sql_declares_order() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260611_mes_scheduler_workbench_smart_scheduling_tab.sql"
    assert sql_path.exists(), "missing scheduler workbench smart scheduling tab migration"
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_mes_scheduler_workbench_smart_scheduling_tab",
        "SIGNAL SQLSTATE '45000'",
        "`name` = '排产员工作台'",
        "`permission` = 'mes:pro-scheduler-workbench:query'",
        "`path` = '/mes/pro/scheduler-workbench'",
        "`component` = 'mes/pro/scheduler-workbench/index'",
        "`component_name` = 'MesProSchedulerWorkbench'",
        "JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900120' AS JSON), '$')",
        "JOIN `system_menu` m ON m.`id` IN (900120, 5590)",
    ]

    for snippet in required_snippets:
        assert snippet in text

    expected_sorts = {
        5985: 0,
        5590: 1,
        5580: 2,
        5550: 3,
        5262: 4,
        5540: 5,
        900104: 6,
    }
    for menu_id, sort in expected_sorts.items():
        assert re.search(
            rf"UPDATE `system_menu`[\s\S]*?`sort` = {sort}[\s\S]*?`parent_id` = 900120[\s\S]*?WHERE `id` = {menu_id};",
            text,
        )


def test_mes_scheduler_workbench_smart_scheduling_tab_sql_rejects_missing_permissions() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260611_mes_scheduler_workbench_smart_scheduling_tab.sql"
    assert sql_path.exists(), "missing scheduler workbench smart scheduling tab migration"
    text = sql_path.read_text(encoding="utf-8")

    assert re.search(
        r"`id` = 5590[\s\S]*?`permission` = ''[\s\S]*?SIGNAL SQLSTATE '45000'",
        text,
    )
    assert "Missing MES smart scheduling parent menu 900120" in text
    assert "Missing MES scheduler workbench menu 5590" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
