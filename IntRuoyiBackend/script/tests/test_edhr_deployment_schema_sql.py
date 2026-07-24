import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_mes_edhr_deployment_license_interface.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR deployment/license/interface SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_deployment_schema_declares_evidence_and_gate_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_deployment_evidence",
        "mes_pro_edhr_deployment_gate_item",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`project_id` bigint NOT NULL",
        "`deployment_code` varchar(64) NOT NULL",
        "`deployment_name` varchar(128) NOT NULL",
        "`customer_project_name` varchar(128) NOT NULL",
        "`target_environment` varchar(128) NOT NULL",
        "`environment_authorized` bit(1) NOT NULL DEFAULT b'0'",
        "`environment_check_summary` text NOT NULL",
        "`server_summary` text NOT NULL",
        "`network_summary` text NOT NULL",
        "`object_storage_summary` text NOT NULL",
        "`capacity_summary` text NOT NULL",
        "`permission_summary` text NOT NULL",
        "`release_tag` varchar(64) NOT NULL",
        "`artifact_version` varchar(64) NOT NULL",
        "`artifact_checksum` varchar(128) NOT NULL",
        "`schema_version` varchar(64) NOT NULL",
        "`migration_manifest` text NOT NULL",
        "`required_sql_manifest` text NOT NULL",
        "`app_import_result` text NOT NULL",
        "`license_scope` text NOT NULL",
        "`license_valid_until` date DEFAULT NULL",
        "`license_file_evidence` text NOT NULL",
        "`license_check_result` text NOT NULL",
        "`customer_license_confirmation` text NOT NULL",
        "`interface_scope` text NOT NULL",
        "`interface_version` varchar(64) NOT NULL",
        "`integration_environment` varchar(128) NOT NULL",
        "`request_evidence` text NOT NULL",
        "`response_evidence` text NOT NULL",
        "`interface_failure_count` int NOT NULL DEFAULT 0",
        "`remediation_action` text NOT NULL",
        "`retest_evidence` text NOT NULL",
        "`interface_confirmed_by` varchar(128) NOT NULL",
        "`deployment_status` varchar(32) NOT NULL COMMENT '部署状态：DELIVERY_DRAFT、ENVIRONMENT_CHECKED、INSTALLED、INTEGRATED、DELIVERY_BLOCKED'",
        "`blocked_reason` text NOT NULL",
        "`next_action` text NOT NULL",
        "`gate_passed` bit(1) NOT NULL DEFAULT b'0'",
        "`gate_checked_at` datetime DEFAULT NULL",
        "`evidence_snapshot_checksum` varchar(128) NOT NULL",
        "`tenant_id` bigint NOT NULL DEFAULT 0",
    ]:
        assert column in text

    for column in [
        "`deployment_id` bigint NOT NULL",
        "`gate_code` varchar(64) NOT NULL",
        "`gate_name` varchar(128) NOT NULL",
        "`gate_status` varchar(32) NOT NULL COMMENT '门禁状态：PASSED、BLOCKED'",
        "`evidence_source` text NOT NULL",
        "`missing_evidence` text NOT NULL",
        "`owner_name` varchar(128) NOT NULL",
        "`next_action` text NOT NULL",
        "`signoff_impact` text NOT NULL",
    ]:
        assert column in text

    assert "`uk_mes_pro_edhr_deployment_code`" in text
    assert "`uk_mes_pro_edhr_deployment_gate`" in text
    assert "`idx_mes_pro_edhr_deployment_project_status`" in text
    assert "`idx_mes_pro_edhr_deployment_gate_status`" in text


