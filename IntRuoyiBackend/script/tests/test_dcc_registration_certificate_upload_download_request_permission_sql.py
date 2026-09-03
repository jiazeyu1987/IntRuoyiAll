from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260903_dcc_registration_certificate_upload_download_request_permission.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_upload_download_request_permission_migration_grants_exact_permission() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260903_dcc_registration_certificate_upload_hidden_routes; "
        "type=permission; riskLevel=low"
    )
    assert "'dcc_registration_certificate_upload'" in text
    assert "990237" in text
    assert "'dcc:registration-certificate:access-request:create'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "WHERE NOT EXISTS" in text


def test_upload_download_request_permission_does_not_grant_approval_or_bypass() -> None:
    text = read_sql().lower()

    for forbidden in [
        "access-request:approve",
        "upload:approve",
        "renewal:void",
        "registration-certificate:void",
        "delete from",
        "truncate table",
    ]:
        assert forbidden not in text
