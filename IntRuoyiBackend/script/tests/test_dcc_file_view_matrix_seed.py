from __future__ import annotations

import ast
import re
from collections import Counter, defaultdict
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260613_dcc_file_view_matrix_seed.sql"


EXPECTED_CATEGORY_COUNT = 59
EXPECTED_DEDUPED_GRANT_COUNT = 231
EXPECTED_GROUP_COUNTS = {"DHF": 35, "DMR": 24}
EXPECTED_DEPARTMENT_TOTALS = {
    "新品开发部": 54,
    "QA": 28,
    "QC": 8,
    "QMS": 59,
    "注册": 20,
    "设备开发": 8,
    "生产": 16,
    "生产计划": 7,
    "生产采购": 9,
    "包装设计": 14,
    "市场": 3,
    "检测中心": 5,
}
EXPECTED_PRODUCT_TECH_DEPTS = {"新品开发部", "QA", "QMS", "注册"}
EXPECTED_PRODUCT_MANUAL_DEPTS = {
    "新品开发部",
    "QA",
    "QC",
    "QMS",
    "注册",
    "生产",
    "生产计划",
    "生产采购",
    "包装设计",
    "市场",
    "检测中心",
}


def test_dcc_file_view_matrix_seed_exists_and_is_non_destructive() -> None:
    sql = read_seed_sql()

    assert "DROP TABLE `dcc_" not in sql
    assert "TRUNCATE TABLE" not in sql.upper()
    assert re.search(r"\bDELETE\s+FROM\s+`?dcc_", sql, re.IGNORECASE) is None
    assert "DROP TEMPORARY TABLE IF EXISTS" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "DCC_FILE_VIEW_MATRIX_UNCLASSIFIED_AUDIT" in sql


def test_dcc_file_view_matrix_categories_match_excel_shape() -> None:
    categories = parse_values_block(read_seed_sql(), "tmp_dcc_file_view_matrix_category")

    assert len(categories) == EXPECTED_CATEGORY_COUNT
    assert Counter(row[0] for row in categories) == EXPECTED_GROUP_COUNTS
    assert len({row[3] for row in categories}) == EXPECTED_CATEGORY_COUNT
    assert ("DHF", 1, "R&D-项目代码-xxx", "市场调研报告", "DCC_FVM_DHF_001") in categories
    assert ("DMR", 1, "DMR-项目代码-XXX", "产品技术要求", "DCC_FVM_DMR_001") in categories
    assert ("DMR", 24, "RE-SOP-M-模具编号", "生产/检验用工装模具维护保养记录表", "DCC_FVM_DMR_024") in categories


def test_dcc_file_view_matrix_grants_are_deduped_and_do_not_open_download() -> None:
    sql = read_seed_sql()
    categories = parse_values_block(sql, "tmp_dcc_file_view_matrix_category")
    grants = parse_values_block(sql, "tmp_dcc_file_view_matrix_grant")
    category_by_code = {row[4]: row for row in categories}

    assert len(grants) == EXPECTED_DEDUPED_GRANT_COUNT
    assert len({(row[0], row[1]) for row in grants}) == EXPECTED_DEDUPED_GRANT_COUNT
    assert "DOWNLOAD" not in {row[3] for row in grants}
    assert re.search(
        r"`can_download`,\s*`active`,\s*`change_reason`.*?SELECT.*?\n\s*0,\n\s*1,\n\s*'文件查阅矩阵",
        sql,
        re.IGNORECASE | re.DOTALL,
    )
    assert "access_rule.can_download = 0" in sql
    assert "can_download = 1" not in sql.lower()
    assert re.search(r"`action_type`[^;]+DOWNLOAD", sql, re.IGNORECASE | re.DOTALL) is None

    department_totals = Counter(row[1] for row in grants)
    assert department_totals == EXPECTED_DEPARTMENT_TOTALS

    grants_by_file_name: dict[str, set[str]] = defaultdict(set)
    for category_code, department, *_ in grants:
        grants_by_file_name[category_by_code[category_code][3]].add(department)
    assert grants_by_file_name["产品技术要求"] == EXPECTED_PRODUCT_TECH_DEPTS
    assert grants_by_file_name["产品说明书"] == EXPECTED_PRODUCT_MANUAL_DEPTS


