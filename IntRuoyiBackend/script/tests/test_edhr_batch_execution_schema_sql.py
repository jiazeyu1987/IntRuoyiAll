from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260608_edhr_batch_execution_schema.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_batch_execution_schema_declares_batch_flow_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_batch_execution",
        "mes_pro_edhr_batch_execution_task",
        "mes_pro_edhr_batch_execution_signature",
        "mes_pro_edhr_batch_execution_archive",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`work_order_id` bigint NOT NULL",
        "`batch_code` varchar(128) NOT NULL",
        "`route_id` bigint NOT NULL",
        "`task_total` int NOT NULL DEFAULT 0",
        "`task_approved_count` int NOT NULL DEFAULT 0",
        "`blocked_count` int NOT NULL DEFAULT 0",
        "`aggregate_hash` char(64) DEFAULT NULL",
        "`close_signature_id` bigint DEFAULT NULL",
        "`closed_at` datetime DEFAULT NULL",
        "`reject_signature_id` bigint DEFAULT NULL",
        "`rejected_by` bigint DEFAULT NULL",
        "`rejected_at` datetime DEFAULT NULL",
        "`reject_reason` varchar(500) DEFAULT NULL",
    ]:
        assert column in text

    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_batch_execution_context` "
        "(`tenant_id`, `work_order_id`, `batch_code`, `route_id`, `deleted`)"
    ) in text
    assert "ensure_mes_edhr_batch_execution_quality_reject_columns" in text
    assert "ADD COLUMN `reject_signature_id` bigint DEFAULT NULL" in text
    assert "ADD COLUMN `rejected_by` bigint DEFAULT NULL" in text
    assert "ADD COLUMN `rejected_at` datetime DEFAULT NULL" in text
    assert "ADD COLUMN `reject_reason` varchar(500) DEFAULT NULL" in text


def test_edhr_batch_execution_schema_declares_task_signature_archive_contracts() -> None:
    text = read_sql()

    for column in [
        "`route_process_id` bigint NOT NULL",
        "`route_process_sort` int NOT NULL",
        "`batch_record_report_id` varchar(64) DEFAULT NULL",
        "`batch_record_sort` int NOT NULL DEFAULT 1",
        "`execution_id` bigint DEFAULT NULL",
        "`required_flag` bit(1) NOT NULL DEFAULT b'1'",
        "`blocker_code` varchar(64) DEFAULT NULL",
        "`blocker_message` varchar(500) DEFAULT NULL",
    ]:
        assert column in text

    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_batch_task_process_report` "
        "(`tenant_id`, `batch_execution_id`, `route_process_id`, `batch_record_sort`, `deleted`)"
    ) in text
    assert "KEY `idx_mes_pro_edhr_batch_task_execution` (`tenant_id`, `execution_id`)" in text

    for column in [
        "`action_type` varchar(32) NOT NULL",
        "`password_verified` bit(1) NOT NULL DEFAULT b'0'",
        "`signature_challenge_hash` char(64) DEFAULT NULL",
        "`aggregate_hash` char(64) DEFAULT NULL",
        "`archive_status` varchar(32) NOT NULL",
        "`content_hash` char(64) DEFAULT NULL",
        "`source_manifest_json` longtext DEFAULT NULL",
        "`sealed_signature_id` bigint DEFAULT NULL",
    ]:
        assert column in text


def test_edhr_batch_execution_permissions_and_tenant_package_merge_are_fail_fast() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-batch-execution:query",
        "mes:pro-edhr-batch-execution:create",
        "mes:pro-edhr-batch-execution:update",
        "mes:pro-edhr-batch-execution:close",
        "mes:pro-edhr-batch-execution:quality-reject",
        "mes:pro-edhr-batch-execution:overview",
        "mes:pro-edhr-batch-execution-archive:query",
        "mes:pro-edhr-batch-execution-archive:create",
        "mes:pro-edhr-batch-execution-archive:download",
    ]:
        assert permission in text

    assert (
        "`id` IN (900033, 900034, 900035, 900036, 900037, 900038, 900039, 900040, 900041, 900042)"
    ) in text
    assert "COUNT(*) FROM `tmp_edhr_batch_execution_menu_ids`) <> 10" in text
    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR batch execution menus" in text
    assert "Missing eDHR batch execution system_menu rows; cannot merge tenant package menu_ids" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_edhr_batch_execution_schema_avoids_silent_overwrite_patterns() -> None:
    text = read_sql().upper()

    assert "ON DUPLICATE KEY UPDATE" not in text
    assert "INSERT IGNORE INTO `SYSTEM_MENU`" not in text
