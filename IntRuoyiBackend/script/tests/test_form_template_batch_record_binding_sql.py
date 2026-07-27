from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260727_bpm_form_template_batch_record_binding.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing form template batch-record binding migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_template_batch_record_binding_migration_declares_release_contract() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260717_bpm_form_center; type=schema; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "CREATE PROCEDURE ensure_bpm_form_template_batch_record_binding()" in sql
    assert "CALL ensure_bpm_form_template_batch_record_binding();" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_bpm_form_template_batch_record_binding;" in sql


def test_form_template_batch_record_binding_migration_is_additive_and_fail_fast() -> None:
    sql = read_sql()
    upper = sql.upper()

    for destructive in ("DROP TABLE", "TRUNCATE TABLE", "DELETE FROM"):
        assert destructive not in upper

    assert "TABLE_NAME = 'bpm_form_template_version'" in sql
    assert "SET MESSAGE_TEXT = 'bpm_form_template_version is missing'" in sql


def test_form_template_batch_record_binding_columns_are_explicit_not_name_guessed() -> None:
    sql = read_sql()

    for column in (
        "`batch_record_report_id` varchar(64) DEFAULT NULL",
        "`batch_record_report_name` varchar(255) DEFAULT NULL",
        "`batch_record_name` varchar(255) DEFAULT NULL",
        "`batch_record_version_no` varchar(64) DEFAULT NULL",
        "`batch_record_form_slot_type` varchar(32) DEFAULT NULL",
        "`batch_record_binding_status` varchar(32) DEFAULT NULL",
        "`batch_record_binding_error` varchar(500) DEFAULT NULL",
    ):
        assert column in sql

    assert "`idx_bpm_form_template_batch_record_report`" in sql
    assert "(`tenant_id`, `batch_record_report_id`, `deleted`)" in sql
    assert "template_name = report_name" not in sql.lower()
    assert "source_file_name = source_file_name" not in sql.lower()
