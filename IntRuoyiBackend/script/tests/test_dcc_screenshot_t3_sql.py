from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_dcc_screenshot_t3_migration_adds_training_record_column() -> None:
    migration_path = REPO_ROOT / "sql" / "mysql" / "20260525_dcc_screenshot_t3.sql"
    text = migration_path.read_text(encoding="utf-8")

    assert "ALTER TABLE dcc_controlled_file" in text
    assert "ADD COLUMN training_record_file_id bigint NULL" in text
    assert "AFTER drawing_pdf_file_id" in text


def test_dcc_screenshot_schema_contains_all_t1_t3_columns() -> None:
    schema_paths = [
        REPO_ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql",
        REPO_ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "create_tables.sql",
    ]
    for schema_path in schema_paths:
        text = schema_path.read_text(encoding="utf-8").lower()
        assert "drawing_pdf_file_id" in text
        assert "training_record_file_id" in text
        assert "product_code" in text
        assert "need_training" in text
        assert "process_type" in text
