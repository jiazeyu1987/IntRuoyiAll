from pathlib import Path
import re


BACKEND_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = BACKEND_ROOT / "sql" / "mysql" / "20260814_mes_production_release_roles.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing MIG-RF-0 production release role migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_declares_release_metadata_and_transactional_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260611_mes_edhr_work_task_flow,20260618_mes_edhr_release_precheck_engine,"
        "20260707_system_role_category_management; type=permission; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "START TRANSACTION;" in text
    assert "COMMIT;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Ambiguous production release target tenant" in text
    assert "Missing production release role permission menu" in text
    assert "Duplicate production release role code in target tenant" in text


def test_migration_resolves_target_tenant_from_both_initial_users_without_fixed_tenant_id() -> None:
    text = read_sql()

    assert "`user`.`username` IN ('zhulijiang', 'xujianhai')" in text
    assert "COUNT(DISTINCT `user`.`username`) = 2" in text
    assert "Duplicate or disabled initial production release user" in text
    assert "`user`.`tenant_id` = 1" not in text
    assert "SELECT 1 AS `tenant_id`" not in text


def test_migration_creates_two_exact_roles_and_initial_bindings_idempotently() -> None:
    text = read_sql()

    for role_code in ["MES_PQC_RELEASE_OWNER", "MES_MANAGEMENT_REPRESENTATIVE"]:
        assert role_code in text
    assert "INSERT INTO `system_role`" in text
    assert "UPDATE `system_role` AS `role`" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "UPDATE `system_user_role` AS `user_role`" in text
    assert "Production release initial user role binding incomplete" in text

    role_insert = re.search(r"INSERT INTO `system_role`\s*\((.*?)\)\s*SELECT", text, re.S)
    assert role_insert is not None
    assert "`id`" not in role_insert.group(1)


def test_migration_creates_new_pqc_buttons_below_work_task_page_without_fixed_menu_ids() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-production-release:query",
        "mes:pro-production-release:pqc-approve",
        "mes:pro-production-release:pqc-reject",
    ]:
        assert permission in text
    assert "`parent`.`permission` = 'mes:pro-edhr-work-task:query'" in text
    assert "`parent`.`type` = 2" in text

    menu_insert = re.search(r"INSERT INTO `system_menu`\s*\((.*?)\)\s*SELECT", text, re.S)
    assert menu_insert is not None
    assert "`id`" not in menu_insert.group(1)


def test_migration_grants_only_frozen_permission_sets_and_no_broad_batch_approval() -> None:
    text = read_sql()

    required_permissions = {
        "mes:pro-edhr-work-task:query",
        "mes:pro-production-release:query",
        "mes:pro-production-release:pqc-approve",
        "mes:pro-production-release:pqc-reject",
        "mes:pro-edhr-release:query",
        "mes:pro-edhr-release:approve",
    }
    for permission in required_permissions:
        assert permission in text

    assert "mes:pro-batch-record-execution:approve" not in text
    assert "tmp_mes_production_release_required_permission" in text
    assert "tmp_mes_production_release_required_role_menu" in text
    assert "Production release role permission set mismatch" in text


def test_migration_temp_tables_pin_text_collation_for_test_runtime() -> None:
    text = read_sql()

    text_columns = [
        "`permission` varchar(128)",
        "`name` varchar(50)",
        "`role_code` varchar(100)",
        "`role_name` varchar(30)",
        "`initial_username` varchar(30)",
    ]
    for column in text_columns:
        assert (
            f"{column} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" in text
        ), f"{column} must not inherit the test database default collation"

    assert "`tmp_mes_production_release_pqc_button`" in text
    assert "`tmp_mes_production_release_desired_role`" in text
    assert "`tmp_mes_production_release_required_permission`" in text


def test_migration_is_non_destructive_and_has_no_fixed_identity_allocation() -> None:
    upper = read_sql().upper()

    for forbidden in [
        "DELETE FROM `SYSTEM_ROLE`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "TRUNCATE TABLE",
        "MAX(`EXISTING_ROLE`.`ID`)",
        "LAST_INSERT_ID()",
    ]:
        assert forbidden not in upper
