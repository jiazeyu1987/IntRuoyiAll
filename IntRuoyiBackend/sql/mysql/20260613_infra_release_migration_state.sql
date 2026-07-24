-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium

CREATE TABLE IF NOT EXISTS `infra_release_migration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `release_tag` varchar(128) NOT NULL COMMENT '发布标签',
  `migration_id` varchar(191) NOT NULL COMMENT 'Migration 标识',
  `file_name` varchar(512) NOT NULL COMMENT 'SQL 文件名',
  `sha256` varchar(64) NOT NULL COMMENT 'SQL 内容 SHA-256',
  `target_environment` varchar(32) NOT NULL COMMENT '目标环境：test/backup/prod',
  `status` varchar(32) NOT NULL COMMENT '状态：RUNNING/APPLIED/SKIPPED_ALREADY_APPLIED/FAILED',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `error_message` text DEFAULT NULL COMMENT '失败原因',
  `operation_id` varchar(128) NOT NULL COMMENT '发布操作 ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_infra_release_migration_env_id` (`target_environment`, `migration_id`),
  KEY `idx_infra_release_migration_operation` (`operation_id`),
  KEY `idx_infra_release_migration_release_tag` (`release_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布 Migration 状态';

CREATE TABLE IF NOT EXISTS `infra_release_operation_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `target_environment` varchar(32) NOT NULL COMMENT '目标环境',
  `operation_id` varchar(128) NOT NULL COMMENT '发布操作 ID',
  `release_tag` varchar(128) NOT NULL COMMENT '发布标签',
  `status` varchar(32) NOT NULL COMMENT '状态：RUNNING/APPLIED/FAILED',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `error_message` text DEFAULT NULL COMMENT '失败原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_infra_release_operation_lock_env` (`target_environment`),
  KEY `idx_infra_release_operation_lock_status` (`status`),
  KEY `idx_infra_release_operation_lock_release_tag` (`release_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布目标环境操作锁';
