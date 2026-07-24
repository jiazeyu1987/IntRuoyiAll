from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260613_mes_nightly_replan_job_2am.sql"


def test_nightly_replan_job_sql_registers_enabled_2am_job():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "INSERT INTO `infra_job`" in sql
    assert "5616" in sql
    assert "'mesProNightlyReplanJob'" in sql
    assert "'0 0 2 * * ?'" in sql
    assert "SELECT 5616, '每天凌晨 2 点重排 MES 排产工单', 1" in sql


def test_nightly_replan_job_sql_is_idempotent_by_id_or_handler_name():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "WHERE (`id` = 5616 OR `handler_name` = 'mesProNightlyReplanJob')" in sql
    assert "UPDATE `infra_job`" in sql
    assert "WHERE `id` = 5616" in sql
    assert "OR `handler_name` = 'mesProNightlyReplanJob'" in sql
