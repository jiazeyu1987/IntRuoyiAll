-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
ALTER TABLE `dcc_controlled_file_master`
  MODIFY `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

ALTER TABLE `dcc_controlled_file`
  MODIFY `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL;

ALTER TABLE `dcc_controlled_file`
  MODIFY `title` varchar(256) NOT NULL;
