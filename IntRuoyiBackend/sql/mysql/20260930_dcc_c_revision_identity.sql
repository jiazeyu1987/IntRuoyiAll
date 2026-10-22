-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260917_dcc_controlled_file_name_claim,20260906_dcc_new_file_lifecycle_p3; type=schema; riskLevel=medium
-- C / IC-1. Add facts only. No historical business rows are updated or inferred.
-- Review original source names before reconciling legacy claims; unresolved claims fail closed.
-- Complete UTF-8 binary keys preserve case, extensions and trailing spaces; no prefix/hash collisions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_c_column;
DROP PROCEDURE IF EXISTS ensure_dcc_c_index;
DELIMITER //
CREATE PROCEDURE ensure_dcc_c_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl TEXT)
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=target_table AND COLUMN_NAME=target_column) THEN
  SET @dcc_c_sql=ddl;
  PREPARE dcc_c_stmt FROM @dcc_c_sql; EXECUTE dcc_c_stmt; DEALLOCATE PREPARE dcc_c_stmt;
 END IF;
END//
CREATE PROCEDURE ensure_dcc_c_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN should_exist BOOLEAN, IN ddl TEXT)
BEGIN
 IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=target_table AND INDEX_NAME=target_index) <> should_exist THEN
  SET @dcc_c_sql=ddl;
  PREPARE dcc_c_stmt FROM @dcc_c_sql; EXECUTE dcc_c_stmt; DEALLOCATE PREPARE dcc_c_stmt;
 END IF;
END//
DELIMITER ;
CALL ensure_dcc_c_column('dcc_controlled_file', 'revision_change_type', 'ALTER TABLE `dcc_controlled_file` ADD COLUMN `revision_change_type` varchar(32) DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file', 'revision_source_controlled_file_id', 'ALTER TABLE `dcc_controlled_file` ADD COLUMN `revision_source_controlled_file_id` bigint DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file', 'revision_source_version_no', 'ALTER TABLE `dcc_controlled_file` ADD COLUMN `revision_source_version_no` varchar(64) DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file', 'selected_iteration_controlled_file_id', 'ALTER TABLE `dcc_controlled_file` ADD COLUMN `selected_iteration_controlled_file_id` bigint DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file', 'selected_iteration_version_no', 'ALTER TABLE `dcc_controlled_file` ADD COLUMN `selected_iteration_version_no` varchar(64) DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file', 'source_original_file_name', 'ALTER TABLE `dcc_controlled_file` ADD COLUMN `source_original_file_name` varchar(256) CHARACTER SET utf8mb4 DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file_name_claim', 'source_original_file_name', 'ALTER TABLE `dcc_controlled_file_name_claim` ADD COLUMN `source_original_file_name` varchar(256) CHARACTER SET utf8mb4 DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file_name_claim', 'dcc_project_code_id', 'ALTER TABLE `dcc_controlled_file_name_claim` ADD COLUMN `dcc_project_code_id` bigint DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file_name_claim', 'file_type_taxonomy_leaf_id', 'ALTER TABLE `dcc_controlled_file_name_claim` ADD COLUMN `file_type_taxonomy_leaf_id` bigint DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file_name_claim', 'normalized_file_number', 'ALTER TABLE `dcc_controlled_file_name_claim` ADD COLUMN `normalized_file_number` varchar(128) CHARACTER SET utf8mb4 DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file_name_claim', 'obsolete_time', 'ALTER TABLE `dcc_controlled_file_name_claim` ADD COLUMN `obsolete_time` datetime DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file_name_claim', 'retain_until', 'ALTER TABLE `dcc_controlled_file_name_claim` ADD COLUMN `retain_until` datetime DEFAULT NULL');
CALL ensure_dcc_c_column('dcc_controlled_file_name_claim', 'source_name_key', 'ALTER TABLE `dcc_controlled_file_name_claim` ADD COLUMN `source_name_key` varbinary(1024) GENERATED ALWAYS AS (CONVERT(`source_original_file_name` USING binary)) STORED');
CALL ensure_dcc_c_column('dcc_controlled_file_name_claim', 'number_key', 'ALTER TABLE `dcc_controlled_file_name_claim` ADD COLUMN `number_key` varbinary(512) GENERATED ALWAYS AS (CONVERT(`normalized_file_number` USING binary)) STORED');
CALL ensure_dcc_c_column('dcc_controlled_file', 'c_version_key', 'ALTER TABLE `dcc_controlled_file` ADD COLUMN `c_version_key` varbinary(256) GENERATED ALWAYS AS (CASE WHEN `source_original_file_name` IS NOT NULL THEN CONVERT(`version_no` USING binary) ELSE NULL END) STORED');
CALL ensure_dcc_c_index('dcc_controlled_file_name_claim', 'uk_dcc_c_source_name', TRUE,
 'ALTER TABLE `dcc_controlled_file_name_claim` ADD UNIQUE KEY `uk_dcc_c_source_name` (`tenant_id`,`source_name_key`,`active_unique_flag`)');
CALL ensure_dcc_c_index('dcc_controlled_file_name_claim', 'uk_dcc_c_number', TRUE,
 'ALTER TABLE `dcc_controlled_file_name_claim` ADD UNIQUE KEY `uk_dcc_c_number` (`tenant_id`,`dcc_project_code_id`,`file_type_taxonomy_leaf_id`,`number_key`,`active_unique_flag`)');
-- Remove the former PAD SPACE/template-name constraint only after the exact binary constraint exists.
CALL ensure_dcc_c_index('dcc_controlled_file_name_claim', 'uk_dcc_file_name_claim_active', FALSE,
 'ALTER TABLE `dcc_controlled_file_name_claim` DROP INDEX `uk_dcc_file_name_claim_active`');
CALL ensure_dcc_c_index('dcc_controlled_file', 'uk_dcc_c_version', TRUE,
 'ALTER TABLE `dcc_controlled_file` ADD UNIQUE KEY `uk_dcc_c_version` (`tenant_id`,`master_id`,`c_version_key`)');
DROP PROCEDURE IF EXISTS ensure_dcc_c_column;
DROP PROCEDURE IF EXISTS ensure_dcc_c_index;
