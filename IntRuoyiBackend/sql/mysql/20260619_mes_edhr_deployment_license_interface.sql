-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_delivery_cockpit; type=schema; riskLevel=medium
-- eDHR commercial deployment, license and interface evidence slice.
-- This slice depends on mes_pro_edhr_delivery_project from T6-01.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deployment_evidence` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '交付项目ID',
  `deployment_code` varchar(64) NOT NULL COMMENT '部署证据编号',
  `deployment_name` varchar(128) NOT NULL COMMENT '部署证据名称',
  `customer_project_name` varchar(128) NOT NULL COMMENT '客户项目名称',
  `target_environment` varchar(128) NOT NULL COMMENT '目标环境',
  `environment_authorized` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否具备环境授权',
  `environment_check_summary` text NOT NULL COMMENT '环境检查摘要',
  `server_summary` text NOT NULL COMMENT '服务器检查',
  `network_summary` text NOT NULL COMMENT '网络端口域名证书检查',
  `object_storage_summary` text NOT NULL COMMENT '对象存储检查',
  `capacity_summary` text NOT NULL COMMENT '容量检查',
  `permission_summary` text NOT NULL COMMENT '账号权限检查',
  `release_tag` varchar(64) NOT NULL COMMENT '发布标签',
  `artifact_version` varchar(64) NOT NULL COMMENT '安装包或制品版本',
  `artifact_checksum` varchar(128) NOT NULL COMMENT '制品校验值',
  `schema_version` varchar(64) NOT NULL COMMENT '数据库结构版本',
  `migration_manifest` text NOT NULL COMMENT '迁移清单',
  `required_sql_manifest` text NOT NULL COMMENT 'required SQL 清单',
  `app_import_result` text NOT NULL COMMENT '应用导入结果',
  `license_scope` text NOT NULL COMMENT '授权范围',
  `license_valid_until` date DEFAULT NULL COMMENT '授权有效期',
  `license_file_evidence` text NOT NULL COMMENT '授权文件证据',
  `license_check_result` text NOT NULL COMMENT '授权校验结果',
  `customer_license_confirmation` text NOT NULL COMMENT '客户授权确认',
  `interface_scope` text NOT NULL COMMENT '接口范围',
  `interface_version` varchar(64) NOT NULL COMMENT '接口版本',
  `integration_environment` varchar(128) NOT NULL COMMENT '联调环境',
  `request_evidence` text NOT NULL COMMENT '真实请求证据',
  `response_evidence` text NOT NULL COMMENT '真实响应证据',
  `interface_failure_count` int NOT NULL DEFAULT 0 COMMENT '接口失败项数量',
  `remediation_action` text NOT NULL COMMENT '失败整改措施',
  `retest_evidence` text NOT NULL COMMENT '复测证据',
  `interface_confirmed_by` varchar(128) NOT NULL COMMENT '接口确认人',
  `deployment_status` varchar(32) NOT NULL COMMENT '部署状态：DELIVERY_DRAFT、ENVIRONMENT_CHECKED、INSTALLED、INTEGRATED、DELIVERY_BLOCKED',
  `blocked_reason` text NOT NULL COMMENT '阻断原因',
  `next_action` text NOT NULL COMMENT '下一步动作',
  `gate_passed` bit(1) NOT NULL DEFAULT b'0' COMMENT '门禁是否通过',
  `gate_checked_at` datetime DEFAULT NULL COMMENT '门禁检查时间',
  `evidence_snapshot_checksum` varchar(128) NOT NULL COMMENT '证据快照校验值',
  `remark` text DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_deployment_code` (`tenant_id`, `deployment_code`, `deleted`),
  KEY `idx_mes_pro_edhr_deployment_project_status` (`tenant_id`, `project_id`, `deployment_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR部署授权接口证据';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_deployment_gate_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `deployment_id` bigint NOT NULL COMMENT '部署证据ID',
  `gate_code` varchar(64) NOT NULL COMMENT '门禁编码',
  `gate_name` varchar(128) NOT NULL COMMENT '门禁名称',
  `gate_status` varchar(32) NOT NULL COMMENT '门禁状态：PASSED、BLOCKED',
  `evidence_source` text NOT NULL COMMENT '证据来源',
  `missing_evidence` text NOT NULL COMMENT '缺失证据',
  `owner_name` varchar(128) NOT NULL COMMENT '责任人',
  `next_action` text NOT NULL COMMENT '下一步动作',
  `signoff_impact` text NOT NULL COMMENT '签核影响',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_deployment_gate` (`tenant_id`, `deployment_id`, `gate_code`, `deleted`),
  KEY `idx_mes_pro_edhr_deployment_gate_status` (`tenant_id`, `deployment_id`, `gate_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR部署交付门禁项';

-- Clean legacy deployment menu residues first so old path/permission rows
-- cannot block insertion of the new 900315-900319 menu set.
DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_legacy_menu_ids`;
CREATE TEMPORARY TABLE `tmp_mes_edhr_deployment_legacy_menu_ids` AS
SELECT `id` AS `menu_id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND (
    `id` IN (900296, 900297, 900298, 900299, 900300)
    OR (
      (`path` = '/mes/pro/feedback/edhr-deployment'
       OR `permission` LIKE 'mes:pro-edhr-deployment:%')
      AND `id` NOT IN (900315, 900316, 900317, 900318, 900319)
    )
  );

DELETE `role_menu`
FROM `system_role_menu` AS `role_menu`
JOIN `tmp_mes_edhr_deployment_legacy_menu_ids` AS `legacy`
  ON `legacy`.`menu_id` = `role_menu`.`menu_id`
WHERE `role_menu`.`deleted` = b'0';

DELETE `menu`
FROM `system_menu` AS `menu`
JOIN `tmp_mes_edhr_deployment_legacy_menu_ids` AS `legacy`
  ON `legacy`.`menu_id` = `menu`.`id`
WHERE `menu`.`deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_legacy_menu_ids`;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900315, 'eDHR部署授权接口', '', 2, 315, 900220, '/mes/pro/feedback/edhr-deployment', 'ep:guide', 'mes/pro/edhr-deployment/DeploymentPage', 'MesProEdhrDeployment', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900315 OR `path` = '/mes/pro/feedback/edhr-deployment');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900316, 'eDHR部署查询', 'mes:pro-edhr-deployment:query', 3, 1, 900315, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900316 OR `permission` = 'mes:pro-edhr-deployment:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900317, 'eDHR部署创建', 'mes:pro-edhr-deployment:create', 3, 2, 900315, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900317 OR `permission` = 'mes:pro-edhr-deployment:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900318, 'eDHR部署补证据', 'mes:pro-edhr-deployment:update', 3, 3, 900315, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900318 OR `permission` = 'mes:pro-edhr-deployment:update');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900319, 'eDHR部署预检', 'mes:pro-edhr-deployment:precheck', 3, 4, 900315, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900319 OR `permission` = 'mes:pro-edhr-deployment:precheck');

UPDATE `system_menu`
SET `name` = 'eDHR部署授权接口',
    `permission` = '',
    `type` = 2,
    `sort` = 296,
    `parent_id` = 900220,
    `path` = '/mes/pro/feedback/edhr-deployment',
    `icon` = 'ep:guide',
    `component` = 'mes/pro/edhr-deployment/DeploymentPage',
    `component_name` = 'MesProEdhrDeployment',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'0',
    `always_show` = b'1',
    `updater` = 'system',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 900315
  AND `path` = '/mes/pro/feedback/edhr-deployment';

UPDATE `system_menu`
SET `name` = CASE `id`
      WHEN 900316 THEN 'eDHR部署查询'
      WHEN 900317 THEN 'eDHR部署创建'
      WHEN 900318 THEN 'eDHR部署补证据'
      WHEN 900319 THEN 'eDHR部署预检'
    END,
    `type` = 3,
    `sort` = CASE `id`
      WHEN 900316 THEN 1
      WHEN 900317 THEN 2
      WHEN 900318 THEN 3
      WHEN 900319 THEN 4
    END,
    `parent_id` = 900315,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'0',
    `always_show` = b'1',
    `updater` = 'system',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE (`id` = 900316 AND `permission` = 'mes:pro-edhr-deployment:query')
   OR (`id` = 900317 AND `permission` = 'mes:pro-edhr-deployment:create')
   OR (`id` = 900318 AND `permission` = 'mes:pro-edhr-deployment:update')
   OR (`id` = 900319 AND `permission` = 'mes:pro-edhr-deployment:precheck');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_deployment_test_tenant_menus;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_deployment_test_tenant_menus()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_deployment_target_tenant` AS
  SELECT `tenant`.`id` AS `tenant_id`, `tenant`.`package_id`
  FROM `system_tenant` AS `tenant`
  WHERE `tenant`.`name` = '测试租户'
    AND `tenant`.`deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_deployment_target_tenant`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing unique 测试租户; cannot merge eDHR deployment menus';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_deployment_target_tenant` AS `target`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target`.`package_id`
     AND `package`.`deleted` = b'0'
    WHERE NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR deployment menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_deployment_menu_ids` AS
  SELECT `id` AS `menu_id`
  FROM `system_menu`
  WHERE `id` IN (900315, 900316, 900317, 900318, 900319)
    AND `deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_deployment_menu_ids`) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR deployment system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900315
      AND `name` = 'eDHR部署授权接口'
      AND `path` = '/mes/pro/feedback/edhr-deployment'
      AND `component` = 'mes/pro/edhr-deployment/DeploymentPage'
      AND `component_name` = 'MesProEdhrDeployment'
      AND `parent_id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid eDHR deployment page menu definition; cannot merge tenant package menu_ids';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` IN (900316, 900317, 900318, 900319)
      AND (
        `parent_id` <> 900315
        OR (`id` = 900316 AND (`name` <> 'eDHR部署查询' OR `permission` <> 'mes:pro-edhr-deployment:query'))
        OR (`id` = 900317 AND (`name` <> 'eDHR部署创建' OR `permission` <> 'mes:pro-edhr-deployment:create'))
        OR (`id` = 900318 AND (`name` <> 'eDHR部署补证据' OR `permission` <> 'mes:pro-edhr-deployment:update'))
        OR (`id` = 900319 AND (`name` <> 'eDHR部署预检' OR `permission` <> 'mes:pro-edhr-deployment:precheck'))
        OR `deleted` <> b'0'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid eDHR deployment button menu definition; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_package_existing_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_deployment_package_existing_menu_ids` AS
  SELECT `package`.`id` AS `package_id`, CAST(`menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_deployment_target_tenant` AS `target`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target`.`package_id`
   AND `package`.`deleted` = b'0'
  JOIN JSON_TABLE(CAST(`package`.`menu_ids` AS JSON), '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')) AS `menu`;

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_deployment_package_existing_menu_ids`
    WHERE `menu_id` = 900220
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing 测试租户 eDHR parent menu 900220; cannot merge deployment menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_package_merged_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_deployment_package_merged_menu_ids` AS
  SELECT DISTINCT `package_id`, `menu_id`
  FROM `tmp_mes_edhr_deployment_package_existing_menu_ids`
  UNION
  SELECT DISTINCT `target`.`package_id`, `menus`.`menu_id`
  FROM `tmp_mes_edhr_deployment_target_tenant` AS `target`
  JOIN `tmp_mes_edhr_deployment_menu_ids` AS `menus`;

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_deployment_target_tenant` AS `target`
    ON `target`.`package_id` = `package`.`id`
  JOIN (
    SELECT `package_id`, JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_deployment_package_merged_menu_ids`
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
  FROM `tmp_mes_edhr_deployment_target_tenant` AS `target`
  JOIN `system_role` AS `role`
    ON `role`.`tenant_id` = `target`.`tenant_id`
   AND `role`.`code` = 'tenant_admin'
   AND `role`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_deployment_menu_ids` AS `menus`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menus`.`menu_id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_package_merged_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_package_existing_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_deployment_target_tenant`;
END//
DELIMITER ;

CALL ensure_mes_edhr_deployment_test_tenant_menus();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_deployment_test_tenant_menus;
