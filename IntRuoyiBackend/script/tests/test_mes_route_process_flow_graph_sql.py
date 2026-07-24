from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260709_mes_route_process_flow_graph.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"missing migration: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_flow_graph_migration_has_release_metadata_and_fail_fast_precheck():
    sql = read_sql()

    assert "-- release-migration:" in sql
    assert "dependsOn=20260512_mes_base_schema" in sql
    assert "riskLevel=medium" in sql
    assert "CREATE PROCEDURE intruoyi_create_mes_route_process_flow_graph()" in sql
    assert "table_name = 'mes_pro_route_process'" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "route process flow graph migration missing mes_pro_route_process" in sql


def test_route_flow_graph_migration_creates_tenant_scoped_edge_and_layout_tables():
    sql = read_sql()

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_process_flow_edge`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_process_flow_layout`" in sql
    assert "`source_route_process_id` bigint NOT NULL" in sql
    assert "`target_route_process_id` bigint NOT NULL" in sql
    assert "`route_process_id` bigint NOT NULL COMMENT '路线工序ID'" in sql
    assert "`graph_version` bigint NOT NULL DEFAULT 1" in sql
    assert "`tenant_id` bigint NOT NULL DEFAULT 0" in sql


def test_route_flow_graph_migration_keeps_active_relation_keys_unique():
    sql = read_sql()

    assert (
        "UNIQUE KEY `uk_mes_route_process_flow_edge` "
        "(`tenant_id`, `route_id`, `source_route_process_id`, `target_route_process_id`, `deleted`)"
    ) in sql
    assert (
        "UNIQUE KEY `uk_mes_route_process_flow_layout` "
        "(`tenant_id`, `route_id`, `route_process_id`, `deleted`)"
    ) in sql
    assert "KEY `idx_mes_route_process_flow_edge_route` (`tenant_id`, `route_id`, `graph_version`)" in sql
    assert "KEY `idx_mes_route_process_flow_edge_source` (`tenant_id`, `source_route_process_id`)" in sql
    assert "KEY `idx_mes_route_process_flow_edge_target` (`tenant_id`, `target_route_process_id`)" in sql
    assert "KEY `idx_mes_route_process_flow_layout_route` (`tenant_id`, `route_id`, `graph_version`)" in sql


def test_route_flow_graph_migration_declares_explicit_rollback():
    sql = read_sql()

    assert "-- Rollback:" in sql
    assert "DROP TABLE `mes_pro_route_process_flow_layout`" in sql
    assert "DROP TABLE `mes_pro_route_process_flow_edge`" in sql