def test_dcc_file_view_matrix_seed_disables_legacy_non_matrix_rules() -> None:
    sql = read_seed_sql()

    assert "UPDATE `dcc_file_category_permission_rule` legacy_rule" in sql
    assert "legacy_rule.active = 0" in sql
    assert "legacy_rule.deleted = 1" in sql
    assert "DCC_FILE_VIEW_MATRIX_LEGACY_PERMISSION_DISABLED" in sql
    assert "legacy_rule.action_type = 'DOWNLOAD'" in sql
    assert "legacy_rule.subject_type = 'USER'" in sql


def test_dcc_file_view_matrix_subject_mapping_is_explicit() -> None:
    grants = parse_values_block(read_seed_sql(), "tmp_dcc_file_view_matrix_grant")

    for category_code, department, marker, action_type, subject_type, subject_lookup_name, *_ in grants:
        assert category_code.startswith("DCC_FVM_")
        assert action_type == "VIEW"
        if marker == "●":
            assert subject_type == "DEPT"
            assert subject_lookup_name
        elif marker == "▲":
            assert subject_type == "ROLE"
            assert subject_lookup_name.startswith("DCC矩阵-")
        else:
            raise AssertionError(f"Unexpected matrix marker {marker!r}")

    assert any(row[1] == "注册" for row in grants)
    assert sum(1 for row in grants if row[1] == "注册" and row[0] == "DCC_FVM_DMR_001") == 1


def test_dcc_file_view_matrix_seed_preflights_subjects_and_directories() -> None:
    sql = read_seed_sql()

    required_snippets = [
        "tmp_dcc_file_view_matrix_missing_dept",
        "tmp_dcc_file_view_matrix_missing_role",
        "tmp_dcc_file_view_matrix_missing_directory",
        "DCC_FILE_VIEW_MATRIX_SUBJECT_PRECHECK_FAILED",
        "DCC_FILE_VIEW_MATRIX_DIRECTORY_PRECHECK_FAILED",
        "INSERT INTO `system_role`",
        "INSERT INTO `dcc_file_category_permission_rule`",
        "INSERT INTO `dcc_directory_access_rule`",
        "`can_query`, `can_preview`, `can_download`",
        "SELECT DISTINCT directory_subject.`subject_type`, directory_subject.`subject_id`",
    ]
    for snippet in required_snippets:
        assert snippet in sql


def test_dcc_file_view_matrix_seed_pins_collation_for_mysql8() -> None:
    sql = read_seed_sql()

    assert sql.count("ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci") >= 4
    assert "category_record.name = '其他'" not in sql
    assert (
        "category_record.name = CONVERT(UNHEX('E585B6E4BB96') USING utf8mb4) "
        "COLLATE utf8mb4_unicode_ci"
    ) in sql


def test_dcc_file_view_matrix_seed_avoids_reopening_grant_temp_table() -> None:
    sql = read_seed_sql()

    assert "CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_resolved_subject AS" not in sql
    assert "CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_resolved_subject (" in sql
    assert sql.count("INSERT INTO tmp_dcc_file_view_matrix_resolved_subject") == 2


def read_seed_sql() -> str:
    assert SQL_PATH.exists(), f"Missing DCC file-view matrix seed SQL: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def parse_values_block(sql: str, table_name: str) -> list[tuple]:
    pattern = (
        r"INSERT\s+INTO\s+`?"
        + re.escape(table_name)
        + r"`?\s*\([^)]*\)\s*VALUES\s*(?P<values>.*?);"
    )
    match = re.search(pattern, sql, re.IGNORECASE | re.DOTALL)
    assert match, f"Missing VALUES block for {table_name}"
    values_block = match.group("values")
    rows = []
    for row_text in re.findall(r"\((.*?)\)(?:,|$)", values_block, re.DOTALL):
        python_tuple = "(" + row_text + ")"
        rows.append(ast.literal_eval(python_tuple))
    return rows
