from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260911_dcc_controlled_file_submit_idempotency.sql"


def test_submit_idempotency_migration_is_repeatable_and_validates_existing_index():
    sql = SQL.read_text(encoding="utf-8")

    assert "type=schema" in sql
    assert "CREATE PROCEDURE ensure_dcc_controlled_file_submit_idempotency" in sql
    assert "information_schema.COLUMNS" in sql
    assert "information_schema.STATISTICS" in sql
    assert "IF v_column_count = 0 THEN" in sql
    assert "IF v_existing_index_count = 0 THEN" in sql
    assert "Existing DCC submit idempotency index has an incompatible definition" in sql
    assert "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)" in sql
    assert "CALL ensure_dcc_controlled_file_submit_idempotency();" in sql


def test_submit_idempotency_migration_has_no_unconditional_schema_additions():
    sql = SQL.read_text(encoding="utf-8")
    assert "ADD COLUMN `submit_idempotency_key` VARCHAR(128) NULL AFTER `process_definition_key`," not in sql
    assert "ADD COLUMN `submit_payload_hash` CHAR(64) NULL AFTER `submit_idempotency_key`," not in sql
    assert "ADD UNIQUE KEY `uk_dcc_file_submit_idempotency`\n    (`tenant_id`, `submitter_id`, `submit_idempotency_key`, `deleted`);" not in sql
