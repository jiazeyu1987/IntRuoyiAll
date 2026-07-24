from pathlib import Path


SQL_PATH = Path("sql/mysql/20260614_mes_schedule_order_auto_schedulable_backfill.sql")


def test_backfill_sets_route_bound_schedule_orders_auto_schedulable():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "UPDATE `mes_pro_schedule_order`" in sql
    assert "`route_id` IS NOT NULL" in sql
    assert "`auto_schedulable` IS NULL" in sql
    assert "`auto_schedulable` = b'1'" in sql


def test_backfill_sets_missing_route_schedule_orders_not_auto_schedulable():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "`route_id` IS NULL" in sql
    assert "`auto_schedulable` = b'0'" in sql
