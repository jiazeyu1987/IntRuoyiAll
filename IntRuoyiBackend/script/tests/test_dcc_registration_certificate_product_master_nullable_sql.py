from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260830_dcc_registration_certificate_upload_nullable_fields.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_upload_nullable_fields_sql_declares_release_contract_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260817_dcc_registration_certificate_core,20260828_dcc_registration_certificate_minimal_upload_schema; "
        "type=schema; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "normalize_dcc_reg_cert_upload_nullable_fields_20260830" in text
    assert "information_schema.COLUMNS" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing registration certificate master table for product_master_id nullable migration" in text
    assert "Missing registration certificate product_master_id column" in text
    assert "Missing registration certificate snapshot table for registrant_name nullable migration" in text
    assert "Missing registration certificate registrant_name column" in text
    assert "Registration certificate product_master_id nullable migration failed" in text
    assert "Registration certificate registrant_name nullable migration failed" in text


def test_upload_nullable_fields_sql_only_relaxes_upload_obsolete_field_nullability() -> None:
    text = read_sql()
    upper = text.upper()

    assert "ALTER TABLE `dcc_registration_certificate`" in text
    assert (
        "MODIFY COLUMN `product_master_id` bigint DEFAULT NULL "
        "COMMENT 'Optional MDM product master id'"
    ) in text
    assert "TABLE_NAME = 'dcc_registration_certificate'" in text
    assert "COLUMN_NAME = 'product_master_id'" in text
    assert "IS_NULLABLE = 'NO'" in text
    assert "IS_NULLABLE <> 'YES'" in text
    assert "ALTER TABLE `dcc_registration_certificate_snapshot`" in text
    assert (
        "MODIFY COLUMN `registrant_name` varchar(255) DEFAULT NULL "
        "COMMENT 'Registrant name snapshot'"
    ) in text
    assert "TABLE_NAME = 'dcc_registration_certificate_snapshot'" in text
    assert "COLUMN_NAME = 'registrant_name'" in text

    for forbidden in [
        "UPDATE `DCC_REGISTRATION_CERTIFICATE`",
        "UPDATE `DCC_REGISTRATION_CERTIFICATE_SNAPSHOT`",
        "DELETE FROM `DCC_REGISTRATION_CERTIFICATE`",
        "DELETE FROM `DCC_REGISTRATION_CERTIFICATE_SNAPSHOT`",
        "TRUNCATE TABLE",
        "`PRODUCT_MASTER_ID` BIGINT DEFAULT 0",
        "NOT NULL COMMENT 'OPTIONAL MDM PRODUCT MASTER ID'",
        "NOT NULL COMMENT 'REGISTRANT NAME SNAPSHOT'",
    ]:
        assert forbidden not in upper
