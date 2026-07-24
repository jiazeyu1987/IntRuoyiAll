from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260613_mes_smart_scheduling_t1_schema.sql"
)


def _read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_schedule_config_table_uses_item_process_capacity_dimension() -> None:
    sql = _read_sql()

    required_tokens = [
        "CREATE TABLE IF NOT EXISTS `mes_pro_route_schedule_config`",
        "`route_version_id` bigint NOT NULL COMMENT '路线版本ID'",
        "`item_id` bigint NULL DEFAULT NULL COMMENT '历史产品物料ID'",
        "`route_process_id` bigint NOT NULL COMMENT '路线工序ID'",
        "UNIQUE INDEX `uk_mes_pro_route_schedule_config_process` (`tenant_id` ASC, `route_version_id` ASC, `route_process_id` ASC, `deleted` ASC) USING BTREE",
    ]
    for token in required_tokens:
        assert token in sql


def test_route_schedule_config_existing_table_migration_is_idempotent() -> None:
    sql = _read_sql()

    required_tokens = [
        "COLUMN_NAME = 'item_id'",
        "ALTER TABLE `mes_pro_route_schedule_config` ADD COLUMN `item_id` bigint NULL COMMENT ''历史产品物料ID'' AFTER `route_version_id`",
        "INDEX_NAME = 'uk_mes_pro_route_schedule_config_item_process'",
        "ALTER TABLE `mes_pro_route_schedule_config` DROP INDEX `uk_mes_pro_route_schedule_config_item_process`",
        "INDEX_NAME = 'uk_mes_pro_route_schedule_config_process'",
        "ALTER TABLE `mes_pro_route_schedule_config` ADD UNIQUE INDEX `uk_mes_pro_route_schedule_config_process` (`tenant_id`, `route_version_id`, `route_process_id`, `deleted`) USING BTREE",
    ]
    for token in required_tokens:
        assert token in sql


def test_route_schedule_config_item_migration_does_not_guess_legacy_item_mapping() -> None:
    sql = _read_sql().lower()

    assert "tmp_mes_pro_route_schedule_config_conflict_guard" in sql
    assert "mes_pro_route_schedule_config has conflicting product-level configs" in sql
    assert "where @route_schedule_config_conflict_count > 0" in sql
    assert "row_number() over" in sql
