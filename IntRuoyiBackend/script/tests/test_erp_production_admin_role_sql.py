from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260630_erp_production_admin_role.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing ERP production admin role migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_erp_production_admin_role_sql_declares_expected_role_and_scope() -> None:
    text = _read_sql()

    required = [
        "SET NAMES utf8mb4;",
        "ensure_erp_production_admin_role",
        "'erp_production_admin'",
        "0x455250E7949FE4BAA7E7AEA1E79086E59198",
        "0x45525020E7949FE4BAA7E7AEA1E79086E88F9CE58D95E78BACE7AB8BE68E88E69D83E8A792E889B2",
        "2563",
        "6020",
        "6021",
        "6022",
        "6023",
        "6024",
        "6025",
        "6026",
        "`user`.`username` = 'admin'",
        "system_role_menu",
        "system_user_role",
    ]

    for snippet in required:
        assert snippet in text


def test_erp_production_admin_role_sql_creates_or_recovers_role_idempotently() -> None:
    text = _read_sql()

    assert "DECLARE v_role_name VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;" in text
    assert "DECLARE v_role_remark VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;" in text
    assert "SET v_role_name = CONVERT(0x455250E7949FE4BAA7E7AEA1E79086E59198 USING utf8mb4);" in text
    assert "SET v_role_remark = CONVERT(0x45525020E7949FE4BAA7E7AEA1E79086E88F9CE58D95E78BACE7AB8BE68E88E69D83E8A792E889B2 USING utf8mb4);" in text
    assert "UPDATE `system_role`" in text
    assert "SET `name` = v_role_name," in text
    assert "`code` = 'erp_production_admin'" in text
    assert "`status` = 0," in text
    assert "`deleted` = b'0'," in text
    assert "INSERT INTO `system_role`" in text
    assert "SELECT COALESCE(MAX(`existing_role`.`id`), 910294) + 1" in text
    assert "FROM DUAL" in text
    assert "WHERE NOT EXISTS (" in text


def test_erp_production_admin_role_sql_restores_or_inserts_allowed_role_menus() -> None:
    text = _read_sql()

    assert "tmp_erp_production_admin_target_roles" in text
    assert "tmp_erp_production_admin_allowed_menu" in text
    assert "SELECT v_erp_production_admin_role_id AS `role_id`, 1 AS `tenant_id`" in text
    assert "SET `role_menu`.`deleted` = b'0'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "FROM `system_role_menu` AS `existing`" in text


def test_erp_production_admin_role_sql_soft_deletes_production_tree_for_other_roles_only() -> None:
    text = _read_sql()

    assert "UPDATE `system_role_menu` AS `role_menu`" in text
    assert "`role_menu`.`menu_id` IN (6020, 6021, 6022, 6023, 6024, 6025, 6026)" in text
    assert "`role_menu`.`role_id` <> v_erp_production_admin_role_id" in text
    assert "`role_menu`.`deleted` = b'1'" in text
    assert "DELETE FROM `system_role_menu`" not in text


def test_erp_production_admin_role_sql_restores_or_inserts_admin_binding_idempotently() -> None:
    text = _read_sql()

    assert "DECLARE v_admin_user_id BIGINT DEFAULT NULL;" in text
    assert "SELECT `id`\n  INTO v_admin_user_id" in text
    assert "`user`.`username` = 'admin'" in text
    assert "UPDATE `system_user_role` AS `user_role`" in text
    assert "`user_role`.`deleted` = b'0'" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "FROM `system_user_role` AS `existing`" in text
    assert "DELETE FROM `system_user_role`" not in text


def test_erp_production_admin_role_sql_fails_fast_on_missing_baseline() -> None:
    text = _read_sql()

    assert "Missing ERP production admin menu baseline in tenant 1" in text
    assert "Missing enabled super_admin role baseline in tenant 1" in text
    assert "Missing tenant 1 admin user for ERP production role binding" in text
