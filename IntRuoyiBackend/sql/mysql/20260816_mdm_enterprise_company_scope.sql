-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260607_product_master_data; type=schema; riskLevel=medium
-- Recovery: MySQL DDL auto-commits; on failure retain the pre-migration backup, inspect every named table and constraint, reconcile the partial schema, then rerun this idempotent migration.
-- Rollback before business use: drop mdm_role_company_scope, mdm_user_company_scope, then mdm_enterprise after verifying that no downstream object references them.
-- Rollback after business use: destructive table removal is forbidden; restore the pre-migration backup or deliver an approved forward migration.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mdm_enterprise` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Enterprise ID',
  `enterprise_code` varchar(64) NOT NULL COMMENT 'Stable enterprise code',
  `name` varchar(255) NOT NULL COMMENT 'Enterprise name',
  `type` varchar(32) NOT NULL COMMENT 'Enterprise type',
  `status` varchar(32) NOT NULL COMMENT 'Enable status',
  `revision` int NOT NULL DEFAULT 1 COMMENT 'Optimistic revision',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_mdm_enterprise_type` CHECK (`type` IN ('OWNED_COMPANY', 'ENTRUSTED_PARTY')),
  UNIQUE KEY `uk_mdm_enterprise_tenant_code` (`tenant_id`, `enterprise_code`),
  KEY `idx_mdm_enterprise_tenant_type_status` (`tenant_id`, `type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MDM enterprise master';

CREATE TABLE IF NOT EXISTS `mdm_user_company_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Mapping ID',
  `user_id` bigint NOT NULL COMMENT 'System user ID',
  `company_id` bigint NOT NULL COMMENT 'MDM owned company enterprise ID',
  `status` varchar(32) NOT NULL COMMENT 'Enable status',
  `revision` int NOT NULL DEFAULT 1 COMMENT 'Optimistic revision',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mdm_user_company_scope_tenant_user_company` (`tenant_id`, `user_id`, `company_id`),
  KEY `idx_mdm_user_company_scope_tenant_company_status` (`tenant_id`, `company_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MDM user company scope';

CREATE TABLE IF NOT EXISTS `mdm_role_company_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Mapping ID',
  `role_id` bigint NOT NULL COMMENT 'System role ID',
  `company_id` bigint NOT NULL COMMENT 'MDM owned company enterprise ID',
  `status` varchar(32) NOT NULL COMMENT 'Enable status',
  `revision` int NOT NULL DEFAULT 1 COMMENT 'Optimistic revision',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mdm_role_company_scope_tenant_role_company` (`tenant_id`, `role_id`, `company_id`),
  KEY `idx_mdm_role_company_scope_tenant_company_status` (`tenant_id`, `company_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MDM role company scope';
