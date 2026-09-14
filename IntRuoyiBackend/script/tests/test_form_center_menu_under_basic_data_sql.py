import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260721_form_center_menu_under_basic_data.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing form center basic-data menu migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_center_basic_data_menu_migration_has_release_guards() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260717_bpm_form_center; type=menu; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing global basic data menu 990200 for form center" in text
    assert "Missing form center parent menu 605071200" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text

    upper_sql = text.upper()
    assert "DROP TABLE" not in upper_sql
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql


def test_form_center_parent_moves_to_global_basic_data_menu() -> None:
    text = read_sql()

    assert "`id` = 990200 OR `path` = '/mdm'" in text
    assert "ORDER BY CASE WHEN `id` = 990200 THEN 0 ELSE 1 END" in text
    assert "`id` = 605071200" in text
    assert "`path` = 'form-center'" in text
    assert re.search(
        r"UPDATE\s+`system_menu`\s+SET[\s\S]*?`parent_id`\s*=\s*@form_center_basic_data_menu_id"
        r"[\s\S]*?`sort`\s*=\s*30[\s\S]*?WHERE\s+`id`\s*=\s*605071200",
        text,
    )


def test_form_center_basic_data_menu_keeps_package_and_role_parent_permissions() -> None:
    text = read_sql()

    for token in [
        "tmp_form_center_basic_data_menu_ids",
        "tmp_form_center_basic_data_package_ids",
        "tmp_form_center_basic_data_package_menu_ids",
        "JSON_VALID(`package`.`menu_ids`)",
        "JSON_TABLE(",
        "JSON_ARRAYAGG",
        "INSERT INTO `system_role_menu`",
        "NOT EXISTS (",
        "@form_center_basic_data_menu_id",
    ]:
        assert token in text

    assert re.search(r"605071200, 605071201[\s\S]*605071221", text)
    assert "605071209" not in text
    assert "<> 21" in text
    assert "JOIN `tmp_form_center_basic_data_menu_ids` AS `form_menu`" in text
    assert "CAST(`existing_menu`.`menu_id` AS UNSIGNED)" in text
