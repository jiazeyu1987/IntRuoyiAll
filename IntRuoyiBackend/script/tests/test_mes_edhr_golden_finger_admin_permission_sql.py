from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260721_mes_edhr_golden_finger_admin_permission.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR golden-finger admin permission migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260523_mes_batch_record_execution_edhr_v1,20260707_system_role_category_management; "
        "type=permission; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "START TRANSACTION;" in sql
    assert "ensure_mes_edhr_golden_finger_admin_permission_20260721" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql

    for required_guard in [
        "Duplicate eDHR golden-finger permission menu",
        "Missing eDHR execution update permission menu",
        "Missing tenant 1 batch-record role category",
        "Missing enabled tenant 1 admin user",
        "Preferred eDHR golden-finger menu id is already occupied",
        "Preferred eDHR golden-finger role id is already occupied",
    ]:
        assert required_guard in sql


def test_migration_creates_double_key_role_and_permission_binding() -> None:
    sql = read_sql()

    assert "mes:pro-batch-record-execution:golden-finger" in sql
    assert "edhr_golden_finger_admin" in sql
    assert "批记录金手指管理员" in sql
    assert "v_preferred_menu_id BIGINT DEFAULT 900399" in sql
    assert "v_preferred_role_id BIGINT DEFAULT 910399" in sql
    assert "INSERT INTO system_menu" in sql
    assert "INSERT INTO system_role" in sql
    assert "INSERT INTO system_role_menu" in sql
    assert "role_id = v_golden_finger_role_id" in sql
    assert "menu_id = v_golden_finger_menu_id" in sql


def test_system_menu_insert_uses_release_preflight_literal_id() -> None:
    sql = read_sql()

    assert "VALUES (\n      900399, '批记录金手指管理员'" in sql
    assert "VALUES (\n      v_preferred_menu_id, '批记录金手指管理员'" not in sql


def test_migration_assigns_tenant_one_admin_without_destructive_table_changes() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    assert "WHERE username = 'admin'" in sql
    assert "tenant_id = 1" in sql
    assert "INSERT INTO system_user_role" in sql
    assert "SELECT v_admin_user_id, v_golden_finger_role_id" in sql
    assert "COMMIT;" in sql

    for forbidden in [
        "DELETE FROM SYSTEM_ROLE_MENU",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM SYSTEM_USER_ROLE",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "TRUNCATE TABLE SYSTEM_ROLE_MENU",
        "DROP TABLE SYSTEM_ROLE_MENU",
        "TRUNCATE TABLE SYSTEM_USER_ROLE",
        "DROP TABLE SYSTEM_USER_ROLE",
    ]:
        assert forbidden not in upper_sql
