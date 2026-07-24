from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260612_mes_edhr_attachment_ledger.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR 填写过程附件证据账本 SQL 迁移必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_attachment_ledger_table_declares_required_evidence_columns() -> None:
    text = read_sql()
    normalized = " ".join(text.lower().split())

    assert "create table if not exists `mes_pro_batch_record_execution_attachment`" in normalized
    for column in [
        "`execution_id` bigint not null",
        "`batch_execution_id` bigint not null",
        "`batch_task_id` bigint not null",
        "`work_task_id` bigint default null",
        "`row_index` int not null",
        "`column_index` int not null",
        "`field_key` varchar(128) not null",
        "`field_path` varchar(255) not null",
        "`field_label` varchar(255) default null",
        "`attachment_type` varchar(32) not null",
        "`attachment_group_key` varchar(64) not null",
        "`attachment_action` varchar(32) not null",
        "`version_no` int not null",
        "`file_id` bigint not null",
        "`file_url` varchar(1024) not null",
        "`storage_config_id` bigint not null",
        "`storage_path` varchar(512) not null",
        "`file_name` varchar(255) not null",
        "`content_type` varchar(128) not null",
        "`file_size` bigint not null",
        "`sha256` char(64) not null",
        "`storage_retention_json` json default null",
        "`storage_retention_hash` char(64) default null",
        "`audit_batch_id` bigint default null",
        "`signature_id` bigint default null",
        "`previous_attachment_hash` char(64) default null",
        "`attachment_hash` char(64) not null",
        "`operator_id` bigint not null",
        "`operator_name` varchar(64) not null",
        "`operated_at` datetime not null",
        "`reason_category` varchar(64) not null",
        "`reason_text` varchar(500) not null",
        "`tenant_id` bigint not null default 0",
    ]:
        assert column in normalized


def test_attachment_ledger_table_declares_indexes_and_constraints() -> None:
    text = read_sql()
    normalized = " ".join(text.lower().split())

    for constraint in [
        "unique key `uk_mes_pro_bre_attach_version`",
        "unique key `uk_mes_pro_bre_attach_hash`",
        "key `idx_mes_pro_bre_attach_execution_field`",
        "key `idx_mes_pro_bre_attach_batch`",
        "key `idx_mes_pro_bre_attach_work_task`",
        "key `idx_mes_pro_bre_attach_signature`",
    ]:
        assert constraint in normalized

    assert "`tenant_id`, `execution_id`, `field_path`, `field_key`, `row_index`, `column_index`, `attachment_group_key`, `version_no`" in normalized
    assert "`tenant_id`, `execution_id`, `attachment_hash`" in normalized


def test_attachment_ledger_migration_is_idempotent_fail_fast_and_append_only() -> None:
    text = read_sql()
    upper_text = text.upper()

    for required_table in [
        "mes_pro_batch_record_execution",
        "mes_pro_edhr_batch_execution",
        "mes_pro_edhr_batch_execution_task",
        "mes_pro_edhr_work_task",
    ]:
        assert required_table in text

    assert "INFORMATION_SCHEMA.TABLES" in upper_text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "cannot apply eDHR attachment ledger migration" in text
    assert "DROP TABLE" not in upper_text
    assert "TRUNCATE TABLE" not in upper_text
    assert "DELETE FROM `MES_PRO_BATCH_RECORD_EXECUTION_ATTACHMENT`" not in upper_text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
