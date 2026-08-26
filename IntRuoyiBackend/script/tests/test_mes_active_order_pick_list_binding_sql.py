import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260822_mes_active_order_pick_list_binding.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing active order pick list binding schema migration"
    return SQL_PATH.read_text(encoding="utf-8")


def compact(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).strip()


def test_migration_has_release_metadata_and_both_binding_tables() -> None:
    sql = read_sql()
    first_line = sql.splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260730_mes_process_pool_foundation; type=schema; riskLevel=medium"
    )
    flat = compact(sql).lower()
    assert "create table if not exists mes_pro_process_pool_active_order_pick_list_binding (" in flat
    assert "create table if not exists mes_pro_process_pool_active_order_pick_list_binding_item (" in flat
    assert "unique key uk_active_order_pick_binding_active" in flat
    assert "unique key uk_active_order_pick_binding_item" in flat


def test_migration_is_idempotent_and_non_destructive() -> None:
    upper_sql = read_sql().upper()
    assert upper_sql.count("CREATE TABLE IF NOT EXISTS") == 2
    for forbidden in [
        "DROP TABLE MES_PRO_PROCESS_POOL_ACTIVE_ORDER_PICK_LIST_BINDING",
        "TRUNCATE TABLE MES_PRO_PROCESS_POOL_ACTIVE_ORDER_PICK_LIST_BINDING",
        "DELETE FROM MES_PRO_PROCESS_POOL_ACTIVE_ORDER_PICK_LIST_BINDING",
    ]:
        assert forbidden not in upper_sql
