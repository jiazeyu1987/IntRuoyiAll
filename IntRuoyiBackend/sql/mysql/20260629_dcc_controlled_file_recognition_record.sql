-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260623_dcc_browser_batch_recognition_task; type=schema; riskLevel=medium
-- Add recognition ledger for DCC controlled-file basic-info recognition.

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_recognition_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `recognition_scope` varchar(32) NOT NULL,
  `recognition_method` varchar(32) NOT NULL,
  `recognition_version` varchar(64) NOT NULL,
  `status` varchar(16) NOT NULL,
  `batch_task_id` bigint DEFAULT NULL,
  `matched_project_code_id` bigint DEFAULT NULL,
  `matched_project_alias_id` bigint DEFAULT NULL,
  `matched_project_alias_text` varchar(255) DEFAULT NULL,
  `matched_project_alias_source` varchar(32) DEFAULT NULL,
  `recognized_product_code` varchar(255) DEFAULT NULL,
  `recognized_product_name` varchar(255) DEFAULT NULL,
  `match_type` varchar(32) DEFAULT NULL,
  `match_text` varchar(255) DEFAULT NULL,
  `failure_stage` varchar(64) DEFAULT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_message` varchar(2048) DEFAULT NULL,
  `file_type_level1` varchar(64) DEFAULT NULL,
  `file_type_level2` varchar(128) DEFAULT NULL,
  `file_type_level3` varchar(128) DEFAULT NULL,
  `file_type_level4` varchar(128) DEFAULT NULL,
  `file_type_level5` varchar(128) DEFAULT NULL,
  `recognized_by` bigint DEFAULT NULL,
  `recognized_time` datetime DEFAULT NULL,
  `source_file_id` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_file_recognition_record_biz`
    (`controlled_file_id`, `recognition_scope`, `recognition_method`, `recognition_version`),
  KEY `idx_dcc_file_recognition_record_status`
    (`tenant_id`, `status`, `recognition_scope`, `recognition_version`),
  KEY `idx_dcc_file_recognition_record_batch`
    (`tenant_id`, `batch_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
