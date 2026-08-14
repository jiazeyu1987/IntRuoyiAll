import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260809_mes_process_pool_active_order_manual_sort.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing active order manual sort schema migration"
    return SQL_PATH.read_text(encoding="utf-8")


def compact(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).strip()


def test_migration_has_release_metadata_and_fail_fast_schema_guards() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260808_mes_active_order_release_application; type=schema; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_mes_pp_active_order_manual_sort_20260809" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Missing mes_pro_process_pool_active_order" in sql
    assert "Active order sort_order backfill failed" in sql
    assert "Active order sort_order column contract mismatch" in sql


def test_migration_preserves_existing_visible_order_with_partitioned_backfill() -> None:
    sql = read_sql()
    flat = compact(sql)

    assert "ADD COLUMN `sort_order` bigint NULL" in flat
    assert "ROW_NUMBER() OVER (" in sql
    assert "PARTITION BY `tenant_id`, `leader_user_id`" in flat
    assert "ORDER BY `joined_at` ASC, `id` ASC" in flat
    assert "UPDATE `mes_pro_process_pool_active_order` AS `target`" in flat
    assert "SET `target`.`sort_order` = `ranked`.`sort_order`" in flat
    assert "MODIFY COLUMN `sort_order` bigint NOT NULL" in flat
    assert not re.search(r"`sort_order`\s+bigint\s+NOT\s+NULL\s+DEFAULT\s+0", sql, re.IGNORECASE)


def test_migration_adds_stable_leader_active_order_index_without_destructive_dml() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    assert "idx_mes_pp_active_order_manual_sort" in sql
    assert (
        "(`tenant_id`, `leader_user_id`, `active_status`, `sort_order`, `id`, `deleted`)"
        in compact(sql)
    )
    for forbidden in [
        "DELETE FROM `MES_PRO_PROCESS_POOL_ACTIVE_ORDER`",
        "TRUNCATE TABLE `MES_PRO_PROCESS_POOL_ACTIVE_ORDER`",
        "DROP TABLE `MES_PRO_PROCESS_POOL_ACTIVE_ORDER`",
    ]:
        assert forbidden not in upper_sql
