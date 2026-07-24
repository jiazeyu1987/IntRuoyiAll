from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260630_approval_center_role_visibility.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing approval center role visibility migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_sql_declares_expected_roles_menu_ids_and_fail_fast_checks() -> None:
    text = _read_sql()

    required = [
        "SET NAMES utf8mb4;",
        "ensure_approval_center_role_visibility",
        "910295",
        "approval_center_entry",
        "审批中心入口",
        "910296",
        "approval_admin",
        "审批管理员",
        "1200",
        "1221",
        "zhaojie",
        "admin",
        "SIGNAL SQLSTATE '45000'",
    ]

    for snippet in required:
        assert snippet in text


def test_sql_only_binds_minimum_approval_center_menu_set() -> None:
    text = _read_sql()

    assert "tmp_approval_center_target_menu" in text
    assert "SELECT 1200 AS `menu_id`" in text
    assert "UNION ALL SELECT 1221" in text
    assert "980104" in text
    assert "SELECT 980104 AS `menu_id`" not in text
    assert "UNION ALL SELECT 980104" not in text
    assert "SET `deleted` = b'1'" in text
    assert "AND `menu_id` NOT IN (SELECT `menu_id` FROM `tmp_approval_center_target_menu`)" in text
    assert "AND `menu_id` <> 980104" in text


def test_sql_does_not_delete_and_uses_idempotent_restore_insert_patterns() -> None:
    text = _read_sql()

    assert "DELETE FROM `system_role_menu`" not in text
    assert "DELETE FROM `system_user_role`" not in text
    assert "SET `deleted` = b'0'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "NOT EXISTS (" in text


def test_sql_persists_tenant_id_for_role_menu_and_user_role_bindings() -> None:
    text = _read_sql()

    assert "INSERT INTO `system_role_menu`" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "`tenant_id` = 1" in text
    assert "(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)" in text
    assert "(`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)" in text
    assert "NOW(), b'0', 1" in text


def test_sql_assigns_entry_role_to_all_enabled_tenant1_users_and_admin_role_only_to_admin() -> None:
    text = _read_sql()

    assert "tmp_approval_center_enabled_users" in text
    assert "`tenant_id` = 1" in text
    assert "`status` = 0" in text
    assert "approval_center_entry" in text
    assert "approval_admin" in text
    assert "WHERE `username` = 'admin'" in text
    assert "WHERE `username` = 'zhaojie'" in text
    assert "审批中心全量可见管理员角色" in text
    assert "SELECT v_admin_user_id, v_admin_role_id" in text


def test_sql_creates_or_recovers_fixed_role_ids_without_dynamic_max_plus_one() -> None:
    text = _read_sql()

    assert "910295" in text
    assert "910296" in text
    assert "COALESCE(MAX(`existing_role`.`id`)" not in text
    assert "MAX(`existing_role`.`id`)" not in text


def test_sql_uses_fail_fast_guards_for_required_runtime_rows() -> None:
    text = _read_sql()

    assert "Missing tenant 1 admin user" in text
    assert "Missing tenant 1 zhaojie user" in text
    assert "Missing approval center menu 1200" in text
    assert "Missing approval center permission menu 1221" in text
