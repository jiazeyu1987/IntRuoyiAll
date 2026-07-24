from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_dhr_template_lifecycle.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "CR-T1-02 DHR template lifecycle schema SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_dhr_template_schema_declares_catalog_template_version_binding_and_impact_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_dhr_catalog",
        "mes_pro_edhr_dhr_template",
        "mes_pro_edhr_dhr_template_version",
        "mes_pro_edhr_dhr_template_binding",
        "mes_pro_edhr_dhr_template_impact",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`catalog_code` varchar(64) NOT NULL",
        "`catalog_name` varchar(128) NOT NULL",
        "`parent_catalog_id` bigint DEFAULT NULL",
        "`template_code` varchar(64) NOT NULL",
        "`template_name` varchar(128) NOT NULL",
        "`current_version` varchar(32) NOT NULL",
        "`status` varchar(32) NOT NULL",
        "`review_status` varchar(32) NOT NULL",
        "`signoff_status` varchar(32) NOT NULL",
        "`binding_count` int NOT NULL",
        "`integrity_issue_count` int NOT NULL",
        "`integrity_issue_json` longtext DEFAULT NULL",
        "`signoff_evidence_hash` char(64) DEFAULT NULL",
        "`version_no` varchar(32) NOT NULL",
        "`template_snapshot_json` longtext NOT NULL",
        "`binding_type` varchar(32) NOT NULL",
        "`binding_object_id` bigint DEFAULT NULL",
        "`binding_object_code` varchar(128) NOT NULL",
        "`impact_scope_json` longtext NOT NULL",
        "`impact_confirmed` bit(1) NOT NULL",
    ]:
        assert column in text


def test_dhr_template_schema_declares_lifecycle_statuses_constraints_and_permissions() -> None:
    text = read_sql()

    for status in [
        "DRAFT",
        "PRECHECK_FAILED",
        "PENDING_REVIEW",
        "APPROVED",
        "SIGNOFF_PENDING",
        "EFFECTIVE",
        "SUSPENDED",
        "RETIRED",
        "OBSOLETE",
    ]:
        assert status in text

    for key in [
        "uk_mes_pro_edhr_dhr_catalog_code",
        "uk_mes_pro_edhr_dhr_template_code",
        "uk_mes_pro_edhr_dhr_template_version",
        "uk_mes_pro_edhr_dhr_template_binding",
        "idx_mes_pro_edhr_dhr_template_status",
        "idx_mes_pro_edhr_dhr_template_impact",
    ]:
        assert key in text

    for permission in [
        "mes:pro-edhr-dhr-template:query",
        "mes:pro-edhr-dhr-template:create",
        "mes:pro-edhr-dhr-template:check",
        "mes:pro-edhr-dhr-template:approve",
        "mes:pro-edhr-dhr-template:signoff",
        "mes:pro-edhr-dhr-template:activate",
        "mes:pro-edhr-dhr-template:retire",
        "mes:pro-edhr-dhr-template:void",
    ]:
        assert permission in text

    assert "/mes/pro/feedback/edhr-dhr-template" in text
    assert "mes/pro/edhr-dhr-template/DhrTemplatePage" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text


def test_dhr_template_schema_forbids_destructive_or_silent_shortcuts() -> None:
    text = read_sql().upper()

    for fragment in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "DELETE FROM",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DEFAULT_SUCCESS",
        "MOCK_SIGNOFF",
        "`STATUS` VARCHAR(32) NOT NULL DEFAULT 'EFFECTIVE'",
        "`SIGNOFF_STATUS` VARCHAR(32) NOT NULL DEFAULT 'SIGNED'",
    ]:
        assert fragment not in text


def test_dhr_template_schema_uses_dedicated_button_menu_ids_and_avoids_unified_change_range() -> None:
    text = read_sql()

    for menu_id in [
        "900347",
        "900348",
        "900349",
        "900350",
        "900351",
        "900352",
    ]:
        assert menu_id in text

    for conflicting_id in [
        "900293",
        "900294",
        "900295",
        "900296",
        "900297",
        "900298",
        "900299",
    ]:
        assert conflicting_id not in text
