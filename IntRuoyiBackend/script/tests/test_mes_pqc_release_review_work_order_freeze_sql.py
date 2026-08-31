from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260831_mes_pqc_release_review_work_order_freeze.sql"


def test_review_freeze_snapshot_migration_is_release_safe():
    sql = SQL.read_text(encoding="utf-8")
    first_line = sql.splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260831_mes_pqc_release_nonconformance_scope; type=schema; riskLevel=medium"
    )
    assert "information_schema.TABLES" in sql
    assert "information_schema.COLUMNS" in sql
    assert "previous_work_order_temporary_frozen" in sql
    assert "review_status` = 'pending_review'" in sql
    assert "lacks auditable previous work-order freeze state" in sql
    assert "ADD COLUMN `previous_work_order_temporary_frozen` bit(1) DEFAULT NULL" in sql
    assert "DROP PROCEDURE IF EXISTS" in sql
