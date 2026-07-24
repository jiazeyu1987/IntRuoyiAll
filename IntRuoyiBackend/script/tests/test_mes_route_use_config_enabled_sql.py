from pathlib import Path


def test_route_use_config_enabled_migration_contract():
    sql = Path("sql/mysql/20260707_mes_route_use_config_enabled.sql").read_text(encoding="utf-8")

    assert "ALTER TABLE `mes_pro_route_use_config`" in sql
    assert "ADD COLUMN `enabled` bit(1) NOT NULL DEFAULT b'0'" in sql
    assert "COMMENT '用途级启用状态'" in sql
    assert "AFTER `use_type`" in sql


def test_route_use_config_enabled_test_schema_contract():
    schema = Path("yudao-module-mes/src/test/resources/sql/create_tables.sql").read_text(
        encoding="utf-8"
    )
    table_start = schema.index('CREATE TABLE IF NOT EXISTS "mes_pro_route_use_config"')
    table_end = schema.index(");", table_start)
    table_sql = schema[table_start:table_end]

    assert '"enabled" bit NOT NULL DEFAULT FALSE' in table_sql
