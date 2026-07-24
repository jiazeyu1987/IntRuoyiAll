-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- System entitlement ledger and minimal eDHR filler policy.
-- Fail fast: this migration creates auditable dynamic entitlement tables and does not mutate static roles.

CREATE TABLE IF NOT EXISTS `system_entitlement_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `policy_code` varchar(100) NOT NULL,
  `policy_name` varchar(100) NOT NULL,
  `module_code` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` varchar(500) DEFAULT '',
  `allowed_permission_codes_json` json NOT NULL,
  `allowed_menu_refs_json` json DEFAULT NULL,
  `forbidden_permission_codes_json` json DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_entitlement_policy_code` (`policy_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System entitlement policy';

CREATE TABLE IF NOT EXISTS `system_entitlement_claim` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `source_type` varchar(100) NOT NULL,
  `source_key` varchar(255) NOT NULL,
  `source_version` varchar(100) DEFAULT NULL,
  `source_digest` varchar(500) DEFAULT NULL,
  `policy_code` varchar(100) NOT NULL,
  `subject_type` varchar(32) NOT NULL,
  `subject_id` bigint NOT NULL,
  `resolved_user_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL,
  `effective_at` datetime DEFAULT NULL,
  `revoked_at` datetime DEFAULT NULL,
  `last_sync_status` varchar(32) DEFAULT NULL,
  `last_sync_message` varchar(500) DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `operator_username` varchar(64) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_entitlement_claim_source_user`
    (`tenant_id`, `source_type`, `source_key`, `policy_code`, `resolved_user_id`),
  KEY `idx_system_entitlement_claim_user_status` (`tenant_id`, `resolved_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System entitlement claim';

CREATE TABLE IF NOT EXISTS `system_entitlement_grant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `subject_type` varchar(32) NOT NULL,
  `subject_id` bigint NOT NULL,
  `resolved_user_id` bigint NOT NULL,
  `permission_code` varchar(150) NOT NULL,
  `menu_id` bigint NOT NULL,
  `policy_code` varchar(100) NOT NULL,
  `active_claim_count` int NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_entitlement_grant_identity`
    (`tenant_id`, `resolved_user_id`, `permission_code`, `menu_id`, `policy_code`),
  KEY `idx_system_entitlement_grant_user_status` (`tenant_id`, `resolved_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System entitlement grant';

CREATE TABLE IF NOT EXISTS `system_entitlement_audit_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `source_type` varchar(100) NOT NULL,
  `source_key` varchar(255) NOT NULL,
  `policy_code` varchar(100) NOT NULL,
  `subject_type` varchar(32) DEFAULT NULL,
  `subject_id` bigint DEFAULT NULL,
  `before_digest` varchar(500) DEFAULT NULL,
  `after_digest` varchar(500) DEFAULT NULL,
  `result_status` varchar(32) NOT NULL,
  `message` varchar(500) DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `operator_username` varchar(64) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_system_entitlement_audit_source` (`tenant_id`, `source_type`, `source_key`, `policy_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System entitlement audit event';

INSERT INTO `system_entitlement_policy`
(`policy_code`, `policy_name`, `module_code`, `status`, `description`,
 `allowed_permission_codes_json`, `allowed_menu_refs_json`, `forbidden_permission_codes_json`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  'MES_EDHR_FILLER_MINIMAL',
  'MES eDHR filler minimal entitlement',
  'mes',
  0,
  'Sources: EDHR_PROCESS_FORM_FILLER, EDHR_WORK_TASK_ASSIGNEE',
  JSON_ARRAY(
    'mes:pro-edhr-batch-execution:query',
    'mes:pro-edhr-batch-execution:update',
    'mes:pro-batch-record-execution:query',
    'mes:pro-batch-record-execution:update',
    'mes:pro-batch-record-execution:track',
    'mes:pro-batch-record-execution:signature-query',
    'mes:pro-edhr-work-task:query'
  ),
  JSON_ARRAY(
    JSON_OBJECT('permission', 'mes:pro-edhr-batch-execution:query'),
    JSON_OBJECT('permission', 'mes:pro-edhr-batch-execution:update'),
    JSON_OBJECT('permission', 'mes:pro-batch-record-execution:query'),
    JSON_OBJECT('permission', 'mes:pro-batch-record-execution:update'),
    JSON_OBJECT('permission', 'mes:pro-batch-record-execution:track'),
    JSON_OBJECT('permission', 'mes:pro-batch-record-execution:signature-query'),
    JSON_OBJECT('permission', 'mes:pro-edhr-work-task:query')
  ),
  JSON_ARRAY(),
  '1',
  NOW(),
  '1',
  NOW(),
  b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_entitlement_policy`
  WHERE `policy_code` = 'MES_EDHR_FILLER_MINIMAL'
);
