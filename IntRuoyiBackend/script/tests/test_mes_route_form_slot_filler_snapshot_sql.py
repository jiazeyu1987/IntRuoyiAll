from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260722_mes_route_form_slot_filler_snapshot.sql"
TEST_SCHEMA = REPO_ROOT / "yudao-module-mes" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def test_route_form_slot_filler_snapshot_declares_release_contract() -> None:
    sql = read(SQL_PATH)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260722_mes_route_form_center_runtime_columns; "
        "type=schema; riskLevel=medium"
    )
    assert "CREATE PROCEDURE ensure_mes_route_form_slot_filler_snapshot()" in sql
    assert "CALL ensure_mes_route_form_slot_filler_snapshot();" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_mes_route_form_slot_filler_snapshot;" in sql


def test_route_form_slot_filler_snapshot_is_additive_and_fail_fast() -> None:
    sql = read(SQL_PATH)
    upper = sql.upper()

    for destructive in ("DROP TABLE", "TRUNCATE TABLE", "DELETE FROM", "UPDATE `"):
        assert destructive not in upper

    assert "TABLE_NAME = 'mes_pro_route_flow_process_batch_record'" in sql
    assert "SET MESSAGE_TEXT = 'mes_pro_route_flow_process_batch_record is missing'" in sql


def test_route_form_slot_filler_snapshot_columns_exist_in_migration_and_schema() -> None:
    sql = read(SQL_PATH)
    schema = read(TEST_SCHEMA)

    for column in (
        "`candidate_source_type` varchar(32) DEFAULT NULL",
        "`candidate_source_ids` varchar(1000) DEFAULT NULL",
        "`candidate_source_names` varchar(1000) DEFAULT NULL",
    ):
        assert column in sql

    for column in (
        '"candidate_source_type" varchar(32) DEFAULT NULL',
        '"candidate_source_ids" varchar(1000) DEFAULT NULL',
        '"candidate_source_names" varchar(1000) DEFAULT NULL',
    ):
        assert column in schema
