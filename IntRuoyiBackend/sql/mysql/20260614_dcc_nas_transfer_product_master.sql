-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Persist the DCC product selected for NAS and local-folder transfer tasks.
-- Existing task rows are left nullable; request validation requires new tasks to provide the value.

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_product_master_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'product_master_id';

SET @dcc_nas_transfer_task_product_master_sql = IF(
  @dcc_nas_transfer_task_product_master_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `product_master_id` bigint DEFAULT NULL COMMENT ''MDM product selected for DCC submit'' AFTER `template_category_id`',
  'SELECT ''dcc_controlled_file_nas_transfer_task.product_master_id already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_product_master_stmt FROM @dcc_nas_transfer_task_product_master_sql;
EXECUTE dcc_nas_transfer_task_product_master_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_product_master_stmt;
