-- release-migration: allowedEnvironments=test; dependsOn=20260905_dcc_source_governance; type=schema; riskLevel=low
-- Purpose: Align DCC source governance batch schema with the BaseDO tenant column contract.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @column_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_source_governance_batch'
      AND COLUMN_NAME = 'tenant_id'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE `dcc_controlled_file_source_governance_batch` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0 AFTER `id`',
    'SELECT 1'
);

PREPARE dcc_source_governance_batch_tenant_stmt FROM @ddl;
EXECUTE dcc_source_governance_batch_tenant_stmt;
DEALLOCATE PREPARE dcc_source_governance_batch_tenant_stmt;
