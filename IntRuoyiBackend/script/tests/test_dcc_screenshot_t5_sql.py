from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_dcc_screenshot_t5_migration_adds_distribution_receipt_comment() -> None:
    migration_path = REPO_ROOT / "sql" / "mysql" / "20260525_dcc_screenshot_t5.sql"
    text = migration_path.read_text(encoding="utf-8")

    assert "ALTER TABLE dcc_controlled_file_distribution_recipient" in text
    assert "ADD COLUMN ack_comment varchar(1000) NULL" in text
    assert "AFTER acknowledged_at" in text
    assert "ALTER TABLE dcc_controlled_file_distribution" in text
    assert "ADD COLUMN recovered_by bigint NULL" in text
    assert "ADD COLUMN recovered_at datetime NULL" in text


def test_dcc_screenshot_schema_contains_t5_distribution_receipt_column() -> None:
    schema_paths = [
        REPO_ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql",
        REPO_ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "create_tables.sql",
    ]
    for schema_path in schema_paths:
        text = schema_path.read_text(encoding="utf-8").lower()
        assert "ack_comment" in text
        assert "recovered_by" in text
        assert "recovered_at" in text
