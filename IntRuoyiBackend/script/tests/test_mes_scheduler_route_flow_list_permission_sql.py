from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql" / "mysql" / "20260728_mes_scheduler_route_flow_list_permission.sql"
ROLE_SCOPE_SQL = REPO_ROOT / "sql" / "mysql" / "20260629_mes_smart_scheduling_role_scope.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def test_scheduler_route_flow_list_permission_migration_targets_formal_route_menu_permissions() -> None:
    text = _read(MIGRATION_SQL)

    assert "dependsOn=20260629_mes_smart_scheduling_role_scope,20260716_mes_route_version_permission_menu" in text
    assert "mes:pro-route:update" in text
    assert "mes:pro-route:version-query" in text
    assert "5723" in text
    assert "5730" in text
    assert "5724" not in text, "scheduler route-flow list operation scope must not grant delete permission"
    assert "mes:pro-route:delete" not in text


def test_scheduler_route_flow_list_permission_migration_restores_scheduler_role_bindings_only() -> None:
    text = _read(MIGRATION_SQL)

    assert "tmp_mes_scheduler_route_flow_list_operation_menu" in text
    assert "tmp_mes_scheduler_route_flow_list_operation_target" in text
    assert "`role`.`code` = 'mes_scheduler'" in text
    assert "`role`.`name` COLLATE utf8mb4_unicode_ci = _utf8mb4'排产员' COLLATE utf8mb4_unicode_ci" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "NOT EXISTS (" in text
    assert "FROM `system_role_menu` AS `existing`" in text
    assert "DELETE FROM `system_role_menu`" not in text


def test_scheduler_route_flow_list_permission_migration_updates_entitled_tenant_packages() -> None:
    text = _read(MIGRATION_SQL)

    assert "tmp_mes_scheduler_route_flow_list_operation_package_menu" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$')" in text
    assert "JSON_ARRAYAGG(`menu_id`) OVER" in text


def test_canonical_role_scope_keeps_scheduler_route_flow_list_operations() -> None:
    text = _read(ROLE_SCOPE_SQL)
    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]

    assert "5723" in scheduler_block
    assert "5730" in scheduler_block
