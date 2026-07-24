-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Align DCC runtime tables with the repository's MySQL id-generation convention.
-- Existing seed rows keep their explicit ids; new inserts rely on AUTO_INCREMENT.

ALTER TABLE `dcc_file_directory`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_directory_access_rule`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_file_category`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_category_directory_binding`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_approval_position`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_position_assignment`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_category_approval_route`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_category_approval_route_node`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_controlled_file`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_controlled_file_route_snapshot`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_controlled_file_stamp`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `dcc_controlled_file_access_log`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;
