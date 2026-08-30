from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260830_dcc_registration_certificate_associated_company_backfill.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_backfill_sql_declares_release_contract_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260817_dcc_registration_certificate_core; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "START TRANSACTION;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing registration certificate owner company master data source" in text
    assert "Registration certificate owner company backfill count mismatch" in text


def test_backfill_sql_adds_only_registration_certificate_owned_companies() -> None:
    text = read_sql()

    assert "dcc_registration_certificate" in text
    assert "mdm_enterprise" in text
    assert "`certificate`.`owner_company_id`" in text
    assert "`certificate`.`status` = 'ACTIVE'" in text
    assert "'OWNED_COMPANY'" in text
    assert "'ENABLE'" in text
    assert "HIST-REG-OWN-" in text
    assert "`enterprise`.`id` = `certificate`.`owner_company_id`" in text
    assert "`enterprise`.`tenant_id` = `certificate`.`tenant_id`" in text
    assert "`enterprise`.`deleted` = b'0'" in text


def test_backfill_sql_is_non_destructive_and_does_not_auto_authorize_users() -> None:
    upper = read_sql().upper()

    for forbidden in [
        "DELETE FROM `MDM_ENTERPRISE`",
        "DELETE FROM `MDM_USER_COMPANY_SCOPE`",
        "DELETE FROM `MDM_ROLE_COMPANY_SCOPE`",
        "TRUNCATE TABLE",
        "INSERT INTO `MDM_USER_COMPANY_SCOPE`",
        "INSERT INTO `MDM_ROLE_COMPANY_SCOPE`",
    ]:
        assert forbidden not in upper
