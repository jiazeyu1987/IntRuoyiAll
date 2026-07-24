from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260722_mes_route_form_center_runtime_columns.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing route form-center runtime columns migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_form_center_runtime_columns_declares_release_contract() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260717_bpm_form_center,20260720_mes_batch_shared_form_binding; "
        "type=schema; riskLevel=medium"
    )
    assert "CREATE PROCEDURE ensure_mes_route_form_center_runtime_columns()" in sql
    assert "CALL ensure_mes_route_form_center_runtime_columns();" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_mes_route_form_center_runtime_columns;" in sql


def test_route_form_center_runtime_columns_are_additive_and_fail_fast() -> None:
    sql = read_sql()
    upper = sql.upper()

    for destructive in ("DROP TABLE", "TRUNCATE TABLE", "DELETE FROM"):
        assert destructive not in upper

    for table in (
        "mes_pro_route_flow_process_batch_record",
        "mes_pro_edhr_batch_execution_task",
        "bpm_form_template_version",
    ):
        assert f"TABLE_NAME = '{table}'" in sql
        assert f"SET MESSAGE_TEXT = '{table} is missing'" in sql


def test_route_form_center_runtime_columns_cover_route_binding_and_task_runtime() -> None:
    sql = read_sql()

    for column in (
        "`form_binding_key` varchar(128) DEFAULT NULL",
        "`form_template_id` bigint DEFAULT NULL",
        "`form_template_name_snapshot` varchar(128) DEFAULT NULL",
        "`last_published_template_version_id` bigint DEFAULT NULL",
        "`last_published_template_version_no` varchar(64) DEFAULT NULL",
        "`form_template_version_id` bigint DEFAULT NULL",
        "`form_template_version_no` varchar(64) DEFAULT NULL",
        "`form_center_instance_id` bigint DEFAULT NULL",
    ):
        assert column in sql

    assert "`idx_mes_edhr_batch_task_form_instance`" in sql
    assert "(`tenant_id`, `form_center_instance_id`, `deleted`)" in sql
    assert "MODIFY COLUMN `batch_record_report_id` varchar(64) DEFAULT NULL" in sql
    assert "MODIFY COLUMN `form_slot_type` varchar(32) DEFAULT NULL" in sql
    assert "SET `deleted` = b'1'" in sql
    assert "`form_template_id` IS NULL" in sql
    assert "SET v_target_tenant_id = @mes_route_form_binding_target_tenant_id;" in sql
    assert "(v_target_tenant_id IS NULL OR `tenant_id` = v_target_tenant_id)" in sql
    assert "`uk_mes_route_flow_process_form_binding_key`" in sql
    assert "(`tenant_id`, `route_process_id`, `use_type`, `form_binding_key`, `deleted`)" in sql
    assert "`uk_mes_route_flow_process_form_template`" in sql
    assert "(`tenant_id`, `route_process_id`, `use_type`, `form_template_id`, `deleted`)" in sql
