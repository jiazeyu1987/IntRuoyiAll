-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=high
-- Establish exclusive formal source ownership and restartable evidence for historical shared-source remediation.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_source_ownership` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `source_file_id` bigint NOT NULL,
  `origin_source_file_id` bigint NOT NULL,
  `source_sha256` varchar(64) NOT NULL,
  `ownership_type` varchar(32) NOT NULL,
  `claimed_by` bigint DEFAULT NULL,
  `claimed_time` datetime NOT NULL,
  `tenant_id` bigint NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_source_owner_file` (`tenant_id`, `controlled_file_id`),
  UNIQUE KEY `uk_dcc_source_owner_source` (`tenant_id`, `source_file_id`),
  KEY `idx_dcc_source_owner_origin` (`tenant_id`, `origin_source_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Exclusive ownership of one mutable DCC formal source file record';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_source_migration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `controlled_file_id` bigint NOT NULL,
  `legacy_source_file_id` bigint NOT NULL,
  `isolated_source_file_id` bigint DEFAULT NULL,
  `source_sha256` varchar(64) DEFAULT NULL,
  `migration_status` varchar(32) NOT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `migrated_by` bigint DEFAULT NULL,
  `migrated_time` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_source_migration_file` (`tenant_id`, `controlled_file_id`),
  KEY `idx_dcc_source_migration_status` (`tenant_id`, `migration_status`, `id`),
  KEY `idx_dcc_source_migration_legacy` (`tenant_id`, `legacy_source_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Restartable evidence for historical DCC shared source remediation';
