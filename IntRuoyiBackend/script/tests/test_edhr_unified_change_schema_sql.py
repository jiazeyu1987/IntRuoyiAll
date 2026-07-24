from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql/mysql/20260618_mes_edhr_unified_change_impact.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR 统一变更与影响分析 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_unified_change_request_table_keeps_version_and_diff_contract() -> None:
    sql = read_sql()

    for fragment in [
        "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_request`",
        "`change_code` varchar(64) NOT NULL",
        "`controlled_object_type` varchar(64) NOT NULL",
        "`controlled_object_id` varchar(128) NOT NULL",
        "`controlled_object_code` varchar(128) NOT NULL",
        "`current_version` varchar(64) NOT NULL",
        "`target_version` varchar(64) NOT NULL",
        "`change_type` varchar(64) NOT NULL",
        "`change_status` varchar(32) NOT NULL",
        "`risk_level` varchar(32) NOT NULL",
        "`reason_category` varchar(64)",
        "`reason` varchar(500) NOT NULL",
        "`diff_snapshot_json` longtext NOT NULL",
        "`impact_summary_json` longtext NOT NULL",
        "`impact_recalculated_at` datetime NOT NULL",
        "`impact_recalculation_hash` char(64) NOT NULL",
        "`approval_signoff_evidence_hash` char(64)",
        "`effect_signoff_evidence_hash` char(64)",
        "`idempotency_key` varchar(128) NOT NULL",
        "`evidence_hash` char(64) NOT NULL",
        "uk_mes_pro_edhr_unified_change_idempotency",
        "idx_mes_pro_edhr_unified_change_object",
    ]:
        assert fragment in sql


def test_unified_change_impact_table_requires_explicit_scope() -> None:
    sql = read_sql()

    for fragment in [
        "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_impact`",
        "`change_request_id` bigint NOT NULL",
        "`impact_type` varchar(64) NOT NULL",
        "`impact_object_type` varchar(64) NOT NULL",
        "`impact_object_id` varchar(128) NOT NULL",
        "`impact_object_code` varchar(128)",
        "`risk_level` varchar(32) NOT NULL",
        "`responsibility_module` varchar(64) NOT NULL",
        "`requires_training` bit(1) NOT NULL",
        "`requires_revalidation` bit(1) NOT NULL",
        "`requires_release_recheck` bit(1) NOT NULL",
        "`impact_detail` varchar(1000) NOT NULL",
        "`next_action` varchar(500) NOT NULL",
        "`evidence_hash` char(64) NOT NULL",
        "idx_mes_pro_edhr_unified_change_impact_request",
    ]:
        assert fragment in sql


def test_unified_change_event_table_is_idempotent_and_auditable() -> None:
    sql = read_sql()

    for fragment in [
        "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_event`",
        "`change_request_id` bigint NOT NULL",
        "`event_type` varchar(64) NOT NULL",
        "`from_status` varchar(32)",
        "`to_status` varchar(32) NOT NULL",
        "`actor_user_id` bigint",
        "`reason` varchar(500)",
        "`signoff_evidence_hash` char(64)",
        "`event_snapshot_json` longtext NOT NULL",
        "`evidence_hash` char(64) NOT NULL",
        "`occurred_at` datetime NOT NULL",
        "`idempotency_key` varchar(128) NOT NULL",
        "uk_mes_pro_edhr_unified_change_event_idempotency",
        "idx_mes_pro_edhr_unified_change_event_request",
    ]:
        assert fragment in sql


def test_unified_change_permissions_are_explicit() -> None:
    sql = read_sql()

    for fragment in [
        "mes:pro-edhr-change:unified-query",
        "mes:pro-edhr-change:unified-create",
        "mes:pro-edhr-change:unified-submit",
        "mes:pro-edhr-change:unified-approve",
        "mes:pro-edhr-change:unified-effect",
        "mes:pro-edhr-change:impact-query",
        "mes:pro-edhr-change:event-query",
        "eDHR统一变更",
        "eDHR统一变更创建",
        "eDHR统一变更提交",
        "eDHR统一变更审批",
        "eDHR统一变更生效申请",
        "eDHR统一变更影响范围",
        "eDHR统一变更事件",
        "JSON_VALID(`package`.`menu_ids`)",
        "SIGNAL SQLSTATE '45000'",
        "system_role_menu",
        "tenant_admin",
    ]:
        assert fragment in sql


def test_unified_change_sql_has_no_destructive_or_fake_paths() -> None:
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
        "OVERWRITE_CURRENT_VERSION_SUCCESS",
        "FORCE_EFFECT_SUCCESS",
    ]:
        assert forbidden not in sql
