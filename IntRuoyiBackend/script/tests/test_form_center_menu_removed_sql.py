import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260813_z_form_center_menu_hide.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing form-center menu hide migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_center_menu_hide_declares_release_metadata_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260813_form_template_menu_before_form_center; type=menu; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "hide_form_center_sidebar_entry" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing form center parent menu 605071200" in text
    assert "Missing form center effect menu 605071220" in text
    assert "Missing form center effect retry permission 605071221" in text
    assert "Missing active form template sibling menu 605071201" in text

    upper_sql = text.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql


def test_form_center_parent_and_effect_entries_are_hidden() -> None:
    text = read_sql()

    for menu_id in (605071200, 605071220):
        assert re.search(
            rf"UPDATE\s+`system_menu`\s+SET[\s\S]*?`visible`\s*=\s*b'0'"
            rf"[\s\S]*?`always_show`\s*=\s*b'0'[\s\S]*?WHERE\s+`id`\s*=\s*{menu_id}",
            text,
        )

    assert "`name` = '生效待处理'" in text
    assert "`permission` = 'form:effect:query'" in text
    assert "`component` = 'form-center/effect/index'" in text
    assert "`component_name` = 'FormCenterEffect'" in text
    assert "`deleted` = b'1'" not in text
    assert "`status` = 1" not in text


def test_form_center_menu_hide_preserves_template_and_runtime_permissions() -> None:
    text = read_sql()

    assert "`id` = 605071201" in text
    assert "`parent_id` = 990200" in text
    assert "`path` = 'form-center/template'" in text
    assert "`permission` = 'form:template:query'" in text
    assert "`component` = 'form-center/template/index'" in text
    assert "`component_name` = 'FormCenterTemplate'" in text
    assert "`id` = 605071221" in text
    assert "`permission` = 'form:effect:retry'" in text
    assert "system_role_menu" not in text
    assert "system_tenant_package" not in text


def test_form_center_menu_hide_verifies_final_visibility() -> None:
    text = read_sql()

    assert "Form center parent menu is still visible" in text
    assert "Form center effect menu is still visible" in text
    assert "Form template sibling menu was changed" in text
    assert "Form center effect retry permission was changed" in text
