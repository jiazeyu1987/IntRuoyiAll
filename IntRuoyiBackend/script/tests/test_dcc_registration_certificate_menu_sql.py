from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260816_dcc_registration_certificate_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_registration_certificate_menu_declares_menu_release_contract() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260818_dcc_registration_certificate_reminder,20260626_dcc_basic_data_global_submenu; "
        "type=menu; riskLevel=medium"
    )


def test_registration_certificate_menu_keeps_dynamic_menu_and_permissions_in_code_only_scope() -> None:
    text = read_sql()

    for token in [
        "INSERT INTO `system_menu`",
        "dcc:registration-certificate:query-current",
        "dcc:registration-certificate:upload:approve",
        "dcc/registration-certificate/index/index",
        "DccRegistrationCertificateIndex",
    ]:
        assert token in text


def test_registration_certificate_menu_is_non_destructive() -> None:
    upper = read_sql().upper()

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper
