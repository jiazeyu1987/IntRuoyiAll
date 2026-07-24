-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- SRM D7-1 code-rule baseline: schema, menu, permissions, tenant package and role menu binding.
-- Fail fast for conflicting menu ids or invalid tenant package JSON; no fallback default success.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_code_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) DEFAULT NULL COMMENT '规则名称',
  `target_form` varchar(64) NOT NULL COMMENT '目标表单',
  `prefix` varchar(32) NOT NULL COMMENT '编码前缀',
  `date_pattern` varchar(32) DEFAULT NULL COMMENT '日期格式',
  `date_segment_enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用日期段',
  `serial_width` int NOT NULL COMMENT '流水宽度',
  `step` int NOT NULL COMMENT '流水步长',
  `min_serial` bigint NOT NULL COMMENT '最小流水',
  `max_serial` bigint NOT NULL COMMENT '最大流水',
  `separator` varchar(8) DEFAULT NULL COMMENT '分隔符',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_code_rule_tenant_target_form` (`tenant_id`, `target_form`, `deleted`),
  UNIQUE KEY `uk_srm_code_rule_tenant_rule_code` (`tenant_id`, `rule_code`, `deleted`),
  KEY `idx_srm_code_rule_tenant_enabled` (`tenant_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 编码规则';

CREATE TABLE IF NOT EXISTS `srm_code_rule_counter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计数器编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `rule_id` bigint NOT NULL COMMENT '规则编号',
  `target_form` varchar(64) NOT NULL COMMENT '目标表单',
  `period_key` varchar(32) NOT NULL COMMENT '周期键',
  `current_serial` bigint NOT NULL COMMENT '当前流水',
  `last_code` varchar(128) DEFAULT NULL COMMENT '最近生成编号',
  `last_generated_at` datetime DEFAULT NULL COMMENT '最近生成时间',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观版本',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_code_rule_counter_tenant_rule_period` (`tenant_id`, `rule_id`, `period_key`, `deleted`),
  UNIQUE KEY `uk_srm_code_rule_counter_tenant_last_code` (`tenant_id`, `last_code`, `deleted`),
  KEY `idx_srm_code_rule_counter_tenant_target_period` (`tenant_id`, `target_form`, `period_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 编码规则计数器';

DROP PROCEDURE IF EXISTS ensure_srm_d7_1_code_rule_baseline;

DELIMITER $$
CREATE PROCEDURE ensure_srm_d7_1_code_rule_baseline()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge SRM get-permission-info menus';
  END IF;

  UPDATE `system_menu`
  SET `name` = 'SRM',
      `updater` = 'srm-d7-1',
      `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND `id` = 991000
    AND `path` = '/srm'
    AND `name` = '供应商关系管理';

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (
        (`id` = 991000 AND (`path` <> '/srm' OR `name` <> 'SRM'))
        OR (`id` = 991001 AND (`path` <> 'base' OR `name` <> '基础配置'))
        OR (`id` = 991002 AND (`path` <> 'code-rule' OR `component` <> 'srm/code-rule/index' OR `component_name` <> 'SrmCodeRule'))
        OR (`id` = 991003 AND `permission` <> 'srm:code-rule:query')
        OR (`id` = 991004 AND `permission` <> 'srm:code-rule:create')
        OR (`id` = 991005 AND `permission` <> 'srm:code-rule:update')
        OR (`id` = 991006 AND `permission` <> 'srm:code-rule:enable')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM clean menu id range; conflicting system_menu rows exist';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991000, 'SRM', '', 1, 70, 0, '/srm', 'ep:connection', '', '', 0, b'1', b'1', b'1', 'srm-d7-1', NOW(), 'srm-d7-1', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991000 OR `path` = '/srm')
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991001, '基础配置', '', 1, 1, 991000, 'base', 'ep:setting', '', '', 0, b'1', b'1', b'1', 'srm-d7-1', NOW(), 'srm-d7-1', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991001 OR (`parent_id` = 991000 AND `path` = 'base'))
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991002, '编码规则', 'srm:code-rule:query', 2, 1, 991001, 'code-rule', 'ep:tickets', 'srm/code-rule/index', 'SrmCodeRule', 0, b'1', b'1', b'1', 'srm-d7-1', NOW(), 'srm-d7-1', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991002 OR `permission` = 'srm:code-rule:query' OR `component` = 'srm/code-rule/index')
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991003, '编码规则查询', 'srm:code-rule:query', 3, 1, 991002, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-1', NOW(), 'srm-d7-1', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'srm:code-rule:query'
      AND `type` = 3
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991004, '编码规则新增', 'srm:code-rule:create', 3, 2, 991002, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-1', NOW(), 'srm-d7-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991004 OR `permission` = 'srm:code-rule:create'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991005, '编码规则编辑', 'srm:code-rule:update', 3, 3, 991002, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-1', NOW(), 'srm-d7-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991005 OR `permission` = 'srm:code-rule:update'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991006, '编码规则启停', 'srm:code-rule:enable', 3, 4, 991002, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-1', NOW(), 'srm-d7-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991006 OR `permission` = 'srm:code-rule:enable'));

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM root menu path';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `path` = 'code-rule'
      AND `component` = 'srm/code-rule/index'
      AND `component_name` = 'SrmCodeRule'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM code-rule route menu for get-permission-info';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (991000, 991001, 991002, 991003, 991004, 991005, 991006)) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM D7-1 system_menu rows; get-permission-info cannot expose SRM code-rule route';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_1_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d7_1_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991000, 991001, 991002, 991003, 991004, 991005, 991006);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_1_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d7_1_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_d7_1_package_menu_ids` (`package_id`, `menu_id`)
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
  CROSS JOIN `tmp_srm_d7_1_menu_ids` AS `menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('1' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_srm_d7_1_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-d7-1',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-d7-1',
    NOW(),
    'srm-d7-1',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_d7_1_menu_ids` AS `menu`
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_1_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_1_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_d7_1_code_rule_baseline();

DROP PROCEDURE IF EXISTS ensure_srm_d7_1_code_rule_baseline;
