-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260621_srm_phase1_supplier_portal; type=schema; riskLevel=medium
-- SRM Phase 3 purchase-order collaboration baseline, supplier confirmation menu, and code rules.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购订单协同单编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `order_no` varchar(64) NOT NULL COMMENT '采购订单协同单号',
  `source_plan_id` bigint NOT NULL COMMENT '来源采购计划编号',
  `source_plan_no` varchar(64) NOT NULL COMMENT '来源采购计划单号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `order_status` varchar(32) NOT NULL COMMENT '订单状态',
  `order_remark` varchar(500) DEFAULT NULL COMMENT '订单备注',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人用户编号',
  `confirmed_name` varchar(64) DEFAULT NULL COMMENT '确认人昵称',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认时间',
  `confirm_remark` varchar(500) DEFAULT NULL COMMENT '确认备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_purchase_order_tenant_order_no` (`tenant_id`, `order_no`, `deleted`),
  UNIQUE KEY `uk_srm_purchase_order_tenant_source_supplier` (`tenant_id`, `source_plan_id`, `supplier_id`, `deleted`),
  KEY `idx_srm_purchase_order_tenant_supplier_status` (`tenant_id`, `supplier_id`, `order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 采购订单协同单';

CREATE TABLE IF NOT EXISTS `srm_purchase_order_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购订单协同行编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `order_id` bigint NOT NULL COMMENT '采购订单协同单编号',
  `line_no` varchar(64) NOT NULL COMMENT '协同行号',
  `source_plan_line_id` bigint NOT NULL COMMENT '来源采购计划行编号',
  `material_id` bigint NOT NULL COMMENT '物料编号',
  `material_code` varchar(64) NOT NULL COMMENT '物料编码',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称',
  `requested_quantity` decimal(24,6) NOT NULL COMMENT '需求数量',
  `unit` varchar(32) NOT NULL COMMENT '单位',
  `requested_delivery_date` date NOT NULL COMMENT '需求交期',
  `confirmed_quantity` decimal(24,6) DEFAULT NULL COMMENT '确认数量',
  `confirmed_delivery_date` date DEFAULT NULL COMMENT '确认交期',
  `supplier_remark` varchar(500) DEFAULT NULL COMMENT '供应商备注',
  `pending_changed_quantity` decimal(24,6) DEFAULT NULL COMMENT '待确认变更数量',
  `pending_changed_delivery_date` date DEFAULT NULL COMMENT '待确认变更交期',
  `pending_changed_remark` varchar(500) DEFAULT NULL COMMENT '待确认变更备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_purchase_order_line_tenant_line_no` (`tenant_id`, `line_no`, `deleted`),
  KEY `idx_srm_purchase_order_line_tenant_order` (`tenant_id`, `order_id`),
  KEY `idx_srm_purchase_order_line_tenant_source_line` (`tenant_id`, `source_plan_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 采购订单协同行';

CREATE TABLE IF NOT EXISTS `srm_purchase_order_change` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购订单变更单编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `change_no` varchar(64) NOT NULL COMMENT '采购订单变更单号',
  `order_id` bigint NOT NULL COMMENT '采购订单协同单编号',
  `order_no` varchar(64) NOT NULL COMMENT '采购订单协同单号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `change_status` varchar(32) NOT NULL COMMENT '变更状态',
  `change_reason` varchar(500) NOT NULL COMMENT '变更原因',
  `change_remark` varchar(500) DEFAULT NULL COMMENT '采购侧补充说明',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人用户编号',
  `submitted_name` varchar(64) DEFAULT NULL COMMENT '提交人昵称',
  `submitted_time` datetime NOT NULL COMMENT '提交时间',
  `confirmed_by` bigint DEFAULT NULL COMMENT '供应商确认人用户编号',
  `confirmed_name` varchar(64) DEFAULT NULL COMMENT '供应商确认人昵称',
  `confirmed_time` datetime DEFAULT NULL COMMENT '供应商确认时间',
  `confirm_remark` varchar(500) DEFAULT NULL COMMENT '供应商确认说明',
  `rejected_by` bigint DEFAULT NULL COMMENT '供应商拒绝人用户编号',
  `rejected_name` varchar(64) DEFAULT NULL COMMENT '供应商拒绝人昵称',
  `rejected_time` datetime DEFAULT NULL COMMENT '供应商拒绝时间',
  `reject_remark` varchar(500) DEFAULT NULL COMMENT '供应商拒绝原因',
  `withdrawn_by` bigint DEFAULT NULL COMMENT '采购撤回人用户编号',
  `withdrawn_name` varchar(64) DEFAULT NULL COMMENT '采购撤回人昵称',
  `withdrawn_time` datetime DEFAULT NULL COMMENT '采购撤回时间',
  `withdraw_remark` varchar(500) DEFAULT NULL COMMENT '采购撤回原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_purchase_order_change_tenant_change_no` (`tenant_id`, `change_no`, `deleted`),
  KEY `idx_srm_purchase_order_change_tenant_order` (`tenant_id`, `order_id`, `change_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 采购订单变更单';

CREATE TABLE IF NOT EXISTS `srm_purchase_order_change_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购订单变更行编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `change_id` bigint NOT NULL COMMENT '采购订单变更单编号',
  `order_line_id` bigint NOT NULL COMMENT '采购订单协同行编号',
  `material_id` bigint NOT NULL COMMENT '物料编号',
  `material_code` varchar(64) NOT NULL COMMENT '物料编码',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称',
  `before_quantity` decimal(24,6) NOT NULL COMMENT '变更前确认数量',
  `before_delivery_date` date NOT NULL COMMENT '变更前确认交期',
  `before_supplier_remark` varchar(500) DEFAULT NULL COMMENT '变更前供应商备注',
  `changed_quantity` decimal(24,6) NOT NULL COMMENT '申请变更数量',
  `changed_delivery_date` date NOT NULL COMMENT '申请变更交期',
  `changed_supplier_remark` varchar(500) DEFAULT NULL COMMENT '申请变更备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_purchase_order_change_line_tenant_change` (`tenant_id`, `change_id`),
  KEY `idx_srm_purchase_order_change_line_tenant_order_line` (`tenant_id`, `order_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 采购订单变更行';

DROP PROCEDURE IF EXISTS ensure_srm_phase3_purchase_order;

DELIMITER $$
CREATE PROCEDURE ensure_srm_phase3_purchase_order()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_purchase_order_line'
      AND COLUMN_NAME = 'pending_changed_quantity'
  ) THEN
    ALTER TABLE `srm_purchase_order_line`
      ADD COLUMN `pending_changed_quantity` decimal(24,6) DEFAULT NULL COMMENT '待确认变更数量' AFTER `supplier_remark`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_purchase_order_line'
      AND COLUMN_NAME = 'pending_changed_delivery_date'
  ) THEN
    ALTER TABLE `srm_purchase_order_line`
      ADD COLUMN `pending_changed_delivery_date` date DEFAULT NULL COMMENT '待确认变更交期' AFTER `pending_changed_quantity`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_purchase_order_line'
      AND COLUMN_NAME = 'pending_changed_remark'
  ) THEN
    ALTER TABLE `srm_purchase_order_line`
      ADD COLUMN `pending_changed_remark` varchar(500) DEFAULT NULL COMMENT '待确认变更备注' AFTER `pending_changed_delivery_date`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991000
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '缺少 SRM 基础菜单，禁止安装 Phase 3 采购订单协同菜单';
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
  SELECT 991080, '采购订单协同', '', 2, 80, 991000, 'purchase-order', 'ep:management', 'srm/purchase-order/index', 'SrmPurchaseOrder', 0, b'1', b'1', b'1', 'srm-phase3', NOW(), 'srm-phase3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991080);

  UPDATE `system_menu`
  SET `name` = '采购订单协同',
      `permission` = '',
      `path` = 'purchase-order',
      `component` = 'srm/purchase-order/index',
      `component_name` = 'SrmPurchaseOrder',
      `icon` = 'ep:management',
      `updater` = 'srm-phase3',
      `update_time` = NOW()
  WHERE `id` = 991080;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991081, '采购订单协同查询', 'srm:purchase-order:query', 3, 1, 991080, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase3', NOW(), 'srm-phase3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991081);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991082, '采购订单协同生成', 'srm:purchase-order:create', 3, 2, 991080, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase3', NOW(), 'srm-phase3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991082);

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991083, '供应商确认台', '', 2, 81, 991000, 'purchase-order/my', 'ep:checked', 'srm/purchase-order/my', 'SrmPurchaseOrderMy', 0, b'1', b'1', b'1', 'srm-phase3', NOW(), 'srm-phase3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991083);

  UPDATE `system_menu`
  SET `name` = '供应商确认台',
      `permission` = '',
      `path` = 'purchase-order/my',
      `component` = 'srm/purchase-order/my',
      `component_name` = 'SrmPurchaseOrderMy',
      `icon` = 'ep:checked',
      `updater` = 'srm-phase3',
      `update_time` = NOW()
  WHERE `id` = 991083;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991084, '供应商确认查询', 'srm:purchase-order:query', 3, 1, 991083, '', '', '', '', 0, b'1', b'1', b'1', 'srm-phase3', NOW(), 'srm-phase3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991084);

  UPDATE `system_menu`
  SET `name` = CASE `id`
      WHEN 991081 THEN '采购订单协同查询'
      WHEN 991082 THEN '采购订单协同生成'
      WHEN 991084 THEN '供应商确认查询'
      ELSE `name`
    END,
    `permission` = CASE `id`
      WHEN 991081 THEN 'srm:purchase-order:query'
      WHEN 991082 THEN 'srm:purchase-order:create'
      WHEN 991084 THEN 'srm:purchase-order:query'
      ELSE `permission`
    END,
    `updater` = 'srm-phase3',
    `update_time` = NOW()
  WHERE `id` IN (991081, 991082, 991084);

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991080
      AND `component` = 'srm/purchase-order/index'
      AND `component_name` = 'SrmPurchaseOrder'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM purchase-order route menu for get-permission-info';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991083
      AND `component` = 'srm/purchase-order/my'
      AND `component_name` = 'SrmPurchaseOrderMy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM purchase-order supplier route menu for get-permission-info';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_phase3_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_phase3_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991080, 991081, 991082, 991083, 991084);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_phase3_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_phase3_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_phase3_package_menu_ids` (`package_id`, `menu_id`)
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
  CROSS JOIN `tmp_srm_phase3_menu_ids` AS `menu`
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
    FROM `tmp_srm_phase3_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-phase3',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-phase3',
    NOW(),
    'srm-phase3',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_phase3_menu_ids` AS `menu`
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
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_PURCHASE_ORDER', '采购订单协同编码规则', 'PURCHASE_ORDER', 'PO', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'Phase 3 采购订单协同规则', 'srm-phase3', NOW(), 'srm-phase3', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` = 991080
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1
    FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'PURCHASE_ORDER'
      AND existing.`deleted` = b'0'
  );

  INSERT INTO `srm_code_rule`
  (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_PURCHASE_ORDER_LINE', '采购订单协同行编码规则', 'PURCHASE_ORDER_LINE', 'POL', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'Phase 3 采购订单协同行规则', 'srm-phase3', NOW(), 'srm-phase3', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` = 991080
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1
    FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'PURCHASE_ORDER_LINE'
      AND existing.`deleted` = b'0'
  );

  INSERT INTO `srm_code_rule`
  (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_PURCHASE_ORDER_CHANGE', '采购订单变更单编码规则', 'PURCHASE_ORDER_CHANGE', 'POC', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'Phase 3 采购订单变更规则', 'srm-phase3', NOW(), 'srm-phase3', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` = 991080
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1
    FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'PURCHASE_ORDER_CHANGE'
      AND existing.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_phase3_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_phase3_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_phase3_purchase_order();
DROP PROCEDURE IF EXISTS ensure_srm_phase3_purchase_order;
