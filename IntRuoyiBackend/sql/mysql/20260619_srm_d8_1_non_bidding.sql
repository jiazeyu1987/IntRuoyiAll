-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260619_srm_d7_3_plan_framework; type=schema; riskLevel=medium
-- SRM D8-1 non-bidding procurement publish, quotation, deal and contractable source.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_non_bidding_supplier_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '非招标供应商范围编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '非招标项目编号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_non_bidding_scope_tenant_project_supplier` (`tenant_id`, `project_id`, `supplier_id`, `deleted`),
  KEY `idx_srm_non_bidding_scope_tenant_supplier` (`tenant_id`, `supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 非招标供应商范围';

CREATE TABLE IF NOT EXISTS `srm_non_bidding_quote` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '非招标报价编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '非招标项目编号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `quote_amount` decimal(18,2) NOT NULL COMMENT '报价金额',
  `quote_status` varchar(32) NOT NULL COMMENT '报价状态',
  `attachment_url` varchar(500) NOT NULL COMMENT '报价附件地址',
  `quoted_by` bigint DEFAULT NULL COMMENT '报价人用户编号',
  `quoted_name` varchar(64) DEFAULT NULL COMMENT '报价人昵称',
  `quoted_time` datetime NOT NULL COMMENT '报价时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_non_bidding_quote_tenant_project_supplier` (`tenant_id`, `project_id`, `supplier_id`, `deleted`),
  KEY `idx_srm_non_bidding_quote_tenant_project` (`tenant_id`, `project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 非招标供应商报价';

CREATE TABLE IF NOT EXISTS `srm_non_bidding_quote_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '非招标报价行编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `quote_id` bigint NOT NULL COMMENT '报价编号',
  `project_id` bigint NOT NULL COMMENT '非招标项目编号',
  `project_line_id` bigint NOT NULL COMMENT '非招标项目行编号',
  `material_id` bigint NOT NULL COMMENT '物料编号',
  `material_code` varchar(64) NOT NULL COMMENT '物料编码',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称',
  `quantity` decimal(18,2) NOT NULL COMMENT '数量',
  `unit` varchar(32) NOT NULL COMMENT '单位',
  `unit_price` decimal(18,2) NOT NULL COMMENT '报价单价',
  `line_amount` decimal(18,2) NOT NULL COMMENT '报价行金额',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_non_bidding_quote_line_tenant_quote` (`tenant_id`, `quote_id`),
  KEY `idx_srm_non_bidding_quote_line_tenant_project` (`tenant_id`, `project_id`, `project_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 非招标报价行';

DROP PROCEDURE IF EXISTS ensure_srm_d8_1_non_bidding;

DELIMITER $$
CREATE PROCEDURE ensure_srm_d8_1_non_bidding()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'srm_sourcing_project'
      AND `column_name` = 'quote_mode'
  ) THEN
    ALTER TABLE `srm_sourcing_project`
      ADD COLUMN `quote_mode` varchar(32) DEFAULT NULL COMMENT '询价模式';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'srm_sourcing_project'
      AND `column_name` = 'quote_start_time'
  ) THEN
    ALTER TABLE `srm_sourcing_project`
      ADD COLUMN `quote_start_time` datetime DEFAULT NULL COMMENT '报价开始时间',
      ADD COLUMN `quote_end_time` datetime DEFAULT NULL COMMENT '报价截止时间',
      ADD COLUMN `publish_attachment_url` varchar(500) DEFAULT NULL COMMENT '发布附件地址',
      ADD COLUMN `published_time` datetime DEFAULT NULL COMMENT '发布时间',
      ADD COLUMN `deal_quote_id` bigint DEFAULT NULL COMMENT '成交报价编号',
      ADD COLUMN `deal_supplier_id` bigint DEFAULT NULL COMMENT '成交供应商编号',
      ADD COLUMN `deal_supplier_name` varchar(128) DEFAULT NULL COMMENT '成交供应商名称',
      ADD COLUMN `deal_amount` decimal(18,2) DEFAULT NULL COMMENT '成交金额',
      ADD COLUMN `deal_remark` varchar(500) DEFAULT NULL COMMENT '成交说明',
      ADD COLUMN `deal_time` datetime DEFAULT NULL COMMENT '成交时间',
      ADD COLUMN `contract_id` bigint DEFAULT NULL COMMENT '来源合同编号';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge SRM non-bidding menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991000
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '缺少 SRM D7-1 基础菜单，不能继续挂载非招标项目页面';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (
        (`id` = 991050 AND (`path` <> 'non-bidding-project' OR `component` <> 'srm/non-bidding-project/index' OR `component_name` <> 'SrmNonBiddingProject'))
        OR (`id` = 991051 AND `permission` <> 'srm:non-bidding-project:query')
        OR (`id` = 991052 AND `permission` <> 'srm:non-bidding-project:publish')
        OR (`id` = 991053 AND `permission` <> 'srm:non-bidding-project:quote')
        OR (`id` = 991054 AND `permission` <> 'srm:non-bidding-project:deal')
        OR (`id` = 991055 AND `permission` <> 'srm:non-bidding-project:contract')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM D8-1 clean menu id range; conflicting system_menu rows exist';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991050, '非招标项目', 'srm:non-bidding-project:query', 2, 5, 991000, 'non-bidding-project', 'ep:price-tag', 'srm/non-bidding-project/index', 'SrmNonBiddingProject', 0, b'1', b'1', b'1', 'srm-d8-1', NOW(), 'srm-d8-1', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991050 OR `component` = 'srm/non-bidding-project/index' OR `permission` = 'srm:non-bidding-project:query')
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991051, '非招标项目查询', 'srm:non-bidding-project:query', 3, 1, 991050, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d8-1', NOW(), 'srm-d8-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991051 OR (`permission` = 'srm:non-bidding-project:query' AND `type` = 3)));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991052, '非招标项目发布', 'srm:non-bidding-project:publish', 3, 2, 991050, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d8-1', NOW(), 'srm-d8-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991052 OR `permission` = 'srm:non-bidding-project:publish'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991053, '非招标供应商报价', 'srm:non-bidding-project:quote', 3, 3, 991050, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d8-1', NOW(), 'srm-d8-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991053 OR `permission` = 'srm:non-bidding-project:quote'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991054, '非招标成交确认', 'srm:non-bidding-project:deal', 3, 4, 991050, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d8-1', NOW(), 'srm-d8-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991054 OR `permission` = 'srm:non-bidding-project:deal'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991055, '非招标可建合同', 'srm:non-bidding-project:contract', 3, 5, 991050, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d8-1', NOW(), 'srm-d8-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991055 OR `permission` = 'srm:non-bidding-project:contract'));

  UPDATE `system_menu`
  SET `name` = CASE `id`
        WHEN 991050 THEN '非招标项目'
        WHEN 991051 THEN '非招标项目查询'
        WHEN 991052 THEN '非招标项目发布'
        WHEN 991053 THEN '非招标供应商报价'
        WHEN 991054 THEN '非招标成交确认'
        WHEN 991055 THEN '非招标可建合同'
      END,
      `updater` = 'srm-d8-1',
      `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND `id` IN (991050, 991051, 991052, 991053, 991054, 991055)
    AND `name` <> CASE `id`
        WHEN 991050 THEN '非招标项目'
        WHEN 991051 THEN '非招标项目查询'
        WHEN 991052 THEN '非招标项目发布'
        WHEN 991053 THEN '非招标供应商报价'
        WHEN 991054 THEN '非招标成交确认'
        WHEN 991055 THEN '非招标可建合同'
      END;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `name` = '非招标项目'
      AND `component` = 'srm/non-bidding-project/index'
      AND `component_name` = 'SrmNonBiddingProject'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM non-bidding route menu for get-permission-info';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d8_1_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d8_1_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991050, 991051, 991052, 991053, 991054, 991055);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d8_1_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d8_1_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_d8_1_package_menu_ids` (`package_id`, `menu_id`)
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
  CROSS JOIN `tmp_srm_d8_1_menu_ids` AS `menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('991000' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_srm_d8_1_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-d8-1',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-d8-1',
    NOW(),
    'srm-d8-1',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_d8_1_menu_ids` AS `menu`
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d8_1_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d8_1_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_d8_1_non_bidding();

DROP PROCEDURE IF EXISTS ensure_srm_d8_1_non_bidding;
