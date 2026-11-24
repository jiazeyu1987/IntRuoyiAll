-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261124_erp_order_sync_integrity; type=schema; riskLevel=medium
-- Apply before deploying the retry-aware application; do not roll back columns while it is running.
ALTER TABLE erp_kingdee_table_auto_sync_plan
    ADD COLUMN auto_attempt_count int NOT NULL DEFAULT 0 COMMENT 'Automatic attempts on last_auto_attempt_date',
    ADD COLUMN last_auto_attempt_time datetime NULL COMMENT 'Beijing time of automatic claim';
-- Preserve legacy attempts conservatively: they already consumed one attempt today.
UPDATE erp_kingdee_table_auto_sync_plan
SET auto_attempt_count = 1,
    last_auto_attempt_time = COALESCE(last_run_time, CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+08:00'))
WHERE last_auto_attempt_date IS NOT NULL;
