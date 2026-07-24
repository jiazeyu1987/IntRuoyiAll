from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "sql" / "mysql" / "20260710_mes_schedule_order_topology_snapshot_backfill.sql"


def test_migration_backfills_only_legacy_linear_schedule_order_snapshots() -> None:
    sql = MIGRATION.read_text(encoding="utf-8")

    assert "dependsOn=20260710_mes_route_process_single_entry_multi_exit" in sql
    assert "LAG(snapshot.route_process_id) OVER" in sql
    assert "PARTITION BY snapshot.tenant_id, snapshot.schedule_order_id" in sql
    assert "COUNT(DISTINCT route_process_id) <> COUNT(*)" in sql
    assert "legacy schedule order topology snapshot contains null route process" in sql
    assert "IF(backfill.predecessor_route_process_id IS NULL, b'1', b'0')" in sql
