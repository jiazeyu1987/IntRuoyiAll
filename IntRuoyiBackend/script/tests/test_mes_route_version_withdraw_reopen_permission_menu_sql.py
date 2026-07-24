from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260718_mes_route_version_withdraw_reopen_permission_menu.sql"
FULL_SEED_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "ruoyi-vue-pro.sql"

REQUIRED_PERMISSIONS = {
    "5735": ("工艺路线版本撤回", "mes:pro-route:version-withdraw", 15),
    "5736": ("工艺路线版本按意见修改", "mes:pro-route:version-reopen", 16),
}


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_withdraw_reopen_permission_migration_metadata_is_release_ready() -> None:
    sql = read_sql()
    first_line = sql.splitlines()[0]

    assert first_line.startswith("-- release-migration: allowedEnvironments=test,backup,prod; ")
    assert "dependsOn=20260716_mes_route_version_permission_menu,20260717_mes_route_version_approval_bpm_seed" in first_line
    assert "type=data" in first_line
    assert "riskLevel=medium" in first_line
    assert ".sql" not in first_line


def test_withdraw_reopen_permission_migration_seeds_backend_permissions() -> None:
    sql = read_sql()

    for menu_id, (name, permission, sort) in REQUIRED_PERMISSIONS.items():
        assert f"({menu_id}, '{name}', '{permission}', 3, {sort}, 5720" in sql
        assert f"'{permission}'" in sql

    assert "ON DUPLICATE KEY UPDATE" in sql
    assert "`permission` = VALUES(`permission`)" in sql
    assert "`deleted` = b'0'" in sql


def test_withdraw_reopen_permission_migration_grants_existing_route_version_roles_safely() -> None:
    sql = read_sql()
    upper = sql.upper()

    assert "system_role_menu" in sql
    assert "rm.`menu_id` IN (5732, 5734)" in sql
    assert "NOT EXISTS" in sql
    assert "5732 AS `source_menu_id`, 5735 AS `menu_id`" in sql
    assert "UNION ALL SELECT 5734, 5735" in sql
    assert "UNION ALL SELECT 5732, 5736" in sql
    assert "UNION ALL SELECT 5734, 5736" in sql
    assert "DELETE FROM" not in upper
    assert "TRUNCATE" not in upper
    assert "DROP TABLE" not in upper


def test_withdraw_reopen_permission_migration_syncs_tenant_package_menu_ids() -> None:
    sql = read_sql()

    assert "system_tenant_package" in sql
    assert "JSON_VALID(`package`.`menu_ids`)" in sql
    assert "JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$')" in sql
    assert "tmp_mes_route_version_withdraw_reopen_package_menu_ids" in sql
    for menu_id in REQUIRED_PERMISSIONS:
        assert menu_id in sql


def test_withdraw_reopen_permission_full_seed_contains_menu_and_default_admin_bindings() -> None:
    sql = FULL_SEED_SQL_PATH.read_text(encoding="utf-8")

    for menu_id, (name, permission, sort) in REQUIRED_PERMISSIONS.items():
        assert f"VALUES ({menu_id}, '{name}', '{permission}', 3, {sort}, 5720" in sql
        assert f", 1, {menu_id}, '1'," in sql
