from pathlib import Path


SQL_ROOT = Path(__file__).parents[2] / "sql" / "mysql"
LEGACY_MIGRATION = SQL_ROOT / "20260710_mes_route_process_single_entry_multi_exit.sql"
CORRECTION_MIGRATION = SQL_ROOT / "20260826_mes_route_process_allow_multi_predecessor.sql"


def test_route_process_schema_must_not_recreate_single_incoming_constraint() -> None:
    legacy_sql = LEGACY_MIGRATION.read_text(encoding="utf-8")
    correction_sql = CORRECTION_MIGRATION.read_text(encoding="utf-8")

    assert "route process flow contains multiple incoming edges" not in legacy_sql
    assert "ADD UNIQUE INDEX `uk_mes_route_process_flow_target`" not in legacy_sql
    assert "DROP INDEX `uk_mes_route_process_flow_target`" in correction_sql
    assert "idx_mes_route_process_flow_edge_target" in correction_sql
    assert "predecessor_route_process_ids_json" in correction_sql
