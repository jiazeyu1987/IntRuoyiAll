-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR independent form template, instance, value and event baseline.
-- First slice boundary: no recordbook, no controlled tags, no approval, no electronic signature, no attachment, no print/export.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_form_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '独立表单模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '独立表单模板名称',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本',
  `field_schema_json` longtext NOT NULL COMMENT '字段定义JSON',
  `status` varchar(32) NOT NULL COMMENT '模板状态：DRAFT/ACTIVE/DISABLED',
  `active_by` bigint DEFAULT NULL COMMENT '启用人',
  `active_at` datetime DEFAULT NULL COMMENT '启用时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_form_template_code` (`tenant_id`, `template_code`, `deleted`),
  KEY `idx_mes_pro_edhr_form_template_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR独立表单模板';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_form_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance_code` varchar(96) NOT NULL COMMENT '独立表单实例编码',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码快照',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称快照',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本快照',
  `status` varchar(32) NOT NULL COMMENT '实例状态：DRAFT/SUBMITTED',
  `version` int NOT NULL COMMENT '实例版本',
  `business_scope` varchar(64) DEFAULT NULL COMMENT '业务范围；空表示独立表单',
  `business_object_type` varchar(64) DEFAULT NULL COMMENT '业务对象类型',
  `business_object_id` bigint DEFAULT NULL COMMENT '业务对象ID',
  `business_object_code` varchar(96) DEFAULT NULL COMMENT '业务对象编码快照',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_form_instance_code` (`tenant_id`, `instance_code`, `deleted`),
  KEY `idx_mes_pro_edhr_form_instance_template` (`tenant_id`, `template_id`, `status`, `deleted`),
  KEY `idx_mes_pro_edhr_form_instance_business` (`tenant_id`, `business_scope`, `business_object_type`, `business_object_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR独立表单实例';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_form_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance_id` bigint NOT NULL COMMENT '表单实例ID',
  `field_key` varchar(128) NOT NULL COMMENT '字段键',
  `field_label` varchar(128) NOT NULL COMMENT '字段标签快照',
  `field_type` varchar(32) NOT NULL COMMENT '字段类型快照',
  `value_text` varchar(1000) DEFAULT NULL COMMENT '字段值文本',
  `value_json` longtext DEFAULT NULL COMMENT '字段值JSON',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_form_value_field` (`tenant_id`, `instance_id`, `field_key`, `deleted`),
  KEY `idx_mes_pro_edhr_form_value_instance` (`tenant_id`, `instance_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR独立表单字段值';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_form_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance_id` bigint DEFAULT NULL COMMENT '表单实例ID；模板事件为空',
  `template_id` bigint DEFAULT NULL COMMENT '模板ID',
  `instance_code` varchar(96) DEFAULT NULL COMMENT '实例编码快照',
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
  KEY `idx_mes_pro_edhr_form_event_instance` (`tenant_id`, `instance_id`, `occurred_at`),
  KEY `idx_mes_pro_edhr_form_event_template` (`tenant_id`, `template_id`, `occurred_at`),
  KEY `idx_mes_pro_edhr_form_event_type` (`tenant_id`, `event_type`, `result_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR独立表单事件';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900272, 'eDHR独立表单', 'mes:pro-edhr-form-instance:query', 2, 10, 900220, '/mes/pro/feedback/edhr-form', 'ep:document', 'mes/pro/edhr-form/FormPage', 'MesProFeedbackEdhrForm', 0, b'1', b'1', b'1', 'edhr-form-menu', NOW(), 'edhr-form-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900272 OR (`permission` = 'mes:pro-edhr-form-instance:query' AND `type` = 2));

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900273, 'eDHR独立表单模板查询', 'mes:pro-edhr-form-template:query', 3, 1, 900272, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-form-menu', NOW(), 'edhr-form-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-form-template:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900274, 'eDHR独立表单模板创建', 'mes:pro-edhr-form-template:create', 3, 2, 900272, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-form-menu', NOW(), 'edhr-form-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-form-template:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900275, 'eDHR独立表单模板启用', 'mes:pro-edhr-form-template:activate', 3, 3, 900272, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-form-menu', NOW(), 'edhr-form-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-form-template:activate');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900276, 'eDHR独立表单实例查询', 'mes:pro-edhr-form-instance:query', 3, 4, 900272, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-form-menu', NOW(), 'edhr-form-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-form-instance:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900277, 'eDHR独立表单实例创建保存', 'mes:pro-edhr-form-instance:create', 3, 5, 900272, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-form-menu', NOW(), 'edhr-form-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-form-instance:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900278, 'eDHR独立表单实例提交', 'mes:pro-edhr-form-instance:submit', 3, 6, 900272, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-form-menu', NOW(), 'edhr-form-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-form-instance:submit');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900279, 'eDHR独立表单实例草稿保存', 'mes:pro-edhr-form-instance:save', 3, 7, 900272, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-form-menu', NOW(), 'edhr-form-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-form-instance:save');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_form_menus()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR root system_menu row; cannot merge eDHR form menus';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR form menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900272, 900273, 900274, 900275, 900276, 900277, 900278, 900279)) <> 8 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR form system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_form_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_form_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900272, 900273, 900274, 900275, 900276, 900277, 900278, 900279)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_form_package_menu_ids` AS
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_form_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT INTO `tmp_mes_edhr_form_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_form_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_form_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_form_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `distinct_menu`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-form-menu',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-form-menu',
    NOW(),
    'edhr-form-menu',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_form_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900272, 900273, 900274, 900275, 900276, 900277, 900278, 900279)
   AND `menu`.`deleted` = b'0'
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` = 'tenant_admin'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`deleted` = b'0'
        AND `existing`.`tenant_id` = `role`.`tenant_id`
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_form_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_menus;
