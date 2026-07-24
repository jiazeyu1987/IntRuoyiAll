from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260612_mes_edhr_void_reopen_supplement.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "EDHR 作废/重开/补录 SQL 迁移必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_change_event_table_is_append_only_and_indexed() -> None:
    text = read_sql()
    normalized = " ".join(text.lower().split())

    assert "create table if not exists `mes_pro_edhr_record_change_event`" in normalized
    for column in [
        "`change_code` varchar(64) not null",
        "`change_type` varchar(32) not null",
        "`target_scope` varchar(32) not null",
        "`batch_execution_id` bigint default null",
        "`execution_id` bigint default null",
        "`source_execution_id` bigint default null",
        "`new_execution_id` bigint default null",
        "`source_archive_id` bigint default null",
        "`new_archive_id` bigint default null",
        "`change_status` varchar(32) not null",
        "`reason_category` varchar(64) not null",
        "`reason_text` varchar(500) not null",
        "`request_signature_id` bigint default null",
        "`approval_signature_id` bigint default null",
        "`previous_head_hash` char(64) default null",
        "`new_head_hash` char(64) default null",
        "`previous_archive_hash` char(64) default null",
        "`new_archive_hash` char(64) default null",
    ]:
        assert column in normalized

    for index_name in [
        "uk_edhr_change_code",
        "idx_edhr_change_execution",
        "idx_edhr_change_batch",
        "idx_edhr_change_source_execution",
    ]:
        assert index_name in normalized


def test_execution_and_archive_extensions_are_declared() -> None:
    text = read_sql().lower()

    for column in [
        "voided_by_change_event_id",
        "reopened_by_change_event_id",
        "supplement_source_execution_id",
        "supplement_reason",
        "supplement_flag",
        "effective_replaced_by_execution_id",
        "superseded_by_archive_id",
        "invalidated_by_change_event_id",
        "archive_valid_flag",
        "archive_valid_status",
    ]:
        assert column in text

    assert "mes_pro_batch_record_execution_archive" in text
    assert "mes_pro_edhr_batch_execution_archive" in text


def test_migration_is_idempotent_fail_fast_and_has_no_delete_or_upsert() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "INFORMATION_SCHEMA.TABLES" in upper_text
    assert "INFORMATION_SCHEMA.COLUMNS" in upper_text
    assert "INFORMATION_SCHEMA.STATISTICS" in upper_text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "DELETE FROM `MES_PRO_EDHR_RECORD_CHANGE_EVENT`" not in upper_text
