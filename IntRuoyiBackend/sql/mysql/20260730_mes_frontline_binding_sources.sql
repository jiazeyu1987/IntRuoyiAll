-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_foundation; type=schema; riskLevel=medium
-- rollback: DROP TABLE mes_frontline_employee_template_binding; DROP TABLE mes_frontline_device_account_route_binding;

CREATE TABLE IF NOT EXISTS `mes_frontline_device_account_route_binding` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `device_account_user_id` bigint NOT NULL COMMENT '设备/一线共享登录账号用户ID',
    `route_id` bigint NOT NULL COMMENT '允许切换的工艺路线ID',
    `device_id` bigint NOT NULL COMMENT '绑定设备ID',
    `workstation_id` bigint NOT NULL COMMENT '绑定工作站ID',
    `default_approve_user_id` bigint DEFAULT NULL COMMENT '一线报工默认审批人用户ID',
    `recordbook_id` bigint DEFAULT NULL COMMENT '一线原始记录写入记录本ID',
    `feedback_type` tinyint DEFAULT NULL COMMENT '一线报工类型：1自行报工 2统一报工',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0启用 1停用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_frontline_route_binding` (`tenant_id`, `device_account_user_id`, `route_id`, `device_id`, `workstation_id`, `deleted`),
    KEY `idx_mes_frontline_route_binding_account` (`tenant_id`, `device_account_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 一线设备账号工艺路线绑定';

CREATE TABLE IF NOT EXISTS `mes_frontline_employee_template_binding` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `actual_employee_id` bigint NOT NULL COMMENT '实际填写员工用户ID',
    `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `template_no` varchar(64) NOT NULL COMMENT '固定模板编码',
    `template_type` varchar(64) NOT NULL COMMENT '固定模板类型',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0启用 1停用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_frontline_template_binding` (`tenant_id`, `actual_employee_id`, `route_process_id`, `process_id`, `deleted`),
    KEY `idx_mes_frontline_template_binding_employee` (`tenant_id`, `actual_employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 一线员工固定模板绑定';
