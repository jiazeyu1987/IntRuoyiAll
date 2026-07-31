-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260729_dcc_product_catalog_project_code_columns; type=schema; riskLevel=medium
-- Add maintainable DCC file category match rules for project-code associated file classification.

CREATE TABLE IF NOT EXISTS `dcc_file_category_match_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL COMMENT 'dcc_file_category.id',
  `match_text` varchar(255) NOT NULL COMMENT 'Text or extension to match',
  `match_type` varchar(32) NOT NULL COMMENT 'CONTAINS, EXACT, PREFIX, SUFFIX, or EXTENSION',
  `weight` int NOT NULL DEFAULT 0 COMMENT 'Higher score wins over built-in aliases',
  `active` bit(1) NOT NULL DEFAULT b'1',
  `remark` varchar(255) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_file_category_match_rule_category` (`tenant_id`, `category_id`, `active`, `deleted`),
  KEY `idx_dcc_file_category_match_rule_type` (`tenant_id`, `match_type`, `active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
