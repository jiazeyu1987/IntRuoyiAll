-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_erp_base_schema,20260805_erp_kingdee_table_auto_sync; type=schema; riskLevel=medium
-- Preserve local line IDs referenced by receipts/shipments; adopt legacy identities from saved source snapshots in code.
-- No quantity backfill: unknown historical values must be checked against actual related documents.
SET @erp_sync_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
    AND table_name = 'erp_purchase_order_items' AND column_name = 'source_entry_id'),
    'SELECT 1', 'ALTER TABLE erp_purchase_order_items ADD COLUMN source_entry_id varchar(64) NULL COMMENT ''Kingdee EntryID''');
PREPARE erp_sync_stmt FROM @erp_sync_ddl;
EXECUTE erp_sync_stmt;
DEALLOCATE PREPARE erp_sync_stmt;

SET @erp_sync_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
    AND table_name = 'erp_sale_order_items' AND column_name = 'source_entry_id'),
    'SELECT 1', 'ALTER TABLE erp_sale_order_items ADD COLUMN source_entry_id varchar(64) NULL COMMENT ''Kingdee EntryID''');
PREPARE erp_sync_stmt FROM @erp_sync_ddl;
EXECUTE erp_sync_stmt;
DEALLOCATE PREPARE erp_sync_stmt;

SET @erp_sync_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
    AND table_name = 'erp_kingdee_table_auto_sync_plan' AND column_name = 'last_auto_attempt_date'),
    'SELECT 1', 'ALTER TABLE erp_kingdee_table_auto_sync_plan ADD COLUMN last_auto_attempt_date date NULL COMMENT ''最近自动尝试日期（含失败）''');
PREPARE erp_sync_stmt FROM @erp_sync_ddl;
EXECUTE erp_sync_stmt;
DEALLOCATE PREPARE erp_sync_stmt;
