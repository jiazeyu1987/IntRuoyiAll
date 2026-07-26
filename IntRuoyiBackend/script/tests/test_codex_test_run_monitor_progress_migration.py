from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260726_system_codex_test_run_monitor_progress.sql"
CREATE_TABLES = ROOT / "yudao-module-system" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def read_sql(path: Path) -> str:
    assert path.exists(), f"Missing SQL script: {path}"
    return path.read_text(encoding="utf-8")


def normalized_sql(path: Path) -> str:
    return read_sql(path).replace("`", "").lower()


def test_run_monitor_progress_migration_declares_idempotent_schema_change() -> None:
    sql = read_sql(SQL_PATH)
    normalized = normalized_sql(SQL_PATH)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260724_system_codex_test_management; type=schema; riskLevel=medium\n"
    )
    assert "CREATE PROCEDURE ensure_system_codex_test_execution_case_progress" in sql
    assert "ALTER TABLE `system_codex_test_execution_case`" in sql
    for column in (
        "progress_phase varchar(32) null",
        "current_method_sort int null",
        "current_checkpoint_sort int null",
        "progress_message varchar(512) null",
    ):
        assert column in normalized


def test_run_monitor_progress_test_schema_matches_runtime_columns() -> None:
    normalized = normalized_sql(CREATE_TABLES)

    for column in (
        '"progress_phase" varchar(32)',
        '"current_method_sort" int',
        '"current_checkpoint_sort" int',
        '"progress_message" varchar(512)',
    ):
        assert column in normalized
