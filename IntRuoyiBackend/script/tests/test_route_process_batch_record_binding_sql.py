from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_route_process_mysql_base_schema_declares_batch_record_binding_column() -> None:
    schema_path = REPO_ROOT / "sql" / "mysql" / "20260512_mes_base_schema.sql"
    text = schema_path.read_text(encoding="utf-8")

    assert "`mes_pro_route_process`" in text
    assert "`batch_record_report_id` varchar(64) DEFAULT NULL" in text


def test_route_process_mysql_binding_migration_is_present() -> None:
    migration_path = REPO_ROOT / "sql" / "mysql" / "20260522_mes_route_process_batch_record_binding.sql"
    text = migration_path.read_text(encoding="utf-8")

    assert "ALTER TABLE `mes_pro_route_process`" in text
    assert "ADD COLUMN `batch_record_report_id` varchar(64) DEFAULT NULL" in text
