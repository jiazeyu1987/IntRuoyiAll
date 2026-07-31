-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_review_copy; type=schema; riskLevel=medium
-- MES 生产一线工序池 F9/F10：班组长工作台、复核、异常上报和班组级维护

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_team_leader_scope` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `leader_user_id` bigint NOT NULL COMMENT '班组长用户ID',
    `leader_type` varchar(32) NOT NULL COMMENT '班组长类型：PRODUCTION/PQC',
    `scope_type` varchar(32) NOT NULL COMMENT '负责范围类型：EMPLOYEE/PROCESS/WORKSTATION',
    `employee_user_id` bigint DEFAULT NULL COMMENT '负责员工用户ID',
    `process_id` bigint DEFAULT NULL COMMENT '负责工序ID',
    `workstation_id` bigint DEFAULT NULL COMMENT '负责工作站ID',
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
    KEY `idx_mes_pp_tl_scope_workstation` (`tenant_id`, `leader_user_id`, `workstation_id`)
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

DROP PROCEDURE IF EXISTS ensure_mes_process_pool_team_leader_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_process_pool_team_leader_menus()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 5700
        AND `name` = '生产管理'
        AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES production management parent menu 5700; cannot install process-pool team leader menus';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge process-pool team leader menus';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900310, '工序池班组长工作台', 'mes:pro-process-pool-team-leader:query', 2, 60, 5700, 'process-pool/team-leader', 'ep:operation', 'mes/pro/processpool/TeamLeaderWorkbenchPage', 'MesProProcessPoolTeamLeaderWorkbench', 0, b'1', b'1', b'1', 'process-pool-team-leader', NOW(), 'process-pool-team-leader', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900310 OR `permission` = 'mes:pro-process-pool-team-leader:query' AND `type` = 2);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900311, '班组长提交查询', 'mes:pro-process-pool-team-leader:query', 3, 1, 900310, '', '', '', '', 0, b'1', b'1', b'1', 'process-pool-team-leader', NOW(), 'process-pool-team-leader', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900311 OR `permission` = 'mes:pro-process-pool-team-leader:query' AND `type` = 3);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900312, '班组长提交复核', 'mes:pro-process-pool-team-leader:review', 3, 2, 900310, '', '', '', '', 0, b'1', b'1', b'1', 'process-pool-team-leader', NOW(), 'process-pool-team-leader', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900312 OR `permission` = 'mes:pro-process-pool-team-leader:review');

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900313, '生产工单异常上报', 'mes:pro-process-pool-team-leader:abnormal', 3, 3, 900310, '', '', '', '', 0, b'1', b'1', b'1', 'process-pool-team-leader', NOW(), 'process-pool-team-leader', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900313 OR `permission` = 'mes:pro-process-pool-team-leader:abnormal');

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 900314, '班组基础维护', 'mes:pro-process-pool-team-leader:maintain', 3, 4, 900310, '', '', '', '', 0, b'1', b'1', b'1', 'process-pool-team-leader', NOW(), 'process-pool-team-leader', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900314 OR `permission` = 'mes:pro-process-pool-team-leader:maintain');

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900310, 900311, 900312, 900313, 900314)) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing process-pool team leader system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_tl_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_pp_tl_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5700' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_tl_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_pp_tl_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900310, 900311, 900312, 900313, 900314)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_tl_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_pp_tl_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_pp_tl_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_pp_tl_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_pp_tl_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_pp_tl_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_pp_tl_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_pp_tl_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'process-pool-team-leader',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'process-pool-team-leader',
    NOW(),
    'process-pool-team-leader',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_pp_tl_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900310, 900311, 900312, 900313, 900314)
   AND `menu`.`deleted` = b'0'
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` = 'tenant_admin'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_tl_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_tl_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pp_tl_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_process_pool_team_leader_menus();

DROP PROCEDURE IF EXISTS ensure_mes_process_pool_team_leader_menus;
