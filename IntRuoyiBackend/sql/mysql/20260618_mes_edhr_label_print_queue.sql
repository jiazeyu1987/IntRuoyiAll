-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_traveler_instance_binding; type=schema; riskLevel=medium
-- eDHR label template, label instance, print task and print event baseline.
-- Boundary: no browser-side print shortcut, no default success, no print count deduction before controlled confirmation.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_label_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '标签模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '标签模板名称',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本',
  `business_object_type` varchar(64) NOT NULL COMMENT '业务对象类型',
  `field_model_json` longtext NOT NULL COMMENT '字段模型JSON',
  `layout_json` longtext NOT NULL COMMENT '布局JSON',
  `parser_version` varchar(32) NOT NULL COMMENT '解析规则版本',
  `watermark_template` varchar(255) DEFAULT NULL COMMENT '水印模板',
  `status` varchar(32) NOT NULL COMMENT '模板状态：DRAFT/ACTIVE/DISABLED/VOID',
  `active_at` datetime DEFAULT NULL COMMENT '启用时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_label_template_code` (`tenant_id`, `template_code`, `deleted`),
  KEY `idx_mes_pro_edhr_label_template_scope` (`tenant_id`, `business_object_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR标签模板';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_label_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `label_code` varchar(96) NOT NULL COMMENT '标签实例编码',
  `template_id` bigint NOT NULL COMMENT '标签模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '标签模板编码快照',
  `template_version` varchar(32) NOT NULL COMMENT '标签模板版本快照',
  `business_type` varchar(64) NOT NULL COMMENT '业务对象类型',
  `business_object_id` bigint NOT NULL COMMENT '业务对象ID',
  `business_object_code` varchar(128) NOT NULL COMMENT '业务对象编码',
  `render_snapshot_json` longtext NOT NULL COMMENT '渲染快照JSON',
  `parser_version` varchar(32) NOT NULL COMMENT '解析版本快照',
  `status` varchar(32) NOT NULL COMMENT '标签状态：GENERATED/BOUND/VOID',
  `print_status` varchar(32) NOT NULL COMMENT '打印状态：NOT_PRINTED/WAITING/PENDING_CONFIRM/SUCCESS_CONFIRMED/FAILED/VOID_RESTRICTED',
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
  UNIQUE KEY `uk_mes_pro_edhr_label_business` (`tenant_id`, `business_key_hash`, `deleted`),
  UNIQUE KEY `uk_mes_pro_edhr_label_code` (`tenant_id`, `label_code`, `deleted`),
  KEY `idx_mes_pro_edhr_label_object` (`tenant_id`, `business_type`, `business_object_id`),
  KEY `idx_mes_pro_edhr_label_print_status` (`tenant_id`, `print_status`, `generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR标签实例';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_print_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_code` varchar(96) NOT NULL COMMENT '打印任务编码',
  `source_type` varchar(64) NOT NULL COMMENT '来源类型：LABEL/TRAVELER',
  `source_object_id` bigint NOT NULL COMMENT '来源对象ID',
  `source_object_code` varchar(128) NOT NULL COMMENT '来源对象编码',
  `template_type` varchar(64) NOT NULL COMMENT '模板类型：LABEL_TEMPLATE/TRAVELER_TEMPLATE',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码快照',
  `label_instance_id` bigint DEFAULT NULL COMMENT '标签实例ID',
  `traveler_id` bigint DEFAULT NULL COMMENT '流转单ID',
  `status` varchar(32) NOT NULL COMMENT '任务状态：WAITING/PRINTING/PENDING_CONFIRM/SUCCESS_CONFIRMED/FAILED/CANCELED/VOID_RESTRICTED',
  `print_confirm_status` varchar(32) NOT NULL COMMENT '打印确认状态：NOT_CONFIRMED/PENDING_CONFIRM/SUCCESS_CONFIRMED/FAILED',
  `is_reprint` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否补打',
  `original_print_task_id` bigint DEFAULT NULL COMMENT '原打印任务ID',
  `reprint_reason` varchar(500) DEFAULT NULL COMMENT '补打原因',
  `watermark_text` varchar(255) DEFAULT NULL COMMENT '水印文本',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `print_count_deducted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否已扣减打印次数',
  `requested_by` bigint DEFAULT NULL COMMENT '发起人',
  `requested_at` datetime NOT NULL COMMENT '发起时间',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `confirmation_evidence_hash` char(64) DEFAULT NULL COMMENT '确认凭证hash',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_print_task_code` (`tenant_id`, `task_code`, `deleted`),
  UNIQUE KEY `uk_mes_pro_edhr_print_task_idempotency` (`tenant_id`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_print_task_owner` (`tenant_id`, `creator`, `status`, `create_time`),
  KEY `idx_mes_pro_edhr_print_task_source` (`tenant_id`, `source_type`, `source_object_id`),
  KEY `idx_mes_pro_edhr_print_task_status` (`tenant_id`, `status`, `requested_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR打印任务';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_print_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `print_task_id` bigint NOT NULL COMMENT '打印任务ID',
  `task_code` varchar(96) NOT NULL COMMENT '打印任务编码快照',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型：PRINT_REQUESTED/PRINT_REPRINT_REQUESTED/PRINT_MARK_FAILED/PRINT_CONFIRM_SUCCESS',
  `result_status` varchar(32) NOT NULL COMMENT '事件结果：SUCCESS/BLOCKED/FAILED',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `operator_username` varchar(64) DEFAULT NULL COMMENT '操作人名称快照',
  `occurred_at` datetime NOT NULL COMMENT '发生时间',
  `evidence_hash` char(64) DEFAULT NULL COMMENT '证据hash',
  `metadata_json` longtext DEFAULT NULL COMMENT '事件元数据',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_print_event_task` (`tenant_id`, `print_task_id`, `occurred_at`),
  KEY `idx_mes_pro_edhr_print_event_type` (`tenant_id`, `event_type`, `result_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR打印事件';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900320, 'eDHR标签管理', 'mes:pro-edhr-label:query', 2, 10, 900220, '/mes/pro/feedback/edhr-label', 'ep:price-tag', 'mes/pro/edhr-label-print/LabelPrintQueuePage', 'MesProFeedbackEdhrLabelPrint', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900320 OR (`permission` = 'mes:pro-edhr-label:query' AND `type` = 2));

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900321, 'eDHR标签模板查询', 'mes:pro-edhr-label-template:query', 3, 1, 900320, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-label-template:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900322, 'eDHR标签模板创建', 'mes:pro-edhr-label-template:create', 3, 2, 900320, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-label-template:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900323, 'eDHR标签模板启用', 'mes:pro-edhr-label-template:activate', 3, 3, 900320, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-label-template:activate');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900324, 'eDHR标签查询', 'mes:pro-edhr-label:query', 3, 4, 900320, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-label:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900325, 'eDHR标签预览', 'mes:pro-edhr-label:preview', 3, 5, 900320, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-label:preview');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900326, 'eDHR打印任务', 'mes:pro-edhr-print-task:query', 2, 11, 900220, '/mes/pro/feedback/edhr-print-task', 'ep:printer', 'mes/pro/edhr-label-print/LabelPrintQueuePage', 'MesProFeedbackEdhrPrintTask', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900326 OR (`permission` = 'mes:pro-edhr-print-task:query' AND `type` = 2));

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900327, 'eDHR打印任务查询', 'mes:pro-edhr-print-task:query', 3, 1, 900326, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-task:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900328, 'eDHR创建打印任务', 'mes:pro-edhr-print-task:create', 3, 2, 900326, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-task:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900329, 'eDHR标记打印失败', 'mes:pro-edhr-print-task:mark-failed', 3, 3, 900326, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-task:mark-failed');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900330, 'eDHR确认打印成功', 'mes:pro-edhr-print-task:confirm', 3, 4, 900326, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-task:confirm');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900331, 'eDHR打印事件查询', 'mes:pro-edhr-print-task:query', 3, 5, 900326, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-label-print-menu', NOW(), 'edhr-label-print-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900331);

