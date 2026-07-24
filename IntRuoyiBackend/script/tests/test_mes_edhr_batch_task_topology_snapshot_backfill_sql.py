from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = (
    REPO_ROOT
    / "sql"
    / "mysql"
    / "20260710_mes_edhr_batch_task_topology_snapshot_backfill.sql"
)


def test_migration_backfills_edhr_topology_by_distinct_route_process() -> None:
    sql = MIGRATION.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260710_mes_schedule_order_topology_snapshot_backfill; "
        "type=data; riskLevel=medium"
    )
    assert "node_type = 'ROUTE_FORM'" in sql
    assert (
        "GROUP BY task.tenant_id, task.batch_execution_id, task.route_process_id"
        in sql
    )
    assert "LAG(process_snapshot.route_process_id) OVER" in sql
    assert (
        "PARTITION BY process_snapshot.tenant_id, process_snapshot.batch_execution_id"
        in sql
    )
    assert "legacy eDHR topology snapshot is partially populated" in sql
    assert "legacy eDHR topology snapshot has ambiguous process ordering" in sql
    assert "IF(backfill.predecessor_route_process_id IS NULL, b'1', b'0')" in sql
    assert "eDHR topology snapshot backfill verification failed" in sql
    assert "DELETE FROM" not in sql.upper()
    assert "TRUNCATE TABLE" not in sql.upper()
    assert "DROP TABLE `mes_pro_edhr_batch_execution_task`" not in sql
