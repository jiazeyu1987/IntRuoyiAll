from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260617_erp_kingdee_event_callback.sql"


def test_kingdee_event_callback_migration_has_release_metadata() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium"
    )
    assert "CREATE TABLE IF NOT EXISTS `erp_kingdee_event_callback`" in sql


def test_kingdee_event_callback_migration_has_dedupe_and_query_indexes() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "UNIQUE KEY `uk_erp_kingdee_event_callback_event_key` (`event_key`)" in sql
    assert "KEY `idx_erp_kingdee_event_callback_form_bill` (`source_form_id`, `source_bill_no`)" in sql
    assert "KEY `idx_erp_kingdee_event_callback_status_time` (`status`, `event_time`)" in sql


def test_kingdee_event_callback_migration_persists_signature_and_raw_payload() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "`signature` varchar(128) NOT NULL COMMENT '回调签名'" in sql
    assert "`nonce` varchar(64) NOT NULL COMMENT '回调随机串'" in sql
    assert "`callback_timestamp` varchar(64) NOT NULL COMMENT '回调时间戳'" in sql
    assert "`raw_payload` longtext NOT NULL COMMENT '原始请求体'" in sql
    assert "`tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号'" in sql
