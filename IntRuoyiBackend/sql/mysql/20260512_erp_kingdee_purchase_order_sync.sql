-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `erp_kingdee_purchase_order_sync_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `source_form_id` varchar(64) NOT NULL COMMENT 'Kingdee FormId',
    `source_fid` varchar(64) NOT NULL COMMENT 'Kingdee FID',
    `source_bill_no` varchar(128) NOT NULL COMMENT 'Kingdee 单据编号',
    `purchase_order_id` bigint NOT NULL COMMENT 'IntRuoyi ERP 采购订单编号',
    `sync_status` tinyint NOT NULL COMMENT '同步状态',
    `failure_message` varchar(512) DEFAULT NULL COMMENT '失败信息',
    `raw_payload` longtext DEFAULT NULL COMMENT 'Kingdee 原始载荷快照',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_kingdee_po_sync_tenant_source` (`tenant_id`, `source_form_id`, `source_fid`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP Kingdee K3Cloud 采购订单同步记录';

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900001, '同步 Kingdee 采购订单', 'erp:purchase-order:sync-kingdee', 3, 7, 2666, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `permission` = 'erp:purchase-order:sync-kingdee'
);
