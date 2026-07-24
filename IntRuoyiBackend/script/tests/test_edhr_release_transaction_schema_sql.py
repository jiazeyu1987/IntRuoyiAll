from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql/mysql/20260618_mes_edhr_release_transaction_lifecycle.sql"
REPAIR_SQL_PATH = ROOT / "sql/mysql/20260624_mes_edhr_release_transaction_lifecycle_column_repair.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR 放行事务生命周期 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_release_transaction_lifecycle_sql_extends_transaction_table() -> None:
    sql = read_sql()

    for fragment in [
        "ALTER TABLE `mes_pro_edhr_release_transaction`",
        "`submit_idempotency_key` varchar(128)",
        "`submitted_by` bigint",
        "`submitted_at` datetime",
        "`approval_idempotency_key` varchar(128)",
        "`approved_by` bigint",
        "`approved_at` datetime",
        "`approval_signoff_evidence_hash` char(64)",
        "`approval_opinion` varchar(500)",
        "`rejected_by` bigint",
        "`rejected_at` datetime",
        "`reject_reason` varchar(500)",
        "`withdrawn_by` bigint",
        "`withdrawn_at` datetime",
        "`withdraw_reason` varchar(500)",
    ]:
        assert fragment in sql


def test_release_transaction_event_table_is_idempotent_and_auditable() -> None:
    sql = read_sql()

    for fragment in [
        "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_release_transaction_event`",
        "`release_transaction_id` bigint NOT NULL",
        "`event_type` varchar(32) NOT NULL",
        "`from_status` varchar(32) NOT NULL",
        "`to_status` varchar(32) NOT NULL",
        "`actor_user_id` bigint",
        "`reason` varchar(500)",
        "`opinion` varchar(500)",
        "`idempotency_key` varchar(128) NOT NULL",
        "`signoff_evidence_hash` char(64)",
        "`event_snapshot_json` longtext",
        "`evidence_hash` char(64) NOT NULL",
        "`occurred_at` datetime NOT NULL",
        "uk_mes_pro_edhr_release_event_idempotency",
        "idx_mes_pro_edhr_release_event_transaction",
    ]:
        assert fragment in sql


def test_release_transaction_lifecycle_permissions_are_explicit() -> None:
    sql = read_sql()

    for fragment in [
        "SELECT 900353, 'eDHR放行驳回'",
        "SELECT 900354, 'eDHR放行撤回'",
        "SELECT 900355, 'eDHR放行事务事件查询'",
        "id` IN (900263, 900264, 900353, 900354, 900355)",
        "mes:pro-edhr-release:submit",
        "mes:pro-edhr-release:approve",
        "mes:pro-edhr-release:reject",
        "mes:pro-edhr-release:withdraw",
        "mes:pro-edhr-release:event-query",
        "eDHR放行驳回",
        "eDHR放行撤回",
        "eDHR放行事务事件查询",
        "JSON_VALID(`package`.`menu_ids`)",
        "SIGNAL SQLSTATE '45000'",
        "system_role_menu",
        "tenant_admin",
    ]:
        assert fragment in sql


def test_release_transaction_lifecycle_uses_dedicated_menu_ids_not_traveler_slots() -> None:
    sql = read_sql()

    for fragment in [
        "SELECT 900353, 'eDHR放行驳回'",
        "SELECT 900354, 'eDHR放行撤回'",
        "SELECT 900355, 'eDHR放行事务事件查询'",
        "WHERE `id` IN (900263, 900264, 900353, 900354, 900355)",
        "ON `menu`.`id` IN (900263, 900264, 900353, 900354, 900355)",
    ]:
        assert fragment in sql

    for forbidden in [
        "SELECT 900266, 'eDHR放行驳回'",
        "SELECT 900267, 'eDHR放行撤回'",
        "SELECT 900268, 'eDHR放行事务事件查询'",
        "WHERE `id` IN (900263, 900264, 900266, 900267, 900268)",
        "ON `menu`.`id` IN (900263, 900264, 900266, 900267, 900268)",
    ]:
        assert forbidden not in sql


def test_release_transaction_lifecycle_sql_has_no_hidden_shortcuts() -> None:
    sql = read_sql().upper()

    for forbidden in [
        "DROP TABLE",
        "TRUNCATE",
        "DELETE FROM",
        "INSERT IGNORE",
        "ON DUPLICATE",
        "DEFAULT_SUCCESS",
        "MOCK_SIGNOFF",
        "DEFAULT_RELEASED",
    ]:
        assert forbidden not in sql


def test_release_transaction_column_repair_sql_is_idempotent_and_column_only() -> None:
    assert REPAIR_SQL_PATH.exists(), "eDHR 放行事务生命周期列修复 SQL 必须存在。"
    sql = REPAIR_SQL_PATH.read_text(encoding="utf-8")

    for fragment in [
        "CREATE PROCEDURE ensure_mes_edhr_release_transaction_lifecycle_columns",
        "CALL ensure_mes_edhr_release_transaction_lifecycle_columns()",
        "DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_transaction_lifecycle_columns",
        "`TABLE_NAME` = 'mes_pro_edhr_release_transaction'",
        "`COLUMN_NAME` = 'submit_idempotency_key'",
        "ADD COLUMN `submit_idempotency_key` varchar(128)",
        "ADD COLUMN `submitted_by` bigint",
        "ADD COLUMN `submitted_at` datetime",
        "ADD COLUMN `approval_idempotency_key` varchar(128)",
        "ADD COLUMN `approved_by` bigint",
        "ADD COLUMN `approved_at` datetime",
        "ADD COLUMN `approval_signoff_evidence_hash` char(64)",
        "ADD COLUMN `approval_opinion` varchar(500)",
        "ADD COLUMN `rejected_by` bigint",
        "ADD COLUMN `rejected_at` datetime",
        "ADD COLUMN `reject_reason` varchar(500)",
        "ADD COLUMN `withdrawn_by` bigint",
        "ADD COLUMN `withdrawn_at` datetime",
        "ADD COLUMN `withdraw_reason` varchar(500)",
    ]:
        assert fragment in sql

    forbidden = [
        "DROP TABLE",
        "TRUNCATE",
        "DELETE FROM",
        "UPDATE `mes_pro_edhr_release_transaction`",
        "INSERT INTO `mes_pro_edhr_release_transaction`",
        "system_menu",
        "system_role_menu",
        "system_tenant_package",
    ]
    upper_sql = sql.upper()
    for fragment in forbidden:
        assert fragment.upper() not in upper_sql