def test_deployment_schema_seeds_menu_permissions_and_test_tenant_gate() -> None:
    text = read_sql()

    for fragment in [
        "mes:pro-edhr-deployment:query",
        "mes:pro-edhr-deployment:create",
        "mes:pro-edhr-deployment:update",
        "mes:pro-edhr-deployment:precheck",
        "mes/pro/edhr-deployment/DeploymentPage",
        "MesProEdhrDeployment",
        "'/mes/pro/feedback/edhr-deployment'",
        "900220",
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "`tenant`.`name` = '测试租户'",
        "UPDATE `system_menu`",
        "Invalid eDHR deployment page menu definition; cannot merge tenant package menu_ids",
        "Invalid eDHR deployment button menu definition; cannot merge tenant package menu_ids",
    ]:
        assert fragment in text

    for label in [
        "eDHR部署授权接口",
        "eDHR部署查询",
        "eDHR部署创建",
        "eDHR部署补证据",
        "eDHR部署预检",
    ]:
        assert label in text


def test_deployment_schema_fails_fast_for_menu_and_parent_prerequisites() -> None:
    text = read_sql()

    for message in [
        "Missing unique 测试租户; cannot merge eDHR deployment menus",
        "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR deployment menus",
        "Missing 测试租户 eDHR parent menu 900220; cannot merge deployment menus",
        "Missing eDHR deployment system_menu rows; cannot merge tenant package menu_ids",
    ]:
        assert message in text

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text


def test_deployment_schema_uses_dedicated_menu_ids_and_cleans_legacy_rows() -> None:
    text = read_sql()

    for fragment in [
        "SELECT 900315, 'eDHR部署授权接口'",
        "SELECT 900316, 'eDHR部署查询'",
        "SELECT 900317, 'eDHR部署创建'",
        "SELECT 900318, 'eDHR部署补证据'",
        "SELECT 900319, 'eDHR部署预检'",
        "WHERE `id` IN (900315, 900316, 900317, 900318, 900319)",
        "CREATE TEMPORARY TABLE `tmp_mes_edhr_deployment_legacy_menu_ids` AS",
        "`permission` LIKE 'mes:pro-edhr-deployment:%'",
        "`path` = '/mes/pro/feedback/edhr-deployment'",
        "DELETE `role_menu`",
        "FROM `system_role_menu` AS `role_menu`",
        "DELETE `menu`",
        "FROM `system_menu` AS `menu`",
        "`id` NOT IN (900315, 900316, 900317, 900318, 900319)",
    ]:
        assert fragment in text

    for legacy_fragment in [
        "SELECT 900296, 'eDHR部署授权接口'",
        "SELECT 900297, 'eDHR部署查询'",
        "SELECT 900298, 'eDHR部署创建'",
        "SELECT 900299, 'eDHR部署补证据'",
        "SELECT 900300, 'eDHR部署预检'",
        "WHERE `id` IN (900296, 900297, 900298, 900299, 900300)",
    ]:
        assert legacy_fragment not in text


def test_deployment_schema_cleans_legacy_rows_before_inserting_new_menu_ids() -> None:
    text = read_sql()

    cleanup_index = text.index(
        "CREATE TEMPORARY TABLE `tmp_mes_edhr_deployment_legacy_menu_ids` AS"
    )
    page_insert_index = text.index("SELECT 900315, 'eDHR部署授权接口'")
    precheck_insert_index = text.index("SELECT 900319, 'eDHR部署预检'")

    assert cleanup_index < page_insert_index < precheck_insert_index, (
        "Legacy eDHR deployment menus must be cleaned before inserting the new "
        "900315-900319 page/button rows; otherwise old path/permission residues "
        "can block inserts and leave the deployment menu set incomplete."
    )


def test_deployment_schema_avoids_default_success_or_destructive_patterns() -> None:
    text = read_sql()

    assert not re.search(r"DROP\s+TABLE", text, flags=re.IGNORECASE)
    assert not re.search(r"TRUNCATE\s+TABLE", text, flags=re.IGNORECASE)
    assert not re.search(r"INSERT\s+IGNORE", text, flags=re.IGNORECASE)
    assert not re.search(r"ON\s+DUPLICATE\s+KEY\s+UPDATE", text, flags=re.IGNORECASE)
    assert "DEFAULT 'INTEGRATED'" not in text.upper()
    assert "DEFAULT 'PASSED'" not in text.upper()
