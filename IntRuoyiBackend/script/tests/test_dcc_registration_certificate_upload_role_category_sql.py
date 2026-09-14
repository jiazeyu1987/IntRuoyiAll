import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260903_dcc_registration_certificate_upload_role_category.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_upload_role_category_migration_declares_release_contract_and_fails_fast() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260829_dcc_registration_certificate_upload_approver_role,20260707_system_role_category_management,20260816_dcc_registration_certificate_menu; "
        "type=permission; riskLevel=low"
    )
    assert "ensure_dcc_reg_cert_upload_role_category_20260903" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing or ambiguous registration department role category" in text
    assert "Missing or ambiguous registration certificate upload permission menu" in text
    assert "Missing or ambiguous target registration certificate role" in text


def test_upload_role_category_migration_assigns_exact_permission_and_category() -> None:
    text = read_sql()

    assert "'registration'" in text
    assert "'注册部'" in text
    assert "'dcc:registration-certificate:upload:create'" in text
    assert "'dcc_registration_certificate_upload'" in text
    assert "'dcc_registration_certificate_approver'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "UPDATE `system_role` AS `role`" in text


def test_upload_role_category_migration_is_non_destructive_and_idempotent() -> None:
    upper = read_sql().upper()

    for forbidden in [
        "DELETE FROM `SYSTEM_ROLE`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper

    assert "WHERE NOT EXISTS" in upper
    assert "`ROLE_MENU`.`DELETED` = B'0'" in upper


def test_upload_role_category_migration_uses_a_mysql_valid_procedure_identifier() -> None:
    text = read_sql()
    procedure_name = re.search(r"CREATE PROCEDURE (\w+)\(\)", text)

    assert procedure_name is not None
    assert len(procedure_name.group(1)) <= 64
