import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
BATCH_TABS_SQL = REPO_ROOT / "sql" / "mysql" / "20260611_mes_edhr_batch_processing_tabs.sql"
RETIRE_EXECUTION_LIST_SQL = REPO_ROOT / "sql" / "mysql" / "20260721_mes_edhr_execution_list_retire.sql"


def test_mes_edhr_batch_processing_tabs_sql_declares_group_and_children() -> None:
    assert BATCH_TABS_SQL.exists(), "missing eDHR batch processing tabs migration"
    assert RETIRE_EXECUTION_LIST_SQL.exists(), "missing eDHR execution list retire migration"
    text = BATCH_TABS_SQL.read_text(encoding="utf-8") + "\n" + RETIRE_EXECUTION_LIST_SQL.read_text(encoding="utf-8")

    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_mes_edhr_batch_processing_tabs",
        "SIGNAL SQLSTATE '45000'",
        "900220, 'eDHR批记录', 'mes:pro-edhr-batch-processing:query'",
        "`name` = '电子批记录'",
        "`name` = 'eDHR审批'",
        "`name` = 'eDHR追踪'",
        "`name` = 'eDHR签名记录'",
        "`name` = 'eDHR批次执行'",
        "`path` = '/mes/pro/batch-record-template'",
        "`path` = '/mes/pro/feedback/edhr-approval'",
        "`path` = '/mes/pro/feedback/edhr-tracking'",
        "`path` = '/mes/pro/feedback/edhr-signatures'",
        "`path` = '/mes/pro/feedback/edhr-batch-execution'",
        "CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_processing_target_packages`",
        "JOIN `system_menu` m ON m.`id` IN (900220, 900002, 900024, 900025, 900026, 900033)",
    ]

    for snippet in required_snippets:
        assert snippet in text

    for menu_id, sort in (
        (900002, 0),
        (900024, 1),
        (900025, 2),
        (900026, 3),
        (900033, 4),
    ):
        assert re.search(
            rf"UPDATE `system_menu`[\s\S]*?`sort` = {sort}[\s\S]*?`parent_id` = 900220[\s\S]*?WHERE `id` = {menu_id};",
            text,
        )


def test_mes_edhr_batch_processing_tabs_sql_rejects_uncontrolled_visibility() -> None:
    assert BATCH_TABS_SQL.exists(), "missing eDHR batch processing tabs migration"
    text = BATCH_TABS_SQL.read_text(encoding="utf-8")

    assert re.search(
        r"`id` IN \(900002, 900024, 900025, 900026, 900033\)[\s\S]*?`permission` = ''[\s\S]*?SIGNAL SQLSTATE '45000'",
        text,
    )
    assert "Missing MES system parent menu 5100" in text
    assert "Missing eDHR batch processing target menus" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text


def test_mes_edhr_batch_processing_tabs_sql_accepts_renamed_parent_menu() -> None:
    assert BATCH_TABS_SQL.exists(), "missing eDHR batch processing tabs migration"
    text = BATCH_TABS_SQL.read_text(encoding="utf-8")

    assert "`permission` <> 'mes:pro-edhr-batch-processing:query'" in text
    assert "`path` <> 'edhr-batch-processing'" in text
    assert "`name` NOT IN ('eDHR批处理', 'eDHR批记录')" in text
    assert "`name` <> 'eDHR批处理' OR `parent_id` <> 5100" not in text


def test_mes_edhr_execution_list_retire_migration_replaces_old_menu_contract() -> None:
    assert RETIRE_EXECUTION_LIST_SQL.exists(), "missing eDHR execution list retire migration"
    text = RETIRE_EXECUTION_LIST_SQL.read_text(encoding="utf-8")

    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_mes_edhr_execution_list_retired",
        "SIGNAL SQLSTATE '45000'",
        "Missing eDHR batch execution replacement menu 900033",
        "`name` = '已废弃-eDHR执行列表'",
        "`permission` = 'RETIRED_EDHR_EXECUTION_LIST'",
        "`path` = '/retired/edhr-execution-list'",
        "`component` = ''",
        "`component_name` = 'RETIRED_EDHR_EXECUTION_LIST'",
        "`visible` = b'0'",
        "`deleted` = b'1'",
        "DELETE FROM `system_role_menu`",
        "WHERE `menu_id` = 900023",
        "`existing_menu`.`menu_id` <> 900023",
        "`component` = 'mes/pro/edhr-batch/BatchExecutionListPage'",
        "`component_name` = 'MesProEdhrBatchExecutionListPage'",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "/mes/pro/feedback/" + "edhr-execution" not in text
    assert "mes/pro/edhr/" + "ExecutionListPage" not in text
    assert "MesProFeedbackEdhr" + "ExecutionListPage" not in text
