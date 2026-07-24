from pathlib import Path


MIGRATION_SQL = Path("sql/mysql/20260709_mes_route_flow_config_unification.sql")
TEST_SCHEMA = Path("yudao-module-mes/src/test/resources/sql/create_tables.sql")
CLEAN_SQL = Path("yudao-module-mes/src/test/resources/sql/clean.sql")


def read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def test_route_flow_migration_creates_new_authoritative_tables():
    sql = read(MIGRATION_SQL)

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_flow_config`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_flow_process_config`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_flow_process_batch_record`" in sql
    assert "`route_flow_config_id` bigint NOT NULL" in sql
    assert "`route_flow_process_config_id` bigint NOT NULL" in sql
    assert "`production_quantity_factor` decimal(18,6) NOT NULL DEFAULT 1.000000" in sql
    assert "`batch_record_version_id` bigint DEFAULT NULL" in sql
    assert "`slot_config_snapshot_hash` varchar(128) DEFAULT NULL" in sql


def test_route_flow_migration_copies_and_asserts_legacy_counts():
    sql = read(MIGRATION_SQL)

    assert "INSERT INTO `mes_pro_route_flow_config`" in sql
    assert "FROM `mes_pro_route_use_config`" in sql
    assert "INSERT INTO `mes_pro_route_flow_process_config`" in sql
    assert "FROM `mes_pro_route_use_process_config`" in sql
    assert "INSERT INTO `mes_pro_route_flow_process_batch_record`" in sql
    assert "FROM `mes_pro_route_use_process_batch_record`" in sql
    assert "route flow migration config count mismatch" in sql
    assert "route flow migration process config count mismatch" in sql
    assert "route flow migration batch record count mismatch" in sql
    assert "missing legacy mes_pro_route_use_config before route flow migration" in sql


def test_route_flow_migration_removes_old_menus_and_inherits_permissions():
    sql = read(MIGRATION_SQL)

    for old_id in ("900121", "900122", "900221", "900222"):
        assert old_id in sql
    for new_permission in (
        "mes:pro-route:schedule-config:query",
        "mes:pro-route:schedule-config:update",
        "mes:pro-route:batch-record-config:query",
        "mes:pro-route:batch-record-config:update",
    ):
        assert new_permission in sql

    assert "UPDATE `system_role_menu`" in sql
    assert "UPDATE `system_menu`" in sql
    assert "old process route menus must be deleted after route flow migration" in sql


def test_route_flow_migration_renames_legacy_tables_away_from_active_names():
    sql = read(MIGRATION_SQL)

    assert "mes_pro_route_use_config_legacy_20260709" in sql
    assert "mes_pro_route_use_process_config_legacy_20260709" in sql
    assert "mes_pro_route_use_process_batch_record_legacy_20260709" in sql
    assert "legacy route use active table names must not remain after migration" in sql


def test_route_flow_test_schema_uses_new_tables_only():
    schema = read(TEST_SCHEMA)
    clean = read(CLEAN_SQL)

    assert 'CREATE TABLE IF NOT EXISTS "mes_pro_route_flow_config"' in schema
    assert 'CREATE TABLE IF NOT EXISTS "mes_pro_route_flow_process_config"' in schema
    assert 'CREATE TABLE IF NOT EXISTS "mes_pro_route_flow_process_batch_record"' in schema
    assert 'CREATE TABLE IF NOT EXISTS "mes_pro_route_use_config"' not in schema
    assert 'CREATE TABLE IF NOT EXISTS "mes_pro_route_use_process_config"' not in schema
    assert 'CREATE TABLE IF NOT EXISTS "mes_pro_route_use_process_batch_record"' not in schema
    assert 'DELETE FROM "mes_pro_route_flow_process_batch_record";' in clean
    assert 'DELETE FROM "mes_pro_route_flow_process_config";' in clean
    assert 'DELETE FROM "mes_pro_route_flow_config";' in clean
    assert 'DELETE FROM "mes_pro_route_use_process_batch_record";' not in clean

def test_mysql_mes_base_schema_uses_new_route_flow_tables_only():
    base_schema = read(Path("sql/mysql/20260512_mes_base_schema.sql"))
    full_schema = read(Path("sql/mysql/ruoyi-vue-pro.sql"))

    for schema in (base_schema, full_schema):
        assert "mes_pro_route_use_config" not in schema
        assert "mes_pro_route_use_process_config" not in schema
        assert "mes_pro_route_use_process_batch_record" not in schema

    for table_name in (
        "mes_pro_route_flow_config",
        "mes_pro_route_flow_process_config",
        "mes_pro_route_flow_process_batch_record",
    ):
        assert table_name in base_schema
        assert table_name in full_schema
