from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260615_mes_edhr_tail_four_goals.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_tail_four_goals_migration_is_idempotent_fail_fast_and_non_destructive() -> None:
    text = read_sql()
    upper = text.upper()

    assert "-- release-migration: allowedEnvironments=test,backup,prod" in text
    assert "CREATE PROCEDURE ensure_mes_edhr_tail_goal_table" in text
    assert "CREATE PROCEDURE ensure_mes_edhr_tail_goal_column" in text
    assert "CREATE PROCEDURE ensure_mes_edhr_tail_goal_index" in text
    assert "SIGNAL SQLSTATE '45000'" in text

    for forbidden in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "DELETE FROM",
        "ON DUPLICATE KEY UPDATE",
        "INSERT IGNORE",
    ]:
        assert forbidden not in upper


def test_tail_four_goals_migration_declares_internal_record_metadata() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_route_use_process_batch_record",
        "mes_pro_edhr_batch_execution_task",
        "mes_pro_batch_record_execution",
    ]:
        assert f"CALL ensure_mes_edhr_tail_goal_table('{table_name}')" in text

    for column in [
        "'record_category'",
        "'validation_profile'",
        "'permission_scope_id'",
        "'route_binding_id'",
        "'route_binding_snapshot_hash'",
    ]:
        assert column in text

    assert "BATCH_RECORD" in text
    assert "INTERNAL_RECORD" in text
    assert "CONTROLLED_BATCH" in text
    assert "INTERNAL_TRACE" in text
    assert "idx_mes_pro_route_record_scope" in text
    assert "idx_mes_pro_edhr_task_record_scope" in text
    assert "idx_mes_pro_bre_record_scope" in text


def test_tail_four_goals_migration_declares_selected_signature_time_contract() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_batch_record_execution_signature",
        "mes_pro_edhr_batch_execution_signature",
    ]:
        assert f"'{table_name}'" in text

    for column in [
        "'selected_signed_at'",
        "'signature_display_at'",
        "'signature_time_mode'",
        "'selected_time_zone'",
        "'selected_time_reason'",
        "'selected_time_policy_version'",
        "'selected_time_audit_hash'",
    ]:
        assert text.count(column) >= 2

    assert "UPDATE `mes_pro_batch_record_execution_signature`" in text
    assert "UPDATE `mes_pro_edhr_batch_execution_signature`" in text
    assert "`signature_time_mode` = 'SERVER_TIME'" in text


def test_tail_four_goals_migration_declares_operation_audit_and_object_acl() -> None:
    text = read_sql()

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_operation_audit_event`" in text
    for column in [
        "`object_type` varchar(64) NOT NULL",
        "`object_id` varchar(128) NOT NULL",
        "`work_task_id` bigint DEFAULT NULL",
        "`record_category` varchar(32) DEFAULT NULL",
        "`operation_type` varchar(64) NOT NULL",
        "`permission_decision` varchar(32) DEFAULT NULL",
        "`result_status` varchar(32) NOT NULL",
        "`audit_hash` char(64) NOT NULL",
    ]:
        assert column in text

    assert "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_permission_scope`" in text
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_permission_rule`" in text
    assert "UNIQUE KEY `uk_mes_pro_edhr_perm_scope_object`" in text
    assert "`ability` varchar(64) NOT NULL" in text
    assert "`decision` varchar(32) NOT NULL" in text


def test_tail_four_goals_migration_declares_menu_permissions() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-operation-audit:query",
        "mes:pro-edhr-permission-scope:query",
        "mes:pro-edhr-permission-scope:save",
        "mes:pro-edhr-permission-scope:evaluate",
    ]:
        assert permission in text

    for menu_id in ["900241", "900242", "900243", "900244", "900245", "900246"]:
        assert menu_id in text

    assert "system_role_menu" in text
    assert "tenant_admin" in text
