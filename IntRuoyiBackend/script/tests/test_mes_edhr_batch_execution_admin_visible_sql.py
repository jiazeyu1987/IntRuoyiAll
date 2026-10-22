from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260917_mes_edhr_batch_execution_admin_visible.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR batch execution admin visible SQL migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_admin_visible_migration_declares_release_metadata_and_guards() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260808_mes_edhr_batch_record_test_menu" in text
    assert "type=menu" in text
    assert "riskLevel=low" in text
    assert "SET NAMES utf8mb4;" in text
    assert "ensure_mes_edhr_batch_execution_admin_visible" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing eDHR parent menu 900220" in text
    assert "Missing eDHR batch execution page menu 900033" in text
    assert "eDHR batch execution route already exists on a different menu id" in text
    assert "Invalid eDHR batch execution page menu 900033" in text
    assert "Admin user is missing active super_admin role" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "DELETE FROM `SYSTEM_ROLE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_batch_execution_page_menu_is_restored_visible() -> None:
    text = read_sql()

    for required in [
        "`id` = 900033",
        "`name` = '批次执行'",
        "`permission` = 'mes:pro-edhr-batch-execution:query'",
        "`type` = 2",
        "`sort` = 7",
        "`parent_id` = 900220",
        "`path` = '/mes/pro/feedback/edhr-batch-execution'",
        "`component` = 'mes/pro/edhr-batch/BatchExecutionListPage'",
        "`component_name` = 'MesProEdhrBatchExecutionListPage'",
        "`status` = 0",
        "`visible` = b'1'",
        "`deleted` = b'0'",
    ]:
        assert required in text


def test_admin_super_admin_roles_receive_minimal_batch_execution_bindings() -> None:
    text = read_sql()

    for required in [
        "tmp_mes_edhr_batch_execution_admin_roles",
        "tmp_mes_edhr_batch_execution_required_menus",
        "`user`.`username` = 'admin'",
        "`role`.`code` = 'super_admin'",
        "900033 AS `menu_id`",
        "900034 AS `menu_id`",
        "900036 AS `menu_id`",
        "INSERT INTO `system_role_menu`",
        "eDHR batch execution menu is missing from admin super_admin role",
        "mes:pro-edhr-batch-execution:query",
        "mes:pro-edhr-batch-execution:update",
    ]:
        assert required in text

    assert "'tenant_admin'" not in text, "admin request must not widen access to tenant_admin"
