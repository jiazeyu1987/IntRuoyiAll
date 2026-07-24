from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SCHEMA_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260525_edhr_archive_schema.sql"
WORM_GUARD_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260528_edhr_archive_worm_guard.sql"
PERMISSION_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260525_edhr_archive_permission.sql"


def test_edhr_archive_schema_sql_declares_archive_tables() -> None:
    text = SCHEMA_SQL_PATH.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_archive`" in text
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_archive_event`" in text
    assert "`approval_snapshot_id` bigint DEFAULT NULL" in text
    assert "`approval_snapshot_hash` char(64) DEFAULT NULL" in text
    assert "UNIQUE KEY `uk_archive_code` (`tenant_id`, `archive_code`)" in text
    assert "KEY `idx_execution_type_version` (`tenant_id`, `execution_id`, `artifact_type`, `archive_version`)" in text
    assert "KEY `idx_archive_event_execution_type` (`tenant_id`, `execution_id`, `event_type`)" in text


def test_edhr_archive_schema_sql_declares_worm_guards() -> None:
    base_schema = SCHEMA_SQL_PATH.read_text(encoding="utf-8")
    text = WORM_GUARD_SQL_PATH.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_archive`" not in text
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_archive_event`" not in text
    for trigger in [
        "trg_execution_archive_sealed_no_update",
        "trg_execution_archive_sealed_no_delete",
        "trg_execution_archive_event_no_update",
        "trg_execution_archive_event_no_delete",
    ]:
        assert f"DROP TRIGGER IF EXISTS `{trigger}`" in text
        assert f"CREATE TRIGGER `{trigger}`" in text
        assert "SIGNAL SQLSTATE '45000'" in text

    assert "TABLE_NAME = 'mes_pro_batch_record_execution_archive'" in text
    assert "TABLE_NAME = 'mes_pro_batch_record_execution_archive_event'" in text
    assert "Missing mes_pro_batch_record_execution_archive; apply eDHR archive schema first" in text
    assert "Missing mes_pro_batch_record_execution_archive_event; apply eDHR archive schema first" in text
    assert "OLD.archive_status = 'SEALED'" in text
    assert "eDHR SEALED execution archives are immutable" in text
    assert "eDHR archive events are append-only" in text
    assert text.index("CALL ensure_mes_batch_record_archive_worm_guard_prerequisites()") < text.index(
        "CREATE TRIGGER `trg_execution_archive_sealed_no_update`"
    )
    assert "trg_execution_archive_sealed_no_update" not in base_schema


def test_edhr_archive_permission_sql_restores_archive_permissions() -> None:
    text = PERMISSION_SQL_PATH.read_text(encoding="utf-8")

    for permission in [
        "mes:pro-batch-record-execution-archive:query",
        "mes:pro-batch-record-execution-archive:create",
        "mes:pro-batch-record-execution-archive:download",
    ]:
        assert permission in text

    assert "mes:pro-batch-record-execution-archive:delete" not in text
    assert "UPDATE `system_tenant_package`" in text
    assert "INSERT INTO `system_role_menu`" in text
