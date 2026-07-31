-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_dcc_file_type_taxonomy; type=schema; riskLevel=medium
-- DCC maintainable file-category metadata match rules for project-code associated-file classification.

CREATE TABLE IF NOT EXISTS `dcc_file_category_match_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT NOT NULL COMMENT 'DCC file category id',
  `match_text` VARCHAR(255) NOT NULL COMMENT 'Normalized text or extension to match',
  `match_type` VARCHAR(32) NOT NULL DEFAULT 'CONTAINS' COMMENT 'CONTAINS/EXACT/PREFIX/SUFFIX/EXTENSION',
  `weight` INT NOT NULL DEFAULT 0 COMMENT 'Rule score weight; higher beats broad legacy aliases',
  `active` TINYINT NOT NULL DEFAULT 1 COMMENT 'Whether this rule is enabled',
  `remark` VARCHAR(255) NULL COMMENT 'Rule note',
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_file_category_match_rule_unique` (`tenant_id`, `category_id`, `match_text`, `match_type`, `deleted`),
  KEY `idx_dcc_file_category_match_rule_category` (`tenant_id`, `category_id`, `active`, `deleted`),
  KEY `idx_dcc_file_category_match_rule_type` (`tenant_id`, `match_type`, `active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DCC file category metadata match rule';
