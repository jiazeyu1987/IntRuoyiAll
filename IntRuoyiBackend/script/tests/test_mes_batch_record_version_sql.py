from pathlib import Path
import re


MIGRATION_SQL = Path("sql/mysql/20260708_mes_batch_record_version_phase_one.sql")
TEST_SCHEMA_SQL = Path("yudao-module-mes/src/test/resources/sql/create_tables.sql")
DIRECT_EVENT_NULLABLE_SQL = Path("sql/mysql/20260722_mes_batch_record_version_direct_event_nullable.sql")


def executable_sql(sql: str) -> str:
    return "\n".join(
        line for line in sql.splitlines()
        if not re.match(r"^\s*--", line)
    ).upper()


def test_batch_record_version_migration_is_idempotent_and_scoped():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert "CREATE PROCEDURE add_mes_edhr_column_if_missing" in sql
    assert "DROP PROCEDURE IF EXISTS add_mes_edhr_column_if_missing" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_definition`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_version`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_version_migration_item`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_batch_record_version_approval_event`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_request`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_impact`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_unified_change_event`" in sql


def test_batch_record_version_pending_hash_contract_only_blocks_active_drafts():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert "`pending_hash_scope` char(64) GENERATED ALWAYS AS" in sql
    assert "`status` IN ('DRAFT', 'PRECHECK_PASSED', 'PENDING_APPROVAL')" in sql
    assert "UNIQUE KEY `uk_mes_batch_record_version_hash_pending` (`tenant_id`, `definition_id`, `pending_hash_scope`, `deleted`)" in sql
    assert "DROP INDEX `uk_mes_batch_record_version_hash_pending`" in sql
    assert "`source_file_sha256`, `status`, `deleted`" in sql


def test_batch_record_version_migration_evidence_contract_supports_phase_two_confirmation():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    for required_column in [
        "`diff_group` varchar(64) NOT NULL DEFAULT 'TABLE'",
        "`diff_type` varchar(32) NOT NULL DEFAULT 'UNCHANGED'",
        "`source_logical_key` varchar(255) NOT NULL",
        "`target_logical_key` varchar(255) DEFAULT NULL",
        "`match_confidence` decimal(5,4) DEFAULT NULL",
        "`match_evidence_json` longtext",
        "`risk_level` varchar(32) NOT NULL",
        "`rule_type` varchar(64) DEFAULT NULL",
        "`business_owner_type` varchar(32) DEFAULT NULL",
        "`confirmed` bit(1) NOT NULL DEFAULT b'0'",
        "`confirm_idempotency_key` varchar(128) DEFAULT NULL",
    ]:
        assert required_column in sql

    assert "KEY `idx_mes_batch_record_migration_version` (`tenant_id`, `version_id`, `risk_level`, `deleted`)" in sql


def test_batch_record_version_migration_preserves_existing_tables_and_adds_nullable_references():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    for table_name in [
        "mes_pro_batch_record_report",
        "mes_pro_batch_record_execution",
        "mes_pro_edhr_batch_execution_task",
        "mes_pro_edhr_process_form_permission_rule",
    ]:
        assert f"CALL add_mes_edhr_column_if_missing('{table_name}'" in sql

    for required_column in [
        "batch_record_definition_id",
        "batch_record_version_id",
        "product_name",
        "form_slot_type",
        "owner_role_key",
        "permission_scope_id",
        "slot_config_snapshot_hash",
        "rule_type",
        "signature_role",
        "due_minutes",
    ]:
        assert required_column in sql


def test_batch_record_version_migration_handles_route_flow_legacy_rename_order():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert "CREATE PROCEDURE add_mes_edhr_column_if_table_exists" in sql
    assert "DROP PROCEDURE IF EXISTS add_mes_edhr_column_if_table_exists" in sql
    assert "CALL add_mes_edhr_column_if_missing('mes_pro_route_use_process_batch_record'" not in sql

    for table_name in [
        "mes_pro_route_use_process_batch_record",
        "mes_pro_route_use_process_batch_record_legacy_20260709",
    ]:
        assert f"CALL add_mes_edhr_column_if_table_exists('{table_name}'" in sql


def test_batch_record_version_governance_menu_and_test_tenant_permissions_are_seeded():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    for permission in [
        "mes:pro-batch-record-template:version-approve",
        "mes:pro-batch-record-version:governance-query",
        "mes:pro-batch-record-version:confirm",
        "mes:pro-batch-record-version:import",
        "mes:pro-batch-record-version:rollback-request",
    ]:
        assert permission in sql

    assert "/mes/pro/feedback/edhr-version-governance" in sql
    assert "MesProEdhrVersionGovernancePage" in sql
    assert "r.`tenant_id` = 122" in sql
    assert "r.`code` = 'edhr_rehearsal_approver_t1'" in sql
    assert "parent_menu.`id` = 900220" in sql


def test_batch_record_version_approval_event_allows_direct_without_bpm_instance():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")
    test_schema = TEST_SCHEMA_SQL.read_text(encoding="utf-8")
    nullable_migration = DIRECT_EVENT_NULLABLE_SQL.read_text(encoding="utf-8")

    assert "`approval_instance_id` varchar(128) DEFAULT NULL COMMENT '审批实例ID'" in sql
    assert '"approval_instance_id" varchar(128) DEFAULT NULL' in test_schema
    assert "MODIFY COLUMN `approval_instance_id` varchar(128) DEFAULT NULL" in nullable_migration
    assert "Batch record version direct event migration requires approval event table" in nullable_migration


def test_batch_record_version_migration_is_non_destructive():
    sql = MIGRATION_SQL.read_text(encoding="utf-8")
    upper_sql = executable_sql(sql)

    assert "DELETE FROM" not in upper_sql
    assert "TRUNCATE" not in upper_sql
    assert "DROP TABLE" not in upper_sql
    assert "DROP COLUMN" not in upper_sql
