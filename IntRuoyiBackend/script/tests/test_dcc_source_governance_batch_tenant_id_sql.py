from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260922_dcc_source_governance_batch_tenant_id.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing DCC source governance batch tenant migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_source_governance_batch_tenant_migration_has_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test; "
        "dependsOn=20260905_dcc_source_governance; type=schema; riskLevel=low"
    )


def test_source_governance_batch_tenant_migration_is_mysql_idempotent() -> None:
    sql = read_sql()
    lowered = sql.lower()

    assert "information_schema.columns" in lowered
    assert "table_schema = database()" in lowered
    assert "table_name = 'dcc_controlled_file_source_governance_batch'" in lowered
    assert "column_name = 'tenant_id'" in lowered
    assert "prepare dcc_source_governance_batch_tenant_stmt from @ddl" in lowered
    assert "execute dcc_source_governance_batch_tenant_stmt" in lowered
    assert "deallocate prepare dcc_source_governance_batch_tenant_stmt" in lowered
    assert "add column if not exists" not in lowered


def test_source_governance_batch_tenant_migration_is_schema_only() -> None:
    lowered = read_sql().lower()

    assert "alter table `dcc_controlled_file_source_governance_batch` add column `tenant_id`" in lowered
    assert "insert into `dcc_controlled_file_source_governance_batch`" not in lowered
    assert "update `dcc_controlled_file_source_governance_batch`" not in lowered
    assert "delete from `dcc_controlled_file_source_governance_batch`" not in lowered
    assert "drop table" not in lowered
    assert "truncate table" not in lowered
