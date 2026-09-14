from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = BACKEND_ROOT / "sql" / "mysql" / "20260820_mes_pqc_item_equipment_config.sql"
H2_SCHEMA = BACKEND_ROOT / "yudao-module-mes" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def read_text(path: Path) -> str:
    assert path.exists(), f"missing file: {path}"
    return path.read_text(encoding="utf-8")


def table_block(source: str, table_name: str, next_marker: str) -> str:
    start = source.index(f'CREATE TABLE IF NOT EXISTS "{table_name}"')
    end = source.index(next_marker, start)
    return source[start:end]


def test_migration_creates_tenant_level_item_equipment_tables_without_qa_version_scope() -> None:
    sql = read_text(MIGRATION)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260817_mes_pqc_item_level_task_identity; type=schema; riskLevel=medium\n"
    )
    assert "CREATE TABLE IF NOT EXISTS `mes_pqc_item_equipment_config`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pqc_item_equipment_number_config`" in sql
    assert "`item_code` varchar(64) NOT NULL COMMENT '检验项目编号'" in sql
    assert "`equipment_id` bigint NOT NULL COMMENT '设备台账ID'" in sql
    assert "`equipment_number` varchar(64) NOT NULL COMMENT '设备编号快照'" in sql
    assert "regulation_version_id" not in sql.lower()
    assert "inspection_type" not in sql.lower()


def test_migration_locks_unique_keys_by_tenant_item_equipment_and_number() -> None:
    sql = read_text(MIGRATION)

    assert (
        "UNIQUE KEY `uk_mes_pqc_item_equipment` "
        "(`tenant_id`, `item_code`, `equipment_id`, `deleted`)"
    ) in sql
    assert (
        "UNIQUE KEY `uk_mes_pqc_item_equipment_number` "
        "(`tenant_id`, `item_code`, `equipment_id`, `equipment_number`, `deleted`)"
    ) in sql
    assert "KEY `idx_mes_pqc_item_equipment_item` (`tenant_id`, `item_code`, `enabled`, `sort`)" in sql
    assert (
        "KEY `idx_mes_pqc_item_equipment_number_item` "
        "(`tenant_id`, `item_code`, `equipment_id`, `enabled`, `sort`)"
    ) in sql


def test_h2_schema_matches_new_tables_without_qa_version_scope() -> None:
    schema = read_text(H2_SCHEMA)
    config_block = table_block(
        schema,
        "mes_pqc_item_equipment_config",
        'CREATE TABLE IF NOT EXISTS "mes_pqc_item_equipment_number_config"',
    )
    number_block = table_block(
        schema,
        "mes_pqc_item_equipment_number_config",
        'CREATE TABLE IF NOT EXISTS "mes_pqc_process_inspection_aggregate_detail"',
    )

    for block in (config_block, number_block):
        assert '"tenant_id" bigint NOT NULL DEFAULT 0' in block
        assert '"item_code" varchar(64) NOT NULL' in block
        assert "regulation_version_id" not in block.lower()
        assert "inspection_type" not in block.lower()

    assert '"equipment_id" bigint NOT NULL' in config_block
    assert '"equipment_number" varchar(64) NOT NULL' in number_block
