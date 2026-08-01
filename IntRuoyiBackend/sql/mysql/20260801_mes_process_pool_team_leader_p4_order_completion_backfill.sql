-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260801_mes_process_pool_team_leader_p3_report_allocation; type=schema; riskLevel=medium
-- MES 生产组长工作台 P4：订单工序完成状态与正式批记录回填结果

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_order_process_completion` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
    `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `target_quantity` decimal(24,6) NOT NULL COMMENT '订单工序目标数量',
    `confirmed_quantity` decimal(24,6) NOT NULL COMMENT '当前累计确认分配数量',
    `completion_status` varchar(32) NOT NULL COMMENT '订单工序完成状态：IN_PROGRESS/COMPLETED',
    `completed_at` datetime DEFAULT NULL COMMENT '订单工序完成时间',
    `backfill_status` varchar(32) NOT NULL COMMENT '批记录回填状态：NOT_REQUIRED/SUCCESS',
    `backfill_execution_id` bigint DEFAULT NULL COMMENT '批记录执行实例ID',
    `backfill_error` varchar(1000) DEFAULT NULL COMMENT '批记录回填失败原因',
    `last_event_id` bigint NOT NULL COMMENT '最后一次触发的工序池提交事件ID',
    `last_review_id` bigint NOT NULL COMMENT '最后一次班组长复核记录ID',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_order_process_completion` (`tenant_id`, `work_order_id`, `route_process_id`, `process_id`, `deleted`),
    KEY `idx_mes_pp_order_process_completion_status` (`tenant_id`, `completion_status`, `backfill_status`),
    KEY `idx_mes_pp_order_process_completion_last_event` (`tenant_id`, `last_event_id`),
    KEY `idx_mes_pp_order_process_completion_execution` (`tenant_id`, `backfill_execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池生产组长订单工序完成与批记录回填状态';
