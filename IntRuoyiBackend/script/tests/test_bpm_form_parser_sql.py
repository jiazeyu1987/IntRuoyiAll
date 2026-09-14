from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260908_bpm_form_parser_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"missing migration: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_parser_menu_migration_has_release_metadata_and_utf8():
    sql = read_sql()
    assert "-- release-migration:" in sql
    assert "dependsOn=20260813_z_form_center_menu_hide" in sql
    assert "riskLevel=low" in sql
    assert "SET NAMES utf8mb4;" in sql


def test_form_parser_menu_creates_visible_basic_data_entry_and_action_permission():
    sql = read_sql()
    assert "605071222" in sql
    assert "605071223" in sql
    assert "`parent_id` = 990200" in sql
    assert "'表单解析'" in sql
    assert "'form-center/parser'" in sql
    assert "'form-center/parser/index'" in sql
    assert "'FormCenterParser'" in sql
    assert "'form:parser:query'" in sql
    assert "'生产批记录解析'" in sql
    assert "'form:parser:production-batch-record'" in sql
    assert "`visible` = b'1'" in sql


def test_form_parser_menu_scope_follows_existing_form_template_visibility():
    sql = read_sql()
    assert "605071201" in sql
    assert "system_tenant_package" in sql
    assert "system_role_menu" in sql
    assert "JSON_VALID" in sql
    assert "JSON_TABLE" in sql
    assert "JSON_ARRAYAGG" in sql
    assert "form_parser_reference_roles" in sql
    assert "form_parser_reference_packages" in sql


def test_form_parser_menu_migration_does_not_hide_or_remove_form_center_menus():
    sql = read_sql().upper()
    assert "DELETE " not in sql
    assert "TRUNCATE " not in sql
    assert "DROP TABLE" not in sql
    assert "`ID` = 605071200" not in sql
    assert "`ID` = 605071201" not in sql
