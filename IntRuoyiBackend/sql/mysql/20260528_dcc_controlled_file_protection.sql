-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- DCC controlled file protection schema foundation.

DELIMITER //

DROP PROCEDURE IF EXISTS ensure_dcc_column//
CREATE PROCEDURE ensure_dcc_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
      AND COLUMN_NAME = target_column
  ) THEN
    SET @dcc_protection_ddl = ddl_statement;
    PREPARE dcc_protection_stmt FROM @dcc_protection_ddl;
    EXECUTE dcc_protection_stmt;
    DEALLOCATE PREPARE dcc_protection_stmt;
  END IF;
END//

DROP PROCEDURE IF EXISTS ensure_dcc_index//
CREATE PROCEDURE ensure_dcc_index(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
      AND INDEX_NAME = target_index
  ) THEN
    SET @dcc_protection_ddl = ddl_statement;
    PREPARE dcc_protection_stmt FROM @dcc_protection_ddl;
    EXECUTE dcc_protection_stmt;
    DEALLOCATE PREPARE dcc_protection_stmt;
  END IF;
END//

DELIMITER ;

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_access_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `access_event_code` varchar(64) NOT NULL,
  `controlled_file_id` bigint NOT NULL,
  `file_version_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `access_type` varchar(32) NOT NULL,
  `purpose` varchar(64) NOT NULL,
  `result` varchar(32) NOT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `source_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `request_id` varchar(128) DEFAULT NULL,
  `occurred_at` datetime NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_access_event_code` (`tenant_id`, `access_event_code`),
  KEY `idx_dcc_protection_access_event_file` (`controlled_file_id`, `file_version_no`),
  KEY `idx_dcc_protection_access_event_user_time` (`user_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file access event';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_watermark_trace` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trace_code` varchar(64) NOT NULL,
  `access_event_id` bigint NOT NULL,
  `access_event_code` varchar(64) NOT NULL,
  `controlled_file_id` bigint NOT NULL,
  `file_number` varchar(64) NOT NULL,
  `file_version_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `user_identifier` varchar(64) DEFAULT NULL,
  `user_display_name` varchar(128) DEFAULT NULL,
  `dept_id` bigint DEFAULT NULL,
  `dept_name` varchar(128) DEFAULT NULL,
  `tenant_name` varchar(128) DEFAULT NULL,
  `privacy_mode` varchar(32) NOT NULL,
  `watermark_payload_json` longtext NOT NULL,
  `issued_at` datetime NOT NULL,
  `expires_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_watermark_trace_code` (`tenant_id`, `trace_code`),
  KEY `idx_dcc_protection_watermark_event` (`access_event_id`),
  KEY `idx_dcc_protection_watermark_file` (`controlled_file_id`, `file_version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file watermark trace';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_upload_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `policy_code` varchar(64) NOT NULL,
  `scope_type` varchar(32) NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `purpose` varchar(64) DEFAULT NULL,
  `max_bytes` bigint NOT NULL,
  `enabled` bit(1) NOT NULL,
  `priority` int NOT NULL,
  `policy_version` varchar(64) NOT NULL,
  `effective_from` datetime DEFAULT NULL,
  `effective_to` datetime DEFAULT NULL,
  `change_reason` varchar(500) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_upload_policy_code` (`tenant_id`, `policy_code`),
  UNIQUE KEY `uk_dcc_protection_upload_policy_scope` (`tenant_id`, `scope_type`, `category_id`, `purpose`, `policy_version`),
  KEY `idx_dcc_protection_upload_policy_lookup` (`tenant_id`, `scope_type`, `enabled`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file upload size policy';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_temporary_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `upload_ticket` varchar(64) NOT NULL,
  `session_id` varchar(128) NOT NULL,
  `purpose` varchar(64) NOT NULL,
  `uploader_id` bigint NOT NULL,
  `original_file_name` varchar(255) NOT NULL,
  `content_type` varchar(255) DEFAULT NULL,
  `file_size` bigint NOT NULL,
  `file_sha256` varchar(128) NOT NULL,
  `storage_file_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL,
  `expire_time` datetime NOT NULL,
  `bound_controlled_file_id` bigint DEFAULT NULL,
  `bound_time` datetime DEFAULT NULL,
  `cleanup_status` varchar(32) NOT NULL,
  `cleanup_reason` varchar(500) DEFAULT NULL,
  `cleanup_time` datetime DEFAULT NULL,
  `request_id` varchar(128) DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_upload_ticket` (`tenant_id`, `upload_ticket`),
  KEY `idx_dcc_protection_temp_session` (`tenant_id`, `session_id`, `purpose`, `uploader_id`),
  KEY `idx_dcc_protection_temp_request` (`tenant_id`, `request_id`, `uploader_id`),
  KEY `idx_dcc_protection_temp_status` (`tenant_id`, `status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file temporary upload file';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_download_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `download_request_id` varchar(64) NOT NULL,
  `access_event_id` bigint NOT NULL,
  `access_event_code` varchar(64) NOT NULL,
  `controlled_file_id` bigint NOT NULL,
  `file_version_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `policy_version` varchar(64) NOT NULL,
  `encryption_status` varchar(32) NOT NULL,
  `encryption_policy_version` varchar(64) DEFAULT NULL,
  `artifact_id` varchar(128) DEFAULT NULL,
  `cipher_file_ref` varchar(255) DEFAULT NULL,
  `plain_sha256` varchar(128) DEFAULT NULL,
  `cipher_sha256` varchar(128) DEFAULT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `requested_at` datetime NOT NULL,
  `encrypted_at` datetime DEFAULT NULL,
  `returned_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_protection_download_request` (`tenant_id`, `download_request_id`),
  KEY `idx_dcc_protection_download_event` (`access_event_id`),
  KEY `idx_dcc_protection_download_file` (`controlled_file_id`, `file_version_no`),
  KEY `idx_dcc_protection_download_user_time` (`user_id`, `requested_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file encrypted download record';

