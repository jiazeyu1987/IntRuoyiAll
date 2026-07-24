from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260721_approval_center_admin_role_scope.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing approval center admin role scope migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_declares_metadata_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260630_approval_center_role_visibility; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "ensure_approval_center_admin_role_scope_20260721" in text
    assert "approval_admin" in text
    assert "审批中心管理员" in text
    assert "WHERE username = 'admin'" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing enabled tenant 1 admin user" in text
    assert "Missing tenant 1 approval_admin role" in text


def test_migration_assigns_admin_to_approval_admin_without_super_admin_shortcut() -> None:
    text = read_sql()

    assert "DECLARE v_admin_user_id BIGINT DEFAULT NULL;" in text
    assert "DECLARE v_approval_admin_role_id BIGINT DEFAULT NULL;" in text
    assert "UPDATE system_user_role" in text
    assert "INSERT INTO system_user_role" in text
    assert "user_id = v_admin_user_id" in text
    assert "role_id = v_approval_admin_role_id" in text
    assert "SELECT v_admin_user_id, v_approval_admin_role_id" in text
    assert "super_admin" not in text


def test_migration_is_idempotent_and_soft_restores_assignment() -> None:
    text = read_sql()
    upper_text = text.upper()

    for forbidden in [
        "DELETE FROM SYSTEM_USER_ROLE",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "TRUNCATE TABLE SYSTEM_USER_ROLE",
        "DROP TABLE SYSTEM_USER_ROLE",
    ]:
        assert forbidden not in upper_text

    assert "SET deleted = b'0'" in text
    assert "WHERE NOT EXISTS (" in text
    assert "COMMIT;" in text
