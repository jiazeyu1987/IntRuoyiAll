from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_label_print_queue.sql"
FORM_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_form_instance.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "CR-T3-02 label and print queue schema SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def read_form_sql() -> str:
    assert FORM_SQL_PATH.exists(), "eDHR form schema SQL must be delivered"
    return FORM_SQL_PATH.read_text(encoding="utf-8")


def test_label_print_schema_declares_label_template_instance_print_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_label_template",
        "mes_pro_edhr_label_instance",
        "mes_pro_edhr_print_task",
        "mes_pro_edhr_print_event",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`template_code` varchar(64) NOT NULL",
        "`template_name` varchar(128) NOT NULL",
        "`template_version` varchar(32) NOT NULL",
        "`business_object_type` varchar(64) NOT NULL",
        "`field_model_json` longtext NOT NULL",
        "`layout_json` longtext NOT NULL",
        "`parser_version` varchar(32) NOT NULL",
        "`watermark_template` varchar(255) DEFAULT NULL",
        "`status` varchar(32) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`label_code` varchar(96) NOT NULL",
        "`template_id` bigint NOT NULL",
        "`business_type` varchar(64) NOT NULL",
        "`business_object_id` bigint NOT NULL",
        "`business_object_code` varchar(128) NOT NULL",
        "`render_snapshot_json` longtext NOT NULL",
        "`parser_version` varchar(32) NOT NULL",
        "`print_status` varchar(32) NOT NULL",
        "`business_key_hash` char(64) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`task_code` varchar(96) NOT NULL",
        "`source_type` varchar(64) NOT NULL",
        "`source_object_id` bigint NOT NULL",
        "`source_object_code` varchar(128) NOT NULL",
        "`template_type` varchar(64) NOT NULL",
        "`label_instance_id` bigint DEFAULT NULL",
        "`traveler_id` bigint DEFAULT NULL",
        "`status` varchar(32) NOT NULL",
        "`print_confirm_status` varchar(32) NOT NULL",
        "`is_reprint` bit(1) NOT NULL",
        "`original_print_task_id` bigint DEFAULT NULL",
        "`reprint_reason` varchar(500) DEFAULT NULL",
        "`watermark_text` varchar(255) DEFAULT NULL",
        "`failure_reason` varchar(500) DEFAULT NULL",
        "`idempotency_key` varchar(128) NOT NULL",
        "`print_count_deducted` bit(1) NOT NULL",
    ]:
        assert column in text


def test_label_print_schema_enforces_idempotency_statuses_and_audit_indexes() -> None:
    text = read_sql()

    assert "UNIQUE KEY `uk_mes_pro_edhr_label_business` (`tenant_id`, `business_key_hash`, `deleted`)" in text
    assert "UNIQUE KEY `uk_mes_pro_edhr_print_task_idempotency` (`tenant_id`, `idempotency_key`, `deleted`)" in text
    assert "KEY `idx_mes_pro_edhr_print_task_owner` (`tenant_id`, `creator`, `status`, `create_time`)" in text
    assert "KEY `idx_mes_pro_edhr_print_event_task` (`tenant_id`, `print_task_id`, `occurred_at`)" in text

    for status in ["WAITING", "PRINTING", "PENDING_CONFIRM", "SUCCESS_CONFIRMED", "FAILED", "VOID_RESTRICTED"]:
        assert status in text

    for event_type in ["PRINT_REQUESTED", "PRINT_MARK_FAILED", "PRINT_CONFIRM_SUCCESS", "PRINT_REPRINT_REQUESTED"]:
        assert event_type in text


def test_label_print_schema_declares_menu_permissions_and_fail_fast_merge() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-label-template:query",
        "mes:pro-edhr-label-template:create",
        "mes:pro-edhr-label-template:activate",
        "mes:pro-edhr-label:query",
        "mes:pro-edhr-label:preview",
        "mes:pro-edhr-print-task:query",
        "mes:pro-edhr-print-task:create",
        "mes:pro-edhr-print-task:mark-failed",
        "mes:pro-edhr-print-task:confirm",
    ]:
        assert permission in text

    for menu_id in [
        "900320",
        "900321",
        "900322",
        "900323",
        "900324",
        "900325",
        "900326",
        "900327",
        "900328",
        "900329",
        "900330",
        "900331",
    ]:
        assert menu_id in text

    assert "/mes/pro/feedback/edhr-label" in text
    assert "/mes/pro/feedback/edhr-print-task" in text
    assert "mes/pro/edhr-label-print/LabelPrintQueuePage" in text
    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR label print menus" in text
    assert "Missing eDHR label print system_menu rows; cannot merge tenant package menu_ids" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_label_print_schema_uses_menu_ids_distinct_from_existing_edhr_ranges() -> None:
    text = read_sql()
    form_text = read_form_sql()

    for menu_id in ["900272", "900273", "900274", "900275", "900276", "900277", "900278", "900279"]:
        assert menu_id in form_text
        assert menu_id not in text

    for menu_id in ["900280", "900281", "900282", "900283"]:
        assert menu_id not in text


def test_label_print_schema_forbids_silent_success_or_destructive_shortcuts() -> None:
    text = read_sql().upper()

    forbidden_fragments = [
        "DROP TABLE",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DELETE FROM `SYSTEM_MENU`",
        "WINDOW.PRINT",
        "DEFAULT_SUCCESS",
        "MOCK_PRINT",
        "`PRINT_COUNT_DEDUCTED` BIT(1) NOT NULL DEFAULT B'1'",
    ]
    for fragment in forbidden_fragments:
        assert fragment not in text
