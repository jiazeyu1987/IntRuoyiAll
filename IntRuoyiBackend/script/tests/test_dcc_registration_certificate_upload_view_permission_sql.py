from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260903_dcc_registration_certificate_upload_view_permission.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_upload_role_view_permission_migration_declares_contract_and_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260903_dcc_registration_certificate_upload_role_category,20260829_registration_certificate_management_menu_hierarchy; "
        "type=permission; riskLevel=low"
    )
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing or ambiguous registration certificate upload role" in text
    assert "Missing or ambiguous registration certificate query permission menu" in text


def test_upload_role_view_permission_grants_only_registration_certificate_read_permission() -> None:
    text = read_sql()
    upper = text.upper()

    assert "'dcc_registration_certificate_upload'" in text
    assert "'dcc:registration-certificate:query-current'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "WHERE NOT EXISTS" in text

    for forbidden in [
        "UPLOAD:CREATE",
        "UPLOAD:APPROVE",
        "REGISTRATION-CERTIFICATE:CREATE",
        "REGISTRATION-CERTIFICATE:UPDATE",
        "REGISTRATION-CERTIFICATE:FORMALIZE",
        "REGISTRATION-CERTIFICATE:VOID",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "TRUNCATE TABLE",
    ]:
        assert forbidden not in upper


def test_upload_role_view_permission_uses_the_unique_visible_registration_certificate_tab_menu() -> None:
    text = read_sql()

    assert "`menu`.`id` = 990230" in text
    assert "`menu`.`type` = 2" in text
    assert "`menu`.`path` = '/mdm/registration-certificate'" in text
