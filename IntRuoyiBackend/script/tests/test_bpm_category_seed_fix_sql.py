from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql/mysql/20260714_bpm_category_seed_fix.sql"


def read_sql() -> str:
    return SQL.read_text(encoding="utf-8")


def test_bpm_category_seed_fix_sql_exists_and_uses_utf8mb4():
    sql = read_sql()

    assert "SET NAMES utf8mb4" in sql
    assert "????" not in sql


def test_bpm_category_seed_fix_repairs_existing_oa_garbled_name():
    sql = read_sql()

    assert "`bpm_category`" in sql
    assert "'OA'" in sql
    assert "'办公审批'" in sql
    assert "REGEXP '^[?]+$'" in sql


def test_bpm_category_seed_fix_adds_batch_record_category_for_active_tenants():
    sql = read_sql()

    assert "`system_tenant`" in sql
    assert "'BATCH_RECORD'" in sql
    assert "'批记录'" in sql
    assert "批记录升版审批流程分类" in sql
    assert "NOT EXISTS" in sql
