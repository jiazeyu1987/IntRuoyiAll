from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260908_system_temporary_role_grant.sql"
TEST_SCHEMA = REPO_ROOT / "yudao-module-system/src/test/resources/sql/create_tables.sql"


def test_temporary_role_grant_migration_declares_schema_and_job() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260830_system_user_lifecycle_deactivation; type=schema; riskLevel=medium"
    )
    required = [
        "system_temporary_role_grant",
        "system_temporary_role_grant_audit",
        "expire_time",
        "remind_time",
        "PENDING/ACTIVE/REVOKED/EXPIRED",
        "APPLY/APPROVE/REVOKE/EXPIRE/REMIND/USE",
        "temporaryRoleGrantExpireJob",
        "temporaryRoleGrantReminderJob",
        "SYSTEM_TEMPORARY_ROLE_GRANT_EXPIRING",
        "idx_system_temp_role_grant_user_status",
        "idx_system_temp_role_grant_remind",
        "idx_system_temp_role_grant_audit_use",
        "临时角色授权",
        "system/temporary-role-grant/index",
        "SystemTemporaryRoleGrant",
        "system:temporary-role-grant:create",
        "system:temporary-role-grant:approve",
        "system:temporary-role-grant:revoke",
        "system_role_menu",
        "super_admin",
        "tenant_admin",
    ]
    for snippet in required:
        assert snippet in sql


def test_temporary_role_grant_migration_does_not_seed_business_grants() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8").upper()

    forbidden = [
        "INSERT INTO `SYSTEM_USER_ROLE`",
        "UPDATE `SYSTEM_USER_ROLE`",
        "DELETE FROM `SYSTEM_USER_ROLE`",
    ]
    for snippet in forbidden:
        assert snippet not in sql


def test_h2_schema_contains_temporary_role_grant_tables() -> None:
    schema = TEST_SCHEMA.read_text(encoding="utf-8")

    assert 'CREATE TABLE IF NOT EXISTS "system_temporary_role_grant"' in schema
    assert 'CREATE TABLE IF NOT EXISTS "system_temporary_role_grant_audit"' in schema
    assert '"expire_time" timestamp NOT NULL' in schema
    assert '"remind_time" timestamp DEFAULT NULL' in schema
    assert '"permission_code" varchar(150) DEFAULT NULL' in schema
