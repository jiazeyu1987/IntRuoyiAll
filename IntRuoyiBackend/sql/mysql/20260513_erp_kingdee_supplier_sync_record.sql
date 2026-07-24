-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `erp_kingdee_supplier_sync_record` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `source_supplier_number` varchar(64) NOT NULL COMMENT 'Kingdee supplier number',
    `source_supplier_name` varchar(128) DEFAULT NULL COMMENT 'Kingdee supplier name',
    `supplier_id` bigint NOT NULL COMMENT 'Local ERP supplier id',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_kingdee_supplier_tenant_source` (`tenant_id`, `source_supplier_number`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP Kingdee supplier sync mapping';
