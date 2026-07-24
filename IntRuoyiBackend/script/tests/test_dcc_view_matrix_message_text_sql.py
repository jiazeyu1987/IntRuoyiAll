from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260613_dcc_file_view_matrix_seed.sql"


def read_sql() -> str:
    return MIGRATION.read_text(encoding="utf-8")


def test_dynamic_signal_message_text_is_bounded_for_mysql_condition_item():
    sql = read_sql()

    expected_pairs = [
        ("@dcc_fvm_missing_dept_signal_message", "@dcc_fvm_missing_dept"),
        ("@dcc_fvm_missing_role_signal_message", "@dcc_fvm_missing_role"),
        ("@dcc_fvm_ambiguous_category_signal_message", "@dcc_fvm_ambiguous_category"),
        ("@dcc_fvm_missing_category_signal_message", "@dcc_fvm_missing_category"),
    ]
    for signal_message, source_message in expected_pairs:
        assert f"SET {signal_message} = LEFT({source_message}, 128);" in sql
        assert f"SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = {signal_message};" in sql


def test_dynamic_signal_message_text_no_long_variables_unbounded():
    sql = read_sql()

    forbidden = [
        "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @dcc_fvm_missing_dept;",
        "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @dcc_fvm_missing_role;",
        "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @dcc_fvm_ambiguous_category;",
        "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @dcc_fvm_missing_category;",
        "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = LEFT(@dcc_fvm_missing_dept, 128);",
        "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = LEFT(@dcc_fvm_missing_role, 128);",
        "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = LEFT(@dcc_fvm_ambiguous_category, 128);",
        "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = LEFT(@dcc_fvm_missing_category, 128);",
    ]
    for snippet in forbidden:
        assert snippet not in sql


def test_category_resolution_prefers_code_match_when_legacy_name_also_exists():
    sql = read_sql()

    assert "tmp_dcc_file_view_matrix_category_code_match" in sql
    assert "tmp_dcc_file_view_matrix_category_name_match" in sql
    assert "code_match.code_count > 1" in sql
    assert "COALESCE(code_match.code_count, 0) = 0 AND name_match.name_count > 1" in sql
    assert "LEFT JOIN dcc_file_category code_category" in sql
    assert "code_category.code = matrix_category.category_code" in sql
    assert "COALESCE(code_match.code_count, 0) = 0 AND category.name = matrix_category.file_name" in sql


def test_category_insert_reuses_unique_legacy_name_when_code_is_absent():
    sql = read_sql()

    assert "existing.code = matrix_category.category_code OR existing.name = matrix_category.file_name" in sql
