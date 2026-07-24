from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql/mysql/20260618_mes_edhr_flow_intervention_log.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR 流程日志与受控干预 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_flow_event_table_is_auditable() -> None:
    sql = read_sql()

    for fragment in [
        "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_flow_event`",
        "`business_object_type` varchar(64) NOT NULL",
        "`business_object_id` varchar(128) NOT NULL",
        "`flow_instance_id` varchar(128)",
        "`task_id` varchar(128)",
        "`node_key` varchar(128)",
        "`event_type` varchar(32) NOT NULL",
        "`from_status` varchar(32) NOT NULL",
        "`to_status` varchar(32) NOT NULL",
        "`actor_user_id` bigint",
        "`target_user_id` bigint",
        "`permission_code` varchar(128) NOT NULL",
        "`permission_decision` varchar(32) NOT NULL",
        "`reason` varchar(500)",
        "`signoff_evidence_hash` char(64)",
        "`integrity_check_result` varchar(32) NOT NULL",
        "`integrity_check_snapshot_json` longtext",
        "`event_snapshot_json` longtext",
        "`evidence_hash` char(64) NOT NULL",
        "`occurred_at` datetime NOT NULL",
        "idx_mes_pro_edhr_flow_event_object",
        "idx_mes_pro_edhr_flow_event_instance",
    ]:
        assert fragment in sql


def test_flow_intervention_table_is_idempotent_and_controlled() -> None:
    sql = read_sql()

    for fragment in [
        "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_flow_intervention`",
        "`intervention_code` varchar(64) NOT NULL",
        "`business_object_type` varchar(64) NOT NULL",
        "`business_object_id` varchar(128) NOT NULL",
        "`intervention_action` varchar(32) NOT NULL",
        "`intervention_status` varchar(32) NOT NULL",
        "`from_status` varchar(32) NOT NULL",
        "`to_status` varchar(32) NOT NULL",
        "`source_task_id` varchar(128)",
        "`target_task_id` varchar(128)",
        "`target_user_id` bigint",
        "`reason_category` varchar(64)",
        "`reason` varchar(500) NOT NULL",
        "`authorization_basis` varchar(500)",
        "`signoff_evidence_hash` char(64) NOT NULL",
        "`idempotency_key` varchar(128) NOT NULL",
        "`integrity_check_result` varchar(32) NOT NULL",
        "`integrity_check_snapshot_json` longtext",
        "`evidence_hash` char(64) NOT NULL",
        "uk_mes_pro_edhr_flow_intervention_idempotency",
        "idx_mes_pro_edhr_flow_intervention_object",
    ]:
        assert fragment in sql


def test_flow_intervention_permissions_are_explicit() -> None:
    sql = read_sql()

    for fragment in [
        "mes:pro-edhr-flow-intervention:query",
        "mes:pro-edhr-flow-intervention:event-query",
        "mes:pro-edhr-flow-intervention:return",
        "mes:pro-edhr-flow-intervention:withdraw",
        "mes:pro-edhr-flow-intervention:transfer",
        "mes:pro-edhr-flow-intervention:add-sign",
        "mes:pro-edhr-flow-intervention:admin-intervene",
        "eDHR流程干预管理",
        "eDHR流程日志查询",
        "eDHR流程退回",
        "eDHR流程转办",
        "eDHR流程加签",
        "eDHR管理员干预",
        "JSON_VALID(`package`.`menu_ids`)",
        "SIGNAL SQLSTATE '45000'",
        "system_role_menu",
        "tenant_admin",
    ]:
        assert fragment in sql


def test_flow_intervention_schema_uses_dedicated_menu_ids_and_normalizes_legacy_rows() -> None:
    sql = read_sql()

    for dedicated_menu_id in ["900356", "900357", "900358", "900359", "900360", "900361", "900362"]:
        assert dedicated_menu_id in sql

    for fragment in [
        "tmp_mes_edhr_flow_intervention_legacy_menu_map",
        "DELETE `legacy_menu` FROM `system_menu` AS `legacy_menu`",
        "DELETE `role_menu` FROM `system_role_menu` AS `role_menu`",
        "COALESCE(`legacy_map`.`new_menu_id`, CAST(`menu`.`menu_id` AS UNSIGNED))",
        "WHEN `menu`.`path` = '/mes/pro/feedback/edhr-flow-intervention' THEN 900356",
        "WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:event-query' THEN 900357",
        "WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:return' THEN 900358",
        "WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:withdraw' THEN 900359",
        "WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:transfer' THEN 900360",
        "WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:add-sign' THEN 900361",
        "WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:admin-intervene' THEN 900362",
    ]:
        assert fragment in sql


def test_flow_intervention_schema_avoids_conflicting_legacy_menu_ids() -> None:
    sql = read_sql()

    for conflicting_id in ["900286", "900287", "900288", "900289", "900290", "900291", "900292"]:
        assert f"SELECT {conflicting_id}," not in sql
        assert f"IN ({conflicting_id}" not in sql


def test_flow_intervention_sql_has_no_shortcuts() -> None:
    sql = read_sql().upper()

    for forbidden in [
        "DROP TABLE",
        "TRUNCATE",
        "DELETE FROM",
        "INSERT IGNORE",
        "ON DUPLICATE",
        "DEFAULT_SUCCESS",
        "MOCK_SIGNOFF",
        "DIRECT_STATUS_UPDATE",
        "SQL_INTERVENTION_SUCCESS",
        "ADMIN_OVERRIDE_SUCCESS",
    ]:
        assert forbidden not in sql
