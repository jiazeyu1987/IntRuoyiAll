-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260621_srm_phase3_purchase_order; type=schema; riskLevel=medium
-- SRM Phase 4/5 controlled simulated chain baseline: outsource execution, reconciliation, payment execution, menus, and code rules.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_outsource_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '委外执行单编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `execution_no` varchar(64) NOT NULL COMMENT '委外执行单号',
  `source_purchase_order_id` bigint NOT NULL COMMENT '来源采购订单协同单编号',
  `source_purchase_order_no` varchar(64) NOT NULL COMMENT '来源采购订单协同单号',
  `source_plan_id` bigint NOT NULL COMMENT '来源采购计划编号',
  `source_plan_no` varchar(64) NOT NULL COMMENT '来源采购计划单号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `execution_status` varchar(32) NOT NULL COMMENT '执行状态',
  `simulation_source` varchar(32) NOT NULL COMMENT '模拟来源类型',
  `simulation_label` varchar(128) NOT NULL COMMENT '模拟来源展示文案',
  `simulation_remark` varchar(500) DEFAULT NULL COMMENT '模拟说明',
  `planned_quantity` decimal(24,6) NOT NULL COMMENT '计划数量',
  `issue_notice_no` varchar(64) DEFAULT NULL COMMENT '发料通知号',
  `issue_quantity` decimal(24,6) DEFAULT NULL COMMENT '发料数量',
  `progress_percent` decimal(24,6) DEFAULT NULL COMMENT '加工进度百分比',
  `progress_stage` varchar(64) DEFAULT NULL COMMENT '加工阶段',
  `received_quantity` decimal(24,6) DEFAULT NULL COMMENT '收货数量',
  `qualified_quantity` decimal(24,6) DEFAULT NULL COMMENT '合格数量',
  `unit_price` decimal(24,6) NOT NULL COMMENT '结算单价',
  `issued_by` bigint DEFAULT NULL COMMENT '发料登记人用户编号',
  `issued_name` varchar(64) DEFAULT NULL COMMENT '发料登记人昵称',
  `issued_time` datetime DEFAULT NULL COMMENT '发料登记时间',
  `delivered_by` bigint DEFAULT NULL COMMENT '送收货回传人用户编号',
  `delivered_name` varchar(64) DEFAULT NULL COMMENT '送收货回传人昵称',
  `delivered_time` datetime DEFAULT NULL COMMENT '送收货回传时间',
  `inspected_by` bigint DEFAULT NULL COMMENT '检验登记人用户编号',
  `inspected_name` varchar(64) DEFAULT NULL COMMENT '检验登记人昵称',
  `inspected_time` datetime DEFAULT NULL COMMENT '检验登记时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_outsource_execution_tenant_no` (`tenant_id`, `execution_no`, `deleted`),
  UNIQUE KEY `uk_srm_outsource_execution_tenant_order` (`tenant_id`, `source_purchase_order_id`, `deleted`),
  KEY `idx_srm_outsource_execution_tenant_supplier_status` (`tenant_id`, `supplier_id`, `execution_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 委外执行单';

CREATE TABLE IF NOT EXISTS `srm_outsource_execution_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '委外执行事件编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `event_no` varchar(64) NOT NULL COMMENT '事件单号',
  `execution_id` bigint NOT NULL COMMENT '委外执行单编号',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `before_status` varchar(32) DEFAULT NULL COMMENT '变更前状态',
  `after_status` varchar(32) DEFAULT NULL COMMENT '变更后状态',
  `simulation_source` varchar(32) DEFAULT NULL COMMENT '模拟来源类型',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人用户编号',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人昵称',
  `event_remark` varchar(500) DEFAULT NULL COMMENT '事件说明',
  `event_payload` varchar(1000) DEFAULT NULL COMMENT '事件扩展数据',
  `event_time` datetime NOT NULL COMMENT '事件时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_outsource_execution_event_tenant_no` (`tenant_id`, `event_no`, `deleted`),
  KEY `idx_srm_outsource_execution_event_tenant_execution` (`tenant_id`, `execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 委外执行事件';

CREATE TABLE IF NOT EXISTS `srm_reconciliation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对账单编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `reconciliation_no` varchar(64) NOT NULL COMMENT '对账单号',
  `execution_id` bigint NOT NULL COMMENT '委外执行单编号',
  `execution_no` varchar(64) NOT NULL COMMENT '委外执行单号',
  `source_purchase_order_id` bigint NOT NULL COMMENT '来源采购订单协同单编号',
  `source_purchase_order_no` varchar(64) NOT NULL COMMENT '来源采购订单协同单号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `reconciliation_status` varchar(32) NOT NULL COMMENT '对账状态',
  `simulation_source` varchar(32) NOT NULL COMMENT '模拟来源类型',
  `simulation_label` varchar(128) NOT NULL COMMENT '模拟来源展示文案',
  `unit_price` decimal(24,6) NOT NULL COMMENT '结算单价',
  `received_quantity` decimal(24,6) NOT NULL COMMENT '收货数量',
  `qualified_quantity` decimal(24,6) NOT NULL COMMENT '合格数量',
  `diff_quantity` decimal(24,6) NOT NULL COMMENT '差异数量',
  `reconciliation_amount` decimal(24,2) NOT NULL COMMENT '对账金额',
  `diff_amount` decimal(24,2) NOT NULL COMMENT '差异金额',
  `confirmed_by` bigint DEFAULT NULL COMMENT '对账确认人用户编号',
  `confirmed_name` varchar(64) DEFAULT NULL COMMENT '对账确认人昵称',
  `confirmed_time` datetime DEFAULT NULL COMMENT '对账确认时间',
  `confirm_remark` varchar(500) DEFAULT NULL COMMENT '对账说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_reconciliation_tenant_no` (`tenant_id`, `reconciliation_no`, `deleted`),
  UNIQUE KEY `uk_srm_reconciliation_tenant_execution` (`tenant_id`, `execution_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 委外对账单';

CREATE TABLE IF NOT EXISTS `srm_payment_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '付款执行单编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `payment_no` varchar(64) NOT NULL COMMENT '付款执行单号',
  `reconciliation_id` bigint NOT NULL COMMENT '来源对账单编号',
  `reconciliation_no` varchar(64) NOT NULL COMMENT '来源对账单号',
  `execution_id` bigint NOT NULL COMMENT '来源委外执行单编号',
  `execution_no` varchar(64) NOT NULL COMMENT '来源委外执行单号',
  `contract_id` bigint NOT NULL COMMENT '采购合同编号',
  `contract_no` varchar(64) NOT NULL COMMENT '采购合同编号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `payment_status` varchar(32) NOT NULL COMMENT '付款状态',
  `simulation_source` varchar(32) NOT NULL COMMENT '模拟来源类型',
  `simulation_label` varchar(128) NOT NULL COMMENT '模拟来源展示文案',
  `payment_stage` varchar(64) NOT NULL COMMENT '付款阶段',
  `payment_ratio` decimal(24,2) NOT NULL COMMENT '付款比例',
  `due_date` date DEFAULT NULL COMMENT '付款到期日期',
  `payment_term_summary` varchar(500) NOT NULL COMMENT '付款条件摘要',
  `reconciliation_amount` decimal(24,2) NOT NULL COMMENT '来源对账金额',
  `apply_amount` decimal(24,2) NOT NULL COMMENT '本次申请金额',
  `payment_remark` varchar(500) DEFAULT NULL COMMENT '付款说明',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人用户编号',
  `submitted_name` varchar(64) DEFAULT NULL COMMENT '提交人昵称',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审批人用户编号',
  `approved_name` varchar(64) DEFAULT NULL COMMENT '审批人昵称',
  `approved_time` datetime DEFAULT NULL COMMENT '审批时间',
  `rejected_by` bigint DEFAULT NULL COMMENT '驳回人用户编号',
  `rejected_name` varchar(64) DEFAULT NULL COMMENT '驳回人昵称',
  `rejected_time` datetime DEFAULT NULL COMMENT '驳回时间',
  `reject_remark` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `pushed_by` bigint DEFAULT NULL COMMENT '财务回执登记人用户编号',
  `pushed_name` varchar(64) DEFAULT NULL COMMENT '财务回执登记人昵称',
  `pushed_time` datetime DEFAULT NULL COMMENT '财务回执登记时间',
  `push_remark` varchar(500) DEFAULT NULL COMMENT '财务回执说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_payment_execution_tenant_no` (`tenant_id`, `payment_no`, `deleted`),
  UNIQUE KEY `uk_srm_payment_execution_tenant_reconciliation` (`tenant_id`, `reconciliation_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 付款执行单';

CREATE TABLE IF NOT EXISTS `srm_payment_execution_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '付款执行事件编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `event_no` varchar(64) NOT NULL COMMENT '事件单号',
  `payment_id` bigint NOT NULL COMMENT '付款执行单编号',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `before_status` varchar(32) DEFAULT NULL COMMENT '变更前状态',
  `after_status` varchar(32) DEFAULT NULL COMMENT '变更后状态',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人用户编号',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人昵称',
  `event_remark` varchar(500) DEFAULT NULL COMMENT '事件说明',
  `event_payload` varchar(1000) DEFAULT NULL COMMENT '事件扩展数据',
  `event_time` datetime NOT NULL COMMENT '事件时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_payment_execution_event_tenant_no` (`tenant_id`, `event_no`, `deleted`),
  KEY `idx_srm_payment_execution_event_tenant_payment` (`tenant_id`, `payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 付款执行事件';

DROP PROCEDURE IF EXISTS ensure_srm_phase45_simulated_execution;

DELIMITER $$
CREATE PROCEDURE ensure_srm_phase45_simulated_execution()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991000
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '缺少 SRM 基础菜单，禁止安装 Phase 4/5 模拟链路菜单';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND `menu_ids` IS NOT NULL
      AND `menu_ids` <> ''
      AND JSON_VALID(`menu_ids`) = 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991090, '委外执行', '', 2, 90, 991000, 'outsource-execution', 'ep:operation', 'srm/outsource-execution/index', 'SrmOutsourceExecution', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991090);

  UPDATE `system_menu`
  SET `name` = '委外执行',
      `permission` = '',
      `path` = 'outsource-execution',
      `component` = 'srm/outsource-execution/index',
      `component_name` = 'SrmOutsourceExecution',
      `icon` = 'ep:operation',
      `updater` = 'srm-phase45',
      `update_time` = NOW()
  WHERE `id` = 991090;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991091, '委外执行查询', 'srm:outsource-execution:query', 3, 1, 991090, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991091);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991092, '委外执行创建', 'srm:outsource-execution:create', 3, 2, 991090, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991092);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991093, '委外执行流转', 'srm:outsource-execution:update', 3, 3, 991090, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991093);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991094, '供应商委外协同台', '', 2, 91, 991000, 'outsource-execution/my', 'ep:goods', 'srm/outsource-execution/my', 'SrmOutsourceExecutionMy', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991094);

  UPDATE `system_menu`
  SET `name` = '供应商委外协同台',
      `permission` = '',
      `path` = 'outsource-execution/my',
      `component` = 'srm/outsource-execution/my',
      `component_name` = 'SrmOutsourceExecutionMy',
      `icon` = 'ep:goods',
      `updater` = 'srm-phase45',
      `update_time` = NOW()
  WHERE `id` = 991094;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991095, '供应商委外查询', 'srm:outsource-execution:query', 3, 1, 991094, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991095);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991096, '付款执行', '', 2, 92, 991000, 'payment-execution', 'ep:credit-card', 'srm/payment-execution/index', 'SrmPaymentExecution', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991096);

  UPDATE `system_menu`
  SET `name` = '付款执行',
      `permission` = '',
      `path` = 'payment-execution',
      `component` = 'srm/payment-execution/index',
      `component_name` = 'SrmPaymentExecution',
      `icon` = 'ep:credit-card',
      `updater` = 'srm-phase45',
      `update_time` = NOW()
  WHERE `id` = 991096;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991097, '付款执行查询', 'srm:payment-execution:query', 3, 1, 991096, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991097);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991098, '付款执行创建', 'srm:payment-execution:create', 3, 2, 991096, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991098);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991099, '付款执行审批', 'srm:payment-execution:approve', 3, 3, 991096, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991099);

  UPDATE `system_menu`
  SET `name` = CASE `id`
      WHEN 991091 THEN '委外执行查询'
      WHEN 991092 THEN '委外执行创建'
      WHEN 991093 THEN '委外执行流转'
      WHEN 991095 THEN '供应商委外查询'
      WHEN 991097 THEN '付款执行查询'
      WHEN 991098 THEN '付款执行创建'
      WHEN 991099 THEN '付款执行审批'
      ELSE `name`
    END,
    `permission` = CASE `id`
      WHEN 991091 THEN 'srm:outsource-execution:query'
      WHEN 991092 THEN 'srm:outsource-execution:create'
      WHEN 991093 THEN 'srm:outsource-execution:update'
      WHEN 991095 THEN 'srm:outsource-execution:query'
      WHEN 991097 THEN 'srm:payment-execution:query'
      WHEN 991098 THEN 'srm:payment-execution:create'
      WHEN 991099 THEN 'srm:payment-execution:approve'
      ELSE `permission`
    END,
    `updater` = 'srm-phase45',
    `update_time` = NOW()
  WHERE `id` IN (991091, 991092, 991093, 991095, 991097, 991098, 991099);

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991090
      AND `component` = 'srm/outsource-execution/index'
      AND `component_name` = 'SrmOutsourceExecution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM outsource-execution route menu for get-permission-info';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991094
      AND `component` = 'srm/outsource-execution/my'
      AND `component_name` = 'SrmOutsourceExecutionMy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM outsource-execution supplier route menu for get-permission-info';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991096
      AND `component` = 'srm/payment-execution/index'
      AND `component_name` = 'SrmPaymentExecution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM payment-execution route menu for get-permission-info';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_phase45_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_phase45_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991090, 991091, 991092, 991093, 991094, 991095, 991096, 991097, 991098, 991099);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_phase45_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_phase45_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_phase45_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
  UNION
  SELECT
    `package`.`id`,
    `menu`.`id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_srm_phase45_menu_ids` AS `menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('991000' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_srm_phase45_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-phase45',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-phase45',
    NOW(),
    'srm-phase45',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_phase45_menu_ids` AS `menu`
  LEFT JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  LEFT JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  WHERE `role`.`deleted` = b'0'
    AND (
      `role`.`code` = 'super_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`menu`.`id` AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`deleted` = b'0'
    );

  INSERT INTO `srm_code_rule`
  (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_OUTSOURCE_EXECUTION', '委外执行单编码规则', 'OUTSOURCE_EXECUTION', 'OE', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'Phase 4/5 委外执行规则', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` IN (991090, 991096)
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1 FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'OUTSOURCE_EXECUTION'
      AND existing.`deleted` = b'0'
  );

  INSERT INTO `srm_code_rule`
  (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_OUTSOURCE_EXECUTION_EVENT', '委外执行事件编码规则', 'OUTSOURCE_EXECUTION_EVENT', 'OEV', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'Phase 4/5 委外执行事件规则', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` IN (991090, 991096)
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1 FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'OUTSOURCE_EXECUTION_EVENT'
      AND existing.`deleted` = b'0'
  );

  INSERT INTO `srm_code_rule`
  (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_OUTSOURCE_RECONCILIATION', '委外对账单编码规则', 'OUTSOURCE_RECONCILIATION', 'OR', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'Phase 4/5 对账规则', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` IN (991090, 991096)
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1 FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'OUTSOURCE_RECONCILIATION'
      AND existing.`deleted` = b'0'
  );

  INSERT INTO `srm_code_rule`
  (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_PAYMENT_EXECUTION', '付款执行单编码规则', 'PAYMENT_EXECUTION', 'PE', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'Phase 5 付款执行规则', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` = 991096
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1 FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'PAYMENT_EXECUTION'
      AND existing.`deleted` = b'0'
  );

  INSERT INTO `srm_code_rule`
  (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_PAYMENT_EXECUTION_EVENT', '付款执行事件编码规则', 'PAYMENT_EXECUTION_EVENT', 'PEV', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'Phase 5 付款执行事件规则', 'srm-phase45', NOW(), 'srm-phase45', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` = 991096
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1 FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'PAYMENT_EXECUTION_EVENT'
      AND existing.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_phase45_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_phase45_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_phase45_simulated_execution();
DROP PROCEDURE IF EXISTS ensure_srm_phase45_simulated_execution;
