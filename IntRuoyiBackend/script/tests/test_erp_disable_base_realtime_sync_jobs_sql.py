from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260613_erp_disable_base_realtime_sync_jobs.sql"


def test_disable_base_realtime_sync_jobs_sql_exists_and_sets_stop_status():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "UPDATE `infra_job`" in sql
    assert "`status` = 2" in sql
    assert "kingdeeProductItemSyncJob" in sql
    assert "kingdeeStockSyncJob" in sql
    assert "kingdeePurchaseOrderSyncJob" in sql
    assert "kingdeeSaleOrderSyncJob" in sql
    assert "5602" in sql
    assert "5603" in sql
    assert "5604" in sql
    assert "5605" in sql


def test_disable_base_realtime_sync_jobs_sql_does_not_touch_production_sync_jobs():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "kingdeeProductionOrderSyncJob" not in sql
    assert "kingdeeBomSyncJob" not in sql
    assert "kingdeeProductionMaterialListSyncJob" not in sql
    assert "5600" not in sql
    assert "5606" not in sql
    assert "5607" not in sql
