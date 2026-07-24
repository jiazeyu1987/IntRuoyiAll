from pathlib import Path


P1_SQL = Path("sql/mysql/20260610_mes_schedule_order_p1.sql")
T1_SQL = Path("sql/mysql/20260613_mes_smart_scheduling_t1_schema.sql")


def test_schedule_order_process_create_table_contains_key_process_flag() -> None:
    sql = P1_SQL.read_text(encoding="utf-8")

    assert "`key_process_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否关键工序'" in sql
    assert sql.index("`key_process_flag`") < sql.index("`bottleneck_flag`")


def test_schedule_order_process_migration_adds_key_process_flag_idempotently() -> None:
    sql = T1_SQL.read_text(encoding="utf-8")

    assert "ADD COLUMN `key_process_flag` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否关键工序''" in sql
    assert "COLUMN_NAME = 'key_process_flag'" in sql
    assert "ADD COLUMN `plan_date` date NULL COMMENT ''计划日期'' AFTER `key_process_flag`" in sql
