-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260614_dcc_optional_product_binding; type=schema; riskLevel=low
-- Persist the authoritative DCC project-code source for NAS/local-folder transfer tasks.
-- Safe to run repeatedly on MySQL runtime schemas.

SELECT COUNT(1)
INTO @dcc_nas_transfer_task_project_code_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'dcc_project_code_id';

SET @dcc_nas_transfer_task_project_code_sql = IF(
  @dcc_nas_transfer_task_project_code_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `dcc_project_code_id` bigint DEFAULT NULL COMMENT ''DCC project code selected for DCC submit'' AFTER `template_category_id`',
  'SELECT ''dcc_controlled_file_nas_transfer_task.dcc_project_code_id already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_project_code_stmt FROM @dcc_nas_transfer_task_project_code_sql;
EXECUTE dcc_nas_transfer_task_project_code_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_project_code_stmt;
