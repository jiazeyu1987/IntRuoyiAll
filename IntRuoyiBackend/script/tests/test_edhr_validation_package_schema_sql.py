import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_validation_package_matrix.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR validation package matrix SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_validation_schema_declares_package_item_and_trace_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_validation_package",
        "mes_pro_edhr_validation_requirement_item",
        "mes_pro_edhr_validation_trace_link",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`package_code` varchar(64) NOT NULL",
        "`package_name` varchar(128) NOT NULL",
        "`customer_project_name` varchar(128) NOT NULL",
        "`customer_name` varchar(128) NOT NULL",
        "`site_name` varchar(128) NOT NULL",
        "`system_scope` varchar(500) NOT NULL",
        "`validation_scope` varchar(500) NOT NULL",
        "`release_tag` varchar(64) NOT NULL",
        "`schema_version` varchar(64) NOT NULL",
        "`target_environment` varchar(64) NOT NULL",
        "`validation_status` varchar(32) NOT NULL",
        "`oq_ready` bit(1) NOT NULL DEFAULT b'0'",
        "`validation_owner_name` varchar(128) NOT NULL",
        "`qa_owner_name` varchar(128) NOT NULL",
        "`blocked_reason` varchar(500) NOT NULL",
        "`trace_summary_json` longtext NOT NULL",
    ]:
        assert column in text

    for column in [
        "`item_code` varchar(64) NOT NULL",
        "`item_name` varchar(128) NOT NULL",
        "`item_type` varchar(32) NOT NULL COMMENT '条目类型：URS、FRS、RISK、IQ、OQ、PQ'",
        "`item_version` varchar(64) NOT NULL",
        "`item_status` varchar(32) NOT NULL",
        "`owner_name` varchar(128) NOT NULL",
        "`signoff_role` varchar(128) NOT NULL",
        "`source_document` varchar(256) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`source_item_id` bigint NOT NULL",
        "`source_item_code` varchar(64) NOT NULL",
        "`source_item_type` varchar(32) NOT NULL",
        "`target_item_id` bigint NOT NULL",
        "`target_item_code` varchar(64) NOT NULL",
        "`target_item_type` varchar(32) NOT NULL",
        "`link_type` varchar(32) NOT NULL COMMENT '追溯类型：URS_FRS、URS_RISK、URS_VERIFICATION'",
        "`trace_status` varchar(32) NOT NULL",
        "`next_action` varchar(500) NOT NULL",
    ]:
        assert column in text

    assert "`tenant_id` bigint NOT NULL DEFAULT 0" in text
    assert "`uk_mes_pro_edhr_validation_package_code`" in text
    assert "`uk_mes_pro_edhr_validation_item_code`" in text
    assert "`uk_mes_pro_edhr_validation_trace_link`" in text
    assert "`idx_mes_pro_edhr_validation_item_package_type`" in text
    assert "`idx_mes_pro_edhr_validation_trace_source`" in text


def test_validation_schema_seeds_menu_permissions_and_test_tenant_gate() -> None:
    text = read_sql()

    for fragment in [
        "mes:pro-edhr-validation:query",
        "mes:pro-edhr-validation:create",
        "mes:pro-edhr-validation:evaluate-trace",
        "mes/pro/edhr-validation/ValidationPage",
        "MesProEdhrValidation",
        "'/mes/pro/feedback/edhr-validation'",
        "900220",
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "`tenant`.`name` = '测试租户'",
    ]:
        assert fragment in text

    for label in [
        "eDHR验证包矩阵",
        "eDHR验证包查询",
        "eDHR验证包创建",
        "eDHR追溯门禁评估",
    ]:
        assert label in text


def test_validation_schema_fails_fast_for_menu_package_prerequisites() -> None:
    text = read_sql()

    for message in [
        "Missing unique 测试租户; cannot merge eDHR validation menus",
        "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR validation menus",
        "Missing 测试租户 eDHR parent menu 900220; cannot merge validation menus",
        "Missing eDHR validation system_menu rows; cannot merge tenant package menu_ids",
    ]:
        assert message in text

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text


def test_validation_schema_avoids_silent_success_or_overwrite_patterns() -> None:
    text = read_sql()

    assert not re.search(r"INSERT\s+IGNORE", text, flags=re.IGNORECASE)
    assert not re.search(r"ON\s+DUPLICATE\s+KEY\s+UPDATE", text, flags=re.IGNORECASE)
    assert "DEFAULT 'SUCCESS'" not in text.upper()
    assert "VALIDATION_PASSED" not in text
