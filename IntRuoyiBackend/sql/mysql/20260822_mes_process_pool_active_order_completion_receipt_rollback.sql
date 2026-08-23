-- rollback-migration: allowedEnvironments=test,backup,prod; requiresBackup=true; type=schema; riskLevel=high
-- Only execute after exporting the two Flow-4 tables and confirming Flow-6 has not consumed their receipts.
-- This script is intentionally manual and is not part of application startup.
DROP TABLE IF EXISTS `mes_pro_process_pool_active_order_completion_backfill`;
DROP TABLE IF EXISTS `mes_pro_process_pool_active_order_completion_receipt`;
