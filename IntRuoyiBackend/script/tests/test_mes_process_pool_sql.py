from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260730_mes_process_pool_foundation.sql"
)
P0_RUNTIME_VERIFIER_PATH = (
    Path(__file__).resolve().parents[1]
    / "p0"
    / "verify_p0_runtime_migration.py"
)
P0_RUNTIME_APPLY_PREFLIGHT_PATH = (
    Path(__file__).resolve().parents[1]
    / "p0"
    / "verify_p0_runtime_migration_apply_preflight.py"
)
P0_RUNTIME_BACKFILL_SOURCE_AUDIT_PATH = (
    Path(__file__).resolve().parents[1]
    / "p0"
    / "verify_p0_runtime_backfill_sources.py"
)
P0_RUNTIME_BACKFILL_REPAIR_PLAN_PATH = (
    Path(__file__).resolve().parents[1]
    / "p0"
    / "verify_p0_runtime_backfill_repair_plan.py"
)
P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_PATH = (
    Path(__file__).resolve().parents[1]
    / "p0"
    / "verify_p0_runtime_backfill_repair_manifest.py"
)


def test_process_pool_sql_creates_dedicated_tables():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260729_dcc_product_catalog_remove_subsidiary_source; type=schema; riskLevel=medium\n"
    )
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_event`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_quantity_fragment`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_pqc_record`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_feedback_surplus_pool`" not in sql


def test_process_pool_event_sql_requires_traceable_context_and_unique_signature():
    sql = SQL_PATH.read_text(encoding="utf-8")

    required_event_columns = [
        "`work_order_id` bigint NOT NULL",
        "`route_id` bigint NOT NULL",
        "`route_process_id` bigint NOT NULL",
        "`process_id` bigint NOT NULL",
        "`actual_employee_id` bigint NOT NULL",
        "`device_account_id` bigint NOT NULL",
        "`device_id` bigint NOT NULL",
        "`workstation_id` bigint NOT NULL",
        "`template_type` varchar(64) NOT NULL",
        "`feedback_source_type` varchar(64) NOT NULL",
        "`feedback_source_id` bigint NOT NULL",
        "`recordbook_source_type` varchar(64) NOT NULL",
        "`recordbook_source_id` bigint NOT NULL",
        "`raw_payload` json NOT NULL",
        "`server_submit_time` datetime NOT NULL",
        "`signature_id` bigint NOT NULL",
        "`signature_user_id` bigint NOT NULL",
    ]
    for column in required_event_columns:
        assert column in sql
    assert "UNIQUE KEY `uk_mes_pro_process_pool_event_signature` (`tenant_id`, `signature_id`, `deleted`)" in sql
    assert "KEY `idx_mes_pro_process_pool_event_time` (`tenant_id`, `server_submit_time`)" in sql


def test_process_pool_quantity_and_pqc_records_are_linked_to_event():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "`event_id` bigint NOT NULL COMMENT '工序池提交事件ID'" in sql
    assert "`source_quantity_type` varchar(64) NOT NULL" in sql
    assert "`total_quantity` decimal(24,6) NOT NULL" in sql
    assert "`allocated_quantity` decimal(24,6) NOT NULL DEFAULT 0" in sql
    assert "`available_quantity` decimal(24,6) NOT NULL DEFAULT 0" in sql
    assert "`allocation_status` varchar(32) NOT NULL DEFAULT 'AVAILABLE'" in sql
    assert "`inspection_result` varchar(32) NOT NULL" in sql
    assert "KEY `idx_mes_pro_process_pool_fragment_event` (`tenant_id`, `event_id`)" in sql
    assert "KEY `idx_mes_pro_process_pool_pqc_event` (`tenant_id`, `event_id`)" in sql


