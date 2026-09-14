from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260903_dcc_registration_certificate_wanglixuan_upload_role.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_wanglixuan_upload_role_migration_is_test_scoped_and_fails_fast() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test; "
        "dependsOn=20260903_dcc_registration_certificate_upload_view_permission; "
        "type=permission; riskLevel=low"
    )
    assert "`target_user`.`username` = 'wanglixuan'" in text
    assert "`target_user`.`tenant_id` = 1" in text
    assert "'dcc_registration_certificate_upload'" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing or ambiguous wanglixuan user in tenant 1" in text
    assert "Missing or ambiguous registration certificate upload role for wanglixuan" in text


def test_wanglixuan_upload_role_migration_grants_only_the_target_role_idempotently() -> None:
    text = read_sql()
    upper = text.upper()

    assert "INSERT INTO `system_user_role`" in text
    assert "WHERE NOT EXISTS" in text
    assert "`user_role`.`deleted` = b'0'" in text
    assert "Wanglixuan registration certificate upload role grant incomplete" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
        "SUPER_ADMIN",
        "TENANT_ADMIN",
    ]:
        assert forbidden not in upper
