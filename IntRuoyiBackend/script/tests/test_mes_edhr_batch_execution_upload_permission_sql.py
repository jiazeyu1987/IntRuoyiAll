from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260918_mes_edhr_batch_execution_upload_permission.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR batch execution upload permission migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_upload_permission_migration_declares_release_metadata_and_guards() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260917_mes_edhr_batch_execution_admin_visible" in text
    assert "type=menu" in text
    assert "riskLevel=low" in text
    assert "SET NAMES utf8mb4;" in text
    assert "ensure_mes_edhr_batch_execution_upload_permission" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing eDHR batch execution page menu 900033" in text
    assert "Batch execution upload menu id 900043 is already used by another permission" in text
    assert "Batch execution upload permission already exists on another menu id" in text
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


def test_upload_permission_menu_is_seeded_under_batch_execution() -> None:
    text = read_sql()

    for required in [
        "`id` = 900043",
        "`name` = 'eDHR批次执行上传'",
        "`permission` = 'mes:pro-edhr-batch-execution:upload'",
        "`type` = 3",
        "`sort` = 10",
        "`parent_id` = 900033",
        "INSERT INTO `system_menu`",
        "Invalid eDHR batch execution upload permission menu 900043",
    ]:
        assert required in text


def test_admin_super_admin_role_and_package_receive_upload_menu() -> None:
    text = read_sql()

    for required in [
        "tmp_mes_edhr_batch_execution_upload_admin_roles",
        "tmp_mes_edhr_batch_execution_upload_required_menus",
        "`user`.`username` = 'admin'",
        "`role`.`code` = 'super_admin'",
        "900043 AS `menu_id`",
        "INSERT INTO `system_role_menu`",
        "eDHR batch execution upload menu is missing from admin super_admin role",
        "tmp_mes_edhr_batch_execution_upload_admin_packages",
        "JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST(`required_menu`.`menu_id` AS JSON), '$')",
        "eDHR batch execution upload menu is missing from admin tenant package",
    ]:
        assert required in text

    assert "'tenant_admin'" not in text, "admin request must not widen access to tenant_admin"
