-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Keep NAS paths and DCC imported file names as exact identifiers.
-- MySQL utf8mb4_unicode_ci can treat distinct NAS names as equal, for example ASCII I and Roman numeral I.

ALTER TABLE `dcc_controlled_file_nas_transfer_task_item`
  MODIFY `nas_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

ALTER TABLE `dcc_controlled_file_master`
  MODIFY `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

ALTER TABLE `dcc_controlled_file`
  MODIFY `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL;
