from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260714_signature_my_signature_admin_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"{SQL_PATH} must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_my_signature_menu_is_declared_as_ordinary_signature_child():
    sql = read_sql()

    assert "SET @unified_signature_my_signature_menu_id := 900418;" in sql
    assert "'我的签名'" in sql
    assert "`path` = 'my-signature'" in sql
    assert "`component_name` = 'SignatureGovernanceMySignature'" in sql
    assert "`permission` = 'signature-governance:policy:query'" in sql
    assert "`permission` = 'dcc:controlled-file:signature:manage'" not in sql.split("SET @unified_signature_my_signature_menu_id := 900418;", 1)[1].split("SET @unified_signature_authorization_menu_id := 900413;", 1)[0]


def test_user_authorization_menu_is_admin_only_and_not_copied_to_all_root_roles():
    sql = read_sql()

    assert "SET @unified_signature_authorization_menu_id := 900413;" in sql
    assert "'用户授权'" in sql
    assert "`path` = 'authorizations'" in sql
    assert "`permission` = 'dcc:controlled-file:signature:manage'" in sql
    assert "`role`.`code` <> 'electronic_signature_admin'" in sql
    assert "`menu_id` = @unified_signature_authorization_menu_id" in sql
    assert "tmp_signature_regular_menu_ids" in sql
    regular_copy = sql.split("tmp_signature_regular_menu_ids", 1)[1].split("ensure_electronic_signature_admin_role", 1)[0]
    assert "@unified_signature_authorization_menu_id" not in regular_copy
    admin_restriction_block = sql.split("`role`.`code` <> 'electronic_signature_admin'", 1)[0].rsplit("UPDATE `system_role_menu` AS `role_menu`", 1)[1]
    assert "`role_menu`.`tenant_id` = 1" not in admin_restriction_block


def test_admin_receives_electronic_signature_admin_role_and_menu_scope():
    sql = read_sql()

    assert "ensure_electronic_signature_admin_role" in sql
    assert "'电子签名管理员'" in sql
    assert "'electronic_signature_admin'" in sql
    assert "`username` = 'admin'" in sql
    assert "`tenant_id` = 1" in sql
    assert "INSERT INTO `system_role`" in sql
    assert "INSERT INTO `system_user_role`" in sql
    assert "INSERT INTO `system_role_menu`" in sql
    assert "dcc:controlled-file:signature:manage" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql


def test_release_preflight_menu_id_lists_use_static_integer_literals():
    sql = read_sql()

    regular_values = sql.split("INSERT INTO `tmp_signature_regular_menu_ids` (`menu_id`)", 1)[1].split("INSERT INTO `system_role_menu`", 1)[0]
    assert "(900218)" in regular_values
    assert "(900411)" in regular_values
    assert "(900418)" in regular_values
    assert "@unified_signature" not in regular_values

    admin_scope = sql.split("CREATE TEMPORARY TABLE `tmp_signature_admin_menu_ids` AS", 1)[1].split("IF (SELECT COUNT(*) FROM `tmp_signature_admin_menu_ids`)", 1)[0]
    assert "`id` IN (900218, 900411, 900418, 900413)" in admin_scope
    assert "@unified_signature" not in admin_scope


def test_admin_menu_sql_is_idempotent_and_non_destructive():
    sql = read_sql()

    assert "WHERE NOT EXISTS (" in sql
    assert "UPDATE `system_role_menu` AS `role_menu`" in sql
    assert "UPDATE `system_user_role` AS `user_role`" in sql
    assert "DELETE FROM `system_role_menu`" not in sql
    assert "DELETE FROM `system_user_role`" not in sql
    assert "DROP TABLE" not in sql
    assert "mock" not in sql.lower()
    assert "fallback" not in sql.lower()
