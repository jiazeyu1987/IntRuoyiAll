-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260630_mes_pro_work_order_erp_snapshot_fields; type=schema; riskLevel=medium
-- MES 生产一线工序池 F7：生产工单 FIFO 分配明细

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_fifo_allocation_line` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `allocation_batch_no` varchar(64) NOT NULL COMMENT 'FIFO 分配批次号',
    `process_pool_id` bigint NOT NULL COMMENT '来源工序池ID',
    `source_event_id` bigint NOT NULL COMMENT '来源提交事件ID',
    `source_quantity_fragment_id` bigint NOT NULL COMMENT '来源数量片段ID',
    `source_route_process_id` bigint NOT NULL COMMENT '来源工艺路线工序ID',
    `source_process_id` bigint NOT NULL COMMENT '来源工序ID',
    `source_fragment_quantity` decimal(24,6) NOT NULL COMMENT '来源片段原始数量',
    `target_work_order_id` bigint NOT NULL COMMENT '目标生产工单ID',
    `target_work_order_code` varchar(64) NOT NULL COMMENT '目标生产工单编号',
    `target_route_process_id` bigint NOT NULL COMMENT '目标工艺路线工序ID',
    `target_process_id` bigint NOT NULL COMMENT '目标工序ID',
    `allocated_quantity` decimal(24,6) NOT NULL COMMENT '分配数量',
    `allocation_status` varchar(32) NOT NULL DEFAULT 'ALLOCATED' COMMENT '分配状态',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_mes_process_pool_fifo_alloc_fragment` (`tenant_id`, `source_quantity_fragment_id`),
    KEY `idx_mes_process_pool_fifo_alloc_event` (`tenant_id`, `source_event_id`),
    KEY `idx_mes_process_pool_fifo_alloc_target` (`tenant_id`, `target_work_order_id`, `target_route_process_id`),
    KEY `idx_mes_process_pool_fifo_alloc_pool` (`tenant_id`, `process_pool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池 FIFO 分配明细';
