from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260907_mes_pro_work_order_ref_no.sql"


def _sql_text() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_work_order_ref_no_migration_adds_nullable_column_idempotently() -> None:
    sql = _sql_text()

    assert sql.startswith("-- release-migration:")
    assert "TABLE_NAME = 'mes_pro_work_order'" in sql
    assert "COLUMN_NAME = 'ref_no'" in sql
    assert "`ref_no` varchar(128) DEFAULT NULL" in sql
    assert "PREPARE mes_pro_work_order_ref_no_stmt" in sql
    assert "DROP COLUMN" not in sql.upper()
    assert "UPDATE `mes_pro_work_order`" not in sql


def test_work_order_ref_no_migration_fails_when_required_table_is_missing() -> None:
    sql = _sql_text()

    assert "missing required table mes_pro_work_order" in sql
    assert "SIGNAL SQLSTATE ''45000''" in sql
