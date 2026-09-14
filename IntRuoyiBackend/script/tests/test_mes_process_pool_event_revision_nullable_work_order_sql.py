import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = (
    REPO_ROOT
    / "sql/mysql/20260813_mes_process_pool_event_revision_nullable_work_order.sql"
)


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing process-pool event revision nullable work-order migration"
    return SQL_PATH.read_text(encoding="utf-8")


def compact(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).strip()


def test_migration_has_release_metadata_and_explicit_schema_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260808_mes_process_pool_frontline_no_work_order; "
        "type=schema; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_mes_pp_event_revision_nullable_work_order_20260813" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Missing mes_pro_process_pool_event_revision table" in sql
    assert "Missing mes_pro_process_pool_event_revision.work_order_id column" in sql
    assert "Event revision work_order_id nullable contract mismatch" in sql


def test_migration_makes_only_revision_work_order_nullable_and_is_idempotent() -> None:
    sql = read_sql()
    flat = compact(sql)

    assert "table_name = 'mes_pro_process_pool_event_revision'" in flat
    assert "column_name = 'work_order_id'" in flat
    assert "AND is_nullable = 'NO'" in flat
    assert (
        "ALTER TABLE `mes_pro_process_pool_event_revision` "
        "MODIFY COLUMN `work_order_id` bigint DEFAULT NULL"
    ) in flat
    assert "is_nullable <> 'YES'" in flat


def test_migration_does_not_fabricate_or_rewrite_work_order_context() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    assert "DEFAULT 0" not in upper_sql
    assert "COALESCE" not in upper_sql
    assert "UPDATE `MES_PRO_PROCESS_POOL_EVENT_REVISION`" not in upper_sql
    assert "INSERT INTO `MES_PRO_PROCESS_POOL_EVENT_REVISION`" not in upper_sql
    assert "DELETE FROM `MES_PRO_PROCESS_POOL_EVENT_REVISION`" not in upper_sql
    assert "DROP TABLE `MES_PRO_PROCESS_POOL_EVENT_REVISION`" not in upper_sql
