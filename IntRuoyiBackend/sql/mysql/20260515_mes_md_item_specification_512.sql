-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
ALTER TABLE `mes_md_item`
  MODIFY COLUMN `specification` varchar(512) DEFAULT NULL;
