-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260910_dcc_project_file_template; type=schema; riskLevel=medium
ALTER TABLE `dcc_project_file_template_item`
  MODIFY COLUMN `file_type_taxonomy_id` BIGINT NULL,
  ADD COLUMN `taxonomy_unique_key` VARCHAR(21) GENERATED ALWAYS AS
    (CASE WHEN `file_type_taxonomy_id` IS NULL THEN 'none' ELSE CAST(`file_type_taxonomy_id` AS CHAR) END) STORED,
  DROP INDEX `uk_dcc_project_file_template_item_active`,
  ADD UNIQUE KEY `uk_dcc_project_file_template_item_active`
    (`tenant_id`, `project_code_id`, `taxonomy_unique_key`, `file_name`, `active_unique_flag`);
