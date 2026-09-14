from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260805_mes_edhr_frontline_pqc_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR frontline PQC menu SQL migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_frontline_pqc_menu_declares_release_metadata_and_fail_fast_guards() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260804_mes_edhr_qa_menu" in text
    assert "ensure_mes_edhr_frontline_pqc_menu" in text
    assert "SET NAMES utf8mb4;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing eDHR parent menu 900220" in text
    assert "system_menu id 900438 is already used by another active menu" in text
    assert "Frontline PQC menu route already exists on a different menu id" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_frontline_pqc_menu_creates_visible_standalone_entry() -> None:
    text = read_sql()

    for required in [
        "900438 AS `id`, '一线PQC' AS `name`",
        "2 AS `type`,",
        "'mes:pro-edhr-batch-execution:query' AS `permission`",
        "4 AS `sort`",
        "'/mes/pro/feedback/edhr-batch-pqc-fill' AS `path`",
        "'mes/pro/edhr-batch/BatchPqcFillPage' AS `component`",
        "'MesProEdhrBatchPqcFill' AS `component_name`",
        "`parent_id` = 900220",
        "`visible` = b'1'",
        "CAST('900438' AS JSON)",
    ]:
        assert required in text

    assert "900435 AS `menu_id`, 5 AS `sort`" in text
    assert "900033 AS `menu_id`, 6 AS `sort`" in text
    assert "900025 AS `menu_id`, 7 AS `sort`" in text
    assert "900432 AS `menu_id`, 8 AS `sort`" in text
    assert "eDHR frontline PQC visible menu order is incomplete" in text


def test_frontline_pqc_menu_is_bound_to_tenant_packages_and_admin_roles() -> None:
    text = read_sql()

    for required in [
        "tmp_mes_edhr_frontline_pqc_target_packages",
        "tmp_mes_edhr_frontline_pqc_package_menu_ids",
        "tmp_mes_edhr_frontline_pqc_target_roles",
        "system_tenant_package",
        "system_role_menu",
        "system_role",
        "system_tenant",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "'tenant_admin'",
        "'super_admin'",
        "INSERT INTO `system_role_menu`",
        "Frontline PQC menu is not bound to any admin role",
        "Frontline PQC menu is missing from target tenant packages",
    ]:
        assert required in text
