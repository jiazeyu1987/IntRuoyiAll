-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Add DCC controlled-file batch recognition task persistence table.
-- Safe to run repeatedly on MySQL runtime schemas.

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_batch_recognition_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_user_id` bigint NOT NULL,
  `scope_type` varchar(16) NOT NULL COMMENT 'CURRENT or GLOBAL',
  `recognition_version_snapshot` varchar(64) NOT NULL,
  `directory_id` bigint DEFAULT NULL,
  `directory_path_snapshot` varchar(512) DEFAULT NULL,
  `keyword` varchar(255) DEFAULT NULL,
  `status_filter` varchar(32) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `overwrite_existing` bit(1) NOT NULL DEFAULT b'0',
  `existing_record_policy` varchar(32) NOT NULL DEFAULT 'SKIP_ALL_EXISTING' COMMENT 'SKIP_ALL_EXISTING, RETRY_FAILED, OVERWRITE_ALL',
  `sync_file_name_title` bit(1) NOT NULL DEFAULT b'1',
  `worker_count` int NOT NULL DEFAULT 1,
  `candidate_ids_json` longtext NOT NULL,
  `status` varchar(16) NOT NULL,
  `total_count` bigint NOT NULL DEFAULT 0,
  `processed_count` bigint NOT NULL DEFAULT 0,
  `success_count` bigint NOT NULL DEFAULT 0,
  `failed_count` bigint NOT NULL DEFAULT 0,
  `skipped_existing_count` bigint NOT NULL DEFAULT 0,
  `remaining_count` bigint NOT NULL DEFAULT 0,
  `last_failure_message` varchar(2048) DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_batch_recognition_task_status` (`status`, `id`),
  KEY `idx_dcc_batch_recognition_task_operator` (`operator_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
