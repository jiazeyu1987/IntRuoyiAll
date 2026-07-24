-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- DCC controlled-file recognition link to dcc_project_code basic data.

DROP PROCEDURE IF EXISTS ensure_dcc_column;
DROP PROCEDURE IF EXISTS ensure_dcc_index;

DELIMITER //
CREATE PROCEDURE ensure_dcc_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_column_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND COLUMN_NAME = target_column;

  SET @dcc_column_sql = IF(
    @dcc_column_count = 0,
    ddl_statement,
    CONCAT('SELECT ''', target_table, '.', target_column, ' already exists'' AS migration_status')
  );

  PREPARE dcc_column_stmt FROM @dcc_column_sql;
  EXECUTE dcc_column_stmt;
  DEALLOCATE PREPARE dcc_column_stmt;
END//

CREATE PROCEDURE ensure_dcc_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_index_count
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND INDEX_NAME = target_index;

  SET @dcc_index_sql = IF(
    @dcc_index_count = 0,
    ddl_statement,
    CONCAT('SELECT ''', target_table, '.', target_index, ' already exists'' AS migration_status')
  );

  PREPARE dcc_index_stmt FROM @dcc_index_sql;
  EXECUTE dcc_index_stmt;
  DEALLOCATE PREPARE dcc_index_stmt;
END//
DELIMITER ;

ALTER TABLE `dcc_controlled_file`
  MODIFY COLUMN `product_code` varchar(255) DEFAULT NULL;

CALL ensure_dcc_column('dcc_controlled_file', 'dcc_project_code_id',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `dcc_project_code_id` bigint DEFAULT NULL AFTER `product_name`');

CALL ensure_dcc_column('dcc_controlled_file', 'project_code_recognition_type',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `project_code_recognition_type` varchar(32) DEFAULT NULL AFTER `dcc_project_code_id`');

CALL ensure_dcc_column('dcc_controlled_file', 'project_code_recognition_text',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `project_code_recognition_text` varchar(255) DEFAULT NULL AFTER `project_code_recognition_type`');

CALL ensure_dcc_column('dcc_controlled_file', 'project_code_recognized_by',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `project_code_recognized_by` bigint DEFAULT NULL AFTER `project_code_recognition_text`');

CALL ensure_dcc_column('dcc_controlled_file', 'project_code_recognized_time',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `project_code_recognized_time` datetime DEFAULT NULL AFTER `project_code_recognized_by`');

CALL ensure_dcc_index('dcc_controlled_file', 'idx_dcc_controlled_file_project_code',
  'ALTER TABLE `dcc_controlled_file` ADD INDEX `idx_dcc_controlled_file_project_code` (`tenant_id`, `dcc_project_code_id`)');

DROP PROCEDURE IF EXISTS ensure_dcc_column;
DROP PROCEDURE IF EXISTS ensure_dcc_index;
