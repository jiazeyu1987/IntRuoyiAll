from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_init_batch_precheck.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "sql/mysql/20260618_mes_edhr_init_batch_precheck.sql must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_init_batch_schema_declares_first_slice_tables() -> None:
    text = read_sql()

    assert "-- release-migration: allowedEnvironments=test,backup,prod" in text
    for table_name in [
        "mes_pro_edhr_init_batch",
        "mes_pro_edhr_init_manifest",
        "mes_pro_edhr_init_issue",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`project_code` varchar(64) NOT NULL",
        "`project_name` varchar(255) NOT NULL",
        "`target_environment` varchar(32) NOT NULL",
        "`target_tenant_id` bigint NOT NULL",
        "`data_version` varchar(64) NOT NULL",
        "`owner_user_id` bigint NOT NULL",
        "`approval_owner_user_id` bigint NOT NULL",
        "`init_scope_json` longtext NOT NULL",
        "`status` varchar(32) NOT NULL",
        "`manifest_count` int NOT NULL DEFAULT 0",
        "`blocking_issue_count` int NOT NULL DEFAULT 0",
        "`last_precheck_at` datetime DEFAULT NULL",
        "`version` int NOT NULL DEFAULT 1",
    ]:
        assert column in text


def test_edhr_init_manifest_schema_is_idempotent_by_hash() -> None:
    text = read_sql()

    for column in [
        "`init_batch_id` bigint NOT NULL",
        "`package_type` varchar(64) NOT NULL",
        "`manifest_hash` char(64) NOT NULL",
        "`source_file_name` varchar(255) NOT NULL",
        "`source_file_url` varchar(512) DEFAULT NULL",
        "`file_size` bigint DEFAULT NULL",
        "`checksum_json` longtext DEFAULT NULL",
        "`manifest_json` longtext NOT NULL",
        "`upload_status` varchar(32) NOT NULL",
        "`uploaded_by` bigint DEFAULT NULL",
        "`uploaded_at` datetime NOT NULL",
    ]:
        assert column in text

    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_init_manifest_hash` "
        "(`tenant_id`, `init_batch_id`, `manifest_hash`, `deleted`)"
    ) in text


def test_edhr_init_issue_schema_keeps_source_location_responsible_owner_and_impact() -> None:
    text = read_sql()

    for column in [
        "`init_batch_id` bigint NOT NULL",
        "`init_manifest_id` bigint DEFAULT NULL",
        "`issue_code` varchar(64) NOT NULL",
        "`issue_level` varchar(32) NOT NULL",
        "`issue_status` varchar(32) NOT NULL",
        "`package_type` varchar(64) DEFAULT NULL",
        "`source_file_name` varchar(255) DEFAULT NULL",
        "`source_row_no` int DEFAULT NULL",
        "`source_field_name` varchar(128) DEFAULT NULL",
        "`object_type` varchar(64) DEFAULT NULL",
        "`object_key` varchar(128) DEFAULT NULL",
        "`responsible_user_id` bigint DEFAULT NULL",
        "`responsible_name` varchar(128) DEFAULT NULL",
        "`issue_message` varchar(500) NOT NULL",
        "`remediation_suggestion` varchar(500) DEFAULT NULL",
        "`impact_scope_json` longtext DEFAULT NULL",
    ]:
        assert column in text

    assert "KEY `idx_mes_pro_edhr_init_issue_batch` (`tenant_id`, `init_batch_id`, `issue_level`, `issue_status`)" in text


def test_edhr_init_batch_menu_permissions_and_tenant_package_gate_are_declared() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-init-batch:query",
        "mes:pro-edhr-init-batch:create",
        "mes:pro-edhr-init-batch:precheck",
        "mes:pro-edhr-init-batch:import",
        "mes:pro-edhr-init-batch:signoff",
    ]:
        assert permission in text

    for menu_id in ["900247", "900248", "900249", "900250", "900251", "900252"]:
        assert menu_id in text

    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR init batch menus" in text
    assert "Missing eDHR init batch system_menu rows; cannot merge tenant package menu_ids" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_edhr_init_batch_schema_avoids_destructive_or_silent_overwrite_patterns() -> None:
    text = read_sql().upper()

    for forbidden in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "DELETE FROM",
        "ON DUPLICATE KEY UPDATE",
        "INSERT IGNORE",
    ]:
        assert forbidden not in text


def test_edhr_init_batch_menu_merge_avoids_mysql_temp_table_reopen() -> None:
    text = read_sql()

    assert "tmp_mes_edhr_init_batch_missing_package_menu_ids" in text
    assert "LEFT JOIN `tmp_mes_edhr_init_batch_package_menu_ids` AS `existing`" not in text
