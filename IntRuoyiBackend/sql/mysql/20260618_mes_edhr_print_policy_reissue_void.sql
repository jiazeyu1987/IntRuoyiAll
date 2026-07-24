-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_label_print_queue; type=schema; riskLevel=medium
-- eDHR print policy, controlled reprint, void history copy and export audit baseline.
-- Boundary: no physical print success confirmation, no default success, no print count deduction by click.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_print_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `policy_code` varchar(64) NOT NULL COMMENT '策略编码',
  `policy_name` varchar(128) NOT NULL COMMENT '策略名称',
  `business_type` varchar(64) NOT NULL COMMENT '业务类型：LABEL/TRAVELER',
  `template_type` varchar(64) NOT NULL COMMENT '模板类型',
  `first_print_limit` int NOT NULL COMMENT '首次打印次数上限',
  `reprint_limit` int NOT NULL COMMENT '补打次数上限',
  `reason_dict_json` longtext NOT NULL COMMENT '补打原因字典JSON',
  `watermark_template` varchar(255) NOT NULL COMMENT '补打水印模板',
  `void_copy_watermark` varchar(255) NOT NULL COMMENT '作废历史副本水印',
  `status` varchar(32) NOT NULL COMMENT '状态：DRAFT/ACTIVE/DISABLED',
  `active_at` datetime DEFAULT NULL COMMENT '启用时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_print_policy_code` (`tenant_id`, `policy_code`, `deleted`),
  UNIQUE KEY `uk_mes_pro_edhr_print_policy_scope` (`tenant_id`, `business_type`, `template_type`, `status`, `deleted`),
  KEY `idx_mes_pro_edhr_print_policy_scope` (`tenant_id`, `business_type`, `template_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR打印策略';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_reprint_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_code` varchar(96) NOT NULL COMMENT '补打申请编码',
  `print_task_id` bigint NOT NULL COMMENT '新补打任务ID',
  `original_print_task_id` bigint NOT NULL COMMENT '原打印任务ID',
  `reprint_reason_code` varchar(64) NOT NULL COMMENT '补打原因编码',
  `reprint_reason` varchar(500) NOT NULL COMMENT '补打原因',
  `used_reprint_count` int NOT NULL COMMENT '当前已用补打次数',
  `reprint_limit` int NOT NULL COMMENT '补打次数上限',
  `watermark_text` varchar(255) NOT NULL COMMENT '水印文本',
  `status` varchar(32) NOT NULL COMMENT '状态：REQUESTED/CANCELED',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_reprint_code` (`tenant_id`, `request_code`, `deleted`),
  UNIQUE KEY `uk_mes_pro_edhr_reprint_idempotency` (`tenant_id`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_reprint_original` (`tenant_id`, `original_print_task_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR补打申请';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_print_history_copy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `copy_code` varchar(96) NOT NULL COMMENT '历史副本编码',
  `source_print_task_id` bigint NOT NULL COMMENT '来源打印任务ID',
  `source_object_type` varchar(64) NOT NULL COMMENT '来源对象类型',
  `source_object_code` varchar(128) NOT NULL COMMENT '来源对象编码',
  `copy_reason` varchar(500) NOT NULL COMMENT '历史副本原因',
  `watermark_text` varchar(255) NOT NULL COMMENT '作废水印',
  `evidence_hash` char(64) NOT NULL COMMENT '证据hash',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_history_copy_code` (`tenant_id`, `copy_code`, `deleted`),
  UNIQUE KEY `uk_mes_pro_edhr_history_copy_idempotency` (`tenant_id`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_history_copy_source` (`tenant_id`, `source_print_task_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR作废历史副本';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_print_export_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `export_code` varchar(96) NOT NULL COMMENT '导出编码',
  `filter_snapshot_json` longtext NOT NULL COMMENT '筛选快照JSON',
  `result_status` varchar(32) NOT NULL COMMENT '结果状态：EXPORT_RECORDED',
  `evidence_hash` char(64) NOT NULL COMMENT '证据hash',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `exported_by` bigint DEFAULT NULL COMMENT '导出人',
  `exported_at` datetime NOT NULL COMMENT '导出时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_print_export_code` (`tenant_id`, `export_code`, `deleted`),
  UNIQUE KEY `uk_mes_pro_edhr_print_export_idempotency` (`tenant_id`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_print_export_time` (`tenant_id`, `exported_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR打印历史导出审计';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900338, 'eDHR打印策略', 'mes:pro-edhr-print-policy:query', 2, 12, 900220, '/mes/pro/feedback/edhr-print-policy', 'ep:setting', 'mes/pro/edhr-label-print/LabelPrintQueuePage', 'MesProFeedbackEdhrPrintPolicy', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900338 OR (`permission` = 'mes:pro-edhr-print-policy:query' AND `type` = 2));

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900339, 'eDHR打印策略查询', 'mes:pro-edhr-print-policy:query', 3, 1, 900338, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-policy:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900340, 'eDHR打印策略创建', 'mes:pro-edhr-print-policy:create', 3, 2, 900338, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-policy:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900341, 'eDHR打印策略启用', 'mes:pro-edhr-print-policy:activate', 3, 3, 900338, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-policy:activate');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900342, 'eDHR补打申请', 'mes:pro-edhr-print-task:reprint', 3, 6, 900278, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-task:reprint');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900343, 'eDHR作废历史副本', 'mes:pro-edhr-print-task:history-copy', 3, 7, 900278, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-task:history-copy');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900344, 'eDHR打印历史导出', 'mes:pro-edhr-print-task:export', 3, 8, 900278, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-print-task:export');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900345, 'eDHR补打历史查询', 'mes:pro-edhr-print-task:query', 3, 9, 900278, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900345);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900346, 'eDHR导出审计查询', 'mes:pro-edhr-print-task:query', 3, 10, 900278, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-print-policy-menu', NOW(), 'edhr-print-policy-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900346);

DROP PROCEDURE IF EXISTS ensure_mes_edhr_print_policy_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_print_policy_menus()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR root system_menu row; cannot merge eDHR print policy menus';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR print policy menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900338, 900339, 900340, 900341, 900342, 900343, 900344, 900345, 900346)) <> 9 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR print policy system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_print_policy_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_print_policy_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_print_policy_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_print_policy_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900338, 900339, 900340, 900341, 900342, 900343, 900344, 900345, 900346)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_print_policy_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_print_policy_package_menu_ids` AS
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_print_policy_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT INTO `tmp_mes_edhr_print_policy_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_print_policy_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_print_policy_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_print_policy_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `distinct_menu`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-print-policy-menu',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT
    `role`.`id`,
    `menu`.`id`,
    'edhr-print-policy-menu',
    NOW(),
    'edhr-print-policy-menu',
    NOW(),
    b'0'
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_mes_edhr_print_policy_menu_ids` AS `menu`
  WHERE `role`.`code` = 'tenant_admin'
    AND `role`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `role_menu`
      WHERE `role_menu`.`role_id` = `role`.`id`
        AND `role_menu`.`menu_id` = `menu`.`id`
        AND `role_menu`.`deleted` = b'0'
    );
END$$
DELIMITER ;

CALL ensure_mes_edhr_print_policy_menus();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_print_policy_menus;

-- Status/event contract literals:
-- DRAFT ACTIVE DISABLED REQUESTED VOID_HISTORY_COPY EXPORT_RECORDED
-- PRINT_POLICY_CREATED PRINT_POLICY_ACTIVATED PRINT_REPRINT_POLICY_ACCEPTED PRINT_VOID_HISTORY_COPY_CREATED PRINT_HISTORY_EXPORTED
