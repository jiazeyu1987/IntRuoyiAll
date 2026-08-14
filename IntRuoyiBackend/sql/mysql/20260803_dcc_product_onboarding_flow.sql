-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260607_product_master_data; type=schema; riskLevel=medium
-- DCC product onboarding request and MDM product binding for project codes.

DROP PROCEDURE IF EXISTS ensure_dcc_onboarding_column;
DROP PROCEDURE IF EXISTS ensure_dcc_onboarding_index;

DELIMITER //
CREATE PROCEDURE ensure_dcc_onboarding_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_onboarding_column_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND COLUMN_NAME = target_column;

  SET @dcc_onboarding_column_sql = IF(
    @dcc_onboarding_column_count = 0,
    ddl_statement,
    CONCAT('SELECT ''', target_table, '.', target_column, ' already exists'' AS migration_status')
  );

  PREPARE dcc_onboarding_column_stmt FROM @dcc_onboarding_column_sql;
  EXECUTE dcc_onboarding_column_stmt;
  DEALLOCATE PREPARE dcc_onboarding_column_stmt;
END//

CREATE PROCEDURE ensure_dcc_onboarding_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_onboarding_index_count
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND INDEX_NAME = target_index;

  SET @dcc_onboarding_index_sql = IF(
    @dcc_onboarding_index_count = 0,
    ddl_statement,
    CONCAT('SELECT ''', target_table, '.', target_index, ' already exists'' AS migration_status')
  );

  PREPARE dcc_onboarding_index_stmt FROM @dcc_onboarding_index_sql;
  EXECUTE dcc_onboarding_index_stmt;
  DEALLOCATE PREPARE dcc_onboarding_index_stmt;
END//
DELIMITER ;

CALL ensure_dcc_onboarding_column('dcc_project_code', 'product_master_id',
  'ALTER TABLE `dcc_project_code` ADD COLUMN `product_master_id` bigint DEFAULT NULL COMMENT ''MDM product master id'' AFTER `id`');

CALL ensure_dcc_onboarding_index('dcc_project_code', 'idx_dcc_project_code_product',
  'ALTER TABLE `dcc_project_code` ADD INDEX `idx_dcc_project_code_product` (`tenant_id`, `product_master_id`)');

CREATE TABLE IF NOT EXISTS `dcc_product_onboarding_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_master_id` bigint DEFAULT NULL COMMENT 'Existing or generated MDM product id',
  `product_code` varchar(64) DEFAULT NULL COMMENT 'MDM product code snapshot',
  `dcc_product_code` varchar(14) DEFAULT NULL COMMENT 'DCC product code snapshot',
  `product_name_cn` varchar(255) DEFAULT NULL COMMENT 'Chinese product name snapshot',
  `product_name_en` varchar(255) DEFAULT NULL COMMENT 'English product name snapshot',
  `model_specification` varchar(255) DEFAULT NULL COMMENT 'Model/specification snapshot',
  `product_category` varchar(128) DEFAULT NULL COMMENT 'MDM product category snapshot',
  `doc_control_no` varchar(64) DEFAULT NULL,
  `project_name` varchar(255) NOT NULL,
  `project_code` varchar(64) NOT NULL DEFAULT '',
  `category` varchar(128) DEFAULT NULL,
  `commissioned_production` varchar(128) DEFAULT NULL,
  `project_leader` varchar(128) DEFAULT NULL,
  `project_engineer` varchar(128) DEFAULT NULL,
  `storage_location` varchar(128) DEFAULT NULL,
  `priority` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `applicant_user_id` bigint NOT NULL,
  `approver_user_id` bigint DEFAULT NULL,
  `approved_time` datetime DEFAULT NULL,
  `generated_project_code_id` bigint DEFAULT NULL,
  `reject_reason` varchar(512) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_product_onboarding_pending_project` (`tenant_id`, `project_name`, `project_code`, `status`, `deleted`),
  KEY `idx_dcc_product_onboarding_status` (`tenant_id`, `status`, `deleted`),
  KEY `idx_dcc_product_onboarding_product` (`tenant_id`, `product_master_id`, `deleted`),
  KEY `idx_dcc_product_onboarding_generated` (`tenant_id`, `generated_project_code_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC product onboarding request';

DROP PROCEDURE IF EXISTS ensure_dcc_onboarding_column;
DROP PROCEDURE IF EXISTS ensure_dcc_onboarding_index;
