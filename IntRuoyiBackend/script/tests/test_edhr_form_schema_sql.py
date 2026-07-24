from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_form_instance.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "CR-T2 independent eDHR form schema SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_schema_declares_template_instance_value_and_event_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_form_template",
        "mes_pro_edhr_form_instance",
        "mes_pro_edhr_form_value",
        "mes_pro_edhr_form_event",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`template_code` varchar(64) NOT NULL",
        "`template_name` varchar(128) NOT NULL",
        "`template_version` varchar(32) NOT NULL",
        "`field_schema_json` longtext NOT NULL",
        "`status` varchar(32) NOT NULL",
        "`active_by` bigint DEFAULT NULL",
        "`active_at` datetime DEFAULT NULL",
    ]:
        assert column in text

    for column in [
        "`instance_code` varchar(96) NOT NULL",
        "`template_id` bigint NOT NULL",
        "`template_code` varchar(64) NOT NULL",
        "`template_version` varchar(32) NOT NULL",
        "`status` varchar(32) NOT NULL",
        "`version` int NOT NULL",
        "`business_scope` varchar(64) DEFAULT NULL",
        "`business_object_type` varchar(64) DEFAULT NULL",
        "`business_object_id` bigint DEFAULT NULL",
        "`business_object_code` varchar(96) DEFAULT NULL",
        "`submitted_by` bigint DEFAULT NULL",
        "`submitted_at` datetime DEFAULT NULL",
    ]:
        assert column in text

    for column in [
        "`instance_id` bigint NOT NULL",
        "`field_key` varchar(128) NOT NULL",
        "`field_label` varchar(128) NOT NULL",
        "`field_type` varchar(32) NOT NULL",
        "`value_text` varchar(1000) DEFAULT NULL",
        "`value_json` longtext DEFAULT NULL",
    ]:
        assert column in text

    for column in [
        "`instance_id` bigint DEFAULT NULL",
        "`template_id` bigint DEFAULT NULL",
        "`event_type` varchar(64) NOT NULL",
        "`result_status` varchar(32) NOT NULL",
        "`failure_reason` varchar(500) DEFAULT NULL",
        "`operator_user_id` bigint DEFAULT NULL",
        "`occurred_at` datetime NOT NULL",
    ]:
        assert column in text


def test_form_schema_enforces_uniqueness_indexes_and_tenant_isolation() -> None:
    text = read_sql()

    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_form_template_code` "
        "(`tenant_id`, `template_code`, `deleted`)"
    ) in text
    assert "UNIQUE KEY `uk_mes_pro_edhr_form_instance_code` (`tenant_id`, `instance_code`, `deleted`)" in text
    assert "UNIQUE KEY `uk_mes_pro_edhr_form_value_field` (`tenant_id`, `instance_id`, `field_key`, `deleted`)" in text
    assert "KEY `idx_mes_pro_edhr_form_template_status` (`tenant_id`, `status`, `deleted`)" in text
    assert "KEY `idx_mes_pro_edhr_form_instance_template` (`tenant_id`, `template_id`, `status`, `deleted`)" in text
    assert "KEY `idx_mes_pro_edhr_form_event_instance` (`tenant_id`, `instance_id`, `occurred_at`)" in text


def test_form_schema_declares_menu_permissions_and_fail_fast_merge() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-form-template:query",
        "mes:pro-edhr-form-template:create",
        "mes:pro-edhr-form-template:activate",
        "mes:pro-edhr-form-instance:query",
        "mes:pro-edhr-form-instance:create",
        "mes:pro-edhr-form-instance:save",
        "mes:pro-edhr-form-instance:submit",
    ]:
        assert permission in text

    for menu_id in ["900272", "900273", "900274", "900275", "900276", "900277", "900278"]:
        assert menu_id in text

    assert "/mes/pro/feedback/edhr-form" in text
    assert "mes/pro/edhr-form/FormPage" in text
    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR form menus" in text
    assert "Missing eDHR form system_menu rows; cannot merge tenant package menu_ids" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_form_schema_avoids_recordbook_signature_or_batch_record_shortcuts() -> None:
    text = read_sql().upper()

    forbidden_fragments = [
        "DROP TABLE",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "MES_PRO_EDHR_RECORDBOOK",
        "MES_PRO_EDHR_CONTROLLED_TAG",
        "SIGNATURE_PASSWORD",
        "BPM_TASK",
        "MES_PRO_BATCH_RECORD_EXECUTION_ID",
    ]
    for fragment in forbidden_fragments:
        assert fragment not in text
