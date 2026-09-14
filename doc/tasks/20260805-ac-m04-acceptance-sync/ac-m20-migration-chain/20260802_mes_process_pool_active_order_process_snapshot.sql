-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_process_pool_active_order_authority; type=schema; riskLevel=medium
-- MES 工序池 M2：统一 activeOrderId 的逐工序生产系数与目标数量快照

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order_process_snapshot` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `active_order_id` bigint NOT NULL COMMENT '活跃订单ID',
    `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
    `route_id` bigint NOT NULL COMMENT '正式工艺路线ID',
    `route_version_id` bigint NOT NULL COMMENT '正式工艺路线版本ID',
    `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `erp_fixed_quantity_snapshot` decimal(24,6) NOT NULL COMMENT 'ERP固定生产数量快照',
    `production_quantity_factor_snapshot` decimal(24,6) NOT NULL COMMENT '生产数量系数快照',
    `planned_quantity_snapshot` decimal(24,6) NOT NULL COMMENT '工序目标数量快照',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_active_order_process_snapshot` (`tenant_id`, `active_order_id`, `route_process_id`, `process_id`, `deleted`),
    KEY `idx_mes_pp_active_order_process_snapshot_order` (`tenant_id`, `work_order_id`, `route_process_id`, `process_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池活跃订单逐工序生产系数与目标数量快照';
