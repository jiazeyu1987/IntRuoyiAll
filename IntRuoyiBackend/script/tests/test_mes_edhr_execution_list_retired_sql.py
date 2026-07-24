from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260721_mes_edhr_execution_list_retire.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_execution_list_retire_migration_exists_and_fails_fast() -> None:
    text = read_sql()
    assert "SET NAMES utf8mb4;" in text
    assert "ensure_mes_edhr_execution_list_retired" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing eDHR batch execution replacement menu 900033" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text


def test_execution_list_menu_900023_is_explicitly_retired() -> None:
    text = read_sql()
    assert "WHERE `id` = 900023" in text
    assert "`deleted` = b'1'" in text
    assert "`visible` = b'0'" in text
    assert "RETIRED_EDHR_EXECUTION_LIST" in text
    assert "mes/pro/edhr/ExecutionListPage" not in text
    assert "MesProFeedbackEdhrExecutionListPage" not in text


def test_execution_list_references_removed_from_roles_and_tenant_packages() -> None:
    text = read_sql()
    assert "DELETE FROM `system_role_menu`" in text
    assert "`menu_id` = 900023" in text
    assert "system_tenant_package" in text
    assert "JSON_TABLE" in text
    assert "JSON_ARRAYAGG" in text
    assert "`menu_id` <> 900023" in text


def test_batch_execution_replacement_menu_is_preserved() -> None:
    text = read_sql()
    assert "`id` = 900033" in text
    assert "`path` = '/mes/pro/feedback/edhr-batch-execution'" in text
    assert "`component` = 'mes/pro/edhr-batch/BatchExecutionListPage'" in text
    assert "`component_name` = 'MesProEdhrBatchExecutionListPage'" in text
    assert "`visible` = b'1'" in text
