-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_validation_package_matrix; type=schema; riskLevel=medium
-- eDHR commercial OQ/PQ execution and deviation retest third slice.
-- This slice depends on mes_pro_edhr_validation_package from T6-02.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_validation_case` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_id` bigint NOT NULL COMMENT '验证包ID',
  `case_code` varchar(64) NOT NULL COMMENT '用例编号',
  `case_name` varchar(128) NOT NULL COMMENT '用例名称',
  `case_type` varchar(16) NOT NULL COMMENT '用例类型：OQ、PQ',
  `case_version` varchar(64) NOT NULL COMMENT '用例版本',
  `case_status` varchar(32) NOT NULL COMMENT '用例状态：ACTIVE、INACTIVE',
  `step_no` varchar(32) NOT NULL COMMENT '步骤编号',
  `step_title` varchar(128) NOT NULL COMMENT '步骤标题',
  `expected_result` varchar(1000) NOT NULL COMMENT '预期结果',
  `evidence_requirement` varchar(500) NOT NULL COMMENT '证据要求',
  `owner_name` varchar(128) NOT NULL COMMENT '责任人',
  `reviewer_name` varchar(128) NOT NULL COMMENT '复核人',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_validation_case_code` (`tenant_id`, `package_id`, `case_type`, `case_code`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_case_package_type` (`tenant_id`, `package_id`, `case_type`, `case_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR OQ/PQ验证用例';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_validation_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_id` bigint NOT NULL COMMENT '验证包ID',
  `case_id` bigint NOT NULL COMMENT '验证用例ID',
  `case_type` varchar(16) NOT NULL COMMENT '用例类型：OQ、PQ',
  `run_code` varchar(64) NOT NULL COMMENT '执行编号',
  `run_status` varchar(32) NOT NULL COMMENT '执行状态：CREATED、RUNNING、DEVIATION_OPEN、PASSED、BLOCKED',
  `execution_environment` varchar(128) NOT NULL COMMENT '执行环境',
  `release_tag` varchar(64) NOT NULL COMMENT '发布标签',
  `schema_version` varchar(64) NOT NULL COMMENT '数据库结构版本',
  `executor_name` varchar(128) NOT NULL COMMENT '执行人',
  `reviewer_name` varchar(128) NOT NULL COMMENT '复核人',
  `executed_at` datetime NOT NULL COMMENT '执行时间',
  `real_business_path` varchar(500) DEFAULT NULL COMMENT 'PQ真实业务路径',
  `real_test_data_source` varchar(500) DEFAULT NULL COMMENT 'PQ真实测试数据来源',
  `target_environment_proof` varchar(500) DEFAULT NULL COMMENT '目标环境证明',
  `attachment_evidence` varchar(500) NOT NULL COMMENT '附件或证据标识',
  `evidence_checksum` varchar(128) NOT NULL COMMENT '证据校验值',
  `open_deviation_count` int NOT NULL DEFAULT 0 COMMENT '开放偏差数',
  `conclusion` varchar(500) DEFAULT NULL COMMENT '执行结论',
  `blocked_reason` varchar(500) NOT NULL COMMENT '阻断原因',
  `next_action` varchar(500) NOT NULL COMMENT '下一步动作',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_validation_run_code` (`tenant_id`, `run_code`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_run_package_status` (`tenant_id`, `package_id`, `run_status`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_run_case` (`tenant_id`, `case_id`, `case_type`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR OQ/PQ执行记录';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_validation_step_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_id` bigint NOT NULL COMMENT '验证包ID',
  `case_id` bigint NOT NULL COMMENT '验证用例ID',
  `run_id` bigint NOT NULL COMMENT '执行记录ID',
  `step_no` varchar(32) NOT NULL COMMENT '步骤编号',
  `step_title` varchar(128) NOT NULL COMMENT '步骤标题',
  `expected_result` varchar(1000) NOT NULL COMMENT '预期结果',
  `actual_result` varchar(1000) NOT NULL COMMENT '实际结果',
  `step_result` varchar(32) NOT NULL COMMENT '步骤结果：PASS、FAIL、BLOCKED',
  `executor_name` varchar(128) NOT NULL COMMENT '执行人',
  `reviewer_name` varchar(128) NOT NULL COMMENT '复核人',
  `executed_at` datetime NOT NULL COMMENT '执行时间',
  `attachment_evidence` varchar(500) NOT NULL COMMENT '附件或证据标识',
  `evidence_checksum` varchar(128) NOT NULL COMMENT '证据校验值',
  `deviation_id` bigint DEFAULT NULL COMMENT '偏差ID',
  `next_action` varchar(500) NOT NULL COMMENT '下一步动作',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_validation_step_run` (`tenant_id`, `run_id`, `step_result`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_step_deviation` (`tenant_id`, `deviation_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR OQ/PQ步骤执行结果';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_validation_deviation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_id` bigint NOT NULL COMMENT '验证包ID',
  `case_id` bigint NOT NULL COMMENT '验证用例ID',
  `run_id` bigint NOT NULL COMMENT '执行记录ID',
  `step_result_id` bigint NOT NULL COMMENT '步骤结果ID',
  `deviation_code` varchar(64) NOT NULL COMMENT '偏差编号',
  `deviation_title` varchar(128) NOT NULL COMMENT '偏差标题',
  `deviation_status` varchar(32) NOT NULL COMMENT '偏差状态：OPEN、REMEDIATED、RETESTED、CLOSED',
  `failed_actual_result` varchar(1000) NOT NULL COMMENT '失败实际结果',
  `root_cause` varchar(1000) DEFAULT NULL COMMENT '原因分析',
  `remediation_action` varchar(1000) DEFAULT NULL COMMENT '整改措施',
  `remediation_owner_name` varchar(128) DEFAULT NULL COMMENT '整改责任人',
  `retest_result` varchar(1000) DEFAULT NULL COMMENT '复测结果',
  `retest_evidence` varchar(500) DEFAULT NULL COMMENT '复测证据',
  `retest_reviewer_name` varchar(128) DEFAULT NULL COMMENT '复测复核人',
  `close_signoff_name` varchar(128) DEFAULT NULL COMMENT '关闭签核人',
  `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
  `blocked_reason` varchar(500) NOT NULL COMMENT '阻断原因',
  `next_action` varchar(500) NOT NULL COMMENT '下一步动作',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_validation_deviation_code` (`tenant_id`, `deviation_code`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_deviation_run_status` (`tenant_id`, `run_id`, `deviation_status`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_deviation_package` (`tenant_id`, `package_id`, `deviation_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR OQ/PQ偏差整改复测';

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_legacy_menu_map`;
CREATE TEMPORARY TABLE `tmp_mes_edhr_oq_pq_legacy_menu_map` (
  `old_menu_id` bigint NOT NULL,
  `new_menu_id` bigint NOT NULL,
  PRIMARY KEY (`old_menu_id`)
) ENGINE=Memory;

INSERT INTO `tmp_mes_edhr_oq_pq_legacy_menu_map` (`old_menu_id`, `new_menu_id`)
SELECT `menu`.`id`,
       CASE
         WHEN `menu`.`path` = '/mes/pro/feedback/edhr-oq-pq' THEN 900332
         WHEN `menu`.`permission` = 'mes:pro-edhr-oq-pq:query' THEN 900333
         WHEN `menu`.`permission` = 'mes:pro-edhr-oq-pq:create' THEN 900334
         WHEN `menu`.`permission` = 'mes:pro-edhr-oq-pq:execute' THEN 900335
         WHEN `menu`.`permission` = 'mes:pro-edhr-oq-pq:retest' THEN 900336
         WHEN `menu`.`permission` = 'mes:pro-edhr-oq-pq:close' THEN 900337
       END AS `new_menu_id`
FROM `system_menu` AS `menu`
WHERE `menu`.`deleted` = b'0'
  AND (
    (`menu`.`id` = 900290 AND `menu`.`path` = '/mes/pro/feedback/edhr-oq-pq')
    OR (`menu`.`id` = 900291 AND `menu`.`permission` = 'mes:pro-edhr-oq-pq:query')
    OR (`menu`.`id` = 900292 AND `menu`.`permission` = 'mes:pro-edhr-oq-pq:create')
    OR (`menu`.`id` = 900293 AND `menu`.`permission` = 'mes:pro-edhr-oq-pq:execute')
    OR (`menu`.`id` = 900294 AND `menu`.`permission` = 'mes:pro-edhr-oq-pq:retest')
    OR (`menu`.`id` = 900295 AND `menu`.`permission` = 'mes:pro-edhr-oq-pq:close')
  );

DELETE `legacy_menu` FROM `system_menu` AS `legacy_menu`
JOIN `tmp_mes_edhr_oq_pq_legacy_menu_map` AS `legacy_map`
  ON `legacy_map`.`old_menu_id` = `legacy_menu`.`id`;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900332, 'eDHR OQ/PQ执行台', '', 2, 290, 900220, '/mes/pro/feedback/edhr-oq-pq', 'ep:operation', 'mes/pro/edhr-oq-pq/OqPqPage', 'MesProEdhrOqPq', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900332 OR `path` = '/mes/pro/feedback/edhr-oq-pq');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900333, 'eDHR OQ/PQ查询', 'mes:pro-edhr-oq-pq:query', 3, 1, 900332, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900333 OR `permission` = 'mes:pro-edhr-oq-pq:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900334, 'eDHR OQ/PQ创建', 'mes:pro-edhr-oq-pq:create', 3, 2, 900332, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900334 OR `permission` = 'mes:pro-edhr-oq-pq:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900335, 'eDHR OQ/PQ执行', 'mes:pro-edhr-oq-pq:execute', 3, 3, 900332, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900335 OR `permission` = 'mes:pro-edhr-oq-pq:execute');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900336, 'eDHR 偏差复测', 'mes:pro-edhr-oq-pq:retest', 3, 4, 900332, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900336 OR `permission` = 'mes:pro-edhr-oq-pq:retest');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900337, 'eDHR 偏差关闭', 'mes:pro-edhr-oq-pq:close', 3, 5, 900332, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900337 OR `permission` = 'mes:pro-edhr-oq-pq:close');

UPDATE `system_menu`
SET `name` = 'eDHR OQ/PQ执行台',
    `permission` = '',
    `type` = 2,
    `sort` = 290,
    `parent_id` = 900220,
    `path` = '/mes/pro/feedback/edhr-oq-pq',
    `icon` = 'ep:operation',
    `component` = 'mes/pro/edhr-oq-pq/OqPqPage',
    `component_name` = 'MesProEdhrOqPq',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'0',
    `always_show` = b'1',
    `updater` = 'system',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 900332
  AND `path` = '/mes/pro/feedback/edhr-oq-pq';

UPDATE `system_menu`
SET `name` = CASE `id`
      WHEN 900333 THEN 'eDHR OQ/PQ查询'
      WHEN 900334 THEN 'eDHR OQ/PQ创建'
      WHEN 900335 THEN 'eDHR OQ/PQ执行'
      WHEN 900336 THEN 'eDHR 偏差复测'
      WHEN 900337 THEN 'eDHR 偏差关闭'
    END,
    `type` = 3,
    `sort` = CASE `id`
      WHEN 900333 THEN 1
      WHEN 900334 THEN 2
      WHEN 900335 THEN 3
      WHEN 900336 THEN 4
      WHEN 900337 THEN 5
    END,
    `parent_id` = 900332,
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
WHERE (`id` = 900333 AND `permission` = 'mes:pro-edhr-oq-pq:query')
   OR (`id` = 900334 AND `permission` = 'mes:pro-edhr-oq-pq:create')
   OR (`id` = 900335 AND `permission` = 'mes:pro-edhr-oq-pq:execute')
   OR (`id` = 900336 AND `permission` = 'mes:pro-edhr-oq-pq:retest')
   OR (`id` = 900337 AND `permission` = 'mes:pro-edhr-oq-pq:close');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_oq_pq_test_tenant_menus;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_oq_pq_test_tenant_menus()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_oq_pq_target_tenant` AS
  SELECT `tenant`.`id` AS `tenant_id`, `tenant`.`package_id`
  FROM `system_tenant` AS `tenant`
  WHERE `tenant`.`name` = '测试租户'
    AND `tenant`.`deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_oq_pq_target_tenant`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing unique 测试租户; cannot merge eDHR OQ/PQ menus';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_oq_pq_target_tenant` AS `target`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target`.`package_id`
     AND `package`.`deleted` = b'0'
    WHERE NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR OQ/PQ menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_oq_pq_menu_ids` AS
  SELECT `id` AS `menu_id`
  FROM `system_menu`
  WHERE `id` IN (900332, 900333, 900334, 900335, 900336, 900337)
    AND `deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_oq_pq_menu_ids`) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR OQ/PQ system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900332
      AND `name` = 'eDHR OQ/PQ执行台'
      AND `path` = '/mes/pro/feedback/edhr-oq-pq'
      AND `component` = 'mes/pro/edhr-oq-pq/OqPqPage'
      AND `component_name` = 'MesProEdhrOqPq'
      AND `parent_id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid eDHR OQ/PQ page menu definition; cannot merge tenant package menu_ids';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` IN (900333, 900334, 900335, 900336, 900337)
      AND (
        `parent_id` <> 900332
        OR (`id` = 900333 AND (`name` <> 'eDHR OQ/PQ查询' OR `permission` <> 'mes:pro-edhr-oq-pq:query'))
        OR (`id` = 900334 AND (`name` <> 'eDHR OQ/PQ创建' OR `permission` <> 'mes:pro-edhr-oq-pq:create'))
        OR (`id` = 900335 AND (`name` <> 'eDHR OQ/PQ执行' OR `permission` <> 'mes:pro-edhr-oq-pq:execute'))
        OR (`id` = 900336 AND (`name` <> 'eDHR 偏差复测' OR `permission` <> 'mes:pro-edhr-oq-pq:retest'))
        OR (`id` = 900337 AND (`name` <> 'eDHR 偏差关闭' OR `permission` <> 'mes:pro-edhr-oq-pq:close'))
        OR `deleted` <> b'0'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid eDHR OQ/PQ button menu definition; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_package_existing_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_oq_pq_package_existing_menu_ids` AS
  SELECT `package`.`id` AS `package_id`,
         COALESCE(`legacy_map`.`new_menu_id`, CAST(`menu`.`menu_id` AS UNSIGNED)) AS `menu_id`
  FROM `tmp_mes_edhr_oq_pq_target_tenant` AS `target`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target`.`package_id`
   AND `package`.`deleted` = b'0'
  JOIN JSON_TABLE(CAST(`package`.`menu_ids` AS JSON), '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')) AS `menu`
  LEFT JOIN `tmp_mes_edhr_oq_pq_legacy_menu_map` AS `legacy_map`
    ON `legacy_map`.`old_menu_id` = CAST(`menu`.`menu_id` AS UNSIGNED);

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_oq_pq_package_existing_menu_ids`
    WHERE `menu_id` = 900220
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing 测试租户 eDHR parent menu 900220; cannot merge OQ/PQ menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_package_merged_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_oq_pq_package_merged_menu_ids` AS
  SELECT DISTINCT `package_id`, `menu_id`
  FROM `tmp_mes_edhr_oq_pq_package_existing_menu_ids`
  UNION
  SELECT DISTINCT `target`.`package_id`, `menus`.`menu_id`
  FROM `tmp_mes_edhr_oq_pq_target_tenant` AS `target`
  JOIN `tmp_mes_edhr_oq_pq_menu_ids` AS `menus`;

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_oq_pq_target_tenant` AS `target`
    ON `target`.`package_id` = `package`.`id`
  JOIN (
    SELECT `package_id`, JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_oq_pq_package_merged_menu_ids`
    ) AS `deduplicated`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'system',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  DELETE `role_menu` FROM `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_edhr_oq_pq_target_tenant` AS `target`
    ON `target`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `tmp_mes_edhr_oq_pq_legacy_menu_map` AS `legacy_map`
    ON `legacy_map`.`old_menu_id` = `role_menu`.`menu_id`
  WHERE `role_menu`.`deleted` = b'0';

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `role`.`id`, `menus`.`menu_id`, 'system', NOW(), 'system', NOW(), b'0', `role`.`tenant_id`
  FROM `tmp_mes_edhr_oq_pq_target_tenant` AS `target`
  JOIN `system_role` AS `role`
    ON `role`.`tenant_id` = `target`.`tenant_id`
   AND `role`.`code` = 'tenant_admin'
   AND `role`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_oq_pq_menu_ids` AS `menus`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menus`.`menu_id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_package_merged_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_package_existing_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_target_tenant`;
END//
DELIMITER ;

CALL ensure_mes_edhr_oq_pq_test_tenant_menus();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_oq_pq_test_tenant_menus;
DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_oq_pq_legacy_menu_map`;
