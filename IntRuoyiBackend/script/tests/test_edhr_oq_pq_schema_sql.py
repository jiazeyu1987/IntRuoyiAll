import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_oq_pq_execution_deviation.sql"
FLOW_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_flow_intervention_log.sql"
UNIFIED_CHANGE_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_mes_edhr_unified_change_impact.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR OQ/PQ execution and deviation SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def read_flow_sql() -> str:
    assert FLOW_SQL_PATH.exists(), "eDHR flow intervention SQL must exist for menu id conflict checks"
    return FLOW_SQL_PATH.read_text(encoding="utf-8")


def read_unified_change_sql() -> str:
    assert UNIFIED_CHANGE_SQL_PATH.exists(), "eDHR unified change SQL must exist for menu id conflict checks"
    return UNIFIED_CHANGE_SQL_PATH.read_text(encoding="utf-8")


def test_oq_pq_schema_declares_execution_case_run_step_and_deviation_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_validation_case",
        "mes_pro_edhr_validation_run",
        "mes_pro_edhr_validation_step_result",
        "mes_pro_edhr_validation_deviation",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`package_id` bigint NOT NULL",
        "`case_code` varchar(64) NOT NULL",
        "`case_name` varchar(128) NOT NULL",
        "`case_type` varchar(16) NOT NULL COMMENT '用例类型：OQ、PQ'",
        "`case_version` varchar(64) NOT NULL",
        "`case_status` varchar(32) NOT NULL",
        "`step_no` varchar(32) NOT NULL",
        "`step_title` varchar(128) NOT NULL",
        "`expected_result` varchar(1000) NOT NULL",
        "`evidence_requirement` varchar(500) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`run_code` varchar(64) NOT NULL",
        "`run_status` varchar(32) NOT NULL COMMENT '执行状态：CREATED、RUNNING、DEVIATION_OPEN、PASSED、BLOCKED'",
        "`execution_environment` varchar(128) NOT NULL",
        "`release_tag` varchar(64) NOT NULL",
        "`schema_version` varchar(64) NOT NULL",
        "`executor_name` varchar(128) NOT NULL",
        "`reviewer_name` varchar(128) NOT NULL",
        "`real_business_path` varchar(500) DEFAULT NULL",
        "`real_test_data_source` varchar(500) DEFAULT NULL",
        "`target_environment_proof` varchar(500) DEFAULT NULL",
        "`attachment_evidence` varchar(500) NOT NULL",
        "`evidence_checksum` varchar(128) NOT NULL",
        "`open_deviation_count` int NOT NULL DEFAULT 0",
        "`blocked_reason` varchar(500) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`step_result` varchar(32) NOT NULL COMMENT '步骤结果：PASS、FAIL、BLOCKED'",
        "`actual_result` varchar(1000) NOT NULL",
        "`deviation_id` bigint DEFAULT NULL",
        "`next_action` varchar(500) NOT NULL",
    ]:
        assert column in text

    for column in [
        "`deviation_code` varchar(64) NOT NULL",
        "`deviation_status` varchar(32) NOT NULL COMMENT '偏差状态：OPEN、REMEDIATED、RETESTED、CLOSED'",
        "`root_cause` varchar(1000) DEFAULT NULL",
        "`remediation_action` varchar(1000) DEFAULT NULL",
        "`remediation_owner_name` varchar(128) DEFAULT NULL",
        "`retest_result` varchar(1000) DEFAULT NULL",
        "`retest_evidence` varchar(500) DEFAULT NULL",
        "`retest_reviewer_name` varchar(128) DEFAULT NULL",
        "`close_signoff_name` varchar(128) DEFAULT NULL",
        "`closed_at` datetime DEFAULT NULL",
    ]:
        assert column in text

    assert "`tenant_id` bigint NOT NULL DEFAULT 0" in text
    assert "`uk_mes_pro_edhr_validation_case_code`" in text
    assert "`uk_mes_pro_edhr_validation_run_code`" in text
    assert "`uk_mes_pro_edhr_validation_deviation_code`" in text
    assert "`idx_mes_pro_edhr_validation_run_package_status`" in text
    assert "`idx_mes_pro_edhr_validation_deviation_run_status`" in text


