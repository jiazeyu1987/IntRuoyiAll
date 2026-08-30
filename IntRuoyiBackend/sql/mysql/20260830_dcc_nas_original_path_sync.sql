-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_dcc_nas_uncontrolled_import_task_snapshot; type=schema; riskLevel=medium
-- Add first-class NAS original-path sync records for unrecognized quality files.

CREATE TABLE IF NOT EXISTS `dcc_nas_original_path_sync_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `audit_task_id` bigint NOT NULL COMMENT 'dcc_nas_control_audit_task.id',
  `audit_file_id` bigint NOT NULL COMMENT 'dcc_nas_control_audit_file.id',
  `transfer_task_id` bigint NOT NULL COMMENT 'dcc_controlled_file_nas_transfer_task.id',
  `transfer_task_item_id` bigint NOT NULL COMMENT 'dcc_controlled_file_nas_transfer_task_item.id',
  `source_file_id` bigint NOT NULL COMMENT 'infra file id storing NAS bytes',
  `nas_share_name` varchar(128) NOT NULL COMMENT 'NAS share name',
  `root_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Scanned NAS root path',
  `normalized_relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Normalized NAS relative path',
  `path_hash` char(64) NOT NULL COMMENT 'Hash of share and normalized relative path',
  `file_name` varchar(255) NOT NULL COMMENT 'NAS file name',
  `file_size` bigint NOT NULL COMMENT 'NAS file size snapshot',
  `modified_at` datetime NOT NULL COMMENT 'NAS modified time snapshot in UTC',
  `source_signature` char(64) NOT NULL COMMENT 'Snapshot signature of path hash, size, and modified time',
  `sync_status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DELETED',
  `synced_by_user_id` bigint NOT NULL COMMENT 'User who synced the file',
  `synced_at` datetime NOT NULL COMMENT 'Sync completion time',
  `deleted_by_user_id` bigint DEFAULT NULL COMMENT 'User who removed the active sync record',
  `deleted_at` datetime DEFAULT NULL COMMENT 'Removal time',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Soft delete flag',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT 'Tenant id',
  PRIMARY KEY (`id`),
  KEY `idx_dcc_nas_original_path_sync_path` (`tenant_id`, `nas_share_name`, `path_hash`, `sync_status`, `deleted`),
  KEY `idx_dcc_nas_original_path_sync_audit_file` (`tenant_id`, `audit_file_id`, `deleted`),
  KEY `idx_dcc_nas_original_path_sync_source_file` (`tenant_id`, `source_file_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC NAS original-path synced file';

DROP PROCEDURE IF EXISTS ensure_dcc_nas_original_path_sync_column;

DELIMITER $$

CREATE PROCEDURE ensure_dcc_nas_original_path_sync_column(
  IN target_table varchar(64),
  IN target_column varchar(64),
  IN ddl_statement text
)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_nas_original_path_sync_column_exists
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND COLUMN_NAME = target_column;

  IF @dcc_nas_original_path_sync_column_exists = 0 THEN
    SET @dcc_nas_original_path_sync_column_sql = ddl_statement;
    PREPARE dcc_nas_original_path_sync_column_stmt FROM @dcc_nas_original_path_sync_column_sql;
    EXECUTE dcc_nas_original_path_sync_column_stmt;
    DEALLOCATE PREPARE dcc_nas_original_path_sync_column_stmt;
  END IF;
END$$

DELIMITER ;

DROP PROCEDURE IF EXISTS ensure_dcc_nas_original_path_sync_index;

DELIMITER $$

CREATE PROCEDURE ensure_dcc_nas_original_path_sync_index(
  IN target_table varchar(64),
  IN target_index varchar(64),
  IN ddl_statement text
)
BEGIN
  SELECT COUNT(*)
  INTO @dcc_nas_original_path_sync_index_exists
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = target_table
    AND INDEX_NAME = target_index;

  IF @dcc_nas_original_path_sync_index_exists = 0 THEN
    SET @dcc_nas_original_path_sync_index_sql = ddl_statement;
    PREPARE dcc_nas_original_path_sync_index_stmt FROM @dcc_nas_original_path_sync_index_sql;
    EXECUTE dcc_nas_original_path_sync_index_stmt;
    DEALLOCATE PREPARE dcc_nas_original_path_sync_index_stmt;
  END IF;
END$$

DELIMITER ;

CALL ensure_dcc_nas_original_path_sync_column(
  'dcc_nas_control_audit_file',
  'original_path_sync_status',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_status` varchar(32) DEFAULT NULL COMMENT ''ORIGINAL_PATH_WAITING/ORIGINAL_PATH_RUNNING/ORIGINAL_PATH_ACTIVE/ORIGINAL_PATH_FAILED/ORIGINAL_PATH_DELETED'' AFTER `controlled_file_id`'
);
CALL ensure_dcc_nas_original_path_sync_column(
  'dcc_nas_control_audit_file',
  'original_path_sync_file_id',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_file_id` bigint DEFAULT NULL COMMENT ''dcc_nas_original_path_sync_file.id'' AFTER `original_path_sync_status`'
);
CALL ensure_dcc_nas_original_path_sync_column(
  'dcc_nas_control_audit_file',
  'original_path_sync_task_id',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_task_id` bigint DEFAULT NULL COMMENT ''dcc_controlled_file_nas_transfer_task.id'' AFTER `original_path_sync_file_id`'
);
CALL ensure_dcc_nas_original_path_sync_column(
  'dcc_nas_control_audit_file',
  'original_path_sync_task_item_id',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_task_item_id` bigint DEFAULT NULL COMMENT ''dcc_controlled_file_nas_transfer_task_item.id'' AFTER `original_path_sync_task_id`'
);
CALL ensure_dcc_nas_original_path_sync_column(
  'dcc_nas_control_audit_file',
  'original_path_sync_error_code',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_error_code` varchar(64) DEFAULT NULL COMMENT ''Original-path sync failure code'' AFTER `original_path_sync_task_item_id`'
);
CALL ensure_dcc_nas_original_path_sync_column(
  'dcc_nas_control_audit_file',
  'original_path_sync_error',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_error` varchar(512) DEFAULT NULL COMMENT ''Original-path sync failure detail'' AFTER `original_path_sync_error_code`'
);

CALL ensure_dcc_nas_original_path_sync_index(
  'dcc_nas_control_audit_file',
  'idx_dcc_nas_audit_file_original_sync',
  'ALTER TABLE `dcc_nas_control_audit_file` ADD INDEX `idx_dcc_nas_audit_file_original_sync` (`tenant_id`, `original_path_sync_status`, `original_path_sync_file_id`, `deleted`)'
);

DROP PROCEDURE IF EXISTS ensure_dcc_nas_original_path_sync_index;
DROP PROCEDURE IF EXISTS ensure_dcc_nas_original_path_sync_column;
