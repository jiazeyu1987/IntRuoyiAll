from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_print_policy_reissue_void.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "CR-T3-03 print policy/reissue/void schema SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_print_policy_schema_declares_policy_reprint_copy_and_export_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_print_policy",
        "mes_pro_edhr_reprint_request",
        "mes_pro_edhr_print_history_copy",
        "mes_pro_edhr_print_export_audit",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`policy_code` varchar(64) NOT NULL",
        "`policy_name` varchar(128) NOT NULL",
        "`business_type` varchar(64) NOT NULL",
        "`template_type` varchar(64) NOT NULL",
        "`first_print_limit` int NOT NULL",
        "`reprint_limit` int NOT NULL",
        "`reason_dict_json` longtext NOT NULL",
        "`watermark_template` varchar(255) NOT NULL",
        "`void_copy_watermark` varchar(255) NOT NULL",
        "`status` varchar(32) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`request_code` varchar(96) NOT NULL",
        "`print_task_id` bigint NOT NULL",
        "`original_print_task_id` bigint NOT NULL",
        "`reprint_reason_code` varchar(64) NOT NULL",
        "`reprint_reason` varchar(500) NOT NULL",
        "`used_reprint_count` int NOT NULL",
        "`reprint_limit` int NOT NULL",
        "`watermark_text` varchar(255) NOT NULL",
        "`status` varchar(32) NOT NULL",
        "`idempotency_key` varchar(128) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`copy_code` varchar(96) NOT NULL",
        "`source_print_task_id` bigint NOT NULL",
        "`source_object_type` varchar(64) NOT NULL",
        "`source_object_code` varchar(128) NOT NULL",
        "`copy_reason` varchar(500) NOT NULL",
        "`watermark_text` varchar(255) NOT NULL",
        "`evidence_hash` char(64) NOT NULL",
        "`idempotency_key` varchar(128) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`export_code` varchar(96) NOT NULL",
        "`filter_snapshot_json` longtext NOT NULL",
        "`result_status` varchar(32) NOT NULL",
        "`evidence_hash` char(64) NOT NULL",
        "`idempotency_key` varchar(128) NOT NULL",
        "`exported_at` datetime NOT NULL",
    ]:
        assert column in text


def test_print_policy_schema_enforces_idempotency_status_and_audit_indexes() -> None:
    text = read_sql()

    for index in [
        "UNIQUE KEY `uk_mes_pro_edhr_print_policy_code` (`tenant_id`, `policy_code`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_print_policy_scope` (`tenant_id`, `business_type`, `template_type`, `status`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_reprint_idempotency` (`tenant_id`, `idempotency_key`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_history_copy_idempotency` (`tenant_id`, `idempotency_key`, `deleted`)",
        "UNIQUE KEY `uk_mes_pro_edhr_print_export_idempotency` (`tenant_id`, `idempotency_key`, `deleted`)",
        "KEY `idx_mes_pro_edhr_reprint_original` (`tenant_id`, `original_print_task_id`, `create_time`)",
        "KEY `idx_mes_pro_edhr_history_copy_source` (`tenant_id`, `source_print_task_id`, `create_time`)",
    ]:
        assert index in text

    for status in [
        "DRAFT",
        "ACTIVE",
        "DISABLED",
        "REQUESTED",
        "VOID_HISTORY_COPY",
        "EXPORT_RECORDED",
    ]:
        assert status in text

    for event_type in [
        "PRINT_POLICY_CREATED",
        "PRINT_POLICY_ACTIVATED",
        "PRINT_REPRINT_POLICY_ACCEPTED",
        "PRINT_VOID_HISTORY_COPY_CREATED",
        "PRINT_HISTORY_EXPORTED",
    ]:
        assert event_type in text


def test_print_policy_schema_declares_menu_permissions_and_fail_fast_merge() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-print-policy:query",
        "mes:pro-edhr-print-policy:create",
        "mes:pro-edhr-print-policy:activate",
        "mes:pro-edhr-print-task:reprint",
        "mes:pro-edhr-print-task:history-copy",
        "mes:pro-edhr-print-task:export",
    ]:
        assert permission in text

    for menu_id in [
        "900338",
        "900339",
        "900340",
        "900341",
        "900342",
        "900343",
        "900344",
        "900345",
        "900346",
    ]:
        assert menu_id in text

    assert "/mes/pro/feedback/edhr-print-policy" in text
    assert "mes/pro/edhr-label-print/LabelPrintQueuePage" in text
    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR print policy menus" in text
    assert "Missing eDHR print policy system_menu rows; cannot merge tenant package menu_ids" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "tenant_admin" in text


def test_print_policy_schema_forbids_fake_print_success_or_destructive_shortcuts() -> None:
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


def test_print_policy_schema_uses_dedicated_menu_ids_and_avoids_existing_ranges() -> None:
    text = read_sql()

    for menu_id in [
        "900338",
        "900339",
        "900340",
        "900341",
        "900342",
        "900343",
        "900344",
        "900345",
        "900346",
    ]:
        assert menu_id in text

    for conflicting_id in [
        "900284",
        "900285",
        "900286",
        "900287",
        "900288",
        "900289",
        "900290",
        "900291",
        "900292",
    ]:
        assert conflicting_id not in text
