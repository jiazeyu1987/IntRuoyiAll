-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=low
-- Preserve complete DCC lifecycle audit reasons, including nested failure messages.

ALTER TABLE `dcc_controlled_file_access_log`
  MODIFY COLUMN `reason` varchar(2000) DEFAULT NULL;
