-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR commercial delivery cockpit first slice.
-- Required first-slice package codes: CSV_VALIDATION, OQ_PQ, TRAINING, DEPLOYMENT_AUTH, INTERFACE, OPERATIONS.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_delivery_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_code` varchar(64) NOT NULL COMMENT '交付项目编码',
  `project_name` varchar(128) NOT NULL COMMENT '交付项目名称',
  `customer_name` varchar(128) NOT NULL COMMENT '客户名称',
  `site_name` varchar(128) NOT NULL COMMENT '客户现场',
  `system_scope` varchar(500) NOT NULL COMMENT '系统范围',
  `validation_scope` varchar(500) NOT NULL COMMENT '验证范围',
  `release_tag` varchar(64) NOT NULL COMMENT '发布标签',
  `schema_version` varchar(64) NOT NULL COMMENT '数据库结构版本',
  `target_environment` varchar(64) NOT NULL COMMENT '目标环境',
  `project_status` varchar(32) NOT NULL COMMENT '项目状态：BLOCKED、READY、SIGNED',
  `signoff_allowed` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否允许签核',
  `owner_name` varchar(128) NOT NULL COMMENT '负责人',
  `owner_department` varchar(128) DEFAULT NULL COMMENT '负责部门',
  `blocked_reason` varchar(500) NOT NULL COMMENT '阻断原因',
  `gate_summary_json` longtext NOT NULL COMMENT '交付门禁摘要JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_delivery_project_code` (`tenant_id`, `project_code`, `deleted`),
  KEY `idx_mes_pro_edhr_delivery_project_status` (`tenant_id`, `project_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR商业化交付项目';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_evidence_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '交付项目ID',
  `package_code` varchar(64) NOT NULL COMMENT '证据包编码',
  `package_name` varchar(128) NOT NULL COMMENT '证据包名称',
  `package_type` varchar(64) NOT NULL COMMENT '证据包类型',
  `package_status` varchar(32) NOT NULL COMMENT '证据包状态：MISSING、PARTIAL、READY、ACCEPTED',
  `evidence_status` varchar(32) NOT NULL COMMENT '证据状态：MISSING、PARTIAL、READY',
  `owner_name` varchar(128) NOT NULL COMMENT '责任人',
  `owner_department` varchar(128) DEFAULT NULL COMMENT '负责部门',
  `required_evidence_json` longtext NOT NULL COMMENT '必需证据JSON',
  `available_evidence_json` longtext NOT NULL COMMENT '已有证据JSON',
  `missing_evidence_json` longtext NOT NULL COMMENT '缺失证据JSON',
  `signoff_impact` varchar(500) NOT NULL COMMENT '签核影响',
  `next_action` varchar(500) NOT NULL COMMENT '下一步动作',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_evidence_package_code` (`tenant_id`, `project_id`, `package_code`, `deleted`),
  KEY `idx_mes_pro_edhr_evidence_package_project` (`tenant_id`, `project_id`, `package_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR商业化交付证据包';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_delivery_gate_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '交付项目ID',
  `package_id` bigint NOT NULL COMMENT '证据包ID',
  `gate_code` varchar(64) NOT NULL COMMENT '门禁编码',
  `gate_name` varchar(128) NOT NULL COMMENT '门禁名称',
  `gate_status` varchar(32) NOT NULL COMMENT '门禁状态：BLOCKED、READY、WAIVED',
  `missing_evidence` varchar(500) NOT NULL COMMENT '缺失证据',
  `owner_name` varchar(128) NOT NULL COMMENT '责任人',
  `next_action` varchar(500) NOT NULL COMMENT '下一步动作',
  `signoff_impact` varchar(500) NOT NULL COMMENT '签核影响',
  `blocking_flag` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否阻断签核',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_delivery_gate_code` (`tenant_id`, `project_id`, `gate_code`, `deleted`),
  KEY `idx_mes_pro_edhr_delivery_gate_project` (`tenant_id`, `project_id`, `gate_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR商业化交付门禁项';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900283, 'eDHR交付驾驶舱', '', 2, 283, 900220, '/mes/pro/feedback/edhr-delivery', 'ep:finished', 'mes/pro/edhr-delivery/DeliveryPage', 'MesProEdhrDelivery', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900283 OR `path` = '/mes/pro/feedback/edhr-delivery');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900284, 'eDHR交付查询', 'mes:pro-edhr-delivery:query', 3, 1, 900283, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900284 OR `permission` = 'mes:pro-edhr-delivery:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900285, 'eDHR交付项目创建', 'mes:pro-edhr-delivery:create', 3, 2, 900283, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900285 OR `permission` = 'mes:pro-edhr-delivery:create');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_delivery_test_tenant_menus;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_delivery_test_tenant_menus()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_delivery_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_delivery_target_tenant` AS
  SELECT `tenant`.`id` AS `tenant_id`, `tenant`.`package_id`
  FROM `system_tenant` AS `tenant`
  WHERE `tenant`.`name` = '测试租户'
    AND `tenant`.`deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_delivery_target_tenant`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing unique 测试租户; cannot merge eDHR delivery menus';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_delivery_target_tenant` AS `target`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target`.`package_id`
     AND `package`.`deleted` = b'0'
    WHERE NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR delivery menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_delivery_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_delivery_menu_ids` AS
  SELECT `id` AS `menu_id`
  FROM `system_menu`
  WHERE `id` IN (900283, 900284, 900285)
    AND `deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_delivery_menu_ids`) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR delivery system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_delivery_package_existing_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_delivery_package_existing_menu_ids` AS
  SELECT `package`.`id` AS `package_id`, CAST(`menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_delivery_target_tenant` AS `target`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target`.`package_id`
   AND `package`.`deleted` = b'0'
  JOIN JSON_TABLE(CAST(`package`.`menu_ids` AS JSON), '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')) AS `menu`;

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_delivery_package_existing_menu_ids`
    WHERE `menu_id` = 900220
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing 测试租户 eDHR parent menu 900220; cannot merge delivery menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_delivery_package_merged_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_delivery_package_merged_menu_ids` AS
  SELECT DISTINCT `package_id`, `menu_id`
  FROM `tmp_mes_edhr_delivery_package_existing_menu_ids`
  UNION
  SELECT DISTINCT `target`.`package_id`, `menus`.`menu_id`
  FROM `tmp_mes_edhr_delivery_target_tenant` AS `target`
  JOIN `tmp_mes_edhr_delivery_menu_ids` AS `menus`;

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_delivery_target_tenant` AS `target`
    ON `target`.`package_id` = `package`.`id`
  JOIN (
    SELECT `package_id`, JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_delivery_package_merged_menu_ids`
    ) AS `deduplicated`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'system',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `role`.`id`, `menus`.`menu_id`, 'system', NOW(), 'system', NOW(), b'0', `role`.`tenant_id`
  FROM `tmp_mes_edhr_delivery_target_tenant` AS `target`
  JOIN `system_role` AS `role`
    ON `role`.`tenant_id` = `target`.`tenant_id`
   AND `role`.`code` = 'tenant_admin'
   AND `role`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_delivery_menu_ids` AS `menus`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menus`.`menu_id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_delivery_package_merged_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_delivery_package_existing_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_delivery_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_delivery_target_tenant`;
END//
DELIMITER ;

CALL ensure_mes_edhr_delivery_test_tenant_menus();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_delivery_test_tenant_menus;
