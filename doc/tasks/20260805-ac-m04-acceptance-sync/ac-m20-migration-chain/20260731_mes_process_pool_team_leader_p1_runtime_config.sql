-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_team_leader; type=schema; riskLevel=medium
-- MES 生产组长工作台 P1：活跃订单、临时员工档案、班组设备、工序设备关系、设备参数默认值

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_active_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '生产组长用户ID',
    `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
    `active_status` varchar(32) NOT NULL COMMENT '活跃状态：ACTIVE/REMOVED',
    `joined_at` datetime NOT NULL COMMENT '加入活跃队列时间',
    `removed_at` datetime DEFAULT NULL COMMENT '移出活跃队列时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_active_order` (`tenant_id`, `leader_user_id`, `work_order_id`, `deleted`),
    KEY `idx_mes_pp_active_order_fifo` (`tenant_id`, `leader_user_id`, `active_status`, `joined_at`, `work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池生产组长活跃订单';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_employee_profile` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '生产组长用户ID',
    `system_user_id` bigint DEFAULT NULL COMMENT '系统用户ID；临时工为空',
    `employee_code` varchar(64) NOT NULL COMMENT '员工编号',
    `employee_name` varchar(128) NOT NULL COMMENT '员工姓名',
    `employee_type` varchar(32) NOT NULL COMMENT '员工类型：SYSTEM/TEMPORARY',
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
    UNIQUE KEY `uk_mes_pp_team_employee_profile` (`tenant_id`, `leader_user_id`, `employee_code`, `deleted`),
    KEY `idx_mes_pp_team_employee_profile_user` (`tenant_id`, `leader_user_id`, `system_user_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组员工档案';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_device` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '生产组长用户ID',
    `device_code` varchar(64) NOT NULL COMMENT '设备编号',
    `device_name` varchar(128) NOT NULL COMMENT '设备名称',
    `device_status` varchar(32) NOT NULL COMMENT '设备状态：ENABLED/REPAIRING/DISABLED',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `status_changed_at` datetime NOT NULL COMMENT '状态变更时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_team_device` (`tenant_id`, `leader_user_id`, `device_code`, `deleted`),
    KEY `idx_mes_pp_team_device_status` (`tenant_id`, `leader_user_id`, `device_status`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组设备';

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_process_device` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '生产组长用户ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `device_id` bigint NOT NULL COMMENT '班组设备ID',
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
    UNIQUE KEY `uk_mes_pp_team_process_device` (`tenant_id`, `leader_user_id`, `process_id`, `device_id`, `deleted`),
    KEY `idx_mes_pp_team_process_device` (`tenant_id`, `process_id`, `device_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池班组工序设备关系';

DROP PROCEDURE IF EXISTS ensure_mes_pp_tl_p1_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_tl_p1_column(
    IN p_table_name varchar(128),
    IN p_column_name varchar(128),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_tl_p1_column(
    'mes_pro_process_pool_team_employee_binding',
    'employee_profile_id',
    'bigint DEFAULT NULL COMMENT ''班组员工档案ID'' AFTER `process_id`'
);

ALTER TABLE `mes_pro_process_pool_team_employee_binding`
    MODIFY COLUMN `employee_user_id` bigint DEFAULT NULL COMMENT '员工系统用户ID；临时工为空';

CALL ensure_mes_pp_tl_p1_column(
    'mes_pro_process_pool_device_parameter_rule',
    'unit',
    'varchar(32) DEFAULT NULL COMMENT ''参数单位'' AFTER `parameter_name`'
);

CALL ensure_mes_pp_tl_p1_column(
    'mes_pro_process_pool_device_parameter_rule',
    'default_value',
    'decimal(24,6) DEFAULT NULL COMMENT ''默认值'' AFTER `upper_limit`'
);

DROP PROCEDURE IF EXISTS ensure_mes_pp_tl_p1_column;