ALTER TABLE `dcc_controlled_file_access_log`
  MODIFY COLUMN `controlled_file_id` bigint DEFAULT NULL;

CALL ensure_dcc_column('dcc_controlled_file_access_log', 'access_event_id',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD COLUMN `access_event_id` bigint DEFAULT NULL AFTER `controlled_file_id`');
CALL ensure_dcc_column('dcc_controlled_file_access_log', 'access_event_code',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD COLUMN `access_event_code` varchar(64) DEFAULT NULL AFTER `access_event_id`');
CALL ensure_dcc_column('dcc_controlled_file_access_log', 'watermark_trace_code',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD COLUMN `watermark_trace_code` varchar(64) DEFAULT NULL AFTER `access_event_code`');
CALL ensure_dcc_column('dcc_controlled_file_access_log', 'file_version_no',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD COLUMN `file_version_no` varchar(64) DEFAULT NULL AFTER `watermark_trace_code`');
CALL ensure_dcc_column('dcc_controlled_file_access_log', 'purpose',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD COLUMN `purpose` varchar(64) DEFAULT NULL AFTER `action_type`');
CALL ensure_dcc_column('dcc_controlled_file_access_log', 'request_id',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD COLUMN `request_id` varchar(128) DEFAULT NULL AFTER `source_ip`');
CALL ensure_dcc_column('dcc_controlled_file_access_log', 'user_agent',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD COLUMN `user_agent` varchar(512) DEFAULT NULL AFTER `request_id`');
CALL ensure_dcc_column('dcc_controlled_file_access_log', 'failure_code',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD COLUMN `failure_code` varchar(64) DEFAULT NULL AFTER `result`');

CALL ensure_dcc_index('dcc_controlled_file_access_log', 'idx_dcc_access_log_event',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD INDEX `idx_dcc_access_log_event` (`access_event_id`)');
CALL ensure_dcc_index('dcc_controlled_file_access_log', 'idx_dcc_access_log_event_code',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD INDEX `idx_dcc_access_log_event_code` (`access_event_code`)');
CALL ensure_dcc_index('dcc_controlled_file_access_log', 'idx_dcc_access_log_request',
  'ALTER TABLE `dcc_controlled_file_access_log` ADD INDEX `idx_dcc_access_log_request` (`request_id`)');
CALL ensure_dcc_index('dcc_controlled_file_temporary_file', 'idx_dcc_protection_temp_request',
  'ALTER TABLE `dcc_controlled_file_temporary_file` ADD INDEX `idx_dcc_protection_temp_request` (`tenant_id`, `request_id`, `uploader_id`)');

DROP PROCEDURE IF EXISTS ensure_dcc_index;
DROP PROCEDURE IF EXISTS ensure_dcc_column;
