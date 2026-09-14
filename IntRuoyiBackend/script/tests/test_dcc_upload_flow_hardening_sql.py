from pathlib import Path


SQL_ROOT = Path(__file__).resolve().parents[2] / "sql" / "mysql"


def read_sql(name: str) -> str:
    return (SQL_ROOT / name).read_text(encoding="utf-8")


def test_submit_idempotency_migration_is_repeatable():
    sql = read_sql("20260911_dcc_controlled_file_submit_idempotency.sql")
    assert "information_schema.COLUMNS" in sql
    assert "information_schema.STATISTICS" in sql
    assert "COLUMN_NAME = 'submit_idempotency_key'" in sql
    assert "COLUMN_NAME = 'creation_idempotency_key'" in sql
    assert "INDEX_NAME = 'uk_dcc_file_creation_idempotency'" in sql
    assert "INDEX_NAME = 'uk_dcc_file_submit_idempotency'" in sql


def test_upload_ticket_category_migration_is_repeatable():
    sql = read_sql("20260911_dcc_upload_ticket_category_guard.sql")
    assert "COLUMN_NAME = 'category_id'" in sql
    assert "INDEX_NAME = 'idx_dcc_temp_category'" in sql
    assert "DCC_UPLOAD_TICKET_CATEGORY_TABLE_MISSING" in sql


def test_direct_publish_policy_migration_fails_when_target_policy_is_missing():
    sql = read_sql("20260911_dcc_publish_direct_policy.sql")
    assert "DCC_PUBLISH_POLICY_TARGET_MISSING" in sql
    assert "IF NOT EXISTS" in sql
    assert "`policy_mode` = 'DIRECT'" in sql
