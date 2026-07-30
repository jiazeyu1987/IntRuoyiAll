from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260730_signature_regular_users_my_signature_only.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"{SQL_PATH} must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_metadata_and_fail_fast_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260714_signature_my_signature_admin_menu,20260721_admin_full_scope_role_standardization; "
        "type=menu; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "START TRANSACTION;" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Missing electronic signature root menu 900218" in sql
    assert "Missing electronic signature my signature menu 900418" in sql


def test_regular_roles_keep_only_root_and_my_signature() -> None:
    sql = read_sql()

    assert "tmp_signature_regular_allowed_menu" in sql
    assert "tmp_signature_admin_role_code" in sql
    assert "900218" in sql
    assert "900418" in sql
    assert "900411" in sql
    assert "role.code NOT IN ('electronic_signature_admin', 'audit_admin', 'super_admin')" in sql
    assert "role_menu.deleted = b'1'" in sql
    assert "role_menu.deleted = b'0'" in sql
    assert "role_menu.menu_id NOT IN (SELECT menu_id FROM tmp_signature_regular_allowed_menu)" in sql


def test_regular_role_scope_is_soft_updated_without_destructive_delete() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    for forbidden in [
        "DELETE FROM SYSTEM_ROLE_MENU",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "TRUNCATE TABLE SYSTEM_ROLE_MENU",
        "DROP TABLE SYSTEM_ROLE_MENU",
        "MOCK",
        "FALLBACK",
    ]:
        assert forbidden not in upper_sql

    assert "COMMIT;" in sql
