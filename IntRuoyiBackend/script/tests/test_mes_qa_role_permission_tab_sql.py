from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260806_mes_qa_role_permission_tab.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing QA role permission migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_declares_metadata_permission_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260804_mes_edhr_qa_menu; type=menu; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "START TRANSACTION;" in text
    assert "ensure_mes_qa_role_permission_tab_20260806" in text
    assert "mes:qa-inspection-regulation:query" in text
    assert "SIGNAL SQLSTATE '45000'" in text

    for required_guard in [
        "Missing enabled QA regulation menu 900434",
        "QA menu 900434 route/component mismatch",
        "Missing tenant 1 admin user",
        "Missing menu role category for QA role tenant",
        "Duplicate QA role code in target tenant",
    ]:
        assert required_guard in text


def test_migration_creates_recovers_and_grants_qa_role() -> None:
    text = read_sql()

    assert "INSERT INTO `system_role`" in text
    assert "'QA'" in text
    assert "'qa'" in text
    assert "SELECT 1 AS `tenant_id`" in text
    assert "UPDATE `system_role` AS `role`" in text
    assert "`role`.`code` = 'qa'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "`user`.`username` = 'admin'" in text

    for menu_id in [900434, 5631, 5633]:
        assert f"SELECT {menu_id} AS `menu_id`" in text


def test_migration_soft_restricts_qa_tab_to_qa_role_without_destructive_sql() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "SET `role_menu`.`deleted` = b'1'" in text
    assert "`role`.`code` <> 'qa'" in text
    assert "`role_menu`.`menu_id` = 900434" in text
    assert "NOT IN ('qa', 'tenant_admin', 'super_admin')" not in text

    for forbidden in [
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM SYSTEM_ROLE_MENU",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "DELETE FROM SYSTEM_USER_ROLE",
        "TRUNCATE TABLE",
    ]:
        assert forbidden not in upper_text


def test_migration_is_idempotent_and_does_not_use_dynamic_max_plus_one_ids() -> None:
    text = read_sql()

    assert "WHERE NOT EXISTS (" in text
    assert "MAX(`existing_role`.`id`)" not in text
    assert "MAX(id)" not in text
    assert "LAST_INSERT_ID()" not in text
    assert "COMMIT;" in text
