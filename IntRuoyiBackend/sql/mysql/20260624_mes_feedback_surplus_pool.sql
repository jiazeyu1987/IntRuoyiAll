-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260610_mes_feedback_import_attribution_p6; type=schema; riskLevel=medium
-- MES 报工超产余量池与系统外其他订单分配

SET @feedback_import_target_type_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'attribution_target_type'
);
SET @feedback_import_target_type_sql = IF(
    @feedback_import_target_type_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `attribution_target_type` varchar(64) DEFAULT NULL COMMENT ''归属目标类型：CURRENT_ORDER 当前订单；EXTERNAL_OTHER_ORDER 其他订单'' AFTER `schedule_order_process_id`',
    'SELECT 1'
);
PREPARE feedback_import_target_type_stmt FROM @feedback_import_target_type_sql;
EXECUTE feedback_import_target_type_stmt;
DEALLOCATE PREPARE feedback_import_target_type_stmt;

CREATE TABLE IF NOT EXISTS `mes_pro_feedback_surplus_pool` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_type` varchar(64) NOT NULL COMMENT '来源类型：CURRENT_ORDER_OVERPRODUCE 当前订单超产；EXTERNAL_OTHER_ORDER 其他订单',
  `source_import_record_id` bigint NOT NULL COMMENT '来源导入记录ID',
  `source_feedback_id` bigint DEFAULT NULL COMMENT '来源正式报工ID',
  `source_schedule_order_id` bigint DEFAULT NULL COMMENT '来源排产工单ID',
  `source_schedule_order_process_id` bigint DEFAULT NULL COMMENT '来源排产工单工序ID',
  `source_work_order_code` varchar(64) DEFAULT NULL COMMENT '来源工单编码快照',
  `source_task_code` varchar(64) DEFAULT NULL COMMENT '来源派工单号快照',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) DEFAULT NULL COMMENT '工序编码快照',
  `process_name` varchar(255) DEFAULT NULL COMMENT '工序名称快照',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '产品编码快照',
  `item_name` varchar(255) DEFAULT NULL COMMENT '产品名称快照，其他订单可显示其他产品',
  `specification` varchar(255) DEFAULT NULL COMMENT '规格快照',
  `total_quantity` decimal(24,6) NOT NULL COMMENT '余量总数量',
  `allocated_quantity` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '已分配数量',
  `available_quantity` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '可用数量',
  `status` varchar(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE 可用；ALLOCATED 已分配',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_feedback_surplus_pool_import_record` (`tenant_id`, `source_import_record_id`),
  KEY `idx_mes_pro_feedback_surplus_pool_process` (`tenant_id`, `process_id`, `available_quantity`),
  KEY `idx_mes_pro_feedback_surplus_pool_source_type` (`tenant_id`, `source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 报工工序余量池';

CREATE TABLE IF NOT EXISTS `mes_pro_feedback_surplus_allocation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pool_id` bigint NOT NULL COMMENT '余量池ID',
  `import_record_id` bigint NOT NULL COMMENT '来源导入记录ID',
  `target_type` varchar(64) NOT NULL COMMENT '目标类型：EXTERNAL_OTHER_ORDER 其他订单',
  `target_schedule_order_id` bigint DEFAULT NULL COMMENT '预留目标排产工单ID',
  `target_schedule_order_process_id` bigint DEFAULT NULL COMMENT '预留目标排产工单工序ID',
  `target_order_label` varchar(128) NOT NULL DEFAULT '其他订单' COMMENT '目标订单显示名',
  `target_product_label` varchar(128) NOT NULL DEFAULT '其他产品' COMMENT '目标产品显示名',
  `allocated_quantity` decimal(24,6) NOT NULL COMMENT '分配数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_feedback_surplus_allocation_pool` (`tenant_id`, `pool_id`),
  KEY `idx_mes_pro_feedback_surplus_allocation_import_record` (`tenant_id`, `import_record_id`),
  KEY `idx_mes_pro_feedback_surplus_allocation_target` (`tenant_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 报工工序余量分配流水';
