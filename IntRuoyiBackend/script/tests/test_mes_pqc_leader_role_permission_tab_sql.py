from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260806_mes_pqc_leader_role_permission_tab.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing PQC leader role permission migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_declares_metadata_permission_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260804_mes_edhr_qa_menu,20260707_system_role_category_management; "
        "type=menu; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "START TRANSACTION;" in text
    assert "ensure_mes_pqc_leader_role_permission_tab_20260806" in text
    assert "mes:pro-process-pool-pqc-leader:query" in text
    assert "SIGNAL SQLSTATE '45000'" in text

    for required_guard in [
        "Invalid system_tenant_package.menu_ids JSON; cannot assign PQC leader role",
        "Missing enabled PQC leader menu 900435",
        "PQC leader menu 900435 route/component mismatch",
        "Missing hidden team leader query menu 900439",
        "Missing tenant 1 admin user",
        "Missing menu role category for PQC leader role tenant",
        "Duplicate PQC leader role code in target tenant",
    ]:
        assert required_guard in text


def test_migration_creates_recovers_and_grants_pqc_leader_role() -> None:
    text = read_sql()

    assert "INSERT INTO `system_role`" in text
    assert "'PQC组长权限角色'" in text
    assert "'pqc_leader_permission'" in text
    assert "SELECT 1 AS `tenant_id`" in text
    assert "UPDATE `system_role` AS `role`" in text
    assert "`role`.`code` = 'pqc_leader_permission'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "`user`.`username` = 'admin'" in text

    for menu_id in [900435, 900439, 900312, 900313, 900314]:
        assert f"SELECT {menu_id} AS `menu_id`" in text


def test_migration_creates_hidden_query_permission_without_changing_api_permissions() -> None:
    text = read_sql()

    assert "SELECT 900439" in text
    assert "'PQC组长通用查询'" in text
    assert "'mes:pro-process-pool-team-leader:query'" in text
    assert "`type` = 3" in text
    assert "`parent_id` = 900435" in text
    assert "MesProcessPoolTeamLeaderController" not in text


def test_migration_soft_restricts_pqc_tab_to_pqc_role_without_destructive_sql() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "SET `role_menu`.`deleted` = b'1'" in text
    assert "`role`.`code` <> 'pqc_leader_permission'" in text
    assert "`role_menu`.`menu_id` = 900435" in text
    assert "NOT IN ('pqc_leader_permission', 'tenant_admin', 'super_admin')" not in text

    for forbidden in [
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM SYSTEM_ROLE_MENU",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "DELETE FROM SYSTEM_USER_ROLE",
        "TRUNCATE TABLE",
        "MAX(`EXISTING_ROLE`.`ID`)",
        "MAX(ID)",
        "LAST_INSERT_ID()",
    ]:
        assert forbidden not in upper_text


def test_migration_verifies_final_role_menu_and_admin_assignment() -> None:
    text = read_sql()

    assert "PQC leader role menu permission grant incomplete" in text
    assert "PQC leader role menu permission grant has duplicate active bindings" in text
    assert "MIN(`role_menu`.`id`) AS `keep_id`" in text
    assert "Admin user is not assigned PQC leader role" in text
    assert "Active PQC leader tab menu is still granted to a non-PQC leader role" in text
    assert "COMMIT;" in text
