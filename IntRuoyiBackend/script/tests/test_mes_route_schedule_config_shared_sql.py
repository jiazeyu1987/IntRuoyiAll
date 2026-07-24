from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260613_mes_smart_scheduling_t1_schema.sql"
)


def _read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_schedule_config_table_uses_route_process_dimension() -> None:
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


def test_route_schedule_config_migration_fails_fast_on_product_level_conflicts() -> None:
    sql = _read_sql()

    required_tokens = [
        "@route_schedule_config_conflict_count",
        "HAVING COUNT(DISTINCT CONCAT_WS('#'",
        "COALESCE(`capacity_mode`, '')",
        "COALESCE(CAST(`hourly_capacity` AS CHAR), '')",
        "COALESCE(CAST(`infinite_duration_quantity_factor` AS CHAR), '')",
        "COALESCE(CAST(`infinite_duration_base_minutes` AS CHAR), '')",
        "COALESCE(CAST(`night_shift_enabled` AS CHAR), '')",
        "COALESCE(CAST(`calendar_rule_id` AS CHAR), '')",
        "COALESCE(`config_version`, '')",
        "COALESCE(`remark`, '')",
        "tmp_mes_pro_route_schedule_config_conflict_guard",
        "`guard_id` tinyint NOT NULL PRIMARY KEY",
        "VALUES (1, 'mes_pro_route_schedule_config conflict check passed')",
        "SELECT 1, 'mes_pro_route_schedule_config has conflicting product-level configs'",
        "WHERE @route_schedule_config_conflict_count > 0",
    ]
    for token in required_tokens:
        assert token in sql


def test_route_schedule_config_migration_merges_non_conflicting_product_rows() -> None:
    sql = _read_sql()

    required_tokens = [
        "UPDATE `mes_pro_route_schedule_config` target",
        "SET target.`item_id` = NULL",
        "ROW_NUMBER() OVER (",
        "PARTITION BY `tenant_id`, `route_version_id`, `route_process_id`",
        "SET target.`deleted` = b'1'",
        "ALTER TABLE `mes_pro_route_schedule_config` DROP INDEX `uk_mes_pro_route_schedule_config_item_process`",
        "ALTER TABLE `mes_pro_route_schedule_config` ADD UNIQUE INDEX `uk_mes_pro_route_schedule_config_process` (`tenant_id`, `route_version_id`, `route_process_id`, `deleted`) USING BTREE",
    ]
    for token in required_tokens:
        assert token in sql
