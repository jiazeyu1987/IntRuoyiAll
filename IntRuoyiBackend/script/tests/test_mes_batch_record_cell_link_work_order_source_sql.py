from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260726_mes_batch_record_cell_link_work_order_source.sql"
WIDTHS_SQL_PATH = (
    REPO_ROOT / "sql" / "mysql" / "20260830_mes_batch_record_cell_link_structured_source_widths.sql"
)


def test_work_order_source_migration_guards_the_table_and_each_column() -> None:
    migration = SQL_PATH.read_text(encoding="utf-8")

    assert "CREATE PROCEDURE ensure_mes_batch_record_cell_link_work_order_source_columns" in migration
    assert "TABLE_NAME = 'mes_pro_batch_record_cell_link_rule'" in migration
    assert "SET MESSAGE_TEXT = 'mes_pro_batch_record_cell_link_rule is missing'" in migration
    for column_name in ("source_type", "source_field_code", "source_field_name"):
        assert f"COLUMN_NAME = '{column_name}'" in migration
    assert migration.count("IF NOT EXISTS (") == 4
    assert migration.count("ALTER TABLE `mes_pro_batch_record_cell_link_rule`") == 3


def test_work_order_source_migration_keeps_width_upgrade_in_the_later_migration() -> None:
    migration = SQL_PATH.read_text(encoding="utf-8")
    widths_migration = WIDTHS_SQL_PATH.read_text(encoding="utf-8")

    assert "ADD COLUMN `source_type` varchar(32) NOT NULL DEFAULT 'BATCH_RECORD_CELL'" in migration
    assert "ADD COLUMN `source_field_code` varchar(1024) DEFAULT NULL" in migration
    assert "ADD COLUMN `source_field_name` varchar(255) DEFAULT NULL" in migration
    assert "MODIFY COLUMN" not in migration
    assert "dependsOn=20260726_mes_batch_record_cell_link_work_order_source" in widths_migration
    assert "MODIFY COLUMN `source_field_code` varchar(1024)" in widths_migration
    assert "MODIFY COLUMN `source_field_name` varchar(255)" in widths_migration
