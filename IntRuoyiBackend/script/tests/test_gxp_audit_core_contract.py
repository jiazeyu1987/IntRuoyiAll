from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260908_gxp_audit_trail_core.sql"


def test_core_sql_has_release_migration_metadata():
    first_line = SQL.read_text(encoding="utf-8").splitlines()[0]
    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium"
    )


def test_core_sql_contains_required_append_only_tables_and_constraints():
    text = SQL.read_text(encoding="utf-8")
    required = [
        "CREATE TABLE IF NOT EXISTS `gxp_audit_event`",
        "CREATE TABLE IF NOT EXISTS `gxp_audit_policy_version`",
        "CREATE TABLE IF NOT EXISTS `gxp_audit_policy_operation`",
        "CREATE TABLE IF NOT EXISTS `gxp_audit_coverage_report`",
        "CREATE TABLE IF NOT EXISTS `gxp_audit_daily_manifest`",
        "CREATE TABLE IF NOT EXISTS `gxp_audit_seal_watermark`",
        "`idempotency_payload_hash` char(64) NOT NULL",
        "UNIQUE KEY `uk_gxp_audit_event_sequence` (`tenant_id`, `ledger_sequence`)",
        "UNIQUE KEY `uk_gxp_audit_event_idempotency` (`tenant_id`, `idempotency_key`)",
        "BEFORE UPDATE ON `gxp_audit_event`",
        "BEFORE DELETE ON `gxp_audit_event`",
        "BEFORE UPDATE ON `gxp_audit_daily_manifest`",
        "BEFORE DELETE ON `gxp_audit_daily_manifest`",
        "BEFORE UPDATE ON `gxp_audit_seal_watermark`",
        "BEFORE DELETE ON `gxp_audit_seal_watermark`",
    ]
    for item in required:
        assert item in text


def test_policy_operation_table_contains_runtime_base_fields():
    text = SQL.read_text(encoding="utf-8")
    start = text.index("CREATE TABLE IF NOT EXISTS `gxp_audit_policy_operation`")
    end = text.index("CREATE TABLE IF NOT EXISTS `gxp_audit_coverage_report`", start)
    policy_operation_sql = text[start:end]
    assert "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP" in policy_operation_sql
    assert "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" in policy_operation_sql
    assert "`creator` varchar(64) DEFAULT NULL" in policy_operation_sql
    assert "`updater` varchar(64) DEFAULT NULL" in policy_operation_sql
    assert "`deleted` bit(1) NOT NULL DEFAULT b'0'" in policy_operation_sql


def test_event_table_contains_runtime_base_fields():
    text = SQL.read_text(encoding="utf-8")
    start = text.index("CREATE TABLE IF NOT EXISTS `gxp_audit_event`")
    end = text.index("CREATE TABLE IF NOT EXISTS `gxp_audit_ledger_sequence`", start)
    event_sql = text[start:end]
    assert "`subject_id` varchar(2048) NOT NULL" in event_sql
    assert "`idempotency_key` varchar(512) NOT NULL" in event_sql
    assert "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP" in event_sql
    assert "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" in event_sql
    assert "`creator` varchar(64) DEFAULT NULL" in event_sql
    assert "`updater` varchar(64) DEFAULT NULL" in event_sql
    assert "`deleted` bit(1) NOT NULL DEFAULT b'0'" in event_sql
    assert "KEY `idx_gxp_audit_event_subject` (`tenant_id`, `domain`, `subject_type`, `subject_id`(191), `server_occurred_at`)" in event_sql


def test_core_sql_contains_existing_table_forward_migration_guards():
    text = SQL.read_text(encoding="utf-8")
    assert "CREATE PROCEDURE `ensure_gxp_audit_core_column`" in text
    assert "CREATE PROCEDURE `ensure_gxp_audit_core_index`" in text
    assert "CREATE PROCEDURE `drop_gxp_audit_core_index`" in text
    assert "information_schema.COLUMNS" in text
    assert "information_schema.STATISTICS" in text
    assert "CALL ensure_gxp_audit_core_column('gxp_audit_event', 'subject_id'" in text
    assert "CALL ensure_gxp_audit_core_column('gxp_audit_event', 'idempotency_key'" in text
    assert "MODIFY COLUMN `subject_id` varchar(2048) NOT NULL COMMENT '对象编号'" in text
    assert "MODIFY COLUMN `idempotency_key` varchar(512) NOT NULL COMMENT '幂等键'" in text
    assert "CALL drop_gxp_audit_core_index('gxp_audit_event', 'idx_gxp_audit_event_subject')" in text
    assert "CALL ensure_gxp_audit_core_index('gxp_audit_event', 'idx_gxp_audit_event_subject'" in text
    assert "CALL ensure_gxp_audit_core_column('gxp_audit_policy_operation', 'deleted'" in text


def test_core_sql_does_not_use_destructive_legacy_table_changes():
    text = SQL.read_text(encoding="utf-8").upper()
    assert "DROP TABLE" not in text
    assert "TRUNCATE TABLE" not in text
    assert "DELETE FROM" not in text
