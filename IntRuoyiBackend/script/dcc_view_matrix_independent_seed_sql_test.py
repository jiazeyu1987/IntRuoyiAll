from __future__ import annotations

import ast
import re
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SQL_PATH = ROOT / "sql" / "mysql" / "20260624_dcc_view_matrix_independent_seed.sql"


def parse_values_block(sql: str, table_name: str) -> list[tuple]:
    pattern = (
        r"INSERT\s+INTO\s+`?"
        + re.escape(table_name)
        + r"`?\s*\([^)]*\)\s*VALUES\s*(?P<values>.*?);"
    )
    match = re.search(pattern, sql, re.IGNORECASE | re.DOTALL)
    if not match:
        raise AssertionError(f"missing values block for {table_name}")
    rows = [ast.literal_eval("(" + re.sub(r"\bNULL\b", "None", row_text) + ")")
            for row_text in re.findall(r"\((.*?)\)(?:,\s*|$)", match.group("values"), re.DOTALL)]
    if not rows:
        raise AssertionError(f"empty values block for {table_name}")
    return rows


def main() -> None:
    if not SQL_PATH.exists():
        raise AssertionError(f"missing SQL seed: {SQL_PATH}")
    sql = SQL_PATH.read_text(encoding="utf-8")
    assert "dcc_category_view_matrix_rule" in sql
    categories = parse_values_block(sql, "tmp_dcc_view_matrix_seed_category")
    grants = parse_values_block(sql, "tmp_dcc_view_matrix_seed_grant")
    subjects = parse_values_block(sql, "tmp_dcc_view_matrix_seed_subject")

    assert len(categories) == 59
    assert len(grants) == 243
    assert Counter(row[7] for row in grants) == Counter({"●": 195, "▲": 48})
    assert Counter(row[5] for row in subjects)["ROLE"] >= 5
    assert sum(1 for row in grants if row[8] == "MANAGER_AND_ABOVE") == 48
    assert "VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED" in sql
    assert "VIEW_MATRIX_SEED_CATEGORY_AMBIGUOUS" in sql
    assert "VIEW_MATRIX_SEED_TENANT1_AUTHORIZATION_REQUIRED" in sql
    assert "@dcc_view_matrix_seed_allow_yudao_tenant" in sql
    assert sql.index("VIEW_MATRIX_SEED_TENANT1_AUTHORIZATION_REQUIRED") < sql.index("START TRANSACTION")
    assert "leader_user_id" in sql
    assert "system_user_role" in sql
    assert "CONCAT(subject.subject_lookup_name, '?', subject.remark)" not in sql
    assert "CONCAT(subject.subject_lookup_name, ' | ', subject.remark)" in sql
    assert "CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_resolved_subject AS" not in sql
    assert "CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_role_user AS" not in sql
    assert "rule_table.creator = @dcc_view_matrix_seed_actor" not in sql
    assert "parent_choice" not in sql
    assert "child_choice" not in sql
    assert "MAX(id) AS id" not in sql

    print("dcc view matrix independent seed SQL contract PASS")


if __name__ == "__main__":
    main()
