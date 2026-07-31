-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_review_copy; type=schema; riskLevel=medium
-- MES 生产一线工序池 F5：审核副本自动规则源

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_review_copy_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `device_id` bigint NOT NULL COMMENT '设备ID',
    `template_type` varchar(64) NOT NULL COMMENT '一线固定模板类型',
    `field_code` varchar(128) NOT NULL COMMENT '字段编码',
    `field_name` varchar(255) NOT NULL COMMENT '字段名称',
    `lower_limit` decimal(24,6) NOT NULL COMMENT '下限',
    `upper_limit` decimal(24,6) NOT NULL COMMENT '上限',
    `value_type` varchar(32) DEFAULT NULL COMMENT '字段值类型',
    `affects_allocation` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否影响 FIFO 分配',
    `allocation_field` varchar(64) DEFAULT NULL COMMENT '影响 FIFO 的原始字段',
    `source_quantity_type` varchar(64) DEFAULT NULL COMMENT '关联数量片段类型',
    `template_field_metadata_json` json NOT NULL COMMENT '模板字段元数据快照',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_review_copy_rule_field` (`tenant_id`, `process_id`, `device_id`, `template_type`, `field_code`, `deleted`),
    KEY `idx_mes_pp_review_copy_rule_context` (`tenant_id`, `process_id`, `device_id`, `template_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池审核副本自动规则';
