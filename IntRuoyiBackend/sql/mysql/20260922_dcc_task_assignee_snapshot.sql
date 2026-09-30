-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260922_dcc_three_workflow_bpmn_seed; type=schema; riskLevel=medium
-- DCC per-department task assignee snapshot. One department obligation is one row; do not deduplicate by assignee_user_id.

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_task_assignee_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `node_instance_id` varchar(128) DEFAULT NULL,
  `stage_code` varchar(64) NOT NULL,
  `stage_no` int DEFAULT NULL,
  `department_id` bigint NOT NULL,
  `department_name` varchar(128) DEFAULT NULL,
  `assignee_user_id` bigint NOT NULL,
  `assignee_name` varchar(128) DEFAULT NULL,
  `leader_config_digest` varchar(255) DEFAULT NULL,
  `bpm_task_id` varchar(128) DEFAULT NULL,
  `obligation_id` varchar(128) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_task_assignee_obligation` (`tenant_id`, `controlled_file_id`, `stage_code`, `department_id`, `deleted`),
  UNIQUE KEY `uk_dcc_task_assignee_obligation_id` (`tenant_id`, `obligation_id`, `deleted`),
  KEY `idx_dcc_task_assignee_user` (`tenant_id`, `assignee_user_id`, `stage_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file task assignee snapshot';
