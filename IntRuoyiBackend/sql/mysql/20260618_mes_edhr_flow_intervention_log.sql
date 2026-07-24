-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_release_transaction_lifecycle; type=schema; riskLevel=medium
-- eDHR commercial flow intervention log: return, withdraw, transfer, add-sign, admin intervention, and auditable flow events.
-- This migration is idempotent and fail-fast for invalid tenant package menu JSON.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_flow_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_object_type` varchar(64) NOT NULL COMMENT '业务对象类型',
  `business_object_id` varchar(128) NOT NULL COMMENT '业务对象ID',
  `business_object_code` varchar(128) DEFAULT NULL COMMENT '业务对象编号',
  `intervention_id` bigint DEFAULT NULL COMMENT '流程干预ID',
  `flow_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `task_id` varchar(128) DEFAULT NULL COMMENT '流程任务ID',
  `node_key` varchar(128) DEFAULT NULL COMMENT '节点标识',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `from_status` varchar(32) NOT NULL COMMENT '原状态',
  `to_status` varchar(32) NOT NULL COMMENT '目标状态',
  `actor_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标处理人',
  `permission_code` varchar(128) NOT NULL COMMENT '权限编码',
  `permission_decision` varchar(32) NOT NULL COMMENT '权限判定',
  `reason` varchar(500) DEFAULT NULL COMMENT '原因',
  `signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '签核证据摘要',
  `integrity_check_result` varchar(32) NOT NULL COMMENT '完整性复检结果',
  `integrity_check_snapshot_json` longtext DEFAULT NULL COMMENT '完整性复检快照JSON',
  `event_snapshot_json` longtext DEFAULT NULL COMMENT '事件快照JSON',
  `evidence_hash` char(64) NOT NULL COMMENT '事件证据摘要',
  `occurred_at` datetime NOT NULL COMMENT '事件发生时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_flow_event_object` (`tenant_id`, `business_object_type`, `business_object_id`, `occurred_at`, `deleted`),
  KEY `idx_mes_pro_edhr_flow_event_instance` (`tenant_id`, `flow_instance_id`, `task_id`, `occurred_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 流程事件';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_flow_intervention` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `intervention_code` varchar(64) NOT NULL COMMENT '干预编号',
  `business_object_type` varchar(64) NOT NULL COMMENT '业务对象类型',
  `business_object_id` varchar(128) NOT NULL COMMENT '业务对象ID',
  `business_object_code` varchar(128) DEFAULT NULL COMMENT '业务对象编号',
  `flow_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `intervention_action` varchar(32) NOT NULL COMMENT '干预动作',
  `intervention_status` varchar(32) NOT NULL COMMENT '干预状态',
  `from_status` varchar(32) NOT NULL COMMENT '原状态',
  `to_status` varchar(32) NOT NULL COMMENT '目标状态',
  `source_task_id` varchar(128) DEFAULT NULL COMMENT '来源任务ID',
  `target_task_id` varchar(128) DEFAULT NULL COMMENT '目标任务ID',
  `node_key` varchar(128) DEFAULT NULL COMMENT '节点标识',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标处理人',
  `requested_by` bigint DEFAULT NULL COMMENT '申请人',
  `requested_at` datetime NOT NULL COMMENT '申请时间',
  `reason_category` varchar(64) DEFAULT NULL COMMENT '原因分类',
  `reason` varchar(500) NOT NULL COMMENT '原因',
  `authorization_basis` varchar(500) DEFAULT NULL COMMENT '授权依据',
  `signoff_evidence_hash` char(64) NOT NULL COMMENT '签核证据摘要',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `integrity_check_result` varchar(32) NOT NULL COMMENT '完整性复检结果',
  `integrity_check_snapshot_json` longtext DEFAULT NULL COMMENT '完整性复检快照JSON',
  `evidence_hash` char(64) NOT NULL COMMENT '干预证据摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_flow_intervention_idempotency` (`tenant_id`, `business_object_type`, `business_object_id`, `intervention_action`, `idempotency_key`, `deleted`),
  KEY `idx_mes_pro_edhr_flow_intervention_object` (`tenant_id`, `business_object_type`, `business_object_id`, `requested_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 流程干预';

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_legacy_menu_map`;
CREATE TEMPORARY TABLE `tmp_mes_edhr_flow_intervention_legacy_menu_map` (
  `old_menu_id` bigint NOT NULL,
  `new_menu_id` bigint NOT NULL,
  PRIMARY KEY (`old_menu_id`)
) ENGINE=Memory;

INSERT INTO `tmp_mes_edhr_flow_intervention_legacy_menu_map` (`old_menu_id`, `new_menu_id`)
SELECT `menu`.`id`,
       CASE
         WHEN `menu`.`path` = '/mes/pro/feedback/edhr-flow-intervention' THEN 900356
         WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:event-query' THEN 900357
         WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:return' THEN 900358
         WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:withdraw' THEN 900359
         WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:transfer' THEN 900360
         WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:add-sign' THEN 900361
         WHEN `menu`.`permission` = 'mes:pro-edhr-flow-intervention:admin-intervene' THEN 900362
       END AS `new_menu_id`
FROM `system_menu` AS `menu`
WHERE `menu`.`deleted` = b'0'
  AND (
    (`menu`.`id` = 900286 AND `menu`.`path` = '/mes/pro/feedback/edhr-flow-intervention')
    OR (`menu`.`id` = 900287 AND `menu`.`permission` = 'mes:pro-edhr-flow-intervention:event-query')
    OR (`menu`.`id` = 900288 AND `menu`.`permission` = 'mes:pro-edhr-flow-intervention:return')
    OR (`menu`.`id` = 900289 AND `menu`.`permission` = 'mes:pro-edhr-flow-intervention:withdraw')
    OR (`menu`.`id` = 900290 AND `menu`.`permission` = 'mes:pro-edhr-flow-intervention:transfer')
    OR (`menu`.`id` = 900291 AND `menu`.`permission` = 'mes:pro-edhr-flow-intervention:add-sign')
    OR (`menu`.`id` = 900292 AND `menu`.`permission` = 'mes:pro-edhr-flow-intervention:admin-intervene')
  );

DELETE `legacy_menu` FROM `system_menu` AS `legacy_menu`
JOIN `tmp_mes_edhr_flow_intervention_legacy_menu_map` AS `legacy_map`
  ON `legacy_map`.`old_menu_id` = `legacy_menu`.`id`;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900356, 'eDHR流程干预管理', 'mes:pro-edhr-flow-intervention:query', 2, 356, 900220,
       '/mes/pro/feedback/edhr-flow-intervention', 'ep:connection', 'mes/pro/edhr-flow-intervention/FlowInterventionPage', 'MesProEdhrFlowIntervention', 0, b'1', b'0', b'1', 'edhr-flow-intervention', NOW(), 'edhr-flow-intervention', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900356 OR (`permission` = 'mes:pro-edhr-flow-intervention:query' AND `type` = 2));

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900357, 'eDHR流程日志查询', 'mes:pro-edhr-flow-intervention:event-query', 3, 1, 900356,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-flow-intervention', NOW(), 'edhr-flow-intervention', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900356 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900357 OR `permission` = 'mes:pro-edhr-flow-intervention:event-query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900358, 'eDHR流程退回', 'mes:pro-edhr-flow-intervention:return', 3, 2, 900356,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-flow-intervention', NOW(), 'edhr-flow-intervention', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900356 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900358 OR `permission` = 'mes:pro-edhr-flow-intervention:return');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900359, 'eDHR流程撤回', 'mes:pro-edhr-flow-intervention:withdraw', 3, 3, 900356,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-flow-intervention', NOW(), 'edhr-flow-intervention', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900356 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900359 OR `permission` = 'mes:pro-edhr-flow-intervention:withdraw');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900360, 'eDHR流程转办', 'mes:pro-edhr-flow-intervention:transfer', 3, 4, 900356,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-flow-intervention', NOW(), 'edhr-flow-intervention', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900356 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900360 OR `permission` = 'mes:pro-edhr-flow-intervention:transfer');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900361, 'eDHR流程加签', 'mes:pro-edhr-flow-intervention:add-sign', 3, 5, 900356,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-flow-intervention', NOW(), 'edhr-flow-intervention', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900356 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900361 OR `permission` = 'mes:pro-edhr-flow-intervention:add-sign');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900362, 'eDHR管理员干预', 'mes:pro-edhr-flow-intervention:admin-intervene', 3, 6, 900356,
       '', '', '', '', 0, b'1', b'0', b'1', 'edhr-flow-intervention', NOW(), 'edhr-flow-intervention', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900356 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900362 OR `permission` = 'mes:pro-edhr-flow-intervention:admin-intervene');

