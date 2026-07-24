from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_recordbook_management.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "CR-T2 eDHR recordbook schema SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_recordbook_schema_declares_template_book_entry_tag_and_event_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_recordbook_template",
        "mes_pro_edhr_recordbook",
        "mes_pro_edhr_recordbook_entry",
        "mes_pro_edhr_controlled_tag",
        "mes_pro_edhr_recordbook_tag_binding",
        "mes_pro_edhr_recordbook_event",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`template_code` varchar(64) NOT NULL",
        "`template_name` varchar(128) NOT NULL",
        "`template_version` varchar(32) NOT NULL",
        "`recordbook_type` varchar(64) NOT NULL",
        "`entry_schema_json` longtext NOT NULL",
        "`tag_policy_json` longtext DEFAULT NULL",
        "`status` varchar(32) NOT NULL",
        "`active_by` bigint DEFAULT NULL",
        "`active_at` datetime DEFAULT NULL",
    ]:
        assert column in text

    for column in [
        "`recordbook_code` varchar(96) NOT NULL",
        "`recordbook_name` varchar(128) NOT NULL",
        "`template_id` bigint NOT NULL",
        "`template_code` varchar(64) NOT NULL",
        "`template_version` varchar(32) NOT NULL",
        "`owner_user_id` bigint DEFAULT NULL",
        "`owner_dept_id` bigint DEFAULT NULL",
        "`business_scope` varchar(64) DEFAULT NULL",
        "`business_object_code` varchar(96) DEFAULT NULL",
        "`opened_at` datetime NOT NULL",
        "`entry_count` int NOT NULL",
    ]:
        assert column in text

    for column in [
        "`entry_code` varchar(96) NOT NULL",
        "`recordbook_id` bigint NOT NULL",
        "`entry_title` varchar(160) NOT NULL",
        "`entry_content_json` longtext NOT NULL",
        "`tag_snapshot_json` longtext DEFAULT NULL",
        "`submitted_by` bigint DEFAULT NULL",
        "`submitted_at` datetime DEFAULT NULL",
        "`locked_at` datetime DEFAULT NULL",
        "`idempotency_key` varchar(128) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`tag_code` varchar(64) NOT NULL",
        "`tag_name` varchar(128) NOT NULL",
        "`tag_status` varchar(32) NOT NULL",
        "`bound_by` bigint DEFAULT NULL",
        "`bound_at` datetime DEFAULT NULL",
        "`event_type` varchar(64) NOT NULL",
        "`from_status` varchar(32) DEFAULT NULL",
        "`to_status` varchar(32) DEFAULT NULL",
        "`event_snapshot_json` longtext DEFAULT NULL",
    ]:
        assert column in text


def test_recordbook_schema_enforces_uniqueness_indexes_and_tenant_isolation() -> None:
    text = read_sql()

    for fragment in [
        "UNIQUE KEY `uk_mes_pro_edhr_recordbook_template_code` (`tenant_id`, `template_code`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_recordbook_code` (`tenant_id`, `recordbook_code`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_recordbook_entry_code` (`tenant_id`, `entry_code`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_recordbook_entry_idempotency` (`tenant_id`, `recordbook_id`, `idempotency_key`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_controlled_tag_code` (`tenant_id`, `tag_code`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_recordbook_tag_binding` (`tenant_id`, `entry_id`, `tag_code`, `deleted`)",
        "KEY `idx_mes_pro_edhr_recordbook_owner` (`tenant_id`, `owner_user_id`, `status`, `deleted`)",
        "KEY `idx_mes_pro_edhr_recordbook_entry_book` (`tenant_id`, `recordbook_id`, `status`, `deleted`)",
        "KEY `idx_mes_pro_edhr_recordbook_tag_status` (`tenant_id`, `tag_status`, `deleted`)",
        "KEY `idx_mes_pro_edhr_recordbook_event_entry` (`tenant_id`, `entry_id`, `occurred_at`)",
    ]:
        assert fragment in text


def test_recordbook_schema_declares_menu_permissions_and_fail_fast_merge() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-recordbook-template:query",
        "mes:pro-edhr-recordbook-template:create",
        "mes:pro-edhr-recordbook-template:activate",
        "mes:pro-edhr-recordbook:query",
        "mes:pro-edhr-recordbook:create",
        "mes:pro-edhr-recordbook-entry:query",
        "mes:pro-edhr-recordbook-entry:create",
        "mes:pro-edhr-recordbook-entry:save",
        "mes:pro-edhr-recordbook-entry:submit",
        "mes:pro-edhr-tag:query",
        "mes:pro-edhr-tag:create",
        "mes:pro-edhr-tag:activate",
        "mes:pro-edhr-tag:disable",
    ]:
        assert permission in text

    for menu_id in [
        "900301",
        "900302",
        "900303",
        "900304",
        "900305",
        "900306",
        "900307",
        "900308",
        "900309",
        "900310",
        "900311",
        "900312",
        "900313",
        "900314",
    ]:
        assert menu_id in text

    assert "/mes/pro/edhr-recordbook" in text
    assert "mes/pro/edhr-recordbook/RecordbookPage" in text
    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR recordbook menus" in text
    assert "Missing eDHR recordbook system_menu rows; cannot merge tenant package menu_ids" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_recordbook_schema_avoids_batch_form_signature_or_approval_shortcuts() -> None:
    text = read_sql().upper()

    forbidden_fragments = [
        "DROP TABLE",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "MES_PRO_BATCH_RECORD_EXECUTION_ID",
        "MES_PRO_EDHR_FORM_INSTANCE_ID",
        "SIGNATURE_PASSWORD",
        "BPM_TASK",
        "DEFAULT_SUCCESS",
        "MOCK_TAG",
    ]
    for fragment in forbidden_fragments:
        assert fragment not in text
