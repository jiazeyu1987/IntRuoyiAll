-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
ALTER TABLE `erp_product`
    MODIFY COLUMN `standard` varchar(1024) DEFAULT NULL;
