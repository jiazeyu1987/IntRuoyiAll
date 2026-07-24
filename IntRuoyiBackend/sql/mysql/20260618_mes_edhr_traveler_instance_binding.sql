-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260608_edhr_batch_execution_schema; type=schema; riskLevel=medium
-- eDHR traveler template, instance and event baseline.
-- First slice boundary: no physical print success, no print count deduction, no label parsing.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_traveler_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '流转单模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '流转单模板名称',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本',
  `status` varchar(32) NOT NULL COMMENT '模板状态：DRAFT/ACTIVE/DISABLED',
  `applicable_product_code` varchar(64) DEFAULT NULL COMMENT '适用产品编码；空表示通用',
  `applicable_route_id` bigint DEFAULT NULL COMMENT '适用工艺路线ID；空表示通用',
  `applicable_route_code` varchar(64) DEFAULT NULL COMMENT '适用工艺路线编码快照',
  `applicable_process_id` bigint DEFAULT NULL COMMENT '适用工序ID；空表示通用',
  `applicable_process_code` varchar(64) DEFAULT NULL COMMENT '适用工序编码快照',
  `applicable_process_name` varchar(128) DEFAULT NULL COMMENT '适用工序名称快照',
  `active_at` datetime DEFAULT NULL COMMENT '启用时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_traveler_template_code` (`tenant_id`, `template_code`, `deleted`),
  KEY `idx_mes_pro_edhr_traveler_template_scope` (`tenant_id`, `status`, `applicable_product_code`, `applicable_route_id`, `applicable_process_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR流转单模板';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_traveler_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `traveler_code` varchar(96) NOT NULL COMMENT '流转单编码',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码快照',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本快照',
  `batch_execution_id` bigint NOT NULL COMMENT 'eDHR批次执行ID',
  `batch_execution_code` varchar(64) NOT NULL COMMENT 'eDHR批次执行编码快照',
  `work_order_id` bigint NOT NULL COMMENT '工单ID',
  `work_order_code` varchar(64) NOT NULL COMMENT '工单编码快照',
  `batch_code` varchar(128) NOT NULL COMMENT '批次号',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID快照',
  `product_code` varchar(64) DEFAULT NULL COMMENT '产品编码快照',
  `product_name` varchar(255) DEFAULT NULL COMMENT '产品名称快照',
  `serial_no` varchar(128) DEFAULT NULL COMMENT 'SN码；空表示批次级流转单',
  `scope_type` varchar(32) NOT NULL COMMENT '绑定粒度：BATCH_LEVEL/SN_LEVEL',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `route_code` varchar(64) DEFAULT NULL COMMENT '工艺路线编码快照',
  `route_name` varchar(255) DEFAULT NULL COMMENT '工艺路线名称快照',
  `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
  `route_process_sort` int NOT NULL COMMENT '路线工序顺序',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `process_code` varchar(64) NOT NULL COMMENT '工序编码快照',
  `process_name` varchar(128) NOT NULL COMMENT '工序名称快照',
  `status` varchar(32) NOT NULL COMMENT '流转单状态：GENERATED/VOID',
  `print_status` varchar(32) NOT NULL COMMENT '打印状态：NOT_PRINTED/QUEUED',
  `business_key_hash` char(64) NOT NULL COMMENT '业务唯一键hash',
  `generated_by` bigint DEFAULT NULL COMMENT '生成人',
  `generated_at` datetime NOT NULL COMMENT '生成时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_traveler_business` (`tenant_id`, `business_key_hash`, `deleted`),
  UNIQUE KEY `uk_mes_pro_edhr_traveler_code` (`tenant_id`, `traveler_code`, `deleted`),
  KEY `idx_mes_pro_edhr_traveler_batch_process` (`tenant_id`, `batch_execution_id`, `route_process_id`),
  KEY `idx_mes_pro_edhr_traveler_status` (`tenant_id`, `status`, `generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR流转单实例';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_traveler_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `traveler_id` bigint NOT NULL COMMENT '流转单ID',
  `traveler_code` varchar(96) NOT NULL COMMENT '流转单编码快照',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `result_status` varchar(32) NOT NULL COMMENT '事件结果',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `operator_username` varchar(64) DEFAULT NULL COMMENT '操作人名称快照',
  `occurred_at` datetime NOT NULL COMMENT '发生时间',
  `metadata_json` longtext DEFAULT NULL COMMENT '事件元数据',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_traveler_event_traveler` (`tenant_id`, `traveler_id`, `occurred_at`),
  KEY `idx_mes_pro_edhr_traveler_event_type` (`tenant_id`, `event_type`, `result_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR流转单事件';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900266, 'eDHR流转单', 'mes:pro-edhr-traveler:query', 2, 9, 900220, '/mes/pro/feedback/edhr-traveler', 'ep:tickets', 'mes/pro/edhr-traveler/TravelerPage', 'MesProFeedbackEdhrTraveler', 0, b'1', b'1', b'1', 'edhr-traveler-menu', NOW(), 'edhr-traveler-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900266 OR `permission` = 'mes:pro-edhr-traveler:query' AND `type` = 2);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900267, 'eDHR流转单查询', 'mes:pro-edhr-traveler:query', 3, 1, 900266, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-traveler-menu', NOW(), 'edhr-traveler-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-traveler:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900268, 'eDHR流转单生成', 'mes:pro-edhr-traveler:generate', 3, 2, 900266, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-traveler-menu', NOW(), 'edhr-traveler-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-traveler:generate');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900269, 'eDHR流转单模板查询', 'mes:pro-edhr-traveler-template:query', 3, 3, 900266, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-traveler-menu', NOW(), 'edhr-traveler-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-traveler-template:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900270, 'eDHR流转单模板创建', 'mes:pro-edhr-traveler-template:create', 3, 4, 900266, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-traveler-menu', NOW(), 'edhr-traveler-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-traveler-template:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900271, 'eDHR流转单模板启用', 'mes:pro-edhr-traveler-template:activate', 3, 5, 900266, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-traveler-menu', NOW(), 'edhr-traveler-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-traveler-template:activate');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_traveler_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_traveler_menus()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR root system_menu row; cannot merge eDHR traveler menus';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR traveler menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900266, 900267, 900268, 900269, 900270, 900271)) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR traveler system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_traveler_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_traveler_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_traveler_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_traveler_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900266, 900267, 900268, 900269, 900270, 900271)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_traveler_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_traveler_package_menu_ids` AS
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_traveler_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT INTO `tmp_mes_edhr_traveler_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_traveler_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_traveler_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_traveler_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `distinct_menu`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-traveler-menu',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-traveler-menu',
    NOW(),
    'edhr-traveler-menu',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_traveler_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900266, 900267, 900268, 900269, 900270, 900271)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_traveler_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_traveler_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_traveler_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_traveler_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_traveler_menus;
