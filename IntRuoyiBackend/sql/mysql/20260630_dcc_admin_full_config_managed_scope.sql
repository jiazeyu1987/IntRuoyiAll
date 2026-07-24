-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=low
CREATE TABLE IF NOT EXISTS `dcc_admin_full_config_managed_scope` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_codes_json` longtext NOT NULL COMMENT 'managed category code list JSON',
  `directory_paths_json` longtext NOT NULL COMMENT 'managed directory path list JSON',
  `approval_position_codes_json` longtext NOT NULL COMMENT 'managed approval position code list JSON',
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL DEFAULT NULL,
  `update_time` DATETIME NULL DEFAULT NULL,
  `creator` VARCHAR(64) NULL DEFAULT NULL,
  `updater` VARCHAR(64) NULL DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_admin_full_config_scope_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC admin full config managed scope';
