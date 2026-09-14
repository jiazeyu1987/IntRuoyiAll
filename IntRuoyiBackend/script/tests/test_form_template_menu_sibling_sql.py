import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260813_form_template_menu_before_form_center.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing form-template sibling menu migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_template_sibling_menu_migration_has_release_guards() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260722_form_center_business_action_page_retire,"
        "20260722_form_center_policy_menu_hide; type=menu; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "move_form_template_before_form_center" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing global basic data menu 990200 for form template move" in text
    assert "Missing form center parent menu 605071200 under global basic data" in text
    assert "Missing form template menu 605071201 under form center" in text
    assert "Missing form template permission rows" in text
    assert "Conflicting basic data form-center/template menu path" in text

    upper_sql = text.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert "fallback" not in text.lower()


def test_form_template_becomes_basic_data_sibling_before_form_center() -> None:
    text = read_sql()

    assert "`id` = 990200 OR `path` = '/mdm'" in text
    assert "`id` = 605071200" in text
    assert "`id` = 605071201" in text
    assert "`name` = '表单模板'" in text
    assert "`permission` = 'form:template:query'" in text
    assert "`component` = 'form-center/template/index'" in text
    assert "`component_name` = 'FormCenterTemplate'" in text
    assert re.search(
        r"UPDATE\s+`system_menu`\s+SET[\s\S]*?`parent_id`\s*=\s*@form_template_basic_data_menu_id"
        r"[\s\S]*?`sort`\s*=\s*29[\s\S]*?`path`\s*=\s*'form-center/template'"
        r"[\s\S]*?WHERE\s+`id`\s*=\s*605071201",
        text,
    )
    assert re.search(
        r"UPDATE\s+`system_menu`\s+SET[\s\S]*?`sort`\s*=\s*30"
        r"[\s\S]*?WHERE\s+`id`\s*=\s*605071200",
        text,
    )


def test_form_template_move_preserves_permissions_and_bindings() -> None:
    text = read_sql()

    for permission_id in range(605071202, 605071209):
        assert str(permission_id) in text

    assert "system_role_menu" not in text
    assert "system_tenant_package" not in text
    assert "`deleted` = b'1'" not in text
    assert "`visible` = b'0'" not in text
    assert "`status` = 1" not in text


def test_form_template_move_verifies_final_menu_contract() -> None:
    text = read_sql()

    assert "Form template menu move did not reach the required final state" in text
    assert "Form center menu order changed unexpectedly" in text
    assert "Form template permission rows changed unexpectedly" in text
    assert "`parent_id` = @form_template_basic_data_menu_id" in text
    assert "`path` = 'form-center/template'" in text
    assert "`sort` = 29" in text
