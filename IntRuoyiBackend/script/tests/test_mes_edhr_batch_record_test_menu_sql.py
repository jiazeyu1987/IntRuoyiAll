from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260808_mes_edhr_batch_record_test_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR batch record test menu SQL migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_batch_record_test_menu_declares_metadata_and_fail_fast_guards() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260805_mes_edhr_frontline_pqc_menu" in text
    assert "type=menu" in text
    assert "ensure_mes_edhr_batch_record_test_menu" in text
    assert "SET NAMES utf8mb4;" in text
    assert "SIGNAL SQLSTATE '45000'" in text

    for required_guard in [
        "Invalid system_tenant_package.menu_ids JSON; cannot insert batch record test menu",
        "Missing eDHR parent menu 900220; cannot insert batch record test menu",
        "Missing eDHR batch execution menu 900033; cannot insert batch record test menu",
        "system_menu id 900440 is already used by another active menu",
        "Batch record test menu route already exists on a different menu id",
    ]:
        assert required_guard in text

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_batch_record_test_menu_creates_visible_admin_entry() -> None:
    text = read_sql()

    for required in [
        "900440 AS `id`",
        "'批记录测试' AS `name`",
        "'mes:pro-edhr-batch-execution:query' AS `permission`",
        "2 AS `type`",
        "6 AS `sort`",
        "900220 AS `parent_id`",
        "'/mes/pro/feedback/edhr-batch-test' AS `path`",
        "'mes/pro/edhr-batch/BatchRecordTestPage' AS `component`",
        "'MesProEdhrBatchRecordTest' AS `component_name`",
        "`visible` = b'1'",
        "Batch record test visible menu is incomplete",
    ]:
        assert required in text

    assert "900440 AS `menu_id`, 6 AS `sort`" in text
    assert "900033 AS `menu_id`, 7 AS `sort`" in text
    assert "900025 AS `menu_id`, 8 AS `sort`" in text
    assert "900432 AS `menu_id`, 9 AS `sort`" in text


def test_batch_record_test_menu_is_bound_to_tenant_packages_and_admin_roles() -> None:
    text = read_sql()

    for required in [
        "tmp_mes_edhr_batch_record_test_target_packages",
        "tmp_mes_edhr_batch_record_test_package_menu_ids",
        "tmp_mes_edhr_batch_record_test_target_roles",
        "system_tenant_package",
        "system_role_menu",
        "system_role",
        "system_tenant",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "CAST('900440' AS JSON)",
        "'tenant_admin'",
        "'super_admin'",
        "INSERT INTO `system_role_menu`",
        "Batch record test menu is missing from target tenant packages",
        "Batch record test menu is not bound to any admin role",
    ]:
        assert required in text
