from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260805_erp_nas_table_auto_sync.sql"


def test_nas_table_auto_sync_sql_creates_plan_and_run_tables() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    for table in [
        "erp_nas_table_sync_plan",
        "erp_nas_table_sync_plan_item",
        "erp_nas_table_sync_run",
        "erp_nas_table_sync_run_item",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table}`" in sql

    for column in [
        "`tenant_id`",
        "`enabled`",
        "`daily_start_time`",
        "`cron_expression`",
        "`nas_directory`",
        "`file_name_pattern`",
        "`job_id`",
        "`last_run_id`",
        "`last_status`",
        "`failure_message`",
    ]:
        assert column in sql


def test_nas_table_auto_sync_sql_has_integrity_indexes() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "uk_erp_nas_table_sync_plan_tenant" in sql
    assert "`tenant_id` ASC, `deleted` ASC" in sql
    assert "uk_erp_nas_table_sync_plan_item_type" in sql
    assert "`plan_id` ASC, `sync_type` ASC, `deleted` ASC" in sql
    assert "uk_erp_nas_table_sync_run_item_type" in sql
    assert "`run_id` ASC, `sync_type` ASC, `deleted` ASC" in sql
    assert "idx_erp_nas_table_sync_run_plan_started" in sql


def test_nas_table_auto_sync_sql_seeds_disabled_infra_job() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "INSERT INTO `infra_job`" in sql
    assert "'erpNasTableAutoSyncJob'" in sql
    assert "handler_param" in sql
    assert "'NAS 表格自动同步 Job'" in sql
    assert "UPDATE `infra_job`" in sql


def test_nas_table_auto_sync_sql_has_release_metadata() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "20260805_erp_nas_table_auto_sync" in sql
    assert "NAS 表格自动同步" in sql
    assert "schema" in sql.lower()
    assert "job" in sql.lower()