UPDATE `system_menu`
SET `name` = 'eDHR流程干预管理',
    `permission` = 'mes:pro-edhr-flow-intervention:query',
    `type` = 2,
    `sort` = 356,
    `parent_id` = 900220,
    `path` = '/mes/pro/feedback/edhr-flow-intervention',
    `icon` = 'ep:connection',
    `component` = 'mes/pro/edhr-flow-intervention/FlowInterventionPage',
    `component_name` = 'MesProEdhrFlowIntervention',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'0',
    `always_show` = b'1',
    `updater` = 'edhr-flow-intervention',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 900356
  AND `path` = '/mes/pro/feedback/edhr-flow-intervention';

UPDATE `system_menu`
SET `name` = CASE `id`
      WHEN 900357 THEN 'eDHR流程日志查询'
      WHEN 900358 THEN 'eDHR流程退回'
      WHEN 900359 THEN 'eDHR流程撤回'
      WHEN 900360 THEN 'eDHR流程转办'
      WHEN 900361 THEN 'eDHR流程加签'
      WHEN 900362 THEN 'eDHR管理员干预'
    END,
    `type` = 3,
    `sort` = CASE `id`
      WHEN 900357 THEN 1
      WHEN 900358 THEN 2
      WHEN 900359 THEN 3
      WHEN 900360 THEN 4
      WHEN 900361 THEN 5
      WHEN 900362 THEN 6
    END,
    `parent_id` = 900356,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'0',
    `always_show` = b'1',
    `updater` = 'edhr-flow-intervention',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE (`id` = 900357 AND `permission` = 'mes:pro-edhr-flow-intervention:event-query')
   OR (`id` = 900358 AND `permission` = 'mes:pro-edhr-flow-intervention:return')
   OR (`id` = 900359 AND `permission` = 'mes:pro-edhr-flow-intervention:withdraw')
   OR (`id` = 900360 AND `permission` = 'mes:pro-edhr-flow-intervention:transfer')
   OR (`id` = 900361 AND `permission` = 'mes:pro-edhr-flow-intervention:add-sign')
   OR (`id` = 900362 AND `permission` = 'mes:pro-edhr-flow-intervention:admin-intervene');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_flow_intervention_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_flow_intervention_menus()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR root system_menu row; cannot merge eDHR flow intervention menus';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR flow intervention menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900356, 900357, 900358, 900359, 900360, 900361, 900362)) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR flow intervention system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900356
      AND `name` = 'eDHR流程干预管理'
      AND `path` = '/mes/pro/feedback/edhr-flow-intervention'
      AND `component` = 'mes/pro/edhr-flow-intervention/FlowInterventionPage'
      AND `component_name` = 'MesProEdhrFlowIntervention'
      AND `parent_id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid eDHR flow intervention page menu definition; cannot merge tenant package menu_ids';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` IN (900357, 900358, 900359, 900360, 900361, 900362)
      AND (
        `parent_id` <> 900356
        OR (`id` = 900357 AND (`name` <> 'eDHR流程日志查询' OR `permission` <> 'mes:pro-edhr-flow-intervention:event-query'))
        OR (`id` = 900358 AND (`name` <> 'eDHR流程退回' OR `permission` <> 'mes:pro-edhr-flow-intervention:return'))
        OR (`id` = 900359 AND (`name` <> 'eDHR流程撤回' OR `permission` <> 'mes:pro-edhr-flow-intervention:withdraw'))
        OR (`id` = 900360 AND (`name` <> 'eDHR流程转办' OR `permission` <> 'mes:pro-edhr-flow-intervention:transfer'))
        OR (`id` = 900361 AND (`name` <> 'eDHR流程加签' OR `permission` <> 'mes:pro-edhr-flow-intervention:add-sign'))
        OR (`id` = 900362 AND (`name` <> 'eDHR管理员干预' OR `permission` <> 'mes:pro-edhr-flow-intervention:admin-intervene'))
        OR `deleted` <> b'0'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid eDHR flow intervention button menu definition; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_flow_intervention_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_flow_intervention_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900356, 900357, 900358, 900359, 900360, 900361, 900362)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_package_existing_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_flow_intervention_package_existing_menu_ids` AS
  SELECT `package`.`id` AS `package_id`,
         COALESCE(`legacy_map`.`new_menu_id`, CAST(`menu`.`menu_id` AS UNSIGNED)) AS `menu_id`
  FROM `tmp_mes_edhr_flow_intervention_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(CAST(`package`.`menu_ids` AS JSON), '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')) AS `menu`
  LEFT JOIN `tmp_mes_edhr_flow_intervention_legacy_menu_map` AS `legacy_map`
    ON `legacy_map`.`old_menu_id` = CAST(`menu`.`menu_id` AS UNSIGNED);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_package_merged_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_flow_intervention_package_merged_menu_ids` AS
  SELECT DISTINCT `package_id`, `menu_id`
  FROM `tmp_mes_edhr_flow_intervention_package_existing_menu_ids`
  UNION
  SELECT DISTINCT `target_package`.`package_id`, `menu`.`id`
  FROM `tmp_mes_edhr_flow_intervention_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_flow_intervention_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT `package_id`, JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_flow_intervention_package_merged_menu_ids`
    ) AS `deduplicated`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-flow-intervention',
      `package`.`update_time` = NOW();

  DELETE `role_menu` FROM `system_role_menu` AS `role_menu`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role_menu`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_flow_intervention_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `tmp_mes_edhr_flow_intervention_legacy_menu_map` AS `legacy_map`
    ON `legacy_map`.`old_menu_id` = `role_menu`.`menu_id`
  WHERE `role_menu`.`deleted` = b'0';

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-flow-intervention',
    NOW(),
    'edhr-flow-intervention',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_flow_intervention_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900356, 900357, 900358, 900359, 900360, 900361, 900362)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_package_merged_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_package_existing_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_flow_intervention_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_flow_intervention_menus;
DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_flow_intervention_legacy_menu_map`;
