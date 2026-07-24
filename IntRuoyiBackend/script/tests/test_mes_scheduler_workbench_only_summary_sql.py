import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_mes_scheduler_workbench_only_summary_sql_hides_board_tab() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260615_mes_scheduler_workbench_only_summary.sql"
    assert sql_path.exists(), "missing scheduler workbench only-summary migration"
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_mes_scheduler_workbench_only_summary",
        "SIGNAL SQLSTATE '45000'",
        "`name` = '排产看板'",
        "`visible` = b'0'",
        "`name` = '排产员工作台'",
        "`permission` = 'mes:pro-scheduler-workbench:query'",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert re.search(
        r"UPDATE `system_menu`[\s\S]*?`name` = '排产看板'[\s\S]*?`visible` = b'0'[\s\S]*?WHERE `id` = 5985;",
        text,
    )


def test_mes_scheduler_workbench_only_summary_sql_preserves_sibling_tabs() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260615_mes_scheduler_workbench_only_summary.sql"
    assert sql_path.exists(), "missing scheduler workbench only-summary migration"
    text = sql_path.read_text(encoding="utf-8")

    expected_sorts = {
        5590: ("排产员工作台", 0),
        5580: ("排产工单", 1),
        5550: ("报工", 2),
        5262: ("排程日历", 3),
        900121: ("工艺排产路线", 4),
        5540: ("生产排产", 5),
        900104: ("璞慧排产", 6),
    }
    for menu_id, (name, sort) in expected_sorts.items():
        assert re.search(
            rf"UPDATE `system_menu`[\s\S]*?`name` = '{name}'[\s\S]*?`sort` = {sort}[\s\S]*?`parent_id` = 900120[\s\S]*?`visible` = b'1'[\s\S]*?WHERE `id` = {menu_id};",
            text,
        )

    assert "Missing MES smart scheduling menus 5985/5590/5580/5550/5262/900121/5540/900104" in text
