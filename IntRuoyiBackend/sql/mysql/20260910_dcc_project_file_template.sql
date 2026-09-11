-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_dcc_file_type_taxonomy; type=schema; riskLevel=medium
-- Editable file templates scoped to one DCC project code.

CREATE TABLE IF NOT EXISTS `dcc_project_file_template_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `project_code_id` BIGINT NOT NULL,
  `file_type_taxonomy_id` BIGINT NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `creator` VARCHAR(64) NULL,
  `create_time` DATETIME NULL,
  `updater` VARCHAR(64) NULL,
  `update_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `active_unique_flag` BIGINT GENERATED ALWAYS AS (
    CASE WHEN `deleted` = 0 THEN 1 ELSE NULL END
  ) STORED,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_dcc_project_file_template_name` CHECK (TRIM(`file_name`) <> ''),
  CONSTRAINT `chk_dcc_project_file_template_sort` CHECK (`sort_order` >= 0),
  UNIQUE KEY `uk_dcc_project_file_template_item_active`
    (`tenant_id`, `project_code_id`, `file_type_taxonomy_id`, `file_name`, `active_unique_flag`),
  KEY `idx_dcc_project_file_template_project`
    (`tenant_id`, `project_code_id`, `deleted`, `sort_order`),
  KEY `idx_dcc_project_file_template_taxonomy`
    (`tenant_id`, `file_type_taxonomy_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DCC project scoped file template item';
