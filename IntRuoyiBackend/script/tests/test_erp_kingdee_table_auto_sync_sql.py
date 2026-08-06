from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260805_erp_kingdee_table_auto_sync.sql"


def test_kingdee_table_auto_sync_sql_creates_plan_tables() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    for table in [
        "erp_kingdee_table_auto_sync_plan",
        "erp_kingdee_table_auto_sync_plan_item",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table}`" in sql

    for column in [
        "`tenant_id`",
        "`enabled`",
        "`daily_start_time`",
        "`cron_expression`",
        "`job_id`",
        "`last_auto_run_date`",
        "`last_run_time`",
        "`last_status`",
        "`last_message`",
    ]:
        assert column in sql


def test_kingdee_table_auto_sync_sql_has_integrity_indexes() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "uk_erp_kingdee_table_auto_sync_plan_tenant" in sql
    assert "`tenant_id` ASC, `deleted` ASC" in sql
    assert "uk_erp_kingdee_table_auto_sync_plan_item_type" in sql
    assert "`plan_id` ASC, `sync_type` ASC, `deleted` ASC" in sql
    assert "idx_erp_kingdee_table_auto_sync_plan_item_plan" in sql


def test_kingdee_table_auto_sync_sql_seeds_disabled_dispatcher_job() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "INSERT INTO `infra_job`" in sql
    assert "'erpKingdeeTableAutoSyncJob'" in sql
    assert "handler_param" in sql
    assert "'ERP 表格自动同步 Job'" in sql
    assert "UPDATE `infra_job`" in sql


def test_kingdee_table_auto_sync_sql_has_release_metadata_and_runtime_dependency() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "20260805_erp_kingdee_table_auto_sync" in sql
    assert "ERP 表格自动同步" in sql
    assert "dependsOn=20260612_erp_kingdee_sync_runtime;" in sql
    assert "dependsOn=20260612_erp_kingdee_sync_runtime.sql" not in sql
    assert "type=schema; riskLevel=medium" in sql
    assert "type=schema,job" not in sql
