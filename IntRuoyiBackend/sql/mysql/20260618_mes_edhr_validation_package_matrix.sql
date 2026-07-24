-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260618_mes_edhr_delivery_cockpit; type=schema; riskLevel=medium
-- eDHR commercial validation package and traceability matrix second slice.
-- This slice declares CSV baseline fields, URS/FRS/RISK/IQ/OQ/PQ items and trace links only.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_validation_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_code` varchar(64) NOT NULL COMMENT '验证包编码',
  `package_name` varchar(128) NOT NULL COMMENT '验证包名称',
  `customer_project_name` varchar(128) NOT NULL COMMENT '客户项目名称',
  `customer_name` varchar(128) NOT NULL COMMENT '客户名称',
  `site_name` varchar(128) NOT NULL COMMENT '客户现场',
  `system_scope` varchar(500) NOT NULL COMMENT '系统范围',
  `validation_scope` varchar(500) NOT NULL COMMENT '验证范围',
  `release_tag` varchar(64) NOT NULL COMMENT '发布标签',
  `schema_version` varchar(64) NOT NULL COMMENT '数据库结构版本',
  `target_environment` varchar(64) NOT NULL COMMENT '目标环境',
  `validation_status` varchar(32) NOT NULL COMMENT '验证包状态：BLOCKED、PREPARED',
  `oq_ready` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否具备OQ Ready',
  `validation_owner_name` varchar(128) NOT NULL COMMENT '验证负责人',
  `qa_owner_name` varchar(128) NOT NULL COMMENT 'QA负责人',
  `blocked_reason` varchar(500) NOT NULL COMMENT '阻断原因',
  `trace_summary_json` longtext NOT NULL COMMENT '追溯矩阵摘要JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_validation_package_code` (`tenant_id`, `package_code`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_package_status` (`tenant_id`, `validation_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR商业化验证包';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_validation_requirement_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_id` bigint NOT NULL COMMENT '验证包ID',
  `item_code` varchar(64) NOT NULL COMMENT '条目编号',
  `item_name` varchar(128) NOT NULL COMMENT '条目名称',
  `item_type` varchar(32) NOT NULL COMMENT '条目类型：URS、FRS、RISK、IQ、OQ、PQ',
  `item_version` varchar(64) NOT NULL COMMENT '条目版本',
  `item_status` varchar(32) NOT NULL COMMENT '条目状态：DRAFT、ACTIVE、CLOSED',
  `owner_name` varchar(128) NOT NULL COMMENT '责任人',
  `signoff_role` varchar(128) NOT NULL COMMENT '签核角色',
  `source_document` varchar(256) NOT NULL COMMENT '来源文档',
  `business_process` varchar(256) DEFAULT NULL COMMENT '业务过程',
  `acceptance_criteria` varchar(500) DEFAULT NULL COMMENT '验收标准',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_validation_item_code` (`tenant_id`, `package_id`, `item_type`, `item_code`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_item_package_type` (`tenant_id`, `package_id`, `item_type`, `item_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR商业化验证条目';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_validation_trace_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_id` bigint NOT NULL COMMENT '验证包ID',
  `source_item_id` bigint NOT NULL COMMENT '来源条目ID',
  `source_item_code` varchar(64) NOT NULL COMMENT '来源条目编号',
  `source_item_type` varchar(32) NOT NULL COMMENT '来源条目类型',
  `target_item_id` bigint NOT NULL COMMENT '目标条目ID',
  `target_item_code` varchar(64) NOT NULL COMMENT '目标条目编号',
  `target_item_type` varchar(32) NOT NULL COMMENT '目标条目类型',
  `link_type` varchar(32) NOT NULL COMMENT '追溯类型：URS_FRS、URS_RISK、URS_VERIFICATION',
  `trace_status` varchar(32) NOT NULL COMMENT '追溯状态：ACTIVE、BROKEN',
  `owner_name` varchar(128) NOT NULL COMMENT '责任人',
  `next_action` varchar(500) NOT NULL COMMENT '下一步动作',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_validation_trace_link` (`tenant_id`, `package_id`, `source_item_id`, `target_item_id`, `link_type`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_trace_source` (`tenant_id`, `package_id`, `source_item_id`, `trace_status`, `deleted`),
  KEY `idx_mes_pro_edhr_validation_trace_target` (`tenant_id`, `package_id`, `target_item_id`, `trace_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR商业化验证追溯关系';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900286, 'eDHR验证包矩阵', '', 2, 286, 900220, '/mes/pro/feedback/edhr-validation', 'ep:data-analysis', 'mes/pro/edhr-validation/ValidationPage', 'MesProEdhrValidation', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900286 OR `path` = '/mes/pro/feedback/edhr-validation');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900287, 'eDHR验证包查询', 'mes:pro-edhr-validation:query', 3, 1, 900286, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900287 OR `permission` = 'mes:pro-edhr-validation:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900288, 'eDHR验证包创建', 'mes:pro-edhr-validation:create', 3, 2, 900286, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900288 OR `permission` = 'mes:pro-edhr-validation:create');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900289, 'eDHR追溯门禁评估', 'mes:pro-edhr-validation:evaluate-trace', 3, 3, 900286, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900289 OR `permission` = 'mes:pro-edhr-validation:evaluate-trace');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_validation_test_tenant_menus;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_validation_test_tenant_menus()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_validation_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_validation_target_tenant` AS
  SELECT `tenant`.`id` AS `tenant_id`, `tenant`.`package_id`
  FROM `system_tenant` AS `tenant`
  WHERE `tenant`.`name` = '测试租户'
    AND `tenant`.`deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_validation_target_tenant`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing unique 测试租户; cannot merge eDHR validation menus';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_validation_target_tenant` AS `target`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target`.`package_id`
     AND `package`.`deleted` = b'0'
    WHERE NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR validation menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_validation_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_validation_menu_ids` AS
  SELECT `id` AS `menu_id`
  FROM `system_menu`
  WHERE `id` IN (900286, 900287, 900288, 900289)
    AND `deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_validation_menu_ids`) <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR validation system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_validation_package_existing_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_validation_package_existing_menu_ids` AS
  SELECT `package`.`id` AS `package_id`, CAST(`menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_validation_target_tenant` AS `target`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target`.`package_id`
   AND `package`.`deleted` = b'0'
  JOIN JSON_TABLE(CAST(`package`.`menu_ids` AS JSON), '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')) AS `menu`;

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_validation_package_existing_menu_ids`
    WHERE `menu_id` = 900220
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing 测试租户 eDHR parent menu 900220; cannot merge validation menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_validation_package_merged_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_validation_package_merged_menu_ids` AS
  SELECT DISTINCT `package_id`, `menu_id`
  FROM `tmp_mes_edhr_validation_package_existing_menu_ids`
  UNION
  SELECT DISTINCT `target`.`package_id`, `menus`.`menu_id`
  FROM `tmp_mes_edhr_validation_target_tenant` AS `target`
  JOIN `tmp_mes_edhr_validation_menu_ids` AS `menus`;

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_validation_target_tenant` AS `target`
    ON `target`.`package_id` = `package`.`id`
  JOIN (
    SELECT `package_id`, JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_validation_package_merged_menu_ids`
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
  FROM `tmp_mes_edhr_validation_target_tenant` AS `target`
  JOIN `system_role` AS `role`
    ON `role`.`tenant_id` = `target`.`tenant_id`
   AND `role`.`code` = 'tenant_admin'
   AND `role`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_validation_menu_ids` AS `menus`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menus`.`menu_id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_validation_package_merged_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_validation_package_existing_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_validation_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_validation_target_tenant`;
END//
DELIMITER ;

CALL ensure_mes_edhr_validation_test_tenant_menus();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_validation_test_tenant_menus;
