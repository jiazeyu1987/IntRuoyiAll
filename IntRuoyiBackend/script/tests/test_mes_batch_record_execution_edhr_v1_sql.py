from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_mes_batch_record_execution_edhr_v1_migration_is_present() -> None:
    migration_path = REPO_ROOT / "sql" / "mysql" / "20260523_mes_batch_record_execution_edhr_v1.sql"
    text = migration_path.read_text(encoding="utf-8")

    assert "ALTER TABLE `mes_pro_batch_record_execution`" in text
    assert "ADD COLUMN `route_process_id` bigint DEFAULT NULL" in text
    assert "ADD COLUMN `task_id` bigint DEFAULT NULL" in text
    assert "ADD COLUMN `workstation_id` bigint DEFAULT NULL" in text
    assert "ADD COLUMN `batch_record_report_id` varchar(64) DEFAULT NULL" in text
    assert "ADD COLUMN `execution_snapshot_json` longtext" in text
    assert "MODIFY COLUMN `template_id` bigint DEFAULT NULL" in text
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_signature`" in text


def test_mes_batch_record_execution_base_schema_matches_edhr_runtime() -> None:
    schema_path = REPO_ROOT / "sql" / "mysql" / "20260512_mes_base_schema.sql"
    text = schema_path.read_text(encoding="utf-8")

    assert "`route_process_id` bigint DEFAULT NULL" in text
    assert "`task_id` bigint DEFAULT NULL" in text
    assert "`workstation_id` bigint DEFAULT NULL" in text
    assert "`batch_record_report_id` varchar(64) DEFAULT NULL" in text
    assert "`execution_snapshot_json` longtext" in text
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_signature`" in text
