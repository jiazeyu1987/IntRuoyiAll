-- release-migration: allowedEnvironments=test; dependsOn=20260811_dcc_source_ownership; type=schema; riskLevel=high
-- Additive governance manifest and item evidence for controlled source ownership remediation.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_source_governance_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_key` varchar(128) NOT NULL,
  `tenant_scope_json` text NOT NULL,
  `tenant_scope_sha256` varchar(64) NOT NULL,
  `snapshot_max_controlled_file_id` bigint NOT NULL,
  `effective_controlled_file_count` bigint NOT NULL DEFAULT 0,
  `rule_version` varchar(64) NOT NULL,
  `schema_version` varchar(32) NOT NULL,
  `manifest_sha256` varchar(64) NOT NULL,
  `request_sha256` varchar(64) NOT NULL,
  `batch_status` varchar(32) NOT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_time` datetime DEFAULT NULL,
  `completed_count` bigint NOT NULL DEFAULT 0,
  `blocked_count` bigint NOT NULL DEFAULT 0,
  `failed_count` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_source_governance_batch_task` (`task_key`),
  KEY `idx_dcc_source_governance_batch_status` (`batch_status`, `id`),
  KEY `idx_dcc_source_governance_batch_manifest` (`manifest_sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Frozen manifest and idempotency contract for DCC source governance';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_source_governance_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `controlled_file_id` bigint NOT NULL,
  `legacy_source_file_id` bigint DEFAULT NULL,
  `isolated_source_file_id` bigint DEFAULT NULL,
  `origin_source_file_id` bigint DEFAULT NULL,
  `snapshot_source_file_id` bigint DEFAULT NULL,
  `snapshot_source_sha256` varchar(64) DEFAULT NULL,
  `snapshot_location_hash` varchar(64) DEFAULT NULL,
  `snapshot_source_config_id` bigint DEFAULT NULL,
  `snapshot_source_path` varchar(512) DEFAULT NULL,
  `snapshot_source_deleted` bit(1) DEFAULT NULL,
  `source_sha256` varchar(64) DEFAULT NULL,
  `shared_group_key` varchar(128) DEFAULT NULL,
  `governance_action` varchar(32) NOT NULL,
  `item_status` varchar(32) NOT NULL,
  `blocker_reason_code` varchar(64) DEFAULT NULL,
  `blocker_detail` varchar(1000) DEFAULT NULL,
  `last_error` varchar(1000) DEFAULT NULL,
  `processed_by` bigint DEFAULT NULL,
  `processed_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_source_governance_item_file` (`batch_id`, `tenant_id`, `controlled_file_id`),
  KEY `idx_dcc_source_governance_item_status` (`batch_id`, `item_status`, `id`),
  KEY `idx_dcc_source_governance_item_blocker` (`batch_id`, `blocker_reason_code`, `id`),
  KEY `idx_dcc_source_governance_item_source` (`tenant_id`, `snapshot_source_file_id`),
  KEY `idx_dcc_source_governance_item_shared` (`batch_id`, `shared_group_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Per-controlled-file source evidence and governance decision';
