from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_traveler_instance_binding.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "CR-T3 traveler schema SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_traveler_schema_declares_template_instance_and_event_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_traveler_template",
        "mes_pro_edhr_traveler_instance",
        "mes_pro_edhr_traveler_event",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`template_code` varchar(64) NOT NULL",
        "`template_name` varchar(128) NOT NULL",
        "`template_version` varchar(32) NOT NULL",
        "`status` varchar(32) NOT NULL",
        "`applicable_product_code` varchar(64) DEFAULT NULL",
        "`applicable_route_id` bigint DEFAULT NULL",
        "`applicable_process_id` bigint DEFAULT NULL",
        "`active_at` datetime DEFAULT NULL",
    ]:
        assert column in text

    for column in [
        "`traveler_code` varchar(96) NOT NULL",
        "`template_id` bigint NOT NULL",
        "`batch_execution_id` bigint NOT NULL",
        "`work_order_id` bigint NOT NULL",
        "`work_order_code` varchar(64) NOT NULL",
        "`batch_code` varchar(128) NOT NULL",
        "`serial_no` varchar(128) DEFAULT NULL",
        "`route_id` bigint NOT NULL",
        "`route_code` varchar(64) DEFAULT NULL",
        "`route_process_id` bigint NOT NULL",
        "`process_id` bigint NOT NULL",
        "`process_code` varchar(64) NOT NULL",
        "`process_name` varchar(128) NOT NULL",
        "`status` varchar(32) NOT NULL",
        "`print_status` varchar(32) NOT NULL",
        "`business_key_hash` char(64) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`traveler_id` bigint NOT NULL",
        "`event_type` varchar(64) NOT NULL",
        "`result_status` varchar(32) NOT NULL",
        "`failure_reason` varchar(500) DEFAULT NULL",
        "`operator_user_id` bigint DEFAULT NULL",
        "`occurred_at` datetime NOT NULL",
    ]:
        assert column in text


def test_traveler_schema_enforces_business_uniqueness_and_query_indexes() -> None:
    text = read_sql()

    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_traveler_business` "
        "(`tenant_id`, `business_key_hash`, `deleted`)"
    ) in text
    assert "UNIQUE KEY `uk_mes_pro_edhr_traveler_code` (`tenant_id`, `traveler_code`, `deleted`)" in text
    assert "KEY `idx_mes_pro_edhr_traveler_batch_process` (`tenant_id`, `batch_execution_id`, `route_process_id`)" in text
    assert "KEY `idx_mes_pro_edhr_traveler_event_traveler` (`tenant_id`, `traveler_id`, `occurred_at`)" in text


def test_traveler_schema_declares_menu_permissions_and_fail_fast_merge() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-traveler-template:query",
        "mes:pro-edhr-traveler-template:create",
        "mes:pro-edhr-traveler-template:activate",
        "mes:pro-edhr-traveler:query",
        "mes:pro-edhr-traveler:generate",
    ]:
        assert permission in text

    for menu_id in ["900266", "900267", "900268", "900269", "900270", "900271"]:
        assert menu_id in text

    assert "/mes/pro/feedback/edhr-traveler" in text
    assert "mes/pro/edhr-traveler/TravelerPage" in text
    assert "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR traveler menus" in text
    assert "Missing eDHR traveler system_menu rows; cannot merge tenant package menu_ids" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_traveler_schema_avoids_silent_overwrite_or_print_success_shortcuts() -> None:
    text = read_sql().upper()

    forbidden_fragments = [
        "DROP TABLE",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DELETE FROM `SYSTEM_MENU`",
        "PRINT_SUCCESS",
        "REPRINT",
        "DEDUCT_PRINT",
    ]
    for fragment in forbidden_fragments:
        assert fragment not in text
