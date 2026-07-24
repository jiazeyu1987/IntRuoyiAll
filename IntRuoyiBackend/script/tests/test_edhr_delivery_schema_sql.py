import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_delivery_cockpit.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR delivery cockpit SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_delivery_schema_declares_project_package_and_gate_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_delivery_project",
        "mes_pro_edhr_evidence_package",
        "mes_pro_edhr_delivery_gate_item",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`project_code` varchar(64) NOT NULL",
        "`project_name` varchar(128) NOT NULL",
        "`customer_name` varchar(128) NOT NULL",
        "`site_name` varchar(128) NOT NULL",
        "`system_scope` varchar(500) NOT NULL",
        "`validation_scope` varchar(500) NOT NULL",
        "`release_tag` varchar(64) NOT NULL",
        "`schema_version` varchar(64) NOT NULL",
        "`target_environment` varchar(64) NOT NULL",
        "`project_status` varchar(32) NOT NULL",
        "`signoff_allowed` bit(1) NOT NULL DEFAULT b'0'",
        "`gate_summary_json` longtext NOT NULL",
    ]:
        assert column in text

    for column in [
        "`package_code` varchar(64) NOT NULL",
        "`package_name` varchar(128) NOT NULL",
        "`package_type` varchar(64) NOT NULL",
        "`package_status` varchar(32) NOT NULL",
        "`evidence_status` varchar(32) NOT NULL",
        "`required_evidence_json` longtext NOT NULL",
        "`available_evidence_json` longtext NOT NULL",
        "`missing_evidence_json` longtext NOT NULL",
        "`signoff_impact` varchar(500) NOT NULL",
        "`next_action` varchar(500) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`gate_code` varchar(64) NOT NULL",
        "`gate_name` varchar(128) NOT NULL",
        "`gate_status` varchar(32) NOT NULL",
        "`missing_evidence` varchar(500) NOT NULL",
        "`owner_name` varchar(128) NOT NULL",
        "`next_action` varchar(500) NOT NULL",
        "`signoff_impact` varchar(500) NOT NULL",
        "`blocking_flag` bit(1) NOT NULL DEFAULT b'1'",
    ]:
        assert column in text

    assert "`tenant_id` bigint NOT NULL DEFAULT 0" in text
    assert "`uk_mes_pro_edhr_delivery_project_code`" in text
    assert "`uk_mes_pro_edhr_evidence_package_code`" in text
    assert "`idx_mes_pro_edhr_delivery_gate_project`" in text


def test_delivery_schema_seeds_menu_permissions_and_test_tenant_gate() -> None:
    text = read_sql()

    for fragment in [
        "mes:pro-edhr-delivery:query",
        "mes:pro-edhr-delivery:create",
        "mes/pro/edhr-delivery/DeliveryPage",
        "MesProEdhrDelivery",
        "'/mes/pro/feedback/edhr-delivery'",
        "900220",
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "`tenant`.`name` = '测试租户'",
    ]:
        assert fragment in text

    for package_code in [
        "CSV_VALIDATION",
        "OQ_PQ",
        "TRAINING",
        "DEPLOYMENT_AUTH",
        "INTERFACE",
        "OPERATIONS",
    ]:
        assert package_code in text


def test_delivery_schema_fails_fast_for_menu_package_prerequisites() -> None:
    text = read_sql()

    for message in [
        "Missing unique 测试租户; cannot merge eDHR delivery menus",
        "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR delivery menus",
        "Missing 测试租户 eDHR parent menu 900220; cannot merge delivery menus",
        "Missing eDHR delivery system_menu rows; cannot merge tenant package menu_ids",
    ]:
        assert message in text

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text


def test_delivery_schema_avoids_silent_success_or_overwrite_patterns() -> None:
    text = read_sql()

    assert not re.search(r"INSERT\s+IGNORE", text, flags=re.IGNORECASE)
    assert not re.search(r"ON\s+DUPLICATE\s+KEY\s+UPDATE", text, flags=re.IGNORECASE)
    assert "DEFAULT 'SUCCESS'" not in text.upper()
