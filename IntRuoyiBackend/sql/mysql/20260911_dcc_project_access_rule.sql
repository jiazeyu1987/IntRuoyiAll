-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `dcc_project_access_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `dcc_project_code_id` bigint NOT NULL,
  `subject_type` varchar(32) NOT NULL,
  `subject_id` bigint NOT NULL,
  `access_level` varchar(16) NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  `valid_from` datetime NULL,
  `expire_time` datetime NULL,
  `change_reason` varchar(512) NOT NULL,
  `create_time` datetime NULL,
  `update_time` datetime NULL,
  `creator` varchar(64) NULL,
  `updater` varchar(64) NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `active_rule_unique_flag` tinyint GENERATED ALWAYS AS (
    CASE WHEN `active` = b'1' AND `deleted` = b'0' THEN 1 ELSE NULL END
  ) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_project_access_rule_active` (`tenant_id`,`dcc_project_code_id`,`subject_type`,`subject_id`,`active_rule_unique_flag`),
  KEY `idx_dcc_project_access_rule_project` (`tenant_id`,`dcc_project_code_id`,`active`,`deleted`),
  CONSTRAINT `ck_dcc_project_access_subject_type` CHECK (`subject_type` IN ('USER','DEPT','ROLE','POSITION')),
  CONSTRAINT `ck_dcc_project_access_level` CHECK (`access_level` IN ('OWNER','EDIT','VIEW')),
  CONSTRAINT `ck_dcc_project_access_reason` CHECK (CHAR_LENGTH(TRIM(`change_reason`)) > 0),
  CONSTRAINT `ck_dcc_project_access_validity` CHECK (`valid_from` IS NULL OR `expire_time` IS NULL OR `expire_time` > `valid_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC project authoritative access rules';
