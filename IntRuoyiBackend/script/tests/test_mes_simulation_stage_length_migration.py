from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260829_mes_simulation_stage_length.sql"

SIMULATION_STAGE_TABLES = [
    "mes_pro_process_pool_active_order",
    "mes_pro_process_pool_active_order_process_snapshot",
    "mes_pro_process_pool_active_order_pick_list_binding",
    "mes_pro_process_pool_active_order_pick_list_binding_item",
    "mes_pro_process_pool_event",
    "mes_pro_process_pool_pqc_record",
    "mes_pro_process_pool_quantity_fragment",
    "mes_pro_process_pool_submission_review",
    "mes_pro_process_pool_report_allocation",
    "mes_pqc_inspection_task",
    "mes_pqc_inspection_piece_detail",
    "mes_pqc_process_inspection_aggregate_detail",
]


def test_mes_simulation_stage_columns_are_widened_for_explicit_stage4_mode() -> None:
    assert SQL_PATH.exists(), "Stage4 independent input mode requires a formal schema migration."

    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "-- release-migration:" in sql
    assert "dependsOn=20260825_mes_stage1_simulation_metadata" in sql
    assert "ensure_mes_simulation_stage_length" in sql
    assert "CHARACTER_MAXIMUM_LENGTH < 64" in sql
    assert "MODIFY COLUMN `simulation_stage` varchar(64)" in sql
    assert "STAGE4_INDEPENDENT_BATCH_EXECUTION" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_mes_simulation_stage_length" in sql

    for table_name in SIMULATION_STAGE_TABLES:
        assert (
            f"CALL ensure_mes_simulation_stage_length('{table_name}')" in sql
        ), f"{table_name}.simulation_stage must be widened with an idempotent guard."
