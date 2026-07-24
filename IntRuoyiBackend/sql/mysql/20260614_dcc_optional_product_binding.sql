-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Allow DCC NAS/local-folder transfer tasks without product binding.
-- Product-specific files can still store product_master_id; non-product files keep it NULL.

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_product_master_required_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'product_master_id'
  AND IS_NULLABLE = 'NO';

SET @dcc_optional_product_binding_sql = IF(
  @dcc_nas_transfer_task_product_master_required_count > 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` MODIFY COLUMN `product_master_id` bigint DEFAULT NULL COMMENT ''MDM product selected for DCC submit''',
  'SELECT ''dcc_controlled_file_nas_transfer_task.product_master_id already nullable'' AS migration_status'
);

PREPARE dcc_optional_product_binding_stmt FROM @dcc_optional_product_binding_sql;
EXECUTE dcc_optional_product_binding_stmt;
DEALLOCATE PREPARE dcc_optional_product_binding_stmt;
