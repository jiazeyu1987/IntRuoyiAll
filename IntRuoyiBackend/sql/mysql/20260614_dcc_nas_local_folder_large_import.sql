-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Support large browser-selected local folder imports by tracking upload-session progress.
-- UPLOADING tasks are active but are not processed by the NAS transfer scheduler until completed.

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_expected_file_count_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'expected_file_count';

SET @dcc_nas_transfer_task_expected_file_count_sql = IF(
  @dcc_nas_transfer_task_expected_file_count_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `expected_file_count` bigint NOT NULL DEFAULT 0 COMMENT ''Expected files for LOCAL_FOLDER upload session'' AFTER `source_type`',
  'SELECT ''dcc_controlled_file_nas_transfer_task.expected_file_count already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_expected_file_count_stmt FROM @dcc_nas_transfer_task_expected_file_count_sql;
EXECUTE dcc_nas_transfer_task_expected_file_count_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_expected_file_count_stmt;

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_expected_total_bytes_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'expected_total_bytes';

SET @dcc_nas_transfer_task_expected_total_bytes_sql = IF(
  @dcc_nas_transfer_task_expected_total_bytes_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `expected_total_bytes` bigint NOT NULL DEFAULT 0 COMMENT ''Expected bytes for LOCAL_FOLDER upload session'' AFTER `expected_file_count`',
  'SELECT ''dcc_controlled_file_nas_transfer_task.expected_total_bytes already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_expected_total_bytes_stmt FROM @dcc_nas_transfer_task_expected_total_bytes_sql;
EXECUTE dcc_nas_transfer_task_expected_total_bytes_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_expected_total_bytes_stmt;

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_uploaded_file_count_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'uploaded_file_count';

SET @dcc_nas_transfer_task_uploaded_file_count_sql = IF(
  @dcc_nas_transfer_task_uploaded_file_count_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `uploaded_file_count` bigint NOT NULL DEFAULT 0 COMMENT ''Uploaded files for LOCAL_FOLDER upload session'' AFTER `expected_total_bytes`',
  'SELECT ''dcc_controlled_file_nas_transfer_task.uploaded_file_count already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_uploaded_file_count_stmt FROM @dcc_nas_transfer_task_uploaded_file_count_sql;
EXECUTE dcc_nas_transfer_task_uploaded_file_count_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_uploaded_file_count_stmt;

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_uploaded_total_bytes_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'uploaded_total_bytes';

SET @dcc_nas_transfer_task_uploaded_total_bytes_sql = IF(
  @dcc_nas_transfer_task_uploaded_total_bytes_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `uploaded_total_bytes` bigint NOT NULL DEFAULT 0 COMMENT ''Uploaded bytes for LOCAL_FOLDER upload session'' AFTER `uploaded_file_count`',
  'SELECT ''dcc_controlled_file_nas_transfer_task.uploaded_total_bytes already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_uploaded_total_bytes_stmt FROM @dcc_nas_transfer_task_uploaded_total_bytes_sql;
EXECUTE dcc_nas_transfer_task_uploaded_total_bytes_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_uploaded_total_bytes_stmt;

SELECT COUNT(*)
INTO @dcc_nas_transfer_task_upload_completed_at_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file_nas_transfer_task'
  AND COLUMN_NAME = 'upload_completed_at';

SET @dcc_nas_transfer_task_upload_completed_at_sql = IF(
  @dcc_nas_transfer_task_upload_completed_at_column_count = 0,
  'ALTER TABLE `dcc_controlled_file_nas_transfer_task` ADD COLUMN `upload_completed_at` datetime DEFAULT NULL COMMENT ''LOCAL_FOLDER upload completion time'' AFTER `uploaded_total_bytes`',
  'SELECT ''dcc_controlled_file_nas_transfer_task.upload_completed_at already exists'' AS migration_status'
);

PREPARE dcc_nas_transfer_task_upload_completed_at_stmt FROM @dcc_nas_transfer_task_upload_completed_at_sql;
EXECUTE dcc_nas_transfer_task_upload_completed_at_stmt;
DEALLOCATE PREPARE dcc_nas_transfer_task_upload_completed_at_stmt;
