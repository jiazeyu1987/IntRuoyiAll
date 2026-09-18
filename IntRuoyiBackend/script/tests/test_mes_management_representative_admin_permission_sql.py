from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260918_mes_management_representative_admin_permission.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing management representative admin permission migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_targets_only_exact_admin_tenant_and_existing_role() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260814_mes_production_release_roles" in text
    assert "MES_MANAGEMENT_REPRESENTATIVE" in text
    assert "`user`.`username` = 'admin'" in text
    assert "`user`.`tenant_id`" in text
    assert "`role`.`tenant_id` = v_tenant_id" in text
    assert "'tenant_admin'" not in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Expected exactly one active admin user in one enabled tenant" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_migration_preserves_required_release_permission_chain() -> None:
    text = read_sql()

    assert "'mes:pro-edhr-release:query'" in text
    assert "'mes:pro-edhr-release:approve'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "`existing`.`user_id` = v_admin_id" in text
    assert "`existing`.`role_id` = v_role_id" in text
    assert "`existing`.`tenant_id` = v_tenant_id" in text
    assert "Admin management representative role binding was not persisted" in text
