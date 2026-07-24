-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- DCC controlled browser lazy directory and summary-list indexes.

DROP PROCEDURE IF EXISTS ensure_dcc_browser_index;

DELIMITER //
CREATE PROCEDURE ensure_dcc_browser_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_browser_index_count
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND INDEX_NAME = target_index;

  SET @dcc_browser_index_sql = IF(
    @dcc_browser_index_count = 0,
    ddl_statement,
    CONCAT('SELECT ''', target_table, '.', target_index, ' already exists'' AS migration_status')
  );

  PREPARE dcc_browser_index_stmt FROM @dcc_browser_index_sql;
  EXECUTE dcc_browser_index_stmt;
  DEALLOCATE PREPARE dcc_browser_index_stmt;
END//
DELIMITER ;

CALL ensure_dcc_browser_index('dcc_file_directory', 'idx_dcc_directory_lazy_parent',
  'ALTER TABLE `dcc_file_directory` ADD INDEX `idx_dcc_directory_lazy_parent` (`tenant_id`, `deleted`, `parent_id`, `active`, `sort`, `id`)');

CALL ensure_dcc_browser_index('dcc_controlled_file', 'idx_dcc_controlled_file_browser_directory',
  'ALTER TABLE `dcc_controlled_file` ADD INDEX `idx_dcc_controlled_file_browser_directory` (`tenant_id`, `deleted`, `directory_id`, `status`, `create_time`, `id`)');

CALL ensure_dcc_browser_index('dcc_controlled_file', 'idx_dcc_controlled_file_browser_master',
  'ALTER TABLE `dcc_controlled_file` ADD INDEX `idx_dcc_controlled_file_browser_master` (`tenant_id`, `deleted`, `master_id`, `status`, `id`)');

DROP PROCEDURE IF EXISTS ensure_dcc_browser_index;
