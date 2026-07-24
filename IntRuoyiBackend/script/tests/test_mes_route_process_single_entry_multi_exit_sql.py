from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = (
    REPO_ROOT / "sql/mysql/20260710_mes_route_process_single_entry_multi_exit.sql"
)


def read_migration() -> str:
    return MIGRATION_SQL.read_text(encoding="utf-8")


def test_route_process_single_entry_migration_declares_release_and_rollback_contract() -> None:
    sql = read_migration()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260709_mes_route_process_flow_graph; type=schema; riskLevel=medium"
    )
    assert "Rollback: DROP INDEX uk_mes_route_process_flow_target" in sql
    assert "Rollback: ALTER TABLE mes_pro_schedule_order_process" in sql
    assert "Rollback: ALTER TABLE mes_pro_edhr_batch_execution_task" in sql


def test_route_process_single_entry_migration_is_idempotent_and_fail_fast() -> None:
    sql = read_migration()

    assert "information_schema.tables" in sql
    assert "information_schema.columns" in sql
    assert "information_schema.statistics" in sql
    assert "route process flow contains multiple incoming edges" in sql
    assert "HAVING COUNT(*) > 1" in sql
    assert (
        "ADD UNIQUE INDEX `uk_mes_route_process_flow_target`\n"
        "      (`tenant_id`, `route_id`, `target_route_process_id`, `deleted`)"
    ) in sql
    for table_name in (
        "mes_pro_schedule_order_process",
        "mes_pro_edhr_batch_execution_task",
    ):
        assert table_name in sql
    for column_name in (
        "predecessor_route_process_id",
        "root_process_flag",
    ):
        assert column_name in sql
    assert "DROP TABLE" not in sql.upper()
    assert "TRUNCATE TABLE" not in sql.upper()
    assert "DELETE FROM" not in sql.upper()
