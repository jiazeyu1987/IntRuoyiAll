from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260612_erp_bom_sync_job.sql"


def test_bom_sync_job_sql_registers_paused_job():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "SELECT 5606, '每 10 分钟同步 ERP BOM', 2" in sql
    assert "'kingdeeBomSyncJob'" in sql
    assert "'0 3/10 * * * ?'" in sql


def test_bom_sync_job_sql_is_idempotent_by_id_or_handler_name():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "WHERE (`id` = 5606 OR `handler_name` = 'kingdeeBomSyncJob')" in sql
    assert "WHERE `id` = 5606" in sql
    assert "OR `handler_name` = 'kingdeeBomSyncJob'" in sql
