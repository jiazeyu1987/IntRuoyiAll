-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260906_dcc_new_file_lifecycle_p1; type=schema; riskLevel=medium
-- Additive checkout history and check-in evidence for the new-file lifecycle.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_p2_column;

DELIMITER //
CREATE PROCEDURE ensure_dcc_p2_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*) INTO @dcc_p2_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = target_table AND COLUMN_NAME = target_column;
  SET @dcc_p2_column_sql = IF(@dcc_p2_column_count = 0, ddl_statement,
      CONCAT('SELECT ''', target_table, '.', target_column, ' already exists'' AS migration_status'));
  PREPARE dcc_p2_column_stmt FROM @dcc_p2_column_sql;
  EXECUTE dcc_p2_column_stmt;
  DEALLOCATE PREPARE dcc_p2_column_stmt;
END//
DELIMITER ;

CALL ensure_dcc_p2_column('dcc_controlled_file', 'checked_out_reason',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `checked_out_reason` varchar(500) DEFAULT NULL COMMENT ''检出原因'' AFTER `checked_out_time`');
CALL ensure_dcc_p2_column('dcc_controlled_file', 'predecessor_controlled_file_id',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `predecessor_controlled_file_id` bigint DEFAULT NULL COMMENT ''直接前驱版本'' AFTER `version_no`');
CALL ensure_dcc_p2_column('dcc_controlled_file', 'source_sha256',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `source_sha256` varchar(64) DEFAULT NULL COMMENT ''本版本源文件 SHA-256'' AFTER `predecessor_controlled_file_id`');
CALL ensure_dcc_p2_column('dcc_controlled_file', 'previous_source_sha256',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `previous_source_sha256` varchar(64) DEFAULT NULL COMMENT ''前驱版本源文件 SHA-256'' AFTER `source_sha256`');
CALL ensure_dcc_p2_column('dcc_controlled_file', 'change_description',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `change_description` varchar(1000) DEFAULT NULL COMMENT ''检入修改说明'' AFTER `previous_source_sha256`');

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_checkout` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `master_id` bigint NOT NULL,
  `base_iteration_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `reason` varchar(500) NOT NULL,
  `base_source_sha256` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `active_master_id` bigint GENERATED ALWAYS AS (CASE WHEN `status` = 'ACTIVE' THEN `master_id` ELSE NULL END) STORED,
  `checkin_upload_ticket` varchar(128) DEFAULT NULL,
  `checkin_iteration_id` bigint DEFAULT NULL,
  `checkin_source_file_id` bigint DEFAULT NULL,
  `checkin_source_sha256` varchar(64) DEFAULT NULL,
  `checked_in_time` datetime DEFAULT NULL,
  `cancel_reason` varchar(500) DEFAULT NULL,
  `cancelled_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` varchar(64) DEFAULT '',
  `updater` varchar(64) DEFAULT '',
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_checkout_active_master` (`tenant_id`, `active_master_id`),
  UNIQUE KEY `uk_dcc_checkout_checkin_ticket` (`tenant_id`, `checkin_upload_ticket`),
  KEY `idx_dcc_checkout_base_iteration` (`tenant_id`, `base_iteration_id`, `id`),
  KEY `idx_dcc_checkout_actor` (`tenant_id`, `actor_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='DCC checkout lock and immutable check-in history';

CALL ensure_dcc_p2_column('dcc_controlled_file_checkout', 'cancel_reason',
  'ALTER TABLE `dcc_controlled_file_checkout` ADD COLUMN `cancel_reason` varchar(500) DEFAULT NULL AFTER `checked_in_time`');
CALL ensure_dcc_p2_column('dcc_controlled_file_checkout', 'cancelled_time',
  'ALTER TABLE `dcc_controlled_file_checkout` ADD COLUMN `cancelled_time` datetime DEFAULT NULL AFTER `cancel_reason`');

DROP PROCEDURE IF EXISTS ensure_dcc_p2_column;
