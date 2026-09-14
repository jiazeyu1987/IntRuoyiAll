import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260728_user_list_access_role.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing user list access role migration"
    return SQL_PATH.read_text(encoding="utf-8")


def compact(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).strip()


def test_migration_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260707_system_role_category_management; type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "START TRANSACTION;" in sql
    assert "ensure_user_list_access_role_20260728" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Missing tenant 1 menu role category" in sql
    assert "Missing enabled system:user:query menu permission" in sql
    assert "Duplicate active tenant 1 user_list_access roles" in sql
    assert "COMMIT;" in sql


def test_role_is_created_or_repaired_with_stable_code_and_menu_category() -> None:
    sql = read_sql()

    assert "_utf8mb4'用户列表访问' COLLATE utf8mb4_unicode_ci" in sql
    assert "_utf8mb4'user_list_access' COLLATE utf8mb4_unicode_ci" in sql
    assert "`system_role_category`" in sql
    assert "`code` = _utf8mb4'menu' COLLATE utf8mb4_unicode_ci" in sql
    assert "`tenant_id` = 1" in sql
    assert "`deleted` = b'0'" in sql
    assert "LAST_INSERT_ID()" in sql

    insert_role_match = re.search(r"INSERT\s+INTO\s+`system_role`\s*\((?P<columns>[^)]*)\)", sql, re.I)
    assert insert_role_match is not None
    assert "`id`" not in insert_role_match.group("columns")


def test_role_menu_binding_resolves_permission_by_stable_menu_permission() -> None:
    sql = read_sql()
    flat = compact(sql)

    assert "system:user:query" in sql
    assert "CREATE TEMPORARY TABLE `tmp_user_list_access_permission`" in sql
    assert (
        "`permission` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY"
        in sql
    )
    assert "JOIN `tmp_user_list_access_permission` AS `expected_permission`" in sql
    assert "`menu`.`permission` = `expected_permission`.`permission`" in sql
    assert "`menu`.`status` = 0" in sql
    assert "`menu`.`deleted` = b'0'" in sql
    assert "UPDATE `system_role_menu`" in sql
    assert "INSERT INTO `system_role_menu`" in sql
    assert "WHERE `existing`.`role_id` = v_role_id" in sql
    assert "AND `existing`.`menu_id` = v_user_query_menu_id" in sql
    assert "1001" not in sql
    assert "menu_id = 1001" not in flat.lower()


def test_migration_is_non_destructive_and_does_not_expand_user_permissions() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    for forbidden in [
        "DROP TABLE `SYSTEM_ROLE`",
        "DROP TABLE `SYSTEM_MENU`",
        "DROP TABLE `SYSTEM_ROLE_MENU`",
        "TRUNCATE TABLE `SYSTEM_ROLE`",
        "TRUNCATE TABLE `SYSTEM_MENU`",
        "TRUNCATE TABLE `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_ROLE`",
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
    ]:
        assert forbidden not in upper_sql

    assert "INSERT INTO `system_menu`" not in sql
    assert "system:user:list" not in sql
    assert "system:user:create" not in sql
    assert "system:user:update" not in sql
    assert "system:user:delete" not in sql
    assert "system:user:export" not in sql

    insert_menu_match = re.search(r"INSERT\s+INTO\s+`system_role_menu`\s*\((?P<columns>[^)]*)\)", sql, re.I)
    assert insert_menu_match is not None
    assert "`id`" not in insert_menu_match.group("columns")
