from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260903_dcc_registration_certificate_upload_hidden_routes.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_upload_hidden_routes_migration_grants_detail_and_history_routes() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260903_dcc_registration_certificate_upload_view_permission; "
        "type=permission; riskLevel=low"
    )
    assert "'dcc_registration_certificate_upload'" in text
    assert "990231" in text
    assert "'/mdm/registration-certificate/detail/:id'" in text
    assert "990232" in text
    assert "'/mdm/registration-certificate/history/:id'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "WHERE NOT EXISTS" in text


def test_upload_hidden_routes_migration_does_not_grant_project_code_or_write_permissions() -> None:
    text = read_sql().lower()

    for forbidden in [
        "dcc:project-code:query",
        "upload:approve",
        "renewal:void",
        "registration-certificate:void",
        "delete from",
        "truncate table",
    ]:
        assert forbidden not in text
