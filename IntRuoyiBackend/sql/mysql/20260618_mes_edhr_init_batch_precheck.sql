-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260615_mes_edhr_tail_four_goals; type=schema; riskLevel=medium
-- eDHR commercial initialization batch first slice: batch, manifest, precheck issue, menu baseline.
-- This migration is idempotent and fail-fast for invalid tenant package menu JSON.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_init_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_code` varchar(64) NOT NULL COMMENT '项目编码',
  `project_name` varchar(255) NOT NULL COMMENT '项目名称',
  `target_environment` varchar(32) NOT NULL COMMENT '目标环境：LOCAL/TEST/PROD',
  `target_tenant_id` bigint NOT NULL COMMENT '目标租户ID',
  `data_version` varchar(64) NOT NULL COMMENT '数据版本',
  `owner_user_id` bigint NOT NULL COMMENT '交付负责人用户ID',
  `approval_owner_user_id` bigint NOT NULL COMMENT '审批负责人用户ID',
  `planned_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
  `planned_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
  `init_scope_json` longtext NOT NULL COMMENT '初始化范围JSON',
  `status` varchar(32) NOT NULL COMMENT '状态：DRAFT/PRECHECK_FAILED/PRECHECK_PASSED',
  `manifest_count` int NOT NULL DEFAULT 0 COMMENT 'manifest 数量',
  `blocking_issue_count` int NOT NULL DEFAULT 0 COMMENT '阻塞问题数',
  `last_precheck_at` datetime DEFAULT NULL COMMENT '最后预检时间',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_init_batch_project` (`tenant_id`, `project_code`, `data_version`, `deleted`),
  KEY `idx_mes_pro_edhr_init_batch_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 初始化批次';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_init_manifest` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `init_batch_id` bigint NOT NULL COMMENT '初始化批次ID',
  `package_type` varchar(64) NOT NULL COMMENT '包类型',
  `manifest_hash` char(64) NOT NULL COMMENT 'manifest 摘要',
  `source_file_name` varchar(255) NOT NULL COMMENT '源文件名',
  `source_file_url` varchar(512) DEFAULT NULL COMMENT '源文件URL',
  `file_size` bigint DEFAULT NULL COMMENT '源文件大小',
  `checksum_json` longtext DEFAULT NULL COMMENT '校验和JSON',
  `manifest_json` longtext NOT NULL COMMENT 'manifest JSON',
  `upload_status` varchar(32) NOT NULL COMMENT '上传状态',
  `uploaded_by` bigint DEFAULT NULL COMMENT '上传人',
  `uploaded_at` datetime NOT NULL COMMENT '上传时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_init_manifest_hash` (`tenant_id`, `init_batch_id`, `manifest_hash`, `deleted`),
  KEY `idx_mes_pro_edhr_init_manifest_batch` (`tenant_id`, `init_batch_id`, `package_type`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 初始化 manifest';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_init_issue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `init_batch_id` bigint NOT NULL COMMENT '初始化批次ID',
  `init_manifest_id` bigint DEFAULT NULL COMMENT '初始化 manifest ID',
  `issue_code` varchar(64) NOT NULL COMMENT '问题编码',
  `issue_level` varchar(32) NOT NULL COMMENT '问题级别：BLOCKER/WARNING',
  `issue_status` varchar(32) NOT NULL COMMENT '问题状态：OPEN/SUPERSEDED/RESOLVED',
  `package_type` varchar(64) DEFAULT NULL COMMENT '包类型',
  `source_file_name` varchar(255) DEFAULT NULL COMMENT '源文件名',
  `source_row_no` int DEFAULT NULL COMMENT '源行号',
  `source_field_name` varchar(128) DEFAULT NULL COMMENT '源字段名',
  `object_type` varchar(64) DEFAULT NULL COMMENT '对象类型',
  `object_key` varchar(128) DEFAULT NULL COMMENT '对象键',
  `responsible_user_id` bigint DEFAULT NULL COMMENT '责任人用户ID',
  `responsible_name` varchar(128) DEFAULT NULL COMMENT '责任人姓名',
  `issue_message` varchar(500) NOT NULL COMMENT '问题说明',
  `remediation_suggestion` varchar(500) DEFAULT NULL COMMENT '下一步动作',
  `impact_scope_json` longtext DEFAULT NULL COMMENT '影响范围JSON',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_init_issue_batch` (`tenant_id`, `init_batch_id`, `issue_level`, `issue_status`),
  KEY `idx_mes_pro_edhr_init_issue_manifest` (`tenant_id`, `init_manifest_id`, `issue_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES eDHR 初始化预检问题';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900247, 'eDHR初始化批次', 'mes:pro-edhr-init-batch:query', 2, 62, 900220,
       '/mes/pro/feedback/edhr-init-batch', 'ep:set-up',
       'mes/pro/edhr-init-batch/InitBatchPage', 'MesProEdhrInitBatchPage', 0, b'1', b'1', b'1',
       'edhr-init-batch', NOW(), 'edhr-init-batch', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900247 OR `permission` = 'mes:pro-edhr-init-batch:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900248, 'eDHR初始化批次查询', 'mes:pro-edhr-init-batch:query', 3, 1, 900247,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-init-batch', NOW(), 'edhr-init-batch', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900247 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900248);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900249, 'eDHR初始化批次创建', 'mes:pro-edhr-init-batch:create', 3, 2, 900247,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-init-batch', NOW(), 'edhr-init-batch', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900247 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900249 OR `permission` = 'mes:pro-edhr-init-batch:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900250, 'eDHR初始化批次预检', 'mes:pro-edhr-init-batch:precheck', 3, 3, 900247,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-init-batch', NOW(), 'edhr-init-batch', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900247 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900250 OR `permission` = 'mes:pro-edhr-init-batch:precheck');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900251, 'eDHR初始化批次导入', 'mes:pro-edhr-init-batch:import', 3, 4, 900247,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-init-batch', NOW(), 'edhr-init-batch', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900247 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900251 OR `permission` = 'mes:pro-edhr-init-batch:import');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900252, 'eDHR初始化批次放行确认', 'mes:pro-edhr-init-batch:signoff', 3, 5, 900247,
       '', '', '', '', 0, b'1', b'1', b'1', 'edhr-init-batch', NOW(), 'edhr-init-batch', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900247 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900252 OR `permission` = 'mes:pro-edhr-init-batch:signoff');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_init_batch_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_init_batch_menus()
