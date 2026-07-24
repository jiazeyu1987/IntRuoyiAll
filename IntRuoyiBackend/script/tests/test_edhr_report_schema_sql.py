import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_report_catalog.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR report catalog SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_report_catalog_schema_declares_core_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_dataset_definition",
        "mes_pro_edhr_report_catalog",
        "mes_pro_edhr_report_definition",
        "mes_pro_edhr_export_audit",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`dataset_code` varchar(64) NOT NULL",
        "`dataset_version` varchar(32) NOT NULL",
        "`source_owner` varchar(128) NOT NULL",
        "`field_schema_json` longtext NOT NULL",
        "`join_key_json` longtext NOT NULL",
        "`sensitive_field_json` longtext DEFAULT NULL",
        "`permission_policy_json` longtext NOT NULL",
        "`caliber_version` varchar(32) NOT NULL",
        "`data_source_status` varchar(32) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`report_code` varchar(64) NOT NULL",
        "`report_category` varchar(64) NOT NULL",
        "`business_purpose` varchar(500) NOT NULL",
        "`primary_dimensions` varchar(500) NOT NULL",
        "`related_dimensions` varchar(500) NOT NULL",
        "`data_source_summary` varchar(500) NOT NULL",
        "`permission_policy` varchar(500) NOT NULL",
        "`export_policy` varchar(500) NOT NULL",
        "`acceptance_status` varchar(32) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`report_type` varchar(32) NOT NULL",
        "`dataset_id` bigint NOT NULL",
        "`dataset_code` varchar(64) NOT NULL",
        "`field_caliber_json` longtext NOT NULL",
        "`filter_schema_json` longtext NOT NULL",
        "`drilldown_target_json` longtext NOT NULL",
        "`permission_summary_json` longtext NOT NULL",
        "`sample_query_json` longtext DEFAULT NULL",
    ]:
        assert column in text

    for column in [
        "`operation_type` varchar(32) NOT NULL",
        "`filter_snapshot_json` longtext NOT NULL",
        "`permission_summary_json` longtext NOT NULL",
        "`data_range_summary` varchar(500) NOT NULL",
        "`result_status` varchar(32) NOT NULL",
        "`operator_user_id` bigint DEFAULT NULL",
        "`operator_username` varchar(64) DEFAULT NULL",
        "`occurred_at` datetime NOT NULL",
    ]:
        assert column in text

    assert "`tenant_id` bigint NOT NULL DEFAULT 0" in text
    assert "@mes_edhr_report_test_tenant_id" in text


def test_report_catalog_schema_seeds_twelve_commercial_reports_and_permissions() -> None:
    text = read_sql()

    for report_code in [
        "PRODUCTION_TRACE",
        "INSPECTION_TRACE",
        "TRANSACTION_RECORD",
        "WORK_REPORT_RECORD",
        "SCRAP_RECORD",
        "DHR_TRACE",
        "FORM_TRACE",
        "RECORDBOOK_TRACE",
        "CONSUMPTION_RECORD",
        "INVENTORY_LIST",
        "INVENTORY_LEDGER",
        "MESSAGE_RECORD",
    ]:
        assert f"'{report_code}'" in text

    assert "mes:pro-edhr-report:query" in text
    assert "mes:pro-edhr-report:export" in text
    assert "mes/pro/edhr-report/ReportPage" in text
    assert "MesProEdhrReport" in text
    assert "'/mes/pro/feedback/edhr-report'" in text
    assert "`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`" in text
    assert "900220" in text
    assert "'system', 'system', 0" not in text


def test_report_catalog_schema_fails_fast_for_menu_and_tenant_package_merge() -> None:
    text = read_sql()

    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR report menus" in text
    assert "Missing eDHR report system_menu rows; cannot merge tenant package menu_ids" in text
    assert "Missing unique 测试租户; cannot merge eDHR report menus" in text
    assert "Missing 测试租户 eDHR parent menu 900220; cannot merge report menus" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "`tenant`.`name` = '测试租户'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "system_tenant_package" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_report_catalog_schema_avoids_silent_overwrite_patterns() -> None:
    text = read_sql()

    assert not re.search(r"INSERT\s+IGNORE", text, flags=re.IGNORECASE)
    assert not re.search(r"ON\s+DUPLICATE\s+KEY\s+UPDATE", text, flags=re.IGNORECASE)
    assert "DEFAULT 'SUCCESS'" not in text.upper()
