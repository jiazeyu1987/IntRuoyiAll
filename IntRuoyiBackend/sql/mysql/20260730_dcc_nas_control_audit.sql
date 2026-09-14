-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Add DCC NAS controlled-file audit source mapping and async report task tables.

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_nas_source` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL COMMENT 'dcc_controlled_file.id',
  `nas_share_name` varchar(128) NOT NULL COMMENT 'NAS share name',
  `normalized_relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Normalized path relative to NAS share',
  `path_hash` char(64) NOT NULL COMMENT 'SHA-256 of share and normalized path',
  `source_type` varchar(32) NOT NULL COMMENT 'NAS_TRANSFER or LEGACY_NAS_TRANSFER',
  `source_confidence` varchar(32) NOT NULL COMMENT 'EXACT, LEGACY_EXACT, or PENDING_CONFIRMATION',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_nas_source_controlled_file` (`tenant_id`, `controlled_file_id`, `deleted`),
  KEY `idx_dcc_nas_source_path_hash` (`tenant_id`, `nas_share_name`, `path_hash`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dcc_nas_control_audit_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_user_id` bigint NOT NULL,
  `nas_share_name` varchar(128) NOT NULL,
  `scan_roots_json` longtext NOT NULL,
  `status` varchar(32) NOT NULL,
  `current_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `scanned_file_count` bigint NOT NULL DEFAULT 0,
  `controlled_file_count` bigint NOT NULL DEFAULT 0,
  `not_controlled_file_count` bigint NOT NULL DEFAULT 0,
  `ambiguous_file_count` bigint NOT NULL DEFAULT 0,
  `source_missing_count` bigint NOT NULL DEFAULT 0,
  `skipped_directory_count` bigint NOT NULL DEFAULT 0,
  `report_file_id` bigint DEFAULT NULL,
  `report_file_name` varchar(255) DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `failure_reason` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_nas_control_audit_status` (`tenant_id`, `status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dcc_nas_control_audit_skipped_directory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `directory_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `skip_reason` varchar(64) NOT NULL,
  `skipped_at` datetime NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_nas_control_audit_skipped_task` (`tenant_id`, `task_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
