from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260808_system_codex_test_analysis_mode.sql"
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


def test_analysis_mode_migration_declares_idempotent_schema_change() -> None:
    sql = read_sql(SQL_PATH)
    normalized = normalized_sql(SQL_PATH)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260727_system_codex_test_node_chain,20260726_system_codex_test_run_monitor_progress; "
        "type=schema; riskLevel=medium\n"
    )
    assert "CREATE PROCEDURE ensure_system_codex_test_analysis_mode" in sql
    assert "Missing system_codex_test_case table" in sql
    assert "Missing system_codex_test_execution_case table" in sql
    assert "Invalid system_codex_test_case.analysis_mode" in sql
    assert "Invalid system_codex_test_execution_case.analysis_mode_snapshot" in sql
    for column in (
        "analysis_mode varchar(32) not null default 'playwright_e2e'",
        "analysis_mode_snapshot varchar(32) not null default 'playwright_e2e'",
    ):
        assert column in normalized


def test_analysis_mode_test_schema_matches_runtime_columns() -> None:
    normalized = normalized_sql(CREATE_TABLES)

    for column in (
        '"analysis_mode" varchar(32) not null default \'playwright_e2e\'',
        '"analysis_mode_snapshot" varchar(32) not null default \'playwright_e2e\'',
    ):
        assert column in normalized
