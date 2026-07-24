-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Add confirmed alias and directory mapping for DCC project-code recognition before Codex fallback.

CREATE TABLE IF NOT EXISTS `dcc_project_code_alias_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_code_id` bigint NOT NULL,
  `alias_text` varchar(255) NOT NULL,
  `normalized_alias_text` varchar(255) NOT NULL,
  `alias_source` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_project_alias_tenant_text` (`tenant_id`, `normalized_alias_text`, `alias_source`, `project_code_id`),
  KEY `idx_dcc_project_alias_status` (`tenant_id`, `status`, `active`),
  KEY `idx_dcc_project_alias_project` (`tenant_id`, `project_code_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC project code confirmed alias and directory mapping';

DROP PROCEDURE IF EXISTS ensure_dcc_alias_record_column;
DELIMITER //
CREATE PROCEDURE ensure_dcc_alias_record_column(
  IN table_name_value varchar(64),
  IN column_name_value varchar(64),
  IN ddl_sql text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_value
      AND COLUMN_NAME = column_name_value
  ) THEN
    SET @ddl_sql = ddl_sql;
    PREPARE stmt FROM @ddl_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL ensure_dcc_alias_record_column(
  'dcc_controlled_file_recognition_record',
  'matched_project_alias_id',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `matched_project_alias_id` bigint DEFAULT NULL AFTER `matched_project_code_id`'
);

CALL ensure_dcc_alias_record_column(
  'dcc_controlled_file_recognition_record',
  'matched_project_alias_text',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `matched_project_alias_text` varchar(255) DEFAULT NULL AFTER `matched_project_alias_id`'
);

CALL ensure_dcc_alias_record_column(
  'dcc_controlled_file_recognition_record',
  'matched_project_alias_source',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `matched_project_alias_source` varchar(32) DEFAULT NULL AFTER `matched_project_alias_text`'
);

DROP PROCEDURE IF EXISTS ensure_dcc_alias_record_column;
