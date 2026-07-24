from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260612_erp_product_item_sync_job.sql"


def test_product_item_sync_job_sql_registers_paused_job():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "INSERT INTO `infra_job`" in sql
    assert "5602" in sql
    assert "'kingdeeProductItemSyncJob'" in sql
    assert "'0 0/10 * * * ?'" in sql
    assert "SELECT 5602, '每 10 分钟同步 ERP 商品和 MES 物料', 2" in sql


def test_product_item_sync_job_sql_is_idempotent_by_id_or_handler_name():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "WHERE (`id` = 5602 OR `handler_name` = 'kingdeeProductItemSyncJob')" in sql
    assert "UPDATE `infra_job`" in sql
    assert "WHERE `id` = 5602" in sql
    assert "OR `handler_name` = 'kingdeeProductItemSyncJob'" in sql