BEGIN
  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR init batch menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900247, 900248, 900249, 900250, 900251, 900252)) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR init batch system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_init_batch_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_init_batch_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_init_batch_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_init_batch_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900247, 900248, 900249, 900250, 900251, 900252)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_init_batch_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_init_batch_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_mes_edhr_init_batch_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_init_batch_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_init_batch_missing_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_init_batch_missing_package_menu_ids` AS
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_init_batch_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_init_batch_menu_ids` AS `menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_init_batch_package_menu_ids` AS `existing`
    WHERE `existing`.`package_id` = `target_package`.`package_id`
      AND `existing`.`menu_id` = `menu`.`id`
  );

  INSERT INTO `tmp_mes_edhr_init_batch_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, `id`
  FROM `tmp_mes_edhr_init_batch_missing_package_menu_ids`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `ordered`.`package_id`,
      JSON_ARRAYAGG(`ordered`.`menu_id`) AS `menu_ids`
    FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_init_batch_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `ordered`
    GROUP BY `ordered`.`package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-init-batch',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'edhr-init-batch',
    NOW(),
    'edhr-init-batch',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_init_batch_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900247, 900248, 900249, 900250, 900251, 900252)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_init_batch_missing_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_init_batch_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_init_batch_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_init_batch_target_packages`;
END$$
DELIMITER ;

CALL ensure_mes_edhr_init_batch_menus();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_init_batch_menus;
