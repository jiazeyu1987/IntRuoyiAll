from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260829_registration_certificate_management_menu_hierarchy.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing registration certificate management hierarchy migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_registration_certificate_management_hierarchy_declares_release_and_fail_fast_contracts() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260816_dcc_registration_certificate_menu,20260829_mdm_associated_company_menu; "
        "type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "START TRANSACTION;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Registration certificate management target menu contract mismatch" in text
    assert "Registration certificate management retired menu contract mismatch" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_registration_certificate_management_hierarchy_groups_supported_page_menus_without_route_drift() -> None:
    text = read_sql()

    assert "990253" in text
    assert "注册证管理" in text
    assert "990230" in text
    assert "990249" in text

    for route in [
        "/mdm/registration-certificate",
        "/mdm/enterprise",
    ]:
        assert route in text

    for component in [
        "dcc/registration-certificate/index/index",
        "mdm/enterprise/index",
    ]:
        assert component in text

    for permission in [
        "dcc:registration-certificate:query-current",
        "mdm:enterprise:query",
    ]:
        assert permission in text


def test_registration_certificate_management_hierarchy_removes_missing_page_menus_from_visibility() -> None:
    text = read_sql()

    for token in [
        "tmp_registration_certificate_management_page_menu",
        "tmp_registration_certificate_management_retired_menu",
        "tmp_registration_certificate_management_package_ids",
        "tmp_registration_certificate_management_parent_package_ids",
        "tmp_registration_certificate_management_package_menu_ids",
        "tmp_registration_certificate_management_role_ids",
        "605071320",
        "605071321",
        "JSON_TABLE(",
        "JSON_ARRAYAGG",
        "UPDATE `system_tenant_package`",
        "UPDATE `system_role_menu`",
        "INSERT INTO `system_role_menu`",
        "CALL ensure_registration_certificate_management_menu_hierarchy();",
    ]:
        assert token in text

    assert "INSERT INTO `tmp_registration_certificate_management_page_menu` (`menu_id`) VALUES\n    (990230), (990249);" in text
    assert "SET `menu`.`deleted` = b'1'" in text
    assert "Registration certificate management retired menu final contract mismatch" in text
    assert "Registration certificate management retired package final contract mismatch" in text
