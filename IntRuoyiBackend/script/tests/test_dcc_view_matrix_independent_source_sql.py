from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260623_dcc_view_matrix_independent_source.sql"


def read_sql() -> str:
    return MIGRATION.read_text(encoding="utf-8")


def test_view_matrix_rule_migration_declares_release_contract():
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium\n"
    )


def test_view_matrix_rule_table_migration_is_idempotent_and_non_destructive():
    sql = read_sql()

    assert "CREATE TABLE IF NOT EXISTS `dcc_category_view_matrix_rule`" in sql
    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE)\b", sql, re.I)
    assert not re.search(r"\bDELETE\s+FROM\s+`?dcc_", sql, re.I)


def test_view_matrix_rule_table_contains_runtime_contract_columns():
    sql = read_sql()

    required_columns = [
        "category_id",
        "excel_file_name",
        "excel_row_no",
        "excel_column_letter",
        "subject_label",
        "subject_top_header",
        "subject_sub_header",
        "marker",
        "scope_type",
        "subject_type",
        "subject_id",
        "active",
        "tenant_id",
        "deleted",
    ]
    for column in required_columns:
        assert re.search(rf"`{column}`\s+", sql, re.I), f"missing {column}"

    assert "PRIMARY KEY (`id`)" in sql
    assert "KEY `idx_dcc_category_view_matrix_rule_category` (`category_id`)" in sql
    assert "KEY `idx_dcc_category_view_matrix_rule_subject` (`subject_type`, `subject_id`)" in sql
