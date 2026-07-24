-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `mes_kingdee_production_order_sync_record` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `source_fid` varchar(64) NOT NULL COMMENT 'Kingdee source fid',
    `source_bill_no` varchar(128) NOT NULL COMMENT 'Kingdee source bill no',
    `source_material_number` varchar(64) NOT NULL COMMENT 'Kingdee material number',
    `work_order_id` bigint NOT NULL COMMENT 'Local MES work order id',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_kingdee_production_source` (`source_fid`, `source_material_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES Kingdee production order sync record';

CREATE TABLE IF NOT EXISTS `erp_kingdee_sale_order_sync_record` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `source_form_id` varchar(64) NOT NULL COMMENT 'Kingdee source form id',
    `source_fid` varchar(64) NOT NULL COMMENT 'Kingdee source fid',
    `source_bill_no` varchar(128) NOT NULL COMMENT 'Kingdee source bill no',
    `sale_order_id` bigint NOT NULL COMMENT 'Local ERP sale order id',
    `raw_payload` longtext DEFAULT NULL COMMENT 'Raw source payload',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_kingdee_sale_tenant_source` (`tenant_id`, `source_form_id`, `source_fid`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP Kingdee sale order sync record';

CREATE TABLE IF NOT EXISTS `erp_kingdee_customer_sync_record` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `source_customer_number` varchar(64) NOT NULL COMMENT 'Kingdee customer number',
    `source_customer_name` varchar(128) DEFAULT NULL COMMENT 'Kingdee customer name',
    `customer_id` bigint NOT NULL COMMENT 'Local ERP customer id',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_kingdee_customer_tenant_source` (`tenant_id`, `source_customer_number`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP Kingdee customer sync mapping';

CREATE TABLE IF NOT EXISTS `erp_kingdee_warehouse_sync_record` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `source_stock_org_number` varchar(64) NOT NULL COMMENT 'Kingdee stock org number',
    `source_stock_org_name` varchar(128) DEFAULT NULL COMMENT 'Kingdee stock org name',
    `source_warehouse_number` varchar(64) NOT NULL COMMENT 'Kingdee warehouse number',
    `source_warehouse_name` varchar(128) DEFAULT NULL COMMENT 'Kingdee warehouse name',
    `warehouse_id` bigint NOT NULL COMMENT 'Local ERP warehouse id',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_kingdee_warehouse_source` (`source_stock_org_number`, `source_warehouse_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP Kingdee warehouse sync mapping';
