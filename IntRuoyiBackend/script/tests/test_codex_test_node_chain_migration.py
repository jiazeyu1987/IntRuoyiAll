from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260727_system_codex_test_node_chain.sql"
CREATE_TABLES = (
    ROOT
    / "yudao-module-system"
    / "src"
    / "test"
    / "resources"
    / "sql"
    / "create_tables.sql"
)


def read_sql(path: Path) -> str:
    assert path.exists(), f"Missing SQL script: {path}"
    return path.read_text(encoding="utf-8")


def normalized_sql(path: Path) -> str:
    return read_sql(path).replace("`", "").lower()


def test_node_chain_migration_declares_idempotent_schema_change() -> None:
    sql = read_sql(SQL_PATH)
    normalized = normalized_sql(SQL_PATH)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260726_system_codex_test_case_project; type=schema; riskLevel=medium\n"
    )
    assert "CREATE PROCEDURE ensure_system_codex_test_node_chain" in sql
    assert "ALTER TABLE `system_codex_test_case`" in sql
    for column in (
        "node_chain_name varchar(128) null",
        "node_chain_sort int null",
        "node_chain_execution bit not null default b'0'",
    ):
        assert column in normalized
    assert "idx_system_codex_test_case_tenant_node_chain" in normalized


def test_node_chain_test_schema_matches_runtime_columns() -> None:
    normalized = normalized_sql(CREATE_TABLES)

    for column in (
        '"node_chain_name" varchar(128)',
        '"node_chain_sort" int',
        '"node_chain_execution" bit not null default false',
    ):
        assert column in normalized
