from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260718_bpm_admin_role_assignment.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing BPM admin role assignment migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_declares_metadata_role_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260714_approval_center_workflow_menu_consolidation; type=menu; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "ensure_bpm_admin_role_assignment_20260718" in text
    assert "910311" in text
    assert "BPM管理员" in text
    assert "bpm_admin" in text
    assert "WHERE username = 'admin'" in text
    assert "SIGNAL SQLSTATE '45000'" in text

    for required_guard in [
        "Missing enabled tenant 1 admin user",
        "Missing tenant 1 approval_center_entry role",
        "Missing tenant 1 approval_admin role",
        "Missing enabled BPM workflow management menu",
        "Missing enabled approval center entry menu",
        "BPM workflow management menu is not under approval center",
    ]:
        assert required_guard in text


def test_bpm_admin_role_id_is_resolved_by_business_key_not_hardcoded_id() -> None:
    text = read_sql()

    assert "DECLARE v_preferred_bpm_admin_role_id BIGINT DEFAULT 910311;" in text
    assert "DECLARE v_bpm_admin_role_id BIGINT DEFAULT NULL;" in text
    assert "WHERE code = 'bpm_admin'" in text
    assert "ORDER BY deleted ASC, id" in text
    assert "SET v_bpm_admin_role_id = v_preferred_bpm_admin_role_id;" in text
    assert "LAST_INSERT_ID()" in text
    assert "Role id 910311 is already occupied by another role" not in text
    assert "bpm_admin role already exists with a different id" not in text
    assert "DECLARE v_bpm_admin_role_id BIGINT DEFAULT 910311;" not in text


def test_bpm_admin_receives_workflow_management_and_form_center_menus() -> None:
    text = read_sql()

    assert "tmp_bpm_admin_expected_menu" in text
    for menu_id in [
        1200,
        1186,
        1193,
        1194,
        1195,
        1197,
        1198,
        1199,
        2913,
        1187,
        1188,
        1189,
        1190,
        1191,
        1192,
        2714,
        2715,
        2716,
        2717,
        2718,
        1209,
        1210,
        1211,
        1212,
        1213,
        2731,
        2732,
        2733,
        2734,
        2735,
        605071200,
        605071201,
        605071217,
    ]:
        assert f"SELECT {menu_id}" in text


def test_approval_center_entry_role_keeps_normal_pages_without_workflow_management() -> None:
    text = read_sql()

    assert "tmp_approval_center_entry_menu" in text
    for menu_id in [1200, 1207, 1208, 1201, 2713, 1221]:
        assert f"SELECT {menu_id}" in text

    assert "v_approval_entry_role_id" in text
    assert "role_id IN (v_approval_entry_role_id, v_approval_admin_role_id, v_bpm_admin_role_id)" in text
    assert "menu_id = 1222" in text


def test_migration_soft_restricts_workflow_management_to_bpm_admin_and_super_admin() -> None:
    text = read_sql()
    upper_text = text.upper()

    for forbidden in [
        "DELETE FROM SYSTEM_ROLE_MENU",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM SYSTEM_USER_ROLE",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "TRUNCATE TABLE SYSTEM_ROLE_MENU",
        "DROP TABLE SYSTEM_ROLE_MENU",
    ]:
        assert forbidden not in upper_text

    assert "SET role_menu.deleted = b'1'" in text
    assert "role.code NOT IN ('bpm_admin', 'super_admin')" in text
    assert "role_menu.menu_id <> 1200" in text


def test_migration_uses_idempotent_restore_insert_patterns() -> None:
    text = read_sql()

    assert "SET role_menu.deleted = b'0'" in text
    assert "INSERT INTO system_role_menu" in text
    assert "INSERT INTO system_user_role" in text
    assert "WHERE NOT EXISTS (" in text
    assert "COMMIT;" in text

