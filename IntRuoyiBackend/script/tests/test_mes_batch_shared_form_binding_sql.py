from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260720_mes_batch_shared_form_binding.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing batch shared form binding migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_batch_shared_form_binding_migration_declares_release_contract() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260707_mes_batch_record_extra_form_slots,20260708_mes_batch_record_version_phase_one; "
        "type=schema; riskLevel=medium"
    )
    assert "CREATE PROCEDURE ensure_mes_batch_shared_form_binding()" in sql
    assert "CALL ensure_mes_batch_shared_form_binding();" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_mes_batch_shared_form_binding;" in sql


def test_batch_shared_form_binding_migration_is_additive_and_fail_fast() -> None:
    sql = read_sql()
    upper = sql.upper()

    for destructive in ("DROP TABLE", "TRUNCATE TABLE", "DELETE FROM", "UPDATE `"):
        assert destructive not in upper

    for table in (
        "mes_pro_route_flow_process_batch_record",
        "mes_pro_batch_record_execution",
        "mes_pro_edhr_batch_execution_task",
    ):
        assert f"TABLE_NAME = '{table}'" in sql
        assert f"SET MESSAGE_TEXT = '{table} is missing'" in sql


def test_batch_shared_form_binding_migration_adds_shared_columns_and_indexes() -> None:
    sql = read_sql()

    expected_columns = (
        "`instance_scope` varchar(32) NOT NULL DEFAULT 'PROCESS'",
        "`shared_form_key` varchar(64) DEFAULT NULL",
        "`fillable_scope_json` json DEFAULT NULL",
        "`batch_execution_id` bigint DEFAULT NULL",
        "`active_context_key` varchar(512) DEFAULT NULL",
    )
    for column in expected_columns:
        assert column in sql

    assert "`idx_mes_batch_record_execution_shared`" in sql
    assert "(`tenant_id`, `batch_execution_id`, `instance_scope`, `shared_form_key`, `batch_code`, `deleted`)" in sql
    assert "`idx_mes_edhr_batch_task_shared`" in sql
    assert "(`tenant_id`, `batch_execution_id`, `instance_scope`, `shared_form_key`, `deleted`)" in sql