DROP PROCEDURE IF EXISTS ensure_mes_edhr_label_print_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_label_print_menus()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR root system_menu row; cannot merge eDHR label print menus';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR label print menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900320, 900321, 900322, 900323, 900324, 900325, 900326, 900327, 900328, 900329, 900330, 900331)) <> 12 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR label print system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_label_print_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_label_print_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900320, 900321, 900322, 900323, 900324, 900325, 900326, 900327, 900328, 900329, 900330, 900331)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_label_print_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_label_print_package_menu_ids` AS
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_label_print_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT INTO `tmp_mes_edhr_label_print_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_label_print_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_label_print_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_label_print_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `distinct_menu`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-label-print-menu',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT
    `role`.`id`,
    `menu`.`id`,
    'edhr-label-print-menu',
    NOW(),
    'edhr-label-print-menu',
    NOW(),
    b'0'
  FROM `system_users` AS `tenant_admin`
  JOIN `system_user_role` AS `user_role`
    ON `user_role`.`user_id` = `tenant_admin`.`id`
   AND `user_role`.`deleted` = b'0'
  JOIN `system_role` AS `role`
    ON `role`.`id` = `user_role`.`role_id`
   AND `role`.`deleted` = b'0'
  CROSS JOIN `tmp_mes_edhr_label_print_menu_ids` AS `menu`
  WHERE `tenant_admin`.`deleted` = b'0'
    AND `tenant_admin`.`username` = 'aoteman'
    AND `role`.`tenant_id` = 122
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`deleted` = b'0'
    );
END$$
DELIMITER ;

CALL ensure_mes_edhr_label_print_menus();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_label_print_menus;
