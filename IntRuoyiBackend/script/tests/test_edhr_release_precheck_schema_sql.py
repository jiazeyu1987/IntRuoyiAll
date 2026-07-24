from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_release_precheck_engine.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "sql/mysql/20260618_mes_edhr_release_precheck_engine.sql must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_release_precheck_schema_declares_transaction_and_check_item_tables() -> None:
    text = read_sql()

    assert "-- release-migration: allowedEnvironments=test,backup,prod" in text
    for table_name in [
        "mes_pro_edhr_release_transaction",
        "mes_pro_edhr_release_check_item",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`release_code` varchar(64) NOT NULL",
        "`batch_execution_id` bigint NOT NULL",
        "`batch_execution_code` varchar(64) NOT NULL",
        "`work_order_code` varchar(64) DEFAULT NULL",
        "`batch_code` varchar(128) NOT NULL",
        "`product_code` varchar(64) DEFAULT NULL",
        "`product_name` varchar(255) DEFAULT NULL",
        "`dhr_status` varchar(32) NOT NULL",
        "`inspection_status` varchar(32) NOT NULL",
        "`deviation_status` varchar(32) NOT NULL",
        "`rework_status` varchar(32) NOT NULL",
        "`scrap_status` varchar(32) NOT NULL",
        "`inventory_status` varchar(32) NOT NULL",
        "`release_status` varchar(32) NOT NULL",
        "`required_check_count` int NOT NULL DEFAULT 0",
        "`failed_check_count` int NOT NULL DEFAULT 0",
        "`blocking_check_count` int NOT NULL DEFAULT 0",
        "`last_precheck_at` datetime DEFAULT NULL",
        "`precheck_snapshot_json` longtext DEFAULT NULL",
        "`submit_idempotency_key` varchar(128) DEFAULT NULL",
        "`submitted_by` bigint DEFAULT NULL",
        "`submitted_at` datetime DEFAULT NULL",
        "`approval_idempotency_key` varchar(128) DEFAULT NULL",
        "`approved_by` bigint DEFAULT NULL",
        "`approved_at` datetime DEFAULT NULL",
        "`approval_signoff_evidence_hash` char(64) DEFAULT NULL",
        "`approval_opinion` varchar(500) DEFAULT NULL",
        "`rejected_by` bigint DEFAULT NULL",
        "`rejected_at` datetime DEFAULT NULL",
        "`reject_reason` varchar(500) DEFAULT NULL",
        "`withdrawn_by` bigint DEFAULT NULL",
        "`withdrawn_at` datetime DEFAULT NULL",
        "`withdraw_reason` varchar(500) DEFAULT NULL",
        "`version` int NOT NULL DEFAULT 1",
    ]:
        assert column in text


def test_edhr_release_check_item_schema_keeps_drilldown_and_impact_context() -> None:
    text = read_sql()

    for column in [
        "`release_transaction_id` bigint NOT NULL",
        "`check_code` varchar(64) NOT NULL",
        "`check_category` varchar(64) NOT NULL",
        "`check_name` varchar(128) NOT NULL",
        "`check_result` varchar(32) NOT NULL",
        "`item_status` varchar(32) NOT NULL",
        "`severity` varchar(32) NOT NULL",
        "`responsibility_module` varchar(64) NOT NULL",
        "`source_object_type` varchar(64) DEFAULT NULL",
        "`source_object_id` varchar(128) DEFAULT NULL",
        "`source_object_code` varchar(128) DEFAULT NULL",
        "`source_record_url` varchar(500) DEFAULT NULL",
        "`failure_reason` varchar(500) NOT NULL",
        "`remediation_suggestion` varchar(500) NOT NULL",
        "`impact_scope_json` longtext DEFAULT NULL",
        "`evidence_hash` char(64) DEFAULT NULL",
        "`checked_at` datetime NOT NULL",
    ]:
        assert column in text

    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_release_transaction_batch` "
        "(`tenant_id`, `batch_execution_id`, `deleted`)"
    ) in text
    assert "KEY `idx_mes_pro_edhr_release_check_item_transaction` (`tenant_id`, `release_transaction_id`, `item_status`, `check_result`)" in text


def test_edhr_release_precheck_menu_permissions_and_tenant_package_gate_are_declared() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-release:query",
        "mes:pro-edhr-release:precheck",
        "mes:pro-edhr-release:submit",
        "mes:pro-edhr-release:approve",
        "mes:pro-edhr-release:intervene",
    ]:
        assert permission in text

    for menu_id in ["900260", "900261", "900262", "900263", "900264", "900265"]:
        assert menu_id in text

    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR release menus" in text
    assert "Missing eDHR release system_menu rows; cannot merge tenant package menu_ids" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_edhr_release_precheck_normalizes_legacy_submit_and_approve_permissions() -> None:
    text = read_sql()

    for fragment in [
        "UPDATE `system_menu`",
        "SET `name` = 'eDHR放行提交'",
        "`permission` = 'mes:pro-edhr-release:submit'",
        "WHERE `id` = 900263",
        "SET `name` = 'eDHR放行审批'",
        "`permission` = 'mes:pro-edhr-release:approve'",
        "WHERE `id` = 900264",
    ]:
        assert fragment in text


def test_edhr_release_schema_avoids_destructive_or_silent_overwrite_patterns() -> None:
    text = read_sql().upper()

    for forbidden in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "DELETE FROM",
        "ON DUPLICATE KEY UPDATE",
        "INSERT IGNORE",
    ]:
        assert forbidden not in text


def test_edhr_release_menu_merge_avoids_mysql_temp_table_reopen() -> None:
    text = read_sql()

    assert "tmp_mes_edhr_release_missing_package_menu_ids" in text
    assert "LEFT JOIN `tmp_mes_edhr_release_package_menu_ids` AS `existing`" not in text
