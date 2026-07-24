-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_srm_d7_1_code_rule_baseline; type=schema; riskLevel=medium
-- SRM D7-2 supplier access and risk gate: schema, menu, permission, tenant package and role menu binding.
-- Fail fast when D7-1 base menus are missing or tenant package menu JSON is invalid.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_supplier_access` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '准入档案编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `supplier_id` bigint NOT NULL COMMENT 'ERP 供应商编号',
  `access_status` varchar(32) NOT NULL COMMENT '准入状态',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `access_remark` varchar(500) DEFAULT NULL COMMENT '准入备注',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人用户编号',
  `submitted_name` varchar(64) DEFAULT NULL COMMENT '提交人昵称',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人用户编号',
  `audit_name` varchar(64) DEFAULT NULL COMMENT '审核人昵称',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `disabled_by` bigint DEFAULT NULL COMMENT '停用人用户编号',
  `disabled_name` varchar(64) DEFAULT NULL COMMENT '停用人昵称',
  `disabled_time` datetime DEFAULT NULL COMMENT '停用时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_supplier_access_tenant_supplier` (`tenant_id`, `supplier_id`, `deleted`),
  KEY `idx_srm_supplier_access_tenant_status` (`tenant_id`, `access_status`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 供应商准入档案';

CREATE TABLE IF NOT EXISTS `srm_supplier_risk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '风险记录编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `supplier_id` bigint NOT NULL COMMENT 'ERP 供应商编号',
  `supplier_access_id` bigint DEFAULT NULL COMMENT '准入档案编号',
  `risk_level` varchar(16) NOT NULL COMMENT '风险等级',
  `risk_status` varchar(16) NOT NULL COMMENT '风险状态',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_id` bigint DEFAULT NULL COMMENT '来源编号',
  `source_code` varchar(64) DEFAULT NULL COMMENT '来源编码',
  `source_name` varchar(128) DEFAULT NULL COMMENT '来源名称',
  `risk_description` varchar(500) NOT NULL COMMENT '风险描述',
  `risk_remark` varchar(500) DEFAULT NULL COMMENT '风险备注',
  `reported_by` bigint DEFAULT NULL COMMENT '上报人用户编号',
  `reported_name` varchar(64) DEFAULT NULL COMMENT '上报人昵称',
  `reported_time` datetime DEFAULT NULL COMMENT '上报时间',
  `resolved_by` bigint DEFAULT NULL COMMENT '处理人用户编号',
  `resolved_name` varchar(64) DEFAULT NULL COMMENT '处理人昵称',
  `resolved_time` datetime DEFAULT NULL COMMENT '处理时间',
  `resolution_remark` varchar(500) DEFAULT NULL COMMENT '处理说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_supplier_risk_tenant_supplier` (`tenant_id`, `supplier_id`, `risk_status`, `risk_level`),
  KEY `idx_srm_supplier_risk_tenant_source` (`tenant_id`, `source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 供应商风险记录';

DROP PROCEDURE IF EXISTS ensure_srm_d7_2_supplier_access_risk;

DELIMITER $$
CREATE PROCEDURE ensure_srm_d7_2_supplier_access_risk()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge SRM supplier access/risk menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991000
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '缺少 SRM D7-1 基础菜单，不能继续挂载供应商准入/风险页面';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (
        (`id` = 991010 AND (`path` <> 'supplier' OR `name` <> '供应商管理'))
        OR (`id` = 991011 AND (`path` <> 'access' OR `component` <> 'srm/supplier-access/index' OR `component_name` <> 'SrmSupplierAccess'))
        OR (`id` = 991012 AND `permission` <> 'srm:supplier-access:query')
        OR (`id` = 991013 AND `permission` <> 'srm:supplier-access:create')
        OR (`id` = 991014 AND `permission` <> 'srm:supplier-access:update')
        OR (`id` = 991015 AND `permission` <> 'srm:supplier-access:audit')
        OR (`id` = 991016 AND `permission` <> 'srm:supplier-access:enable')
        OR (`id` = 991017 AND `permission` <> 'srm:supplier-access:check')
        OR (`id` = 991018 AND `permission` <> 'srm:supplier-access:delete')
        OR (`id` = 991020 AND (`path` <> 'risk' OR `component` <> 'srm/supplier-risk/index' OR `component_name` <> 'SrmSupplierRisk'))
        OR (`id` = 991021 AND `permission` <> 'srm:supplier-risk:query')
        OR (`id` = 991022 AND `permission` <> 'srm:supplier-risk:create')
        OR (`id` = 991023 AND `permission` <> 'srm:supplier-risk:resolve')
        OR (`id` = 991024 AND (`path` <> 'profile' OR `component` <> 'srm/supplier-profile/index' OR `component_name` <> 'SrmSupplierProfile'))
        OR (`id` = 991025 AND `permission` <> 'srm:supplier-profile:query')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM D7-2 clean menu id range; conflicting system_menu rows exist';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991010, '供应商管理', '', 1, 2, 991000, 'supplier', 'ep:user-filled', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991010 OR (`parent_id` = 991000 AND `path` = 'supplier'))
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991011, '准入管理', 'srm:supplier-access:query', 2, 1, 991010, 'access', 'ep:checked', 'srm/supplier-access/index', 'SrmSupplierAccess', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991011 OR `component` = 'srm/supplier-access/index' OR (`permission` = 'srm:supplier-access:query' AND `type` = 2))
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991012, '准入查询', 'srm:supplier-access:query', 3, 1, 991011, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991012 OR (`permission` = 'srm:supplier-access:query' AND `type` = 3)));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991013, '准入新增', 'srm:supplier-access:create', 3, 2, 991011, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991013 OR `permission` = 'srm:supplier-access:create'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991014, '准入编辑', 'srm:supplier-access:update', 3, 3, 991011, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991014 OR `permission` = 'srm:supplier-access:update'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991015, '准入审核', 'srm:supplier-access:audit', 3, 4, 991011, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991015 OR `permission` = 'srm:supplier-access:audit'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991016, '准入启停', 'srm:supplier-access:enable', 3, 5, 991011, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991016 OR `permission` = 'srm:supplier-access:enable'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991017, '资格校验', 'srm:supplier-access:check', 3, 6, 991011, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991017 OR `permission` = 'srm:supplier-access:check'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991018, '准入删除', 'srm:supplier-access:delete', 3, 7, 991011, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991018 OR `permission` = 'srm:supplier-access:delete'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991020, '风险管理', 'srm:supplier-risk:query', 2, 2, 991010, 'risk', 'ep:warning', 'srm/supplier-risk/index', 'SrmSupplierRisk', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991020 OR `component` = 'srm/supplier-risk/index' OR (`permission` = 'srm:supplier-risk:query' AND `type` = 2))
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991021, '风险查询', 'srm:supplier-risk:query', 3, 1, 991020, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991021 OR (`permission` = 'srm:supplier-risk:query' AND `type` = 3)));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991022, '风险新增', 'srm:supplier-risk:create', 3, 2, 991020, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991022 OR `permission` = 'srm:supplier-risk:create'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991023, '风险处理', 'srm:supplier-risk:resolve', 3, 3, 991020, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991023 OR `permission` = 'srm:supplier-risk:resolve'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991024, '?????', 'srm:supplier-profile:query', 2, 3, 991010, 'profile', 'ep:office-building', 'srm/supplier-profile/index', 'SrmSupplierProfile', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991024 OR `component` = 'srm/supplier-profile/index' OR (`permission` = 'srm:supplier-profile:query' AND `type` = 2))
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991025, '???????', 'srm:supplier-profile:query', 3, 1, 991024, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-2', NOW(), 'srm-d7-2', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991025 OR (`permission` = 'srm:supplier-profile:query' AND `type` = 3)));

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `path` = 'access'
      AND `component` = 'srm/supplier-access/index'
      AND `component_name` = 'SrmSupplierAccess'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM supplier-access route menu for get-permission-info';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `path` = 'risk'
      AND `component` = 'srm/supplier-risk/index'
      AND `component_name` = 'SrmSupplierRisk'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM supplier-risk route menu for get-permission-info';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `path` = 'profile'
      AND `component` = 'srm/supplier-profile/index'
      AND `component_name` = 'SrmSupplierProfile'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM supplier-profile route menu for get-permission-info';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_2_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d7_2_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991010, 991011, 991012, 991013, 991014, 991015, 991016, 991017, 991018, 991020, 991021, 991022, 991023, 991024, 991025);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_2_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d7_2_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_d7_2_package_menu_ids` (`package_id`, `menu_id`)
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
  CROSS JOIN `tmp_srm_d7_2_menu_ids` AS `menu`
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
    FROM `tmp_srm_d7_2_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-d7-2',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-d7-2',
    NOW(),
    'srm-d7-2',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_d7_2_menu_ids` AS `menu`
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_2_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_2_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_d7_2_supplier_access_risk();

DROP PROCEDURE IF EXISTS ensure_srm_d7_2_supplier_access_risk;
