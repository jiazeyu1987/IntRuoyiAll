from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260612_erp_kingdee_sync_runtime.sql"


def test_sync_runtime_sql_creates_watermark_and_run_tables():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `erp_kingdee_sync_watermark`" in sql
    assert "CREATE TABLE IF NOT EXISTS `erp_kingdee_sync_run`" in sql
    assert "`sync_type`" in sql
    assert "`last_success_time`" in sql
    assert "`trigger_type`" in sql
    assert "`status`" in sql
    assert "`failure_message`" in sql


def test_sync_runtime_sql_has_concurrency_and_query_indexes():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "uk_erp_kingdee_sync_watermark_type" in sql
    assert "`tenant_id` ASC, `sync_type` ASC, `deleted` ASC" in sql
    assert "idx_erp_kingdee_sync_run_type_status" in sql
    assert "`tenant_id` ASC, `sync_type` ASC, `status` ASC, `deleted` ASC" in sql
