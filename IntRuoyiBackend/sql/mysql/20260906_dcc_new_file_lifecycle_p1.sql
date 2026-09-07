-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Additive P1 model fields for new DCC logical-file identity and server-owned A/1.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_p1_column;
DROP PROCEDURE IF EXISTS ensure_dcc_p1_index;
DROP PROCEDURE IF EXISTS drop_dcc_p1_index;

DELIMITER //
CREATE PROCEDURE ensure_dcc_p1_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*) INTO @dcc_p1_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = target_table AND COLUMN_NAME = target_column;
  SET @dcc_p1_column_sql = IF(@dcc_p1_column_count = 0, ddl_statement,
      CONCAT('SELECT ''', target_table, '.', target_column, ' already exists'' AS migration_status'));
  PREPARE dcc_p1_column_stmt FROM @dcc_p1_column_sql;
  EXECUTE dcc_p1_column_stmt;
  DEALLOCATE PREPARE dcc_p1_column_stmt;
END//

CREATE PROCEDURE ensure_dcc_p1_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*) INTO @dcc_p1_index_count
    FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = target_table AND INDEX_NAME = target_index;
  SET @dcc_p1_index_sql = IF(@dcc_p1_index_count = 0, ddl_statement,
      CONCAT('SELECT ''', target_table, '.', target_index, ' already exists'' AS migration_status'));
  PREPARE dcc_p1_index_stmt FROM @dcc_p1_index_sql;
  EXECUTE dcc_p1_index_stmt;
  DEALLOCATE PREPARE dcc_p1_index_stmt;
END//

CREATE PROCEDURE drop_dcc_p1_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64))
BEGIN
  SELECT COUNT(*) INTO @dcc_p1_drop_index_count
    FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = target_table AND INDEX_NAME = target_index;
  SET @dcc_p1_drop_index_sql = IF(@dcc_p1_drop_index_count = 0,
      CONCAT('SELECT ''', target_table, '.', target_index, ' already absent'' AS migration_status'),
      CONCAT('ALTER TABLE `', target_table, '` DROP INDEX `', target_index, '`'));
  PREPARE dcc_p1_drop_index_stmt FROM @dcc_p1_drop_index_sql;
  EXECUTE dcc_p1_drop_index_stmt;
  DEALLOCATE PREPARE dcc_p1_drop_index_stmt;
END//
DELIMITER ;

CALL ensure_dcc_p1_column('dcc_controlled_file_master', 'dcc_project_code_id',
  'ALTER TABLE `dcc_controlled_file_master` ADD COLUMN `dcc_project_code_id` bigint DEFAULT NULL AFTER `file_number`');
CALL ensure_dcc_p1_column('dcc_controlled_file_master', 'file_type_taxonomy_leaf_id',
  'ALTER TABLE `dcc_controlled_file_master` ADD COLUMN `file_type_taxonomy_leaf_id` bigint DEFAULT NULL AFTER `dcc_project_code_id`');
CALL ensure_dcc_p1_column('dcc_controlled_file_master', 'normalized_file_number',
  'ALTER TABLE `dcc_controlled_file_master` ADD COLUMN `normalized_file_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL AFTER `file_type_taxonomy_leaf_id`');
CALL ensure_dcc_p1_column('dcc_controlled_file', 'revision_code',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `revision_code` varchar(8) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL AFTER `version_no`');
CALL ensure_dcc_p1_column('dcc_controlled_file', 'iteration_no',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `iteration_no` int DEFAULT NULL AFTER `revision_code`');

-- Existing historical rows are retained unchanged. Their nullable new identity fields do not
-- collide with the non-null identity written only by the new-file lifecycle.

CALL drop_dcc_p1_index('dcc_controlled_file_master', 'uk_dcc_controlled_file_master_chain');
CALL ensure_dcc_p1_index('dcc_controlled_file_master', 'uk_dcc_new_logical_file_identity',
  'ALTER TABLE `dcc_controlled_file_master` ADD UNIQUE KEY `uk_dcc_new_logical_file_identity` (`tenant_id`, `dcc_project_code_id`, `file_type_taxonomy_leaf_id`, `normalized_file_number`)');
CALL ensure_dcc_p1_index('dcc_controlled_file_master', 'idx_dcc_master_new_identity_lookup',
  'ALTER TABLE `dcc_controlled_file_master` ADD KEY `idx_dcc_master_new_identity_lookup` (`tenant_id`, `dcc_project_code_id`, `file_type_taxonomy_leaf_id`, `normalized_file_number`)');

DROP PROCEDURE IF EXISTS ensure_dcc_p1_column;
DROP PROCEDURE IF EXISTS ensure_dcc_p1_index;
DROP PROCEDURE IF EXISTS drop_dcc_p1_index;
