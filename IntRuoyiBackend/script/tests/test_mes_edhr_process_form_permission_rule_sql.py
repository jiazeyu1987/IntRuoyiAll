from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260706_mes_edhr_process_form_permission_rule.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR process form permission rule migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_process_form_permission_rule_migration_declares_publishable_contract() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260612_mes_process_use_route_tabs; type=schema; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Missing process batch record route menu 900221" in text
    assert "Menu id 900363 or 900364 is already used by another permission" in text


def test_process_form_permission_rule_migration_declares_schema_and_indexes() -> None:
    text = read_sql()

    required_snippets = [
        "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_process_form_permission_rule`",
        "`route_process_id` bigint NOT NULL",
        "`batch_record_report_id` varchar(128) NOT NULL",
        "`rule_type` varchar(32) NOT NULL",
        "`signature_cell_key` varchar(128) NOT NULL DEFAULT ''",
        "`signature_role` varchar(32) DEFAULT NULL",
        "`candidate_source_type` varchar(32) NOT NULL",
        "`candidate_source_ids` varchar(1000) NOT NULL",
        "`completion_policy` varchar(32) NOT NULL",
        "`due_minutes` int NOT NULL",
        "`tenant_id` bigint NOT NULL DEFAULT 0",
        "UNIQUE KEY `uk_mes_pro_edhr_process_form_rule`",
        "KEY `idx_mes_pro_edhr_process_form_rule_route_report`",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_process_form_permission_rule_migration_adds_menu_and_role_permissions_without_hardcoded_roles() -> None:
    text = read_sql()

    required_snippets = [
        "900363, '工序表单权限查询', 'mes:pro-edhr-process-form-permission-rule:query'",
        "900364, '工序表单权限维护', 'mes:pro-edhr-process-form-permission-rule:update'",
        "JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900221' AS JSON), '$')",
        "JOIN `system_role_menu` AS `route_role_menu`",
        "`route_role_menu`.`menu_id` = 900221",
        "JOIN `system_menu` AS `menu`",
        "`menu`.`id` IN (900363, 900364)",
        "NOT EXISTS",
        "DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_process_form_permission_package_menu_ids`",
        "DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_process_form_permission_packages`",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "tenant_admin" not in text
    assert "DELETE FROM `system_menu`" not in text
    assert "DELETE FROM `system_role_menu`" not in text
    assert "DELETE FROM `system_tenant_package`" not in text
