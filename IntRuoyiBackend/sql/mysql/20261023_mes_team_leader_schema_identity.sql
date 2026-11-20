-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_review_copy; type=schema; riskLevel=medium; applyOrder=10; supersedes=20260730_mes_process_pool_team_leader; requiresTargetPreflight=true
-- MES 生产一线工序池 F9/F10：班组长工作台、复核、异常上报和班组级维护

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_leader_scope` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '班组长用户ID',
    `leader_type` varchar(32) NOT NULL COMMENT '班组长类型：PRODUCTION/PQC',
    `scope_type` varchar(32) NOT NULL COMMENT '负责范围类型：EMPLOYEE/PROCESS/WORKSTATION/PRODUCTION_LINE/EQUIPMENT/ORDER',
    `employee_user_id` bigint DEFAULT NULL COMMENT '负责员工用户ID',
    `process_id` bigint DEFAULT NULL COMMENT '负责工序ID',
    `workstation_id` bigint DEFAULT NULL COMMENT '负责工作站ID',
    `production_line_id` bigint DEFAULT NULL COMMENT '负责生产线ID',
    `equipment_id` bigint DEFAULT NULL COMMENT '负责设备ID',
    `work_order_id` bigint DEFAULT NULL COMMENT '负责生产订单ID',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_mes_pp_tl_scope_employee` (`tenant_id`, `leader_user_id`, `employee_user_id`),
    KEY `idx_mes_pp_tl_scope_process` (`tenant_id`, `leader_user_id`, `process_id`),
    KEY `idx_mes_pp_tl_scope_workstation` (`tenant_id`, `leader_user_id`, `workstation_id`),
    KEY `idx_mes_pp_tl_scope_line` (`tenant_id`, `leader_user_id`, `production_line_id`),
    KEY `idx_mes_pp_tl_scope_equipment` (`tenant_id`, `leader_user_id`, `equipment_id`),
    KEY `idx_mes_pp_tl_scope_order` (`tenant_id`, `leader_user_id`, `work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组长负责范围';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_submission_review` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `event_id` bigint NOT NULL COMMENT '工序池提交事件ID',
    `leader_user_id` bigint NOT NULL COMMENT '复核班组长用户ID',
    `review_status` varchar(32) NOT NULL COMMENT '复核状态',
    `review_remark` varchar(1000) DEFAULT NULL COMMENT '复核说明',
    `reviewed_at` datetime NOT NULL COMMENT '服务端复核时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_mes_pp_review_event` (`tenant_id`, `event_id`),
    KEY `idx_mes_pp_review_leader` (`tenant_id`, `leader_user_id`, `reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组长提交复核';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_work_order_abnormal` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `work_order_id` bigint NOT NULL COMMENT '生产工单ID',
    `route_process_id` bigint DEFAULT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint DEFAULT NULL COMMENT '工序ID',
    `source_event_id` bigint DEFAULT NULL COMMENT '可选来源工序池提交事件ID',
    `abnormal_reason_code` varchar(64) NOT NULL COMMENT '异常原因编码',
    `abnormal_description` varchar(1000) NOT NULL COMMENT '异常说明',
    `report_status` varchar(32) NOT NULL COMMENT '上报状态',
    `marker_user_id` bigint NOT NULL COMMENT '标记人用户ID',
    `marked_at` datetime NOT NULL COMMENT '服务端标记时间',
    `reporter_user_id` bigint NOT NULL COMMENT '上报人用户ID',
    `reported_at` datetime NOT NULL COMMENT '服务端上报时间',
    `close_remark` varchar(1000) DEFAULT NULL COMMENT '关闭说明',
    `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_mes_pp_abnormal_work_order` (`tenant_id`, `work_order_id`, `report_status`),
    KEY `idx_mes_pp_abnormal_source_event` (`tenant_id`, `source_event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池生产工单异常上报';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_employee_binding` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '班组长用户ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `employee_user_id` bigint NOT NULL COMMENT '员工用户ID',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `disabled_at` datetime DEFAULT NULL COMMENT '禁用时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_team_employee_binding` (`tenant_id`, `leader_user_id`, `process_id`, `employee_user_id`, `deleted`),
    KEY `idx_mes_pp_team_employee_candidate` (`tenant_id`, `process_id`, `employee_user_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组员工候选绑定';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_defect_reason` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '维护班组长用户ID',
    `reason_type` varchar(32) NOT NULL COMMENT '原因类型：LOSS/UNQUALIFIED/PQC_FAILURE',
    `reason_code` varchar(64) NOT NULL COMMENT '原因编码',
    `reason_name` varchar(255) NOT NULL COMMENT '原因名称',
    `route_process_id` bigint DEFAULT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint DEFAULT NULL COMMENT '工序ID',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_defect_reason` (`tenant_id`, `leader_user_id`, `reason_type`, `reason_code`, `process_id`, `deleted`),
    KEY `idx_mes_pp_defect_reason_process` (`tenant_id`, `process_id`, `reason_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组不良原因';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_device_parameter_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '维护班组长用户ID',
    `route_process_id` bigint DEFAULT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `device_id` bigint NOT NULL COMMENT '设备ID',
    `parameter_code` varchar(128) NOT NULL COMMENT '参数编码',
    `parameter_name` varchar(255) DEFAULT NULL COMMENT '参数名称',
    `lower_limit` decimal(24,6) NOT NULL COMMENT '下限',
    `upper_limit` decimal(24,6) NOT NULL COMMENT '上限',
    `value_type` varchar(32) DEFAULT NULL COMMENT '值类型',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_device_parameter_rule` (`tenant_id`, `process_id`, `device_id`, `parameter_code`, `deleted`),
    KEY `idx_mes_pp_device_parameter_process` (`tenant_id`, `process_id`, `device_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池工序设备参数上下限';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_maintenance_audit` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '维护班组长用户ID',
    `action_type` varchar(64) NOT NULL COMMENT '动作类型',
    `target_type` varchar(64) NOT NULL COMMENT '目标类型',
    `target_id` bigint DEFAULT NULL COMMENT '目标ID',
    `before_snapshot` json DEFAULT NULL COMMENT '维护前快照',
    `after_snapshot` json DEFAULT NULL COMMENT '维护后快照',
    `audit_time` datetime NOT NULL COMMENT '服务端维护审计时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_mes_pp_team_maintenance_audit_leader` (`tenant_id`, `leader_user_id`, `audit_time`),
    KEY `idx_mes_pp_team_maintenance_audit_target` (`tenant_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组维护审计';

-- Menu ownership is installed by the explicit permission successor after the current production/PQC pages exist.
