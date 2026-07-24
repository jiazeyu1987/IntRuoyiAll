from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260710_mes_route_process_flow_boundary_edge.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"missing migration: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_boundary_edge_migration_declares_release_contract():
    sql = read_sql()

    assert "-- release-migration:" in sql
    assert "dependsOn=20260709_mes_route_process_flow_graph" in sql
    assert "riskLevel=medium" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "route process boundary edge migration missing flow graph tables" in sql


def test_boundary_edge_migration_creates_tenant_scoped_boundary_table():
    sql = read_sql()

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_process_flow_boundary_edge`" in sql
    assert "`boundary_type` varchar(16) NOT NULL COMMENT '边界类型：START、END'" in sql
    assert "`route_process_id` bigint NOT NULL COMMENT '关联路线工序ID'" in sql
    assert "`graph_version` bigint NOT NULL DEFAULT 1" in sql
    assert "`sort` int DEFAULT NULL" in sql
    assert "`tenant_id` bigint NOT NULL DEFAULT 0" in sql
    assert (
        "UNIQUE KEY `uk_mes_route_process_flow_boundary_edge` "
        "(`tenant_id`, `route_id`, `boundary_type`, `route_process_id`, `deleted`)"
    ) in sql


def test_boundary_edge_migration_removes_obsolete_single_predecessor_index():
    sql = read_sql()
    normalized_sql = " ".join(sql.split())

    assert "FROM information_schema.statistics" in sql
    assert "index_name = 'uk_mes_route_process_flow_target'" in sql
    assert (
        "ALTER TABLE `mes_pro_route_process_flow_edge` "
        "DROP INDEX `uk_mes_route_process_flow_target`"
    ) in normalized_sql


def test_boundary_edge_migration_only_backfills_unambiguous_real_graphs():
    sql = read_sql()

    assert "INSERT INTO `mes_pro_route_process_flow_boundary_edge`" in sql
    assert "'START' AS `boundary_type`" in sql
    assert "'END' AS `boundary_type`" in sql
    assert "HAVING COUNT(*) = 1" in sql
    assert "EXISTS (" in sql
    assert "FROM `mes_pro_route_process_flow_edge`" in sql


def test_boundary_edge_migration_declares_explicit_rollback():
    sql = read_sql()

    assert "-- Rollback:" in sql
    assert "DROP TABLE `mes_pro_route_process_flow_boundary_edge`" in sql
