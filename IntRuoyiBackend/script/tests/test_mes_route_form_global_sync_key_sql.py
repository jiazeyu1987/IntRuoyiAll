from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260817_mes_route_form_global_sync_key.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing route form global sync key migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_form_global_sync_key_migration_declares_release_contract() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260722_mes_route_form_center_runtime_columns; "
        "type=schema; riskLevel=low"
    )
    assert "CREATE PROCEDURE ensure_mes_route_form_global_sync_key()" in sql
    assert "CALL ensure_mes_route_form_global_sync_key();" in sql


def test_route_form_global_sync_key_migration_is_additive_and_idempotent() -> None:
    sql = read_sql()
    upper = sql.upper()

    for destructive in ("DROP TABLE", "TRUNCATE TABLE", "DELETE FROM", "UPDATE `"):
        assert destructive not in upper
    assert "COLUMN_NAME = 'global_sync_key'" in sql
    assert "ADD COLUMN `global_sync_key` varchar(128) DEFAULT NULL" in sql
    assert "INDEX_NAME = 'idx_mes_route_flow_global_sync'" in sql
    assert "ADD KEY `idx_mes_route_flow_global_sync`" in sql
    assert "(`tenant_id`, `route_id`, `use_type`, `global_sync_key`, `deleted`)" in sql
