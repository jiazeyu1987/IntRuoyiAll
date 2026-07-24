from pathlib import Path


MIGRATION = Path("sql/mysql/20260717_mes_route_process_workstation_binding.sql")
TEST_SCHEMA = Path("yudao-module-mes/src/test/resources/sql/create_tables.sql")


def read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def test_route_process_workstation_binding_migration_is_release_managed_and_idempotent():
    sql = read(MIGRATION)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium\n"
    )
    assert "mes_pro_route_process" in sql
    assert "workstation_id" in sql
    assert "ADD COLUMN" in sql
    assert "information_schema.COLUMNS" in sql
    assert "unique_workstation_process" in sql
    assert "manual_review_route_process_count" in sql


def test_route_process_workstation_id_is_documented_in_test_schema():
    schema = read(TEST_SCHEMA)

    table_start = schema.index('CREATE TABLE IF NOT EXISTS "mes_pro_route_process"')
    table_end = schema.index(");", table_start)
    route_process_schema = schema[table_start:table_end]
    assert '"workstation_id" bigint DEFAULT NULL' in route_process_schema
