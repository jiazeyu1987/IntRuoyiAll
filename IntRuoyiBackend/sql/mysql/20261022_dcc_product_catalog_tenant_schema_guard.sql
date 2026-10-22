-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_dcc_product_catalog_database; type=schema; riskLevel=medium
-- Existing installations can predate the tenant column added to the bootstrap SQL.
-- Keep the global catalog's historical tenant value at 0; do not infer tenant ownership.
-- Rollback: remove tenant_id only after reverting code that reads or writes it and exporting the table.
SET NAMES utf8mb4;

DELIMITER $$
CREATE PROCEDURE migrate_dcc_product_catalog_tenant_20261022()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_product_catalog'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'dcc_product_catalog prerequisite missing';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_product_catalog' AND COLUMN_NAME = 'tenant_id'
  ) THEN
    ALTER TABLE dcc_product_catalog ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号' AFTER remark;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_product_catalog' AND COLUMN_NAME = 'tenant_id'
      AND DATA_TYPE = 'bigint' AND COLUMN_TYPE = 'bigint'
      AND IS_NULLABLE = 'NO' AND COLUMN_DEFAULT = '0' AND EXTRA = ''
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'dcc_product_catalog.tenant_id schema mismatch';
  END IF;
END$$
DELIMITER ;
CALL migrate_dcc_product_catalog_tenant_20261022();
DROP PROCEDURE migrate_dcc_product_catalog_tenant_20261022;
