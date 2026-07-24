from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = (
    REPO_ROOT / "sql" / "mysql" / "20260722_mes_edhr_quality_terminal_reopen_reexecute.sql"
)
BASELINE_PATH = REPO_ROOT / "sql" / "mysql" / "20260608_edhr_batch_execution_schema.sql"
H2_SCHEMA_PATH = (
    REPO_ROOT / "yudao-module-mes" / "src" / "test" / "resources" / "sql" / "create_tables.sql"
)


TRACE_COLUMNS = [
    "attempt_no",
    "source_rejected_batch_execution_id",
    "superseded_by_batch_execution_id",
    "reexecuted_by_change_event_id",
]


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_quality_reexecute_migration_has_release_metadata_and_rollback() -> None:
    sql = _read(MIGRATION_PATH)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260714_mes_edhr_batch_execution_active_context; "
        "type=schema; riskLevel=medium"
    )
    assert "CREATE PROCEDURE intruoyi_upgrade_mes_edhr_quality_reexecute_trace()" in sql
    assert "missing mes_pro_edhr_batch_execution" in sql
    for column in TRACE_COLUMNS:
        assert f"column_name = '{column}'" in sql
        assert f"DROP COLUMN {column}" in sql


def test_quality_reexecute_migration_is_idempotent_and_traceable() -> None:
    sql = _read(MIGRATION_PATH)

    for column in TRACE_COLUMNS:
        assert f"ADD COLUMN `{column}`" in sql
    assert "`attempt_no` int NOT NULL DEFAULT 1" in sql
    assert "UPDATE `mes_pro_edhr_batch_execution`" in sql
    assert "SET `attempt_no` = 1" in sql
    assert "idx_mes_pro_edhr_batch_execution_source_rejected" in sql
    assert "ADD INDEX `idx_mes_pro_edhr_batch_execution_source_rejected`" in sql
    assert "DELETE FROM `mes_pro_edhr_batch_execution`" not in sql
    assert "UPDATE `mes_pro_edhr_batch_execution`\n     SET `status`" not in sql


def test_quality_reexecute_schema_is_synced_to_baseline_and_h2() -> None:
    baseline = _read(BASELINE_PATH)
    h2_schema = _read(H2_SCHEMA_PATH)

    for column in TRACE_COLUMNS:
        assert f"`{column}`" in baseline
        assert f'"{column}"' in h2_schema
    assert "`attempt_no` int NOT NULL DEFAULT 1" in baseline
    assert '"attempt_no" int NOT NULL DEFAULT 1' in h2_schema
