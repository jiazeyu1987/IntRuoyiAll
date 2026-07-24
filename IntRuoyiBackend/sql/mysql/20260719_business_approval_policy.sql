-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center; type=schema; riskLevel=medium
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `bpm_business_approval_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `tenant_id` bigint NOT NULL COMMENT 'tenant id',
  `data_domain` varchar(64) NOT NULL COMMENT 'data domain',
  `system_code` varchar(64) NOT NULL COMMENT 'system code',
  `object_type` varchar(64) NOT NULL COMMENT 'business object type',
  `action_code` varchar(64) NOT NULL COMMENT 'business action code',
  `object_state` varchar(64) NOT NULL COMMENT 'business object state',
  `policy_mode` varchar(32) NOT NULL COMMENT 'BPM_REQUIRED, SIGNATURE_REQUIRED or DIRECT',
  `process_definition_key` varchar(128) DEFAULT NULL COMMENT 'BPM process definition key',
  `effect_executor_code` varchar(128) NOT NULL COMMENT 'domain effect executor code',
  `status` varchar(32) NOT NULL COMMENT 'DRAFT or PUBLISHED or DISABLED',
  `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
  `published_match_key` varchar(512) GENERATED ALWAYS AS (
    CASE
      WHEN `deleted` = b'0' AND `status` = 'PUBLISHED'
      THEN CONCAT_WS('|', `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`)
      ELSE NULL
    END
  ) VIRTUAL,
  `creator` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_business_approval_policy_published` (`published_match_key`),
  KEY `idx_bpm_business_approval_policy_match` (`tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`, `status`),
  KEY `idx_bpm_business_approval_policy_executor` (`tenant_id`, `effect_executor_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='business approval policy';

CREATE TABLE IF NOT EXISTS `bpm_business_approval_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `tenant_id` bigint NOT NULL COMMENT 'tenant id',
  `policy_id` bigint NOT NULL COMMENT 'policy id',
  `policy_mode` varchar(32) NOT NULL COMMENT 'BPM_REQUIRED, SIGNATURE_REQUIRED or DIRECT',
  `data_domain` varchar(64) NOT NULL COMMENT 'data domain',
  `system_code` varchar(64) NOT NULL COMMENT 'system code',
  `object_type` varchar(64) NOT NULL COMMENT 'business object type',
  `object_id` varchar(128) NOT NULL COMMENT 'business object id',
  `object_version` varchar(128) NOT NULL COMMENT 'business object version',
  `action_code` varchar(64) NOT NULL COMMENT 'business action code',
  `object_state` varchar(64) NOT NULL COMMENT 'business object state',
  `request_status` varchar(32) NOT NULL COMMENT 'business approval request status',
  `applicant_user_id` bigint NOT NULL COMMENT 'applicant user id',
  `process_definition_key` varchar(128) DEFAULT NULL COMMENT 'BPM process definition key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT 'BPM process instance id',
  `effect_executor_code` varchar(128) NOT NULL COMMENT 'domain effect executor code',
  `last_event_key` varchar(256) DEFAULT NULL COMMENT 'last handled BPM event key',
  `result_state` varchar(64) DEFAULT NULL COMMENT 'domain result state',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT 'failure reason',
  `business_context_json` longtext NOT NULL COMMENT 'business context json',
  `pending_match_key` varchar(512) GENERATED ALWAYS AS (
    CASE
      WHEN `deleted` = b'0' AND `request_status` = 'PENDING_BPM'
      THEN CONCAT_WS('|', `tenant_id`, `system_code`, `object_type`, `object_id`, `object_version`, `action_code`)
      ELSE NULL
    END
  ) VIRTUAL,
  `creator` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_business_approval_request_pending` (`pending_match_key`),
  UNIQUE KEY `uk_bpm_business_approval_request_process` (`tenant_id`, `process_instance_id`),
  KEY `idx_bpm_business_approval_request_object` (`tenant_id`, `system_code`, `object_type`, `object_id`, `object_version`, `action_code`, `request_status`),
  KEY `idx_bpm_business_approval_request_event` (`tenant_id`, `last_event_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='business approval request';
