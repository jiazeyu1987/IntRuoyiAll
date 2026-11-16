-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_kingdee_multi_sync; type=schema; riskLevel=medium
-- Warehouse mappings belong to a tenant. Deleted mappings must not reserve a source key.
-- Keep existing records and warehouse IDs; create the replacement constraint before dropping the old one.
-- Recovery: the old global index can only be restored after checking for source keys shared by tenants
-- or deleted history. Stop for manual review if any exist; never delete mappings to force a rollback.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_kingdee_warehouse_tenant_active_unique;
DELIMITER //
CREATE PROCEDURE ensure_kingdee_warehouse_tenant_active_unique()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_kingdee_warehouse_sync_record'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'KINGDEE_WAREHOUSE_MAPPING_TABLE_MISSING';
  END IF;

  IF EXISTS (
    SELECT 1 FROM `erp_kingdee_warehouse_sync_record`
    WHERE `deleted` = b'0'
    GROUP BY `tenant_id`, `source_stock_org_number`, `source_warehouse_number`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'KINGDEE_WAREHOUSE_ACTIVE_DUPLICATES_REQUIRE_REVIEW';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_kingdee_warehouse_sync_record'
      AND COLUMN_NAME = 'active_source_flag'
  ) THEN
    ALTER TABLE `erp_kingdee_warehouse_sync_record`
      ADD COLUMN `active_source_flag` tinyint
        GENERATED ALWAYS AS (CASE WHEN `deleted` = b'0' THEN 1 ELSE NULL END) STORED
        COMMENT 'Only active mappings reserve a source key';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_kingdee_warehouse_sync_record'
      AND INDEX_NAME = 'uk_erp_kingdee_warehouse_tenant_active_source'
  ) THEN
    ALTER TABLE `erp_kingdee_warehouse_sync_record`
      ADD UNIQUE KEY `uk_erp_kingdee_warehouse_tenant_active_source`
        (`tenant_id`, `source_stock_org_number`, `source_warehouse_number`, `active_source_flag`);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_kingdee_warehouse_sync_record'
      AND INDEX_NAME = 'uk_erp_kingdee_warehouse_source'
  ) THEN
    ALTER TABLE `erp_kingdee_warehouse_sync_record`
      DROP INDEX `uk_erp_kingdee_warehouse_source`;
  END IF;
END//
DELIMITER ;

CALL ensure_kingdee_warehouse_tenant_active_unique();
DROP PROCEDURE IF EXISTS ensure_kingdee_warehouse_tenant_active_unique;
