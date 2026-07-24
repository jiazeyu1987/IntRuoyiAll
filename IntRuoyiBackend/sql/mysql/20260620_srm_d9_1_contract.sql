-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260620_srm_d10_1_tender; type=schema; riskLevel=medium
-- SRM D9-1 procurement contract, payment, signing, attachment and source writeback.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_procurement_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购合同编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `contract_no` varchar(64) NOT NULL COMMENT '合同编号',
  `contract_title` varchar(128) NOT NULL COMMENT '合同标题',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_id` bigint NOT NULL COMMENT '来源项目编号',
  `source_no` varchar(64) NOT NULL COMMENT '来源项目单号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `contract_amount` decimal(18,2) NOT NULL COMMENT '合同金额',
  `currency` varchar(16) NOT NULL COMMENT '币种',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `expire_date` date NOT NULL COMMENT '到期日期',
  `contract_status` varchar(32) NOT NULL COMMENT '合同状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人用户编号',
  `created_name` varchar(64) DEFAULT NULL COMMENT '创建人昵称',
  `created_time` datetime NOT NULL COMMENT '创建时间',
  `cancelled_by` bigint DEFAULT NULL COMMENT '作废人用户编号',
  `cancelled_name` varchar(64) DEFAULT NULL COMMENT '作废人昵称',
  `cancelled_time` datetime DEFAULT NULL COMMENT '作废时间',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '作废原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_procurement_contract_tenant_no` (`tenant_id`, `contract_no`, `deleted`),
  KEY `idx_srm_procurement_contract_tenant_source` (`tenant_id`, `source_type`, `source_id`, `contract_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 采购合同';

CREATE TABLE IF NOT EXISTS `srm_procurement_contract_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同付款约定编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `contract_id` bigint NOT NULL COMMENT '合同编号',
  `payment_stage` varchar(64) NOT NULL COMMENT '付款阶段',
  `payment_ratio` decimal(8,2) NOT NULL COMMENT '付款比例',
  `payment_amount` decimal(18,2) NOT NULL COMMENT '付款金额',
  `due_date` date NOT NULL COMMENT '应付日期',
  `payment_remark` varchar(500) DEFAULT NULL COMMENT '付款说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_procurement_contract_payment_tenant_contract` (`tenant_id`, `contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 合同付款约定';

CREATE TABLE IF NOT EXISTS `srm_procurement_contract_signing` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同签署编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `contract_id` bigint NOT NULL COMMENT '合同编号',
  `signing_party` varchar(64) NOT NULL COMMENT '签署方',
  `signer_name` varchar(64) NOT NULL COMMENT '签署人',
  `signing_date` date NOT NULL COMMENT '签署日期',
  `signing_remark` varchar(500) DEFAULT NULL COMMENT '签署说明',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_procurement_contract_signing_tenant_contract` (`tenant_id`, `contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 合同签署信息';

CREATE TABLE IF NOT EXISTS `srm_procurement_contract_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同附件编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `contract_id` bigint NOT NULL COMMENT '合同编号',
  `attachment_name` varchar(128) NOT NULL COMMENT '附件名称',
  `attachment_url` varchar(500) NOT NULL COMMENT '附件地址',
  `attachment_type` varchar(32) NOT NULL COMMENT '附件类型',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_procurement_contract_attachment_tenant_contract` (`tenant_id`, `contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 合同附件';

DROP PROCEDURE IF EXISTS ensure_srm_d9_1_contract;

DELIMITER $$
CREATE PROCEDURE ensure_srm_d9_1_contract()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '缺少 SRM D7-1 基础菜单，禁止安装 D9-1 合同菜单';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND `menu_ids` IS NOT NULL
      AND `menu_ids` <> ''
      AND JSON_VALID(`menu_ids`) = 0
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON';
  END IF;

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991070, '采购合同', '', 2, 70, root.id, 'procurement-contract', 'ep:document-checked', 'srm/procurement-contract/index', 'SrmProcurementContract', 0, b'1', b'1', b'1', 'srm-d9-1', NOW(), 'srm-d9-1', NOW(), b'0'
  FROM `system_menu` root
  WHERE root.`deleted` = b'0'
    AND root.`path` = '/srm'
    AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991070);

  UPDATE `system_menu`
  SET `name` = '采购合同',
      `path` = 'procurement-contract',
      `component` = 'srm/procurement-contract/index',
      `component_name` = 'SrmProcurementContract',
      `permission` = '',
      `updater` = 'srm-d9-1',
      `update_time` = NOW()
  WHERE `id` = 991070;

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991071, '采购合同查询', 'srm:procurement-contract:query', 3, 1, 991070, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d9-1', NOW(), 'srm-d9-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991071);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991072, '采购合同创建', 'srm:procurement-contract:create', 3, 2, 991070, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d9-1', NOW(), 'srm-d9-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991072);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991073, '采购合同作废', 'srm:procurement-contract:cancel', 3, 3, 991070, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d9-1', NOW(), 'srm-d9-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991073);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991074, '采购合同删除', 'srm:procurement-contract:delete', 3, 4, 991070, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d9-1', NOW(), 'srm-d9-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991074);

  UPDATE `system_menu`
  SET `name` = CASE `id`
      WHEN 991071 THEN '采购合同查询'
      WHEN 991072 THEN '采购合同创建'
      WHEN 991073 THEN '采购合同作废'
      WHEN 991074 THEN '采购合同删除'
      ELSE `name`
    END,
    `permission` = CASE `id`
      WHEN 991071 THEN 'srm:procurement-contract:query'
      WHEN 991072 THEN 'srm:procurement-contract:create'
      WHEN 991073 THEN 'srm:procurement-contract:cancel'
      WHEN 991074 THEN 'srm:procurement-contract:delete'
      ELSE `permission`
    END,
    `updater` = 'srm-d9-1',
    `update_time` = NOW()
  WHERE `id` BETWEEN 991071 AND 991074;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `name` = '采购合同'
      AND `component` = 'srm/procurement-contract/index'
      AND `component_name` = 'SrmProcurementContract'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM procurement contract route menu for get-permission-info';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d9_1_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d9_1_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991070, 991071, 991072, 991073, 991074);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d9_1_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d9_1_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_d9_1_package_menu_ids` (`package_id`, `menu_id`)
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
  CROSS JOIN `tmp_srm_d9_1_menu_ids` AS `menu`
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
    FROM `tmp_srm_d9_1_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-d9-1',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-d9-1',
    NOW(),
    'srm-d9-1',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_d9_1_menu_ids` AS `menu`
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

  INSERT INTO `srm_code_rule` (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_PROCUREMENT_CONTRACT', '采购合同编码规则', 'PROCUREMENT_CONTRACT', 'PC', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'D9-1 采购合同规则', 'srm-d9-1', NOW(), 'srm-d9-1', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` = 991070
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1
    FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'PROCUREMENT_CONTRACT'
      AND existing.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d9_1_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d9_1_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_d9_1_contract();
DROP PROCEDURE IF EXISTS ensure_srm_d9_1_contract;
