from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260612_erp_stock_sync_job.sql"


def test_stock_sync_job_sql_registers_paused_job():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "INSERT INTO `infra_job`" in sql
    assert "5603" in sql
    assert "'kingdeeStockSyncJob'" in sql
    assert "'0 5/10 * * * ?'" in sql
    assert "SELECT 5603, '每 10 分钟同步 ERP 库存', 2" in sql


def test_stock_sync_job_sql_is_idempotent_by_id_or_handler_name():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "WHERE (`id` = 5603 OR `handler_name` = 'kingdeeStockSyncJob')" in sql
    assert "UPDATE `infra_job`" in sql
    assert "WHERE `id` = 5603" in sql
    assert "OR `handler_name` = 'kingdeeStockSyncJob'" in sql
