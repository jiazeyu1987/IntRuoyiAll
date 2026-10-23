-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Reserve controlled-file names tenant-wide until the owning logical file is approved obsolete.

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_name_claim` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `normalized_name` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `master_id` BIGINT NOT NULL,
  `active_unique_flag` BIGINT GENERATED ALWAYS AS (
    CASE WHEN `deleted` = 0 THEN 1 ELSE NULL END
  ) STORED,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `creator` VARCHAR(64) DEFAULT NULL,
  `updater` VARCHAR(64) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_file_name_claim_active`
    (`tenant_id`, `normalized_name`, `active_unique_flag`),
  KEY `idx_dcc_file_name_claim_master`
    (`tenant_id`, `master_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='DCC tenant-wide controlled-file name reservation';
