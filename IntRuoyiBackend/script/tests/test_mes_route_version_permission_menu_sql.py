from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260716_mes_route_version_permission_menu.sql"
FULL_SEED_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "ruoyi-vue-pro.sql"

REQUIRED_PERMISSIONS = {
    "5730": ("工艺路线版本查询", "mes:pro-route:version-query", 10),
    "5731": ("工艺路线版本创建", "mes:pro-route:version-create", 11),
    "5732": ("工艺路线版本提交", "mes:pro-route:version-submit", 12),
    "5733": ("工艺路线版本取消", "mes:pro-route:version-cancel", 13),
    "5734": ("工艺路线版本发布", "mes:pro-route:version-publish", 14),
}


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def read_full_seed_sql() -> str:
    return FULL_SEED_SQL_PATH.read_text(encoding="utf-8")


def test_route_version_permission_menu_migration_metadata_is_release_ready() -> None:
    sql = read_sql()
    first_line = sql.splitlines()[0]

    assert first_line.startswith("-- release-migration: allowedEnvironments=test,backup,prod; ")
    assert "dependsOn=20260715_mes_route_version_lifecycle" in first_line
    assert "type=data" in first_line
    assert "riskLevel=medium" in first_line
    assert ".sql" not in first_line


def test_route_version_permission_menu_seeds_all_backend_permissions_under_route_parent() -> None:
    sql = read_sql()

    for menu_id, (name, permission, sort) in REQUIRED_PERMISSIONS.items():
        assert f"({menu_id}, '{name}', '{permission}', 3, {sort}, 5720" in sql
        assert f"'{permission}'" in sql

    assert "ON DUPLICATE KEY UPDATE" in sql
    assert "`parent_id` = VALUES(`parent_id`)" in sql
    assert "`permission` = VALUES(`permission`)" in sql


def test_route_version_permission_menu_grants_existing_route_roles_without_destructive_changes() -> None:
    sql = read_sql()
    upper = sql.upper()

    assert "system_role_menu" in sql
    assert "rm.`menu_id` IN (5721, 5722, 5723)" in sql
    assert "NOT EXISTS" in sql
    for source_menu_id, target_menu_id in [
        ("5721", "5730"),
        ("5722", "5731"),
        ("5723", "5732"),
        ("5723", "5733"),
        ("5723", "5734"),
    ]:
        assert f"{source_menu_id} AS `source_menu_id`, {target_menu_id} AS `menu_id`" in sql or (
            f"UNION ALL SELECT {source_menu_id}, {target_menu_id}" in sql
        )

    assert "DELETE FROM" not in upper
    assert "TRUNCATE" not in upper
    assert "DROP TABLE" not in upper


def test_route_version_permission_menu_syncs_tenant_package_menu_ids() -> None:
    sql = read_sql()

    assert "system_tenant_package" in sql
    assert "JSON_VALID(`package`.`menu_ids`)" in sql
    assert "JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$')" in sql
    assert "tmp_mes_route_version_package_menu_ids" in sql
    for menu_id in REQUIRED_PERMISSIONS:
        assert menu_id in sql


def test_route_version_permission_full_seed_contains_menu_and_default_admin_bindings() -> None:
    sql = read_full_seed_sql()

    for menu_id, (name, permission, sort) in REQUIRED_PERMISSIONS.items():
        assert (
            f"VALUES ({menu_id}, '{name}', '{permission}', 3, {sort}, 5720"
            in sql
        )
        assert f", 1, {menu_id}, '1'," in sql
