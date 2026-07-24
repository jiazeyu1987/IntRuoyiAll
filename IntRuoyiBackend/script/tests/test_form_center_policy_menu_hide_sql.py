import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260722_form_center_policy_menu_hide.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing form-center policy menu hide migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_policy_menu_hide_migration_declares_release_metadata_and_guards() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260721_form_center_menu_under_basic_data; type=menu; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "hide_form_center_policy_menu_tab" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing form center parent menu 605071200" in text
    assert "Missing form center policy menu 605071214" in text
    assert "Missing form center policy permission rows" in text

    upper_sql = text.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql


def test_policy_menu_hide_migration_hides_only_visible_policy_route() -> None:
    text = read_sql()

    assert "`id` = 605071214" in text
    assert "`name` = '表单策略'" in text
    assert "`permission` = 'form:policy:query'" in text
    assert "`path` = 'policy'" in text
    assert "`component` = 'form-center/policy/index'" in text
    assert "`component_name` = 'FormCenterPolicy'" in text
    assert re.search(
        r"UPDATE\s+`system_menu`\s+SET[\s\S]*?`visible`\s*=\s*b'0'"
        r"[\s\S]*?`always_show`\s*=\s*b'0'[\s\S]*?WHERE\s+`id`\s*=\s*605071214",
        text,
    )
    assert "`deleted` = b'1'" not in text


def test_policy_menu_hide_migration_preserves_runtime_tables_and_permissions() -> None:
    text = read_sql()

    for permission_id in (605071215, 605071216, 605071217):
        assert str(permission_id) in text

    assert "bpm_form_action_policy" not in text
    assert "bpm_form_action_instance" not in text
    assert "form:policy:create" not in text
    assert "form:policy:publish" not in text
    assert "system_role_menu" not in text
    assert "system_tenant_package" not in text
