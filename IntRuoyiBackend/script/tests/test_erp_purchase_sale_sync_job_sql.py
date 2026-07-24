from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260612_erp_purchase_sale_sync_jobs.sql"


def test_purchase_sale_sync_job_sql_registers_paused_jobs():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "SELECT 5604, '每 10 分钟同步 ERP 采购订单', 2" in sql
    assert "'kingdeePurchaseOrderSyncJob'" in sql
    assert "'0 1/10 * * * ?'" in sql
    assert "SELECT 5605, '每 10 分钟同步 ERP 销售订单', 2" in sql
    assert "'kingdeeSaleOrderSyncJob'" in sql
    assert "'0 2/10 * * * ?'" in sql


def test_purchase_sale_sync_job_sql_is_idempotent_by_id_or_handler_name():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "WHERE (`id` = 5604 OR `handler_name` = 'kingdeePurchaseOrderSyncJob')" in sql
    assert "WHERE `id` = 5604" in sql
    assert "OR `handler_name` = 'kingdeePurchaseOrderSyncJob'" in sql
    assert "WHERE (`id` = 5605 OR `handler_name` = 'kingdeeSaleOrderSyncJob')" in sql
    assert "WHERE `id` = 5605" in sql
    assert "OR `handler_name` = 'kingdeeSaleOrderSyncJob'" in sql
