-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
ALTER TABLE `dcc_controlled_file`
  MODIFY `remark` varchar(1024) DEFAULT NULL;
