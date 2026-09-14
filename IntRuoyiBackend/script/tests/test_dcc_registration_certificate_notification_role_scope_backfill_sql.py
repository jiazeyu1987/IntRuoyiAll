from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260830_dcc_registration_certificate_notification_role_scope_backfill.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_notification_role_scope_backfill_declares_contract_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260816_mdm_enterprise_company_scope,20260818_dcc_registration_certificate_reminder,"
        "20260830_dcc_registration_certificate_associated_company_backfill; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "START TRANSACTION;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "registrationCertificateReminderDailyJob" in text
    assert "Missing registration certificate notification role scope source" in text
    assert "Registration certificate notification role scope backfill count mismatch" in text
    assert "Registration certificate notification role scope soft-deleted reservation exists" in text


def test_notification_role_scope_backfill_uses_configured_roles_and_current_certificate_companies() -> None:
    text = read_sql()

    assert "JSON_TABLE" in text
    assert "$.roleIds[*]" in text
    assert "`infra_job`" in text
    assert "`dcc_registration_certificate`" in text
    assert "`mdm_enterprise`" in text
    assert "`mdm_role_company_scope`" in text
    assert "`system_role`" in text
    assert "`certificate`.`owner_company_id`" in text
    assert "`certificate`.`status` = 'ACTIVE'" in text
    assert "`enterprise`.`type` = 'OWNED_COMPANY'" in text
    assert "`enterprise`.`status` = 'ENABLE'" in text
    assert "`role`.`status` = 0" in text
    assert "`role_scope`.`status` = 'ENABLE'" in text
    assert "910218" not in text
    assert "910231" not in text


def test_notification_role_scope_backfill_is_non_destructive_and_does_not_authorize_users() -> None:
    upper = read_sql().upper()

    for forbidden in [
        "DELETE FROM `MDM_ROLE_COMPANY_SCOPE`",
        "DELETE FROM `MDM_USER_COMPANY_SCOPE`",
        "DELETE FROM `MDM_ENTERPRISE`",
        "UPDATE `MDM_ROLE_COMPANY_SCOPE`",
        "UPDATE `MDM_USER_COMPANY_SCOPE`",
        "TRUNCATE TABLE",
        "INSERT INTO `MDM_USER_COMPANY_SCOPE`",
    ]:
        assert forbidden not in upper
