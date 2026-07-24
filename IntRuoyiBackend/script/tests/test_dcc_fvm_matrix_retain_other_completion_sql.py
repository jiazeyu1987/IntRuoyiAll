from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260625_dcc_fvm_matrix_retain_other_completion.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_completion_sql_retains_other_and_does_not_touch_historical_files() -> None:
    sql = read_sql()
    upper = sql.upper()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=backup,prod; "
        "dependsOn=20260624_dcc_view_matrix_independent_seed; type=data; riskLevel=medium\n"
    )
    assert "DCC_OTHER_TEMPLATE_900250" in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_OTHER_MISSING" in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_OTHER_NOT_RETAINED" in sql
    assert "DELETE FROM `DCC_FILE_CATEGORY`" not in upper
    assert "UPDATE `DCC_FILE_CATEGORY`" not in upper
    assert "DELETE FROM `DCC_CONTROLLED_FILE`" not in upper
    assert "UPDATE `DCC_CONTROLLED_FILE`" not in upper
    assert "DELETE FROM `DCC_CONTROLLED_FILE_MASTER`" not in upper
    assert "UPDATE `DCC_CONTROLLED_FILE_MASTER`" not in upper


def test_completion_sql_backfills_dhf001_view_matrix_only() -> None:
    sql = read_sql()

    assert "DCC_FVM_DHF_001" in sql
    assert "DCC_FVM_DHF_002" in sql
    assert "tmp_dcc_fvm_completion_view_source" in sql
    assert "subject_label = 'QMS'" in sql
    assert "subject_label = CONVERT(UNHEX('E696B0E59381E5BC80E58F91E983A8') USING utf8mb4)" in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_VIEW_NOT_59" in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_VIEW_RULES_NOT_243" in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_DHF001_MISSING" in sql


def test_completion_sql_copies_other_active_route_to_all_dcc_fvm_categories() -> None:
    sql = read_sql()

    assert "tmp_dcc_fvm_completion_template_node" in sql
    assert "tmp_dcc_fvm_completion_route_target" in sql
    assert "INSERT INTO `dcc_category_approval_route`" in sql
    assert "INSERT INTO `dcc_category_approval_route_node`" in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_TEMPLATE_ROUTE_MISSING" in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_REVIEW_NOT_59" in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_REVIEW_NODES_INCOMPLETE" in sql
    assert "category_record.code LIKE 'DCC_FVM%'" in sql


def test_completion_sql_is_transactional_idempotent_and_utf8_safe() -> None:
    sql = read_sql()
    upper = sql.upper()

    assert "START TRANSACTION" in upper
    assert "ROLLBACK" in upper
    assert "COMMIT" in upper
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "NOT EXISTS" in upper
    assert "ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci" in sql
    assert "dcc_fvm_matrix_retain_other_completion_20260625" in sql
    assert "??" not in sql


def test_completion_sql_is_not_allowed_on_test_environment() -> None:
    sql = read_sql()

    assert "-- Scope: local tenant_id=1;" in sql
    assert "allowedEnvironments=backup,prod" in sql
    assert "allowedEnvironments=test" not in sql


def test_completion_sql_does_not_pin_total_active_category_count() -> None:
    sql = read_sql()

    assert "v_dcc_fvm_count <> 59" in sql
    assert "v_active_total <> 60" not in sql
    assert "DCC_FVM_RETAIN_OTHER_COMPLETION_CATEGORY_BASELINE_CHANGED" in sql
