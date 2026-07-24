from pathlib import Path


MIGRATION = Path("sql/mysql/20260715_mes_schedule_capacity_mode_unification.sql")
DEFAULT_MODE_MIGRATION = Path("sql/mysql/20260717_mes_route_schedule_config_default_resource_mode.sql")
FULL_SCHEMA = Path("sql/mysql/ruoyi-vue-pro.sql")
TEST_SCHEMA = Path("yudao-module-mes/src/test/resources/sql/create_tables.sql")


def read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def test_capacity_mode_unification_migration_is_release_managed_and_idempotent():
    sql = read(MIGRATION)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=data; riskLevel=high\n"
    )
    assert "riskLevel=high" in sql
    assert "FINITE_HOURLY" in sql
    assert "MANUAL_OVERRIDE" in sql
    assert "WHERE `capacity_mode` = 'FINITE_HOURLY'" in sql
    assert "`capacity_mode` NOT IN ('RESOURCE_CALCULATED', 'MANUAL_OVERRIDE', 'INFINITE_FORMULA')" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "@mes_capacity_unification_migrated_rows := ROW_COUNT()" in sql
    assert "legacy_finite_hourly_before_count" in sql
    assert "migrated_finite_hourly_rows" in sql
    assert "machinery_process_backfill_rows" in sql
    assert "manual_review_rows" in sql


def test_capacity_mode_unification_backfills_machinery_process_from_master_only_when_unique():
    sql = read(MIGRATION)

    assert "mes_md_workstation_machine" in sql
    assert "mes_md_workstation" in sql
    assert "mes_dv_machinery_process" in sql
    assert "mes_dv_machinery" in sql
    assert "standard_hourly_capacity" in sql
    assert "LEFT JOIN `mes_dv_machinery_process`" in sql
    assert "process_capacity.`id` IS NULL" in sql
    assert "master_machinery.`standard_hourly_capacity` IS NOT NULL" in sql


def test_capacity_mode_values_are_documented_in_schema_comments():
    for schema_path in (FULL_SCHEMA, TEST_SCHEMA):
        schema = read(schema_path)
        assert "RESOURCE_CALCULATED" in schema
        assert "MANUAL_OVERRIDE" in schema
        assert "INFINITE_FORMULA" in schema


def test_route_schedule_config_schema_defaults_to_resource_calculated():
    for schema_path in (FULL_SCHEMA, TEST_SCHEMA):
        schema = read(schema_path)
        assert "DEFAULT 'RESOURCE_CALCULATED'" in schema
        assert "DEFAULT 'MANUAL_OVERRIDE'" not in schema


def test_default_resource_mode_migration_updates_capacity_mode_default_only():
    sql = read(DEFAULT_MODE_MIGRATION)

    assert sql.startswith("-- release-migration:")
    assert "riskLevel=low" in sql
    assert "ALTER TABLE `mes_pro_route_schedule_config`" in sql
    assert "DEFAULT 'RESOURCE_CALCULATED'" in sql
    assert "产能覆盖每小时产能" in sql
    assert "UPDATE `mes_pro_route_schedule_config`" not in sql