def test_p0_runtime_migration_verifier_contract():
    assert P0_RUNTIME_VERIFIER_PATH.exists(), "P0 runtime migration verifier script is required"
    verifier = P0_RUNTIME_VERIFIER_PATH.read_text(encoding="utf-8")

    for env_key in [
        "P0_RUNTIME_DB_HOST",
        "P0_RUNTIME_DB_PORT",
        "P0_RUNTIME_DB_NAME",
        "P0_RUNTIME_DB_USER",
        "P0_RUNTIME_DB_PASSWORD",
    ]:
        assert env_key in verifier

    assert "root" not in verifier
    assert "123456" not in verifier
    assert "production_submit_event_id" in verifier
    assert "event_idempotency_key" in verifier
    assert "recordbook_entry_id" in verifier
    assert "review_signature_id" in verifier
    assert "review_signature_snapshot_json" in verifier
    assert "uk_mes_pro_process_pool_event_idem" in verifier
    assert "idx_mes_pro_process_pool_pqc_submit_event" in verifier
    assert "idx_mes_pro_process_pool_fragment_submit_event" in verifier
    assert "idx_mes_pp_review_signature" in verifier
    assert "P0_RUNTIME_MIGRATION_MISSING_COLUMN" in verifier
    assert "P0_RUNTIME_MIGRATION_MISSING_INDEX" in verifier
    assert "P0_RUNTIME_HISTORICAL_BROKEN_LINK" in verifier
    assert "rawPayload" not in verifier


def test_p0_runtime_migration_verifier_stops_history_when_schema_is_missing():
    assert P0_RUNTIME_VERIFIER_PATH.exists(), "P0 runtime migration verifier script is required"
    verifier = P0_RUNTIME_VERIFIER_PATH.read_text(encoding="utf-8")

    assert "schema_blockers = []" in verifier
    assert "P0_RUNTIME_SCHEMA_BLOCKED" in verifier
    assert "if schema_blockers:" in verifier
    assert verifier.index("if schema_blockers:") < verifier.index("verify_history(cursor)")


def test_p0_runtime_migration_apply_preflight_contract():
    assert P0_RUNTIME_APPLY_PREFLIGHT_PATH.exists(), "P0 runtime migration apply preflight script is required"
    preflight = P0_RUNTIME_APPLY_PREFLIGHT_PATH.read_text(encoding="utf-8")

    assert "P0_RUNTIME_APPLY_PREFLIGHT_BLOCKED" in preflight
    assert "P0_RUNTIME_APPLY_PREFLIGHT_PQC_BACKFILL_REQUIRED" in preflight
    assert "P0_RUNTIME_APPLY_PREFLIGHT_EVENT_IDEMPOTENCY_BACKFILL_REQUIRED" in preflight
    assert "P0_RUNTIME_APPLY_PREFLIGHT_RECORDBOOK_BACKFILL_REQUIRED" in preflight
    assert "P0_RUNTIME_APPLY_PREFLIGHT_FRAGMENT_ROOT_BACKFILL_REQUIRED" in preflight
    assert "P0_RUNTIME_APPLY_PREFLIGHT_EVENT_IDEMPOTENCY_DUPLICATE" in preflight
    assert "production_submit_event_id" in preflight
    assert "event_idempotency_key" in preflight
    assert "recordbook_entry_id" in preflight
    assert "review_signature_snapshot_json" in preflight
    assert "rawPayload" not in preflight
    assert "123456" not in preflight


def test_p0_runtime_backfill_source_audit_contract():
    assert P0_RUNTIME_BACKFILL_SOURCE_AUDIT_PATH.exists(), "P0 runtime backfill source audit script is required"
    audit = P0_RUNTIME_BACKFILL_SOURCE_AUDIT_PATH.read_text(encoding="utf-8")

    assert "P0_RUNTIME_BACKFILL_SOURCE_BLOCKED" in audit
    assert "P0_RUNTIME_BACKFILL_PQC_SOURCE_UNDERIVABLE" in audit
    assert "P0_RUNTIME_BACKFILL_EVENT_IDEMPOTENCY_SOURCE_UNDERIVABLE" in audit
    assert "P0_RUNTIME_BACKFILL_RECORDBOOK_ENTRY_SOURCE_UNDERIVABLE" in audit
    assert "P0_RUNTIME_BACKFILL_FRAGMENT_ROOT_SOURCE_UNDERIVABLE" in audit
    assert "pqc_requires_unique_formal_production_submit_event" in audit
    assert "production_submit_idempotency_requires_formal_recordbook_source" in audit
    assert "production_submit_recordbook_entry_requires_existing_formal_entry" in audit
    assert "quantity_fragment_requires_existing_production_submit_event" in audit
    assert "mes_pro_edhr_recordbook_entry" in audit
    assert "mes_pro_edhr_recordbook_event" in audit
    assert "rawPayload" not in audit
    assert "raw_payload" not in audit
    assert "123456" not in audit
    assert "UPDATE " not in audit
    assert "DELETE " not in audit
    assert "ALTER TABLE" not in audit


