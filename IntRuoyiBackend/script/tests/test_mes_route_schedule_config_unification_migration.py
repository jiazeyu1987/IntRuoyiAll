from pathlib import Path


MIGRATION = Path("sql/mysql/20260710_mes_route_schedule_config_unification.sql")
TEST_SCHEMA = Path("yudao-module-mes/src/test/resources/sql/create_tables.sql")
FULL_SCHEMA = Path("sql/mysql/ruoyi-vue-pro.sql")


def read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def test_migration_is_release_managed_and_fail_fast():
    sql = read(MIGRATION)

    assert sql.startswith("-- release-migration:")
    assert "riskLevel=high" in sql
    assert "route schedule config missing exactly one generic config" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "START TRANSACTION" in sql
    assert "COMMIT" in sql


def test_migration_keeps_generic_config_and_retires_product_configs():
    sql = read(MIGRATION)

    assert "item_id IS NULL" in sql
    assert "item_id IS NOT NULL" in sql
    assert "SET product_config.`deleted` = b'1'" in sql
    assert "route_schedule_config_id" in sql
    assert "canonical_config_id" in sql
    assert "mes_pro_schedule_order_process" in sql
    assert "mes_pro_schedule_order" in sql
    assert "manual_finished" in sql
    assert "frozen" in sql


def test_migration_requires_unique_generic_config_for_active_wip_and_backfills_legacy_route_version():
    sql = read(MIGRATION)

    assert "active WIP route process missing route context" in sql
    assert "active WIP route process missing unique generic schedule config" in sql
    assert "COALESCE(process_snapshot.`route_version_id`, schedule_order.`route_version_id`)" in sql
    assert "HAVING COUNT(*) = 1" in sql
    assert "MIN(`canonical_config_id`) AS `canonical_config_id`" in sql
    assert "process_snapshot.`route_version_id` = unique_canonical.`route_version_id`" in sql


def test_migration_retires_deleted_route_process_configs_without_reclassifying_them_as_current():
    sql = read(MIGRATION)

    assert "tmp_mes_route_schedule_orphan_config" in sql
    assert "route_process.`deleted` = b'0'" in sql
    assert "route_process.`route_id` = route_version.`route_id`" in sql
    assert "process_snapshot.`route_schedule_config_id` = NULL" in sql
    assert "process_snapshot.`enabled` = b'0'" in sql
    assert "orphan_config.`orphan_config_id`" in sql
    assert "active route schedule config references deleted route process" in sql
    assert "active WIP still references deleted route process after migration" in sql


def test_migration_replaces_product_unique_index_with_active_only_route_process_index():
    sql = read(MIGRATION)

    assert "DROP INDEX `uk_mes_pro_route_schedule_config_item_process`" in sql
    assert "`active_unique_flag` tinyint GENERATED ALWAYS AS" in sql
    assert "IF(`deleted` = b'0', 1, NULL)" in sql
    assert "uk_mes_pro_route_schedule_config_active_process" in sql
    assert "`tenant_id`, `route_version_id`, `route_process_id`, `active_unique_flag`" in sql


def test_baseline_schemas_match_active_only_route_process_constraint():
    for schema_path in (TEST_SCHEMA, FULL_SCHEMA):
        schema = read(schema_path)
        assert "active_unique_flag" in schema
        assert "uk_mes_pro_route_schedule_config_active_process" in schema
        assert "uk_mes_pro_route_schedule_config_item_process" not in schema
