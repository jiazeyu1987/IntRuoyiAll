-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260906_dcc_new_file_lifecycle_p2; type=schema; riskLevel=medium
-- Immutable snapshot of the current formal iteration captured when a new major revision is created.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_p3_column;

DELIMITER //
CREATE PROCEDURE ensure_dcc_p3_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*) INTO @dcc_p3_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = target_table AND COLUMN_NAME = target_column;
  SET @dcc_p3_column_sql = IF(@dcc_p3_column_count = 0, ddl_statement,
      CONCAT('SELECT ''', target_table, '.', target_column, ' already exists'' AS migration_status'));
  PREPARE dcc_p3_column_stmt FROM @dcc_p3_column_sql;
  EXECUTE dcc_p3_column_stmt;
  DEALLOCATE PREPARE dcc_p3_column_stmt;
END//
DELIMITER ;

CALL ensure_dcc_p3_column('dcc_controlled_file', 'revision_base_active_controlled_file_id',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `revision_base_active_controlled_file_id` bigint DEFAULT NULL COMMENT ''创建大版本时的当前正式版本快照'' AFTER `predecessor_controlled_file_id`');

DROP PROCEDURE IF EXISTS ensure_dcc_p3_column;
