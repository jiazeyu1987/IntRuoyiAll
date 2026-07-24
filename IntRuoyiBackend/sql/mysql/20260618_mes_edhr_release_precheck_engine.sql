-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260615_mes_edhr_tail_four_goals; type=schema; riskLevel=medium
-- eDHR commercial release precheck first slice: release transaction, check items, menu baseline.
-- This migration is idempotent and fail-fast for invalid tenant package menu JSON.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_release_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `release_code` varchar(64) NOT NULL COMMENT '放行事务编号',
  `batch_execution_id` bigint NOT NULL COMMENT 'eDHR批次执行ID',
  `batch_execution_code` varchar(64) NOT NULL COMMENT 'eDHR批次执行编号',
  `work_order_id` bigint DEFAULT NULL COMMENT '工单ID',
  `work_order_code` varchar(64) DEFAULT NULL COMMENT '工单编号',
  `batch_code` varchar(128) NOT NULL COMMENT '批次号',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `product_code` varchar(64) DEFAULT NULL COMMENT '产品编码',
  `product_name` varchar(255) DEFAULT NULL COMMENT '产品名称',
  `route_id` bigint DEFAULT NULL COMMENT '工艺路线ID',
  `route_code` varchar(64) DEFAULT NULL COMMENT '工艺路线编码',
  `route_name` varchar(255) DEFAULT NULL COMMENT '工艺路线名称',
  `dhr_status` varchar(32) NOT NULL COMMENT 'DHR完整性状态',
  `inspection_status` varchar(32) NOT NULL COMMENT '检验状态',
  `deviation_status` varchar(32) NOT NULL COMMENT '偏差状态',
  `rework_status` varchar(32) NOT NULL COMMENT '返工状态',
  `scrap_status` varchar(32) NOT NULL COMMENT '报废状态',
  `inventory_status` varchar(32) NOT NULL COMMENT '库存状态',
  `release_status` varchar(32) NOT NULL COMMENT '放行状态',
  `required_check_count` int NOT NULL DEFAULT 0 COMMENT '必检项数量',
  `failed_check_count` int NOT NULL DEFAULT 0 COMMENT '失败项数量',
  `blocking_check_count` int NOT NULL DEFAULT 0 COMMENT '阻塞项数量',
  `last_precheck_at` datetime DEFAULT NULL COMMENT '最后预检时间',
  `precheck_snapshot_json` longtext DEFAULT NULL COMMENT '预检快照JSON',
  `submit_idempotency_key` varchar(128) DEFAULT NULL COMMENT '提交幂等键',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `approval_idempotency_key` varchar(128) DEFAULT NULL COMMENT '批准幂等键',
  `approved_by` bigint DEFAULT NULL COMMENT '批准人',
  `approved_at` datetime DEFAULT NULL COMMENT '批准时间',
  `approval_signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '批准签核证据摘要',
  `approval_opinion` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `rejected_by` bigint DEFAULT NULL COMMENT '驳回人',
  `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `withdrawn_by` bigint DEFAULT NULL COMMENT '撤回人',
  `withdrawn_at` datetime DEFAULT NULL COMMENT '撤回时间',
  `withdraw_reason` varchar(500) DEFAULT NULL COMMENT '撤回原因',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_release_transaction_batch` (`tenant_id`, `batch_execution_id`, `deleted`),
  KEY `idx_mes_pro_edhr_release_transaction_status` (`tenant_id`, `release_status`, `deleted`),
  KEY `idx_mes_pro_edhr_release_transaction_batch_code` (`tenant_id`, `batch_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 放行事务';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_release_check_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `release_transaction_id` bigint NOT NULL COMMENT '放行事务ID',
  `check_code` varchar(64) NOT NULL COMMENT '检查编码',
  `check_category` varchar(64) NOT NULL COMMENT '检查分类',
  `check_name` varchar(128) NOT NULL COMMENT '检查名称',
  `check_result` varchar(32) NOT NULL COMMENT '检查结果：PASS/FAIL/BLOCKER',
  `item_status` varchar(32) NOT NULL COMMENT '检查项状态：OPEN/SUPERSEDED/RESOLVED',
  `severity` varchar(32) NOT NULL COMMENT '严重级别',
  `responsibility_module` varchar(64) NOT NULL COMMENT '责任模块',
  `source_object_type` varchar(64) DEFAULT NULL COMMENT '源对象类型',
  `source_object_id` varchar(128) DEFAULT NULL COMMENT '源对象ID',
  `source_object_code` varchar(128) DEFAULT NULL COMMENT '源对象编码',
  `source_record_url` varchar(500) DEFAULT NULL COMMENT '源记录地址',
  `failure_reason` varchar(500) NOT NULL COMMENT '失败原因',
  `remediation_suggestion` varchar(500) NOT NULL COMMENT '修复建议',
  `impact_scope_json` longtext DEFAULT NULL COMMENT '影响范围JSON',
  `evidence_hash` char(64) DEFAULT NULL COMMENT '证据摘要',
  `checked_at` datetime NOT NULL COMMENT '检查时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_release_check_item_transaction` (`tenant_id`, `release_transaction_id`, `item_status`, `check_result`),
  KEY `idx_mes_pro_edhr_release_check_item_code` (`tenant_id`, `check_code`, `item_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 放行前检查项';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900260, '放行追溯', 'mes:pro-edhr-release:query', 2, 66, 900220,
       '/mes/pro/feedback/edhr-release', 'ep:finished',
       'mes/pro/edhr-release/ReleasePage', 'MesProEdhrReleasePage', 0, b'1', b'1', b'1',
       'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 OR `permission` = 'mes:pro-edhr-release:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900261, 'eDHR放行查询', 'mes:pro-edhr-release:query', 3, 1, 900260,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900261);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900262, 'eDHR放行预检', 'mes:pro-edhr-release:precheck', 3, 2, 900260,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900262 OR `permission` = 'mes:pro-edhr-release:precheck');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900263, 'eDHR放行提交', 'mes:pro-edhr-release:submit', 3, 3, 900260,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900263 OR `permission` = 'mes:pro-edhr-release:submit');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900264, 'eDHR放行审批', 'mes:pro-edhr-release:approve', 3, 4, 900260,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900264 OR `permission` = 'mes:pro-edhr-release:approve');

UPDATE `system_menu`
SET `name` = 'eDHR放行提交',
    `permission` = 'mes:pro-edhr-release:submit',
    `type` = 3,
    `sort` = 3,
    `parent_id` = 900260,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'edhr-release',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 900263
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = 'eDHR放行审批',
    `permission` = 'mes:pro-edhr-release:approve',
    `type` = 3,
    `sort` = 4,
    `parent_id` = 900260,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'edhr-release',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 900264
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900265, 'eDHR放行干预', 'mes:pro-edhr-release:intervene', 3, 5, 900260,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-release', NOW(), 'edhr-release', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900260 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900265 OR `permission` = 'mes:pro-edhr-release:intervene');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_release_menus()
BEGIN
  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR release menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900260, 900261, 900262, 900263, 900264, 900265)) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR release system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_release_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_release_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900260, 900261, 900262, 900263, 900264, 900265)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_release_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_mes_edhr_release_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_release_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_missing_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_release_missing_package_menu_ids` AS
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_release_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_release_menu_ids` AS `menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_release_package_menu_ids` AS `existing`
    WHERE `existing`.`package_id` = `target_package`.`package_id`
      AND `existing`.`menu_id` = `menu`.`id`
  );

  INSERT INTO `tmp_mes_edhr_release_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, `id`
  FROM `tmp_mes_edhr_release_missing_package_menu_ids`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `ordered`.`package_id`,
      JSON_ARRAYAGG(`ordered`.`menu_id`) AS `menu_ids`
    FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_release_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `ordered`
    GROUP BY `ordered`.`package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-release',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-release',
    NOW(),
    'edhr-release',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_release_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900260, 900261, 900262, 900263, 900264, 900265)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_missing_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_release_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_release_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_menus;
