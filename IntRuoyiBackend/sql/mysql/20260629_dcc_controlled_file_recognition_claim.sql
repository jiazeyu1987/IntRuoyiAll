-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260629_dcc_controlled_file_recognition_record; type=schema; riskLevel=medium
-- Add cross-worker file-level recognition claim table for DCC controlled-file basic-info recognition.

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_recognition_claim` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `recognition_scope` varchar(32) NOT NULL,
  `claimed_by` bigint NOT NULL,
  `claim_task_id` bigint DEFAULT NULL,
  `claimed_at` datetime NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_file_recognition_claim_scope`
    (`controlled_file_id`, `recognition_scope`),
  KEY `idx_dcc_file_recognition_claim_task`
    (`tenant_id`, `claim_task_id`, `recognition_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
