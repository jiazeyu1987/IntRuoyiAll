from pathlib import Path
import re


MIGRATION_SQL = Path("sql/mysql/20260708_mes_schedule_route_process_quantity_factor.sql")
ROUTE_USE_INIT_SQL = Path("sql/mysql/20260610_mes_route_use_config_p3.sql")
SCHEDULE_ORDER_INIT_SQL = Path("sql/mysql/20260610_mes_schedule_order_p1.sql")


def executable_sql(sql: str) -> str:
    return "\n".join(
        line for line in sql.splitlines()
        if not re.match(r"^\s*--", line)
    ).upper()


def test_quantity_factor_migration_adds_idempotent_columns():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert "release-migration:" in sql
    assert "intruoyi_add_mes_schedule_route_process_quantity_factor" in sql
    assert "information_schema.columns" in sql
    assert "mes_pro_route_use_process_config" in sql
    assert "mes_pro_schedule_order_process" in sql
    assert "production_quantity_factor" in sql
    assert "decimal(18,6) NOT NULL DEFAULT 1.000000" in sql
    assert "AFTER `execution_mode`" in sql
    assert "AFTER `shift_capacity_total`" in sql


def test_quantity_factor_migration_is_non_destructive():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")
    upper_sql = executable_sql(sql)

    assert "DELETE FROM" not in upper_sql
    assert "TRUNCATE" not in upper_sql
    assert "DROP TABLE" not in upper_sql
    assert "DROP COLUMN" not in upper_sql


def test_quantity_factor_new_install_schema_matches_migration():
    route_use_sql = ROUTE_USE_INIT_SQL.read_text(encoding="utf-8")
    schedule_order_sql = SCHEDULE_ORDER_INIT_SQL.read_text(encoding="utf-8")

    assert "`production_quantity_factor` decimal(18,6) NOT NULL DEFAULT 1.000000" in route_use_sql
    assert "`production_quantity_factor` decimal(18,6) NOT NULL DEFAULT 1.000000" in schedule_order_sql
    assert "生产数量系数，工序计划数量=成品数量*生产数量系数" in route_use_sql
    assert "生产数量系数快照" in schedule_order_sql
