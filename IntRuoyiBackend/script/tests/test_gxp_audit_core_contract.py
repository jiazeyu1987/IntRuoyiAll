from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260908_gxp_audit_trail_core.sql"


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


def test_core_sql_does_not_use_destructive_legacy_table_changes():
    text = SQL.read_text(encoding="utf-8").upper()
    assert "DROP TABLE" not in text
    assert "TRUNCATE TABLE" not in text
    assert "DELETE FROM" not in text
