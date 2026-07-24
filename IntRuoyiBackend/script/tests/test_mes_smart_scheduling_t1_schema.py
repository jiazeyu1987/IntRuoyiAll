from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_mes_smart_scheduling_t1_schema_defines_route_version_and_compare_tables() -> None:
    sql = (REPO_ROOT / "sql" / "mysql" / "20260613_mes_smart_scheduling_t1_schema.sql").read_text(
        encoding="utf-8"
    )

    assert "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `route_status`" in sql
    assert "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `auto_schedulable`" in sql
    assert "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `route_version_id`" in sql
    assert "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `latest_start_time`" in sql
    assert "ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `capacity_mode`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_version`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_schedule_config`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_schedule_order_daily_compare`" in sql


def test_mes_smart_scheduling_t1_schema_avoids_default_backfill() -> None:
    sql = (REPO_ROOT / "sql" / "mysql" / "20260613_mes_smart_scheduling_t1_schema.sql").read_text(
        encoding="utf-8"
    )

    assert "UPDATE `mes_pro_schedule_order` SET" not in sql
    assert "UPDATE `mes_pro_schedule_order_process` SET" not in sql
    assert "DEFAULT 0 COMMENT ''路线状态" not in sql
