-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_form_instance; type=schema; riskLevel=medium
-- eDHR recordbook template, recordbook, entry, controlled tag binding and event baseline.
-- First recordbook slice boundary: no approval route, no electronic signature, no attachment, no print/export, no batch-record shortcut.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_recordbook_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '记录本模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '记录本模板名称',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本',
  `recordbook_type` varchar(64) NOT NULL COMMENT '记录本类型',
  `entry_schema_json` longtext NOT NULL COMMENT '条目字段定义JSON',
  `tag_policy_json` longtext DEFAULT NULL COMMENT '标签策略JSON',
  `status` varchar(32) NOT NULL COMMENT '模板状态：DRAFT/ACTIVE/DISABLED',
  `active_by` bigint DEFAULT NULL COMMENT '启用人',
  `active_at` datetime DEFAULT NULL COMMENT '启用时间',
  `disabled_by` bigint DEFAULT NULL COMMENT '停用人',
  `disabled_at` datetime DEFAULT NULL COMMENT '停用时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_recordbook_template_code` (`tenant_id`, `template_code`, `deleted`),
  KEY `idx_mes_pro_edhr_recordbook_template_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR记录本模板';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_recordbook` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recordbook_code` varchar(96) NOT NULL COMMENT '记录本编码',
  `recordbook_name` varchar(128) NOT NULL COMMENT '记录本名称',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码快照',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称快照',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本快照',
  `recordbook_type` varchar(64) NOT NULL COMMENT '记录本类型快照',
  `status` varchar(32) NOT NULL COMMENT '记录本状态：OPEN/DISABLED/CLOSED',
  `owner_user_id` bigint DEFAULT NULL COMMENT '责任人',
  `owner_dept_id` bigint DEFAULT NULL COMMENT '责任部门',
  `business_scope` varchar(64) DEFAULT NULL COMMENT '业务范围',
  `business_object_type` varchar(64) DEFAULT NULL COMMENT '业务对象类型',
  `business_object_id` bigint DEFAULT NULL COMMENT '业务对象ID',
  `business_object_code` varchar(96) DEFAULT NULL COMMENT '业务对象编码快照',
  `opened_at` datetime NOT NULL COMMENT '开本时间',
  `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
  `entry_count` int NOT NULL COMMENT '条目数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_recordbook_code` (`tenant_id`, `recordbook_code`, `deleted`),
  KEY `idx_mes_pro_edhr_recordbook_owner` (`tenant_id`, `owner_user_id`, `status`, `deleted`),
  KEY `idx_mes_pro_edhr_recordbook_template` (`tenant_id`, `template_id`, `status`, `deleted`),
  KEY `idx_mes_pro_edhr_recordbook_business` (`tenant_id`, `business_scope`, `business_object_type`, `business_object_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR记录本';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_recordbook_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `entry_code` varchar(96) NOT NULL COMMENT '记录条目编码',
  `recordbook_id` bigint NOT NULL COMMENT '记录本ID',
  `recordbook_code` varchar(96) NOT NULL COMMENT '记录本编码快照',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码快照',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本快照',
  `status` varchar(32) NOT NULL COMMENT '条目状态：DRAFT/SUBMITTED',
  `version` int NOT NULL COMMENT '条目版本',
  `entry_title` varchar(160) NOT NULL COMMENT '条目标题',
  `entry_content_json` longtext NOT NULL COMMENT '条目正文JSON',
  `tag_snapshot_json` longtext DEFAULT NULL COMMENT '标签快照JSON',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `locked_at` datetime DEFAULT NULL COMMENT '正文锁定时间',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_recordbook_entry_code` (`tenant_id`, `entry_code`, `deleted`),
  UNIQUE KEY `uk_mes_pro_edhr_recordbook_entry_idempotency` (`tenant_id`, `recordbook_id`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_recordbook_entry_book` (`tenant_id`, `recordbook_id`, `status`, `deleted`),
  KEY `idx_mes_pro_edhr_recordbook_entry_submitter` (`tenant_id`, `submitted_by`, `submitted_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR记录本条目';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_controlled_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag_code` varchar(64) NOT NULL COMMENT '受控标签编码',
  `tag_name` varchar(128) NOT NULL COMMENT '受控标签名称',
  `tag_type` varchar(64) DEFAULT NULL COMMENT '标签类型',
  `tag_status` varchar(32) NOT NULL COMMENT '标签状态：DRAFT/ACTIVE/DISABLED',
  `active_by` bigint DEFAULT NULL COMMENT '启用人',
  `active_at` datetime DEFAULT NULL COMMENT '启用时间',
  `disabled_by` bigint DEFAULT NULL COMMENT '停用人',
  `disabled_at` datetime DEFAULT NULL COMMENT '停用时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_controlled_tag_code` (`tenant_id`, `tag_code`, `deleted`),
  KEY `idx_mes_pro_edhr_recordbook_tag_status` (`tenant_id`, `tag_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR受控标签';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_recordbook_tag_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `entry_id` bigint NOT NULL COMMENT '条目ID',
  `recordbook_id` bigint NOT NULL COMMENT '记录本ID',
  `tag_code` varchar(64) NOT NULL COMMENT '标签编码快照',
  `tag_name` varchar(128) NOT NULL COMMENT '标签名称快照',
  `tag_status` varchar(32) NOT NULL COMMENT '标签状态快照',
  `bound_by` bigint DEFAULT NULL COMMENT '绑定人',
  `bound_at` datetime DEFAULT NULL COMMENT '绑定时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_recordbook_tag_binding` (`tenant_id`, `entry_id`, `tag_code`, `deleted`),
  KEY `idx_mes_pro_edhr_recordbook_tag_binding_book` (`tenant_id`, `recordbook_id`, `tag_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR记录本标签绑定';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_recordbook_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recordbook_id` bigint DEFAULT NULL COMMENT '记录本ID',
  `entry_id` bigint DEFAULT NULL COMMENT '条目ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `from_status` varchar(32) DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(32) DEFAULT NULL COMMENT '目标状态',
  `result_status` varchar(32) NOT NULL COMMENT '事件结果',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `operator_username` varchar(64) DEFAULT NULL COMMENT '操作人名称快照',
  `occurred_at` datetime NOT NULL COMMENT '发生时间',
  `event_snapshot_json` longtext DEFAULT NULL COMMENT '事件快照JSON',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键快照',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_recordbook_event_entry` (`tenant_id`, `entry_id`, `occurred_at`),
  KEY `idx_mes_pro_edhr_recordbook_event_book` (`tenant_id`, `recordbook_id`, `occurred_at`),
  KEY `idx_mes_pro_edhr_recordbook_event_type` (`tenant_id`, `event_type`, `result_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR记录本事件';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900301, 'eDHR记录本', 'mes:pro-edhr-recordbook:query', 2, 11, 900220, '/mes/pro/edhr-recordbook', 'ep:notebook', 'mes/pro/edhr-recordbook/RecordbookPage', 'MesProEdhrRecordbook', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900301 OR (`permission` = 'mes:pro-edhr-recordbook:query' AND `type` = 2));

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900302, 'eDHR记录本模板查询', 'mes:pro-edhr-recordbook-template:query', 3, 1, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook-template:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900303, 'eDHR记录本模板创建', 'mes:pro-edhr-recordbook-template:create', 3, 2, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook-template:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900304, 'eDHR记录本模板启用', 'mes:pro-edhr-recordbook-template:activate', 3, 3, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook-template:activate');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900305, 'eDHR记录本查询', 'mes:pro-edhr-recordbook:query', 3, 4, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900306, 'eDHR记录本创建', 'mes:pro-edhr-recordbook:create', 3, 5, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900307, 'eDHR记录条目查询', 'mes:pro-edhr-recordbook-entry:query', 3, 6, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook-entry:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900308, 'eDHR记录条目创建', 'mes:pro-edhr-recordbook-entry:create', 3, 7, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook-entry:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900309, 'eDHR记录条目草稿保存', 'mes:pro-edhr-recordbook-entry:save', 3, 8, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook-entry:save');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900310, 'eDHR记录条目提交', 'mes:pro-edhr-recordbook-entry:submit', 3, 9, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-recordbook-entry:submit');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900311, 'eDHR受控标签查询', 'mes:pro-edhr-tag:query', 3, 10, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-tag:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900312, 'eDHR受控标签创建', 'mes:pro-edhr-tag:create', 3, 11, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-tag:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900313, 'eDHR受控标签启用', 'mes:pro-edhr-tag:activate', 3, 12, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-tag:activate');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900314, 'eDHR受控标签停用', 'mes:pro-edhr-tag:disable', 3, 13, 900301, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-recordbook-menu', NOW(), 'edhr-recordbook-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-tag:disable');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_recordbook_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_recordbook_menus()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR root system_menu row; cannot merge eDHR recordbook menus';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR recordbook menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900301, 900302, 900303, 900304, 900305, 900306, 900307, 900308, 900309, 900310, 900311, 900312, 900313, 900314)) <> 14 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR recordbook system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_recordbook_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_recordbook_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_recordbook_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_recordbook_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900301, 900302, 900303, 900304, 900305, 900306, 900307, 900308, 900309, 900310, 900311, 900312, 900313, 900314)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_recordbook_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_recordbook_package_menu_ids` AS
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_recordbook_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT INTO `tmp_mes_edhr_recordbook_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_recordbook_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_recordbook_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_recordbook_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `distinct_menu`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-recordbook-menu',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-recordbook-menu',
    NOW(),
    'edhr-recordbook-menu',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_recordbook_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900301, 900302, 900303, 900304, 900305, 900306, 900307, 900308, 900309, 900310, 900311, 900312, 900313, 900314)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_recordbook_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_recordbook_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_recordbook_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_recordbook_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_recordbook_menus;