def test_oq_pq_schema_seeds_menu_permissions_and_test_tenant_gate() -> None:
    text = read_sql()

    for fragment in [
        "mes:pro-edhr-oq-pq:query",
        "mes:pro-edhr-oq-pq:create",
        "mes:pro-edhr-oq-pq:execute",
        "mes:pro-edhr-oq-pq:retest",
        "mes:pro-edhr-oq-pq:close",
        "mes/pro/edhr-oq-pq/OqPqPage",
        "MesProEdhrOqPq",
        "'/mes/pro/feedback/edhr-oq-pq'",
        "900220",
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "`tenant`.`name` = '测试租户'",
        "UPDATE `system_menu`",
        "Invalid eDHR OQ/PQ page menu definition; cannot merge tenant package menu_ids",
        "Invalid eDHR OQ/PQ button menu definition; cannot merge tenant package menu_ids",
    ]:
        assert fragment in text

    for label in [
        "eDHR OQ/PQ执行台",
        "eDHR OQ/PQ查询",
        "eDHR OQ/PQ创建",
        "eDHR OQ/PQ执行",
        "eDHR 偏差复测",
        "eDHR 偏差关闭",
    ]:
        assert label in text


def test_oq_pq_schema_fails_fast_for_menu_and_parent_prerequisites() -> None:
    text = read_sql()

    for message in [
        "Missing unique 测试租户; cannot merge eDHR OQ/PQ menus",
        "Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR OQ/PQ menus",
        "Missing 测试租户 eDHR parent menu 900220; cannot merge OQ/PQ menus",
        "Missing eDHR OQ/PQ system_menu rows; cannot merge tenant package menu_ids",
    ]:
        assert message in text

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text


def test_oq_pq_schema_uses_dedicated_menu_ids_and_normalizes_legacy_rows() -> None:
    text = read_sql()
    flow_text = read_flow_sql()
    unified_change_text = read_unified_change_sql()

    for menu_id in ["900290", "900291", "900292"]:
        assert menu_id in flow_text

    for menu_id in ["900293", "900294", "900295"]:
        assert menu_id in unified_change_text

    for menu_id in ["900332", "900333", "900334", "900335", "900336", "900337"]:
        assert menu_id in text

    for fragment in [
        "tmp_mes_edhr_oq_pq_legacy_menu_map",
        "DELETE `legacy_menu` FROM `system_menu` AS `legacy_menu`",
        "DELETE `role_menu` FROM `system_role_menu` AS `role_menu`",
        "COALESCE(`legacy_map`.`new_menu_id`, CAST(`menu`.`menu_id` AS UNSIGNED))",
    ]:
        assert fragment in text


def test_oq_pq_schema_cleans_legacy_rows_before_inserting_new_menu_ids() -> None:
    text = read_sql()

    delete_legacy_index = text.index("DELETE `legacy_menu` FROM `system_menu` AS `legacy_menu`")
    page_insert_index = text.index("SELECT 900332, 'eDHR OQ/PQ执行台'")
    query_insert_index = text.index("SELECT 900333, 'eDHR OQ/PQ查询'")
    create_insert_index = text.index("SELECT 900334, 'eDHR OQ/PQ创建'")

    assert delete_legacy_index < page_insert_index
    assert delete_legacy_index < query_insert_index
    assert delete_legacy_index < create_insert_index


def test_oq_pq_schema_avoids_default_success_or_destructive_patterns() -> None:
    text = read_sql()

    assert not re.search(r"DROP\s+TABLE", text, flags=re.IGNORECASE)
    assert not re.search(r"TRUNCATE\s+TABLE", text, flags=re.IGNORECASE)
    assert not re.search(r"INSERT\s+IGNORE", text, flags=re.IGNORECASE)
    assert not re.search(r"ON\s+DUPLICATE\s+KEY\s+UPDATE", text, flags=re.IGNORECASE)
    assert "DEFAULT 'PASSED'" not in text.upper()
    assert "DEFAULT 'CLOSED'" not in text.upper()
