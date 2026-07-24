-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Support browser-selected local folder imports through the existing DCC NAS transfer task pipeline.
-- Existing rows default to NAS; new local folder tasks use LOCAL_FOLDER and task items reference infra_file.id.

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_source_type_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'source_type';

SET @dcc_nas_transfer_task_source_type_sql = IF(
  @dcc_nas_transfer_task_source_type_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `source_type` varchar(32) NOT NULL DEFAULT ''NAS'' COMMENT ''NAS or LOCAL_FOLDER'' AFTER `selected_nas_paths_json`',
  'SELECT ''dcc_controlled_file_nas_transfer_task.source_type already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_source_type_stmt FROM @dcc_nas_transfer_task_source_type_sql;
EXECUTE dcc_nas_transfer_task_source_type_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_source_type_stmt;

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_item_source_file_id_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task_item'
  AND COLUMN_NAME = 'source_file_id';

SET @dcc_nas_transfer_task_item_source_file_id_sql = IF(
  @dcc_nas_transfer_task_item_source_file_id_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `source_file_id` bigint DEFAULT NULL COMMENT ''infra_file.id for LOCAL_FOLDER file items'' AFTER `item_name`',
  'SELECT ''dcc_controlled_file_nas_transfer_task_item.source_file_id already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_item_source_file_id_stmt FROM @dcc_nas_transfer_task_item_source_file_id_sql;
EXECUTE dcc_nas_transfer_task_item_source_file_id_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_item_source_file_id_stmt;
