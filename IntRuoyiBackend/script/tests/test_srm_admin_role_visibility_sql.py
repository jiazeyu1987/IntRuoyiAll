from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260629_srm_admin_role_visibility.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing SRM admin role visibility migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_srm_admin_role_visibility_sql_declares_expected_role_and_targets() -> None:
    text = _read_sql()

    required = [
        "SET NAMES utf8mb4;",
        "ensure_srm_admin_role_visibility",
        "'SRM管理员'",
        "'srm_admin'",
        "`id` = 991000",
        "`path` = '/srm'",
        "`username` = 'admin'",
        "system_role",
        "system_role_menu",
        "system_user_role",
    ]

    for snippet in required:
        assert snippet in text


def test_srm_admin_role_visibility_sql_is_fail_fast_and_idempotent() -> None:
    text = _read_sql()

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "INSERT INTO `system_role`" in text
    assert "WHERE NOT EXISTS (" in text
    assert "UPDATE `system_role_menu` AS `role_menu`" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "FROM `system_role_menu` AS `existing`" in text
    assert "UPDATE `system_user_role` AS `user_role`" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "DELETE FROM `system_role_menu`" not in text
    assert "DELETE FROM `system_user_role`" not in text


def test_srm_admin_role_visibility_sql_scopes_role_to_srm_menu_tree_only() -> None:
    text = _read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_srm_admin_menu_ids` AS" in text
    assert "`parent_id` = 991000" in text
    assert "`code` = 'srm_admin'" in text


def test_srm_admin_role_visibility_sql_must_not_repurpose_fixed_role_id_910240() -> None:
    text = _read_sql()

    forbidden_snippets = [
        "(`id` = 910240 OR `code` = 'srm_admin')",
        "`role_id` = 910240",
        "SELECT\n    910240,",
        "    910240,\n    'SRM管理员'",
    ]

    for snippet in forbidden_snippets:
        assert snippet not in text, f"migration must not hard-wire historical role id 910240: {snippet}"


def test_srm_admin_role_visibility_sql_must_cleanup_non_srm_role_menus() -> None:
    text = _read_sql()

    assert "UPDATE `system_role_menu` AS `role_menu`" in text
    assert "`role_menu`.`deleted` = b'1'" in text
    assert "LEFT JOIN `tmp_srm_admin_menu_ids` AS `srm_menu`" in text
    assert "`srm_menu`.`id` IS NULL" in text
    assert "`role_id` = v_srm_admin_role_id" in text or "`role_menu`.`role_id` = v_srm_admin_role_id" in text


def test_srm_admin_role_visibility_sql_resolves_srm_admin_role_id_dynamically() -> None:
    text = _read_sql()

    assert "DECLARE v_srm_admin_role_id BIGINT DEFAULT NULL;" in text
    assert "SELECT `id`\n  INTO v_srm_admin_role_id" in text
    assert "`code` = 'srm_admin'" in text
    assert "ORDER BY CASE WHEN `code` = 'srm_admin' THEN 0 ELSE 1 END, `id`" in text
    assert (
        "WHERE v_srm_admin_role_id IS NOT NULL" in text
        or "IF v_srm_admin_role_id IS NULL THEN" in text
    )
