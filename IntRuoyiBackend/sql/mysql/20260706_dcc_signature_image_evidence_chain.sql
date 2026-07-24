-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Add DCC electronic signature image version table and signature record image evidence snapshots.

CREATE TABLE IF NOT EXISTS `dcc_electronic_signature_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `file_id` bigint NOT NULL,
  `file_url` varchar(512) NOT NULL,
  `storage_path` varchar(512) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `content_type` varchar(128) NOT NULL,
  `file_size` bigint NOT NULL,
  `sha256` varchar(128) NOT NULL,
  `image_status` varchar(32) NOT NULL,
  `active` tinyint NOT NULL DEFAULT 0,
  `uploaded_by` bigint NOT NULL,
  `uploaded_at` datetime NOT NULL,
  `enabled_at` datetime DEFAULT NULL,
  `disabled_at` datetime DEFAULT NULL,
  `disable_reason` varchar(500) DEFAULT NULL,
  `referenced_count` int NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_signature_image_user_version` (`tenant_id`, `user_id`, `version_no`, `deleted`),
  KEY `idx_dcc_signature_image_user_active` (`tenant_id`, `user_id`, `active`, `deleted`),
  KEY `idx_dcc_signature_image_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC electronic signature image versions';

DROP PROCEDURE IF EXISTS ensure_dcc_column;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
      AND COLUMN_NAME = target_column
  ) THEN
    SET @ddl = ddl_statement;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_id',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_id` bigint DEFAULT NULL AFTER `controlled_copy_hash_status`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_version_no',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_version_no` int DEFAULT NULL AFTER `signature_image_id`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_file_id',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_file_id` bigint DEFAULT NULL AFTER `signature_image_version_no`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_file_url',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_file_url` varchar(512) DEFAULT NULL AFTER `signature_image_file_id`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_sha256',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_sha256` varchar(128) DEFAULT NULL AFTER `signature_image_file_url`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_content_type',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_content_type` varchar(128) DEFAULT NULL AFTER `signature_image_sha256`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_file_size',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_file_size` bigint DEFAULT NULL AFTER `signature_image_content_type`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_status_snapshot',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_status_snapshot` varchar(32) DEFAULT NULL AFTER `signature_image_file_size`');
CALL ensure_dcc_column('dcc_controlled_file_signature', 'signature_image_verified_status',
  'ALTER TABLE `dcc_controlled_file_signature` ADD COLUMN `signature_image_verified_status` varchar(32) DEFAULT NULL AFTER `signature_image_status_snapshot`');

DROP PROCEDURE IF EXISTS ensure_dcc_column;
