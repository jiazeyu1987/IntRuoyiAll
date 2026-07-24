from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_dcc_screenshot_t1_migration_adds_submit_metadata_columns() -> None:
    migration_path = REPO_ROOT / "sql" / "mysql" / "20260525_dcc_screenshot_t1.sql"
    text = migration_path.read_text(encoding="utf-8")

    assert "ALTER TABLE dcc_controlled_file" in text
    assert "ADD COLUMN drawing_pdf_file_id bigint NULL" in text
    assert "ADD COLUMN product_code varchar(32) NULL" in text
    assert "ADD COLUMN need_training bit(1) NOT NULL DEFAULT b'0'" in text
    assert "ADD COLUMN process_type varchar(32) NOT NULL DEFAULT 'CONTROLLED_FILE'" in text