def test_p0_runtime_backfill_repair_plan_contract():
    assert P0_RUNTIME_BACKFILL_REPAIR_PLAN_PATH.exists(), "P0 runtime backfill repair plan gate is required"
    repair_plan = P0_RUNTIME_BACKFILL_REPAIR_PLAN_PATH.read_text(encoding="utf-8")

    assert "P0_RUNTIME_BACKFILL_REPAIR_PLAN_BLOCKED" in repair_plan
    assert "P0_RUNTIME_BACKFILL_REPAIR_AUTHORIZATION_REQUIRED" in repair_plan
    assert "P0_RUNTIME_BACKFILL_REPAIR_UNDERIVABLE_SOURCE" in repair_plan
    assert "P0_RUNTIME_BACKFILL_REPAIR_NO_DB_WRITE" in repair_plan
    assert "acceptableFormalSources" in repair_plan
    assert "authorizationRequirements" in repair_plan
    assert "rollbackRequirements" in repair_plan
    assert "postRepairVerification" in repair_plan
    assert "verify_p0_runtime_backfill_sources.py" in repair_plan
    assert "verify_p0_runtime_migration_apply_preflight.py" in repair_plan
    assert "rawPayload" not in repair_plan
    assert "raw_payload" not in repair_plan
    assert "123456" not in repair_plan
    assert "UPDATE " not in repair_plan
    assert "DELETE " not in repair_plan
    assert "ALTER TABLE" not in repair_plan


def test_p0_runtime_backfill_repair_manifest_contract():
    assert P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_PATH.exists(), "P0 runtime backfill repair manifest gate is required"
    manifest_gate = P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_PATH.read_text(encoding="utf-8")

    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_MISSING" in manifest_gate
    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_BLOCKED" in manifest_gate
    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_NO_DB_WRITE" in manifest_gate
    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_AUTHORIZATION_MISSING" in manifest_gate
    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_BACKUP_MISSING" in manifest_gate
    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_ROLLBACK_MISSING" in manifest_gate
    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_ENTRY_INVALID" in manifest_gate
    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_FORMAL_SOURCE_INVALID" in manifest_gate
    assert "P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_DRY_RUN_MISMATCH" in manifest_gate
    assert "repairManifestSchema" in manifest_gate
    assert "authorization" in manifest_gate
    assert "backupEvidence" in manifest_gate
    assert "rollbackEvidence" in manifest_gate
    assert "entries" in manifest_gate
    assert "formalSourceType" in manifest_gate
    assert "formalSourceId" in manifest_gate
    assert "oldValue" in manifest_gate
    assert "newValue" in manifest_gate
    assert "rawPayload" not in manifest_gate
    assert "raw_payload" not in manifest_gate
    assert "123456" not in manifest_gate
    assert "UPDATE " not in manifest_gate
    assert "DELETE " not in manifest_gate
    assert "ALTER TABLE" not in manifest_gate


if __name__ == "__main__":
    test_process_pool_sql_creates_dedicated_tables()
    test_process_pool_event_sql_requires_traceable_context_and_unique_signature()
    test_process_pool_quantity_and_pqc_records_are_linked_to_event()
    test_p0_runtime_migration_verifier_contract()
    test_p0_runtime_migration_verifier_stops_history_when_schema_is_missing()
    test_p0_runtime_migration_apply_preflight_contract()
    test_p0_runtime_backfill_source_audit_contract()
    test_p0_runtime_backfill_repair_plan_contract()
    test_p0_runtime_backfill_repair_manifest_contract()
    print("PASS: MES process pool SQL contract")
