import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260526_edhr_field_audit_schema.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_field_audit_schema_adds_execution_projection_columns() -> None:
    text = read_sql()

    for column in [
        "`cell_values_hash` char(64)",
        "`field_audit_revision` bigint",
        "`field_audit_head_hash` char(64)",
        "`field_audit_last_batch_id` bigint",
    ]:
        assert column in text

    assert "TABLE_NAME = 'mes_pro_batch_record_execution'" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing mes_pro_batch_record_execution" in text


def test_field_audit_schema_extends_archive_projection_columns() -> None:
    text = read_sql()

    assert "TABLE_NAME = 'mes_pro_batch_record_execution_archive'" in text
    assert "Missing mes_pro_batch_record_execution_archive" in text
    for column in [
        "`field_audit_revision` bigint",
        "`field_audit_head_hash` char(64)",
    ]:
        assert column in text

    assert (
        "TABLE_NAME = 'mes_pro_batch_record_execution_archive' AND COLUMN_NAME = 'field_audit_revision'"
        in text
    )
    assert (
        "TABLE_NAME = 'mes_pro_batch_record_execution_archive' AND COLUMN_NAME = 'field_audit_head_hash'"
        in text
    )


def test_field_audit_schema_declares_batch_and_item_tables() -> None:
    text = read_sql()

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_field_audit_batch`" in text
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_execution_field_audit_item`" in text

    for column in [
        "`idempotency_key` varchar(64) NOT NULL",
        "`request_hash` char(64) NOT NULL",
        "`action_type` varchar(32) NOT NULL",
        "`reason_category` varchar(64) NOT NULL",
        "`reason_text` varchar(500) NOT NULL",
        "`signature_projection_hash` char(64) NOT NULL",
        "`base_cell_values_hash` char(64) NOT NULL",
        "`before_cell_values_hash` char(64) NOT NULL",
        "`after_cell_values_hash` char(64) NOT NULL",
        "`base_field_audit_revision` bigint NOT NULL",
        "`before_field_audit_revision` bigint NOT NULL",
        "`after_field_audit_revision` bigint NOT NULL",
        "`base_field_audit_head_hash` char(64) NOT NULL",
        "`previous_head_hash` char(64) NOT NULL",
        "`new_head_hash` char(64) NOT NULL",
        "`hash_verification_json` text NOT NULL",
        "`changed_at` datetime NOT NULL",
    ]:
        assert column in text

    for column in [
        "`field_path` varchar(512) NOT NULL",
        "`field_key` varchar(128) NOT NULL",
        "`field_label` varchar(255) NOT NULL",
        "`row_index` int NOT NULL",
        "`column_index` int NOT NULL",
        "`value_type` varchar(32) NOT NULL",
        "`old_value_json` longtext NOT NULL",
        "`old_value_display` varchar(1000) NOT NULL",
        "`old_value_hash` char(64) NOT NULL",
        "`new_value_json` longtext NOT NULL",
        "`new_value_display` varchar(1000) NOT NULL",
        "`new_value_hash` char(64) NOT NULL",
        "`previous_hash` char(64) NOT NULL",
        "`audit_hash` char(64) NOT NULL",
        "`changed_at` datetime NOT NULL",
    ]:
        assert column in text


def test_field_audit_schema_declares_indexes_permissions_and_append_only_triggers() -> None:
    text = read_sql()

    for index_name in [
        "uk_field_audit_batch_idempotency",
        "uk_field_audit_batch_signature",
        "idx_field_audit_batch_execution_revision",
        "idx_field_audit_batch_execution_time",
        "uk_field_audit_item_revision",
        "uk_field_audit_item_hash",
        "idx_field_audit_item_field",
        "idx_field_audit_item_actor_time",
        "idx_field_audit_item_batch",
    ]:
        assert f"`{index_name}`" in text

    for permission in [
        "mes:pro-batch-record-execution:field-audit-update",
        "mes:pro-batch-record-execution:field-audit-query",
        "mes:pro-batch-record-execution:field-audit-verify",
        "mes:pro-batch-record-execution:field-audit-export",
    ]:
        assert permission in text

    for trigger_name in [
        "trg_field_audit_batch_no_update",
        "trg_field_audit_batch_no_delete",
        "trg_field_audit_item_no_update",
        "trg_field_audit_item_no_delete",
    ]:
        assert trigger_name in text
        trigger_pattern = (
            rf"CREATE TRIGGER `{trigger_name}`"
            r".*?SIGNAL SQLSTATE '45000'"
        )
        assert re.search(trigger_pattern, text, flags=re.DOTALL | re.IGNORECASE)

    assert not re.search(
        r"ON\s+DUPLICATE\s+KEY\s+UPDATE",
        text,
        flags=re.IGNORECASE,
    )


def test_field_audit_schema_merges_permissions_into_existing_edhr_tenant_roles() -> None:
    text = read_sql()

    assert "ensure_edhr_field_audit_tenant_package_menus" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "JSON_TABLE" in text
    assert "system_tenant_package" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text
    assert "5700, 900002" in text
    assert "Missing eDHR field audit system_menu permissions" in text
    assert "CALL ensure_edhr_field_audit_tenant_package_menus();" in text

    for menu_id in ["900027", "900028", "900029", "900030"]:
        assert menu_id in text

    for updater in [
        "`package`.`updater` = 'edhr-field-audit-permission'",
        "`role_menu`.`updater` = 'edhr-field-audit-permission'",
    ]:
        assert updater in text


def test_field_audit_permission_merge_resolves_existing_menu_ids_by_permission() -> None:
    text = read_sql()

    assert "Duplicate eDHR field audit system_menu permissions" in text
    assert "COUNT(DISTINCT `permission`)" in text
    assert "GROUP BY `permission`" in text
    assert "UNIQUE KEY `uk_tmp_edhr_field_audit_permission_menu_id` (`id`)" in text
    assert "AND `id` IN (900027, 900028, 900029, 900030)" not in text
