from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260807_test_tenant1_all_role_permission_sync.sql"

SOURCE_ROLE_COUNT = 60
SOURCE_ROLE_PERMISSION_COUNT = 1676
SOURCE_MISSING_PERMISSION_COUNT = 12

MISSING_PERMISSIONS = {
    "dcc:controlled-file:preview",
    "dcc:project-code:create",
    "dcc:project-code:delete",
    "erp:fenbeitong-voucher:config",
    "erp:fenbeitong-voucher:query",
    "erp:fenbeitong-voucher:save",
    "mes:pro-batch-record-version:confirm",
    "mes:pro-batch-record-version:import",
    "mes:pro-batch-record-version:rollback-request",
    "mes:pro-process-pool-team-leader:abnormal",
    "mes:pro-process-pool-team-leader:maintain",
    "mes:pro-process-pool-team-leader:review",
}


def read_sql() -> str:
    assert SQL_PATH.is_file(), f"required migration missing: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_all_role_permission_sync_has_test_only_high_risk_release_contract() -> None:
    sql = read_sql()
    first_line = sql.splitlines()[0]

    assert "allowedEnvironments=test" in first_line
    assert "dependsOn=20260728_mes_scheduler_route_flow_list_permission" in first_line
    assert "type=data" in first_line
    assert "riskLevel=high" in first_line
    assert "backup,prod" not in first_line
    assert f"-- source-active-role-count: {SOURCE_ROLE_COUNT}" in sql
    assert f"-- source-role-permission-count: {SOURCE_ROLE_PERMISSION_COUNT}" in sql
    assert f"-- source-missing-permission-count: {SOURCE_MISSING_PERMISSION_COUNT}" in sql


def test_all_role_permission_sync_uses_stable_role_and_category_keys() -> None:
    sql = read_sql()

    assert "tmp_test_tenant1_role_source" in sql
    assert "tmp_test_tenant1_role_target" in sql
    assert "tmp_test_tenant1_role_category_target" in sql
    assert "`role`.`code` = `source`.`code`" in sql
    assert "`category`.`code` = `source`.`category_code`" in sql
    assert "INSERT INTO `system_role` (" in sql
    role_insert_columns = sql.split("INSERT INTO `system_role` (", 1)[1].split(")", 1)[0]
    assert "`id`" not in role_insert_columns
    assert "`tenant_id`" in role_insert_columns


def test_all_role_permission_sync_resolves_menus_by_permission_and_creates_missing_permissions() -> None:
    sql = read_sql()

    assert "tmp_test_tenant1_missing_menu_source" in sql
    assert "tmp_test_tenant1_permission_menu_target" in sql
    assert "ROW_NUMBER() OVER" in sql
    assert "`menu`.`permission` = `desired`.`permission`" in sql
    assert "Missing source permissions after target menu resolution" in sql
    assert "900436" in sql, "team-leader action permissions must use the formal test parent"
    for permission in MISSING_PERMISSIONS:
        assert permission in sql


def test_all_role_permission_sync_aligns_only_source_role_permission_sets() -> None:
    sql = read_sql()

    assert "tmp_test_tenant1_role_permission_source" in sql
    assert "tmp_test_tenant1_role_menu_desired" in sql
    assert "tmp_test_tenant1_role_menu_ancestor" in sql
    assert "SET `role_menu`.`deleted` = b'1'" in sql
    assert "SET `role_menu`.`deleted` = b'0'" in sql
    assert "INSERT INTO `system_role_menu`" in sql
    assert "`role`.`tenant_id` = 1" in sql
    assert "`role`.`code` = `source_role`.`code`" in sql


def test_all_role_permission_sync_preserves_users_other_tenants_and_target_only_roles() -> None:
    sql = read_sql()
    upper = sql.upper()

    assert "INSERT INTO `SYSTEM_USER_ROLE`" not in upper
    assert "UPDATE `SYSTEM_USER_ROLE`" not in upper
    assert "DELETE FROM `SYSTEM_USER_ROLE`" not in upper
    assert "DELETE FROM `SYSTEM_ROLE`" not in upper
    assert "TRUNCATE" not in upper
    assert "Target-only roles and all user-role bindings must remain unchanged" in sql
    assert "Other-tenant role-menu rows must remain unchanged" in sql
