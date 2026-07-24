from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260722_form_center_business_action_page_retire.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing form center page retirement migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_center_business_action_page_retirement_has_release_guards() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260721_form_center_menu_under_basic_data; type=menu; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "form_center_business_action_page_retire" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Form center parent menu 605071200 is required" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text

    upper_sql = text.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert "fallback" not in text.lower()


def test_form_center_business_action_page_menu_is_retired_without_removing_runtime_permissions() -> None:
    text = read_sql()

    assert "`id` = 605071209" in text
    assert "`menu_id` = 605071209" in text
    assert "`parent_id` = 605071209" in text
    assert "`parent_id` = 605071200" in text
    assert "`deleted` = b'1'" in text
    assert "`visible` = b'0'" in text
    assert "605071210" in text
    assert "605071211" in text
    assert "605071212" in text
    assert "605071213" in text
    assert "605071218" in text
    assert "605071219" in text

    assert "'business-action'" not in text
    assert "'form-center/business-action/index'" not in text
    assert "'FormCenterBusinessAction'" not in text
    assert "'业务动作表单'" not in text


def test_form_center_business_action_page_package_bindings_are_removed() -> None:
    text = read_sql()

    assert "system_tenant_package" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "JSON_TABLE(" in text
    assert "JSON_ARRAYAGG" in text
    assert "JSON_CONTAINS(`package`.`menu_ids`, CAST('605071209' AS JSON), '$')" in text
    assert "tmp_form_center_business_action_page_packages" in text
    assert "tmp_form_center_business_action_page_package_menu_ids" in text
    assert "CAST(`existing_menu`.`menu_id` AS UNSIGNED) <> 605071209" in text
