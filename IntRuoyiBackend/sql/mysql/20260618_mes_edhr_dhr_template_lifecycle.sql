-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_init_batch_precheck; type=schema; riskLevel=medium
-- eDHR DHR catalog and template lifecycle baseline.
-- Boundary: no default effective status, no mock signoff, no direct retire/void without impact scope confirmation.

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_dhr_catalog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `catalog_code` varchar(64) NOT NULL COMMENT 'DHR目录编码',
  `catalog_name` varchar(128) NOT NULL COMMENT 'DHR目录名称',
  `parent_catalog_id` bigint DEFAULT NULL COMMENT '父目录ID',
  `status` varchar(32) NOT NULL COMMENT '目录状态：ACTIVE/DISABLED',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_dhr_catalog_code` (`tenant_id`, `catalog_code`, `deleted`),
  KEY `idx_mes_pro_edhr_dhr_catalog_parent` (`tenant_id`, `parent_catalog_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR DHR目录';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_dhr_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `catalog_id` bigint NOT NULL COMMENT 'DHR目录ID',
  `template_code` varchar(64) NOT NULL COMMENT 'DHR模板编码',
  `template_name` varchar(128) NOT NULL COMMENT 'DHR模板名称',
  `current_version` varchar(32) NOT NULL COMMENT '当前模板版本',
  `status` varchar(32) NOT NULL COMMENT '模板状态：DRAFT/PRECHECK_FAILED/PENDING_REVIEW/APPROVED/SIGNOFF_PENDING/EFFECTIVE/SUSPENDED/RETIRED/OBSOLETE',
  `review_status` varchar(32) NOT NULL COMMENT '审核状态：NOT_SUBMITTED/PENDING/APPROVED/REJECTED',
  `signoff_status` varchar(32) NOT NULL COMMENT '签核状态：NOT_SIGNED/SIGNED',
  `binding_count` int NOT NULL DEFAULT 0 COMMENT '绑定数量',
  `integrity_issue_count` int NOT NULL DEFAULT 0 COMMENT '完整性问题数',
  `integrity_issue_json` longtext DEFAULT NULL COMMENT '完整性问题JSON',
  `signoff_evidence_hash` char(64) DEFAULT NULL COMMENT '签核证据hash',
  `effective_at` datetime DEFAULT NULL COMMENT '生效时间',
  `retired_at` datetime DEFAULT NULL COMMENT '停用时间',
  `voided_at` datetime DEFAULT NULL COMMENT '作废时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_dhr_template_code` (`tenant_id`, `template_code`, `deleted`),
  KEY `idx_mes_pro_edhr_dhr_template_status` (`tenant_id`, `status`, `review_status`, `signoff_status`),
  KEY `idx_mes_pro_edhr_dhr_template_catalog` (`tenant_id`, `catalog_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR DHR模板';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_dhr_template_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT 'DHR模板ID',
  `version_no` varchar(32) NOT NULL COMMENT '版本号',
  `template_snapshot_json` longtext NOT NULL COMMENT '模板快照JSON',
  `change_summary` varchar(500) DEFAULT NULL COMMENT '变更摘要',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_dhr_template_version` (`tenant_id`, `template_id`, `version_no`, `deleted`),
  KEY `idx_mes_pro_edhr_dhr_template_version_template` (`tenant_id`, `template_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR DHR模板版本';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_dhr_template_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT 'DHR模板ID',
  `binding_type` varchar(32) NOT NULL COMMENT '绑定类型：PRODUCT/ROUTE/PROCESS/BATCH_TYPE',
  `binding_object_id` bigint DEFAULT NULL COMMENT '绑定对象ID',
  `binding_object_code` varchar(128) NOT NULL COMMENT '绑定对象编码',
  `binding_object_name` varchar(128) DEFAULT NULL COMMENT '绑定对象名称',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_dhr_template_binding` (`tenant_id`, `template_id`, `binding_type`, `binding_object_code`, `deleted`),
  KEY `idx_mes_pro_edhr_dhr_template_binding_object` (`tenant_id`, `binding_type`, `binding_object_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR DHR模板绑定';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_dhr_template_impact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT 'DHR模板ID',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型：RETIRE/VOID',
  `impact_scope_json` longtext NOT NULL COMMENT '影响范围JSON',
  `impact_confirmed` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否确认影响',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_dhr_template_impact` (`tenant_id`, `template_id`, `action_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES eDHR DHR模板影响分析';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900290, 'eDHR DHR模板', 'mes:pro-edhr-dhr-template:query', 2, 12, 900220, '/mes/pro/feedback/edhr-dhr-template', 'ep:document-copy', 'mes/pro/edhr-dhr-template/DhrTemplatePage', 'MesProFeedbackEdhrDhrTemplate', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900290 OR (`permission` = 'mes:pro-edhr-dhr-template:query' AND `type` = 2));

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900291, 'eDHR DHR模板查询', 'mes:pro-edhr-dhr-template:query', 3, 1, 900290, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-dhr-template:query' AND `type` = 3);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900292, 'eDHR DHR模板创建', 'mes:pro-edhr-dhr-template:create', 3, 2, 900290, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-dhr-template:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900347, 'eDHR DHR模板完整性检查', 'mes:pro-edhr-dhr-template:check', 3, 3, 900290, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-dhr-template:check');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900348, 'eDHR DHR模板审核', 'mes:pro-edhr-dhr-template:approve', 3, 4, 900290, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-dhr-template:approve');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900349, 'eDHR DHR模板签核', 'mes:pro-edhr-dhr-template:signoff', 3, 5, 900290, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-dhr-template:signoff');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900350, 'eDHR DHR模板生效', 'mes:pro-edhr-dhr-template:activate', 3, 6, 900290, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-dhr-template:activate');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900351, 'eDHR DHR模板停用', 'mes:pro-edhr-dhr-template:retire', 3, 7, 900290, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-dhr-template:retire');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900352, 'eDHR DHR模板作废', 'mes:pro-edhr-dhr-template:void', 3, 8, 900290, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-dhr-template-menu', NOW(), 'edhr-dhr-template-menu', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'mes:pro-edhr-dhr-template:void');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_dhr_template_menus;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_dhr_template_menus()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900220 AND `deleted` = b'0') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR root system_menu row; cannot merge eDHR DHR template menus';
  END IF;

  IF EXISTS (
      SELECT 1 FROM `system_tenant_package` AS `package`
      WHERE `package`.`deleted` = b'0'
        AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR DHR template menus';
  END IF;

  IF (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (900290, 900291, 900292, 900347, 900348, 900349, 900350, 900351, 900352)) <> 9 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR DHR template system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_dhr_template_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_dhr_template_target_packages` AS
  SELECT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_dhr_template_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_dhr_template_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900290, 900291, 900292, 900347, 900348, 900349, 900350, 900351, 900352)
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_dhr_template_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_dhr_template_package_menu_ids` AS
  SELECT DISTINCT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_dhr_template_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT INTO `tmp_mes_edhr_dhr_template_package_menu_ids` (`package_id`, `menu_id`)
  SELECT DISTINCT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_edhr_dhr_template_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_dhr_template_menu_ids` AS `menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_dhr_template_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
    ) AS `distinct_menu`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-dhr-template-menu',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT
    `role`.`id`,
    `menu`.`id`,
    'edhr-dhr-template-menu',
    NOW(),
    'edhr-dhr-template-menu',
    NOW(),
    b'0'
  FROM `system_users` AS `tenant_admin`
  JOIN `system_user_role` AS `user_role`
    ON `user_role`.`user_id` = `tenant_admin`.`id`
   AND `user_role`.`deleted` = b'0'
  JOIN `system_role` AS `role`
    ON `role`.`id` = `user_role`.`role_id`
   AND `role`.`deleted` = b'0'
  CROSS JOIN `tmp_mes_edhr_dhr_template_menu_ids` AS `menu`
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

CALL ensure_mes_edhr_dhr_template_menus();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_dhr_template_menus;
