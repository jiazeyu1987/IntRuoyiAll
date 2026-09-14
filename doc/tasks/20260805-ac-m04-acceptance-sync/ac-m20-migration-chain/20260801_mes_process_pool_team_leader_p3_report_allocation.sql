-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260731_mes_process_pool_team_leader_p1_runtime_config; type=schema; riskLevel=medium
-- MES 生产组长工作台 P3：报工确认到活跃订单分配明细

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_report_allocation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `event_id` bigint NOT NULL COMMENT '来源工序池提交事件ID',
    `review_id` bigint NOT NULL COMMENT '班组长复核记录ID',
    `leader_user_id` bigint NOT NULL COMMENT '确认生产组长用户ID',
    `active_order_id` bigint NOT NULL COMMENT '班组活跃订单ID',
    `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
    `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `allocated_quantity` decimal(24,6) NOT NULL COMMENT '确认分配数量',
    `allocation_mode` varchar(32) NOT NULL COMMENT '分配方式：FIFO/MANUAL',
    `confirmed_at` datetime NOT NULL COMMENT '确认时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_mes_pp_report_alloc_event` (`tenant_id`, `event_id`),
    KEY `idx_mes_pp_report_alloc_review` (`tenant_id`, `review_id`),
    KEY `idx_mes_pp_report_alloc_work_order_process` (`tenant_id`, `work_order_id`, `route_process_id`, `process_id`),
    KEY `idx_mes_pp_report_alloc_active_order` (`tenant_id`, `active_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池生产组长报工确认分配明细';
