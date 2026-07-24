-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260619_srm_d8_1_non_bidding; type=schema; riskLevel=medium
-- SRM D10-1 tender project, submission, expert committee, candidate and winning result.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_tender_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招标公告编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '招标项目编号',
  `notice_title` varchar(128) NOT NULL COMMENT '公告标题',
  `notice_attachment_url` varchar(500) NOT NULL COMMENT '公告附件地址',
  `published_time` datetime NOT NULL COMMENT '发布时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_tender_notice_tenant_project` (`tenant_id`, `project_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 招标公告';

CREATE TABLE IF NOT EXISTS `srm_tender_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招标文件编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '招标项目编号',
  `document_name` varchar(128) NOT NULL COMMENT '标书名称',
  `document_attachment_url` varchar(500) NOT NULL COMMENT '标书附件地址',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_tender_document_tenant_project` (`tenant_id`, `project_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 招标文件';

CREATE TABLE IF NOT EXISTS `srm_tender_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '投标记录编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '招标项目编号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `bid_amount` decimal(18,2) NOT NULL COMMENT '投标金额',
  `submission_status` varchar(32) NOT NULL COMMENT '投标状态',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '投标附件地址',
  `submitted_by` bigint DEFAULT NULL COMMENT '投标人用户编号',
  `submitted_name` varchar(64) DEFAULT NULL COMMENT '投标人昵称',
  `submitted_time` datetime NOT NULL COMMENT '投标时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_tender_submission_tenant_project_supplier` (`tenant_id`, `project_id`, `supplier_id`, `deleted`),
  KEY `idx_srm_tender_submission_tenant_project` (`tenant_id`, `project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 招标投标记录';

CREATE TABLE IF NOT EXISTS `srm_tender_expert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招标专家编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `expert_name` varchar(64) NOT NULL COMMENT '专家姓名',
  `specialty_type` varchar(64) NOT NULL COMMENT '专业类型',
  `expert_status` varchar(32) NOT NULL COMMENT '专家状态',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人用户编号',
  `audit_name` varchar(64) DEFAULT NULL COMMENT '审核人昵称',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_tender_expert_tenant_specialty` (`tenant_id`, `specialty_type`, `expert_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 招标专家库';

CREATE TABLE IF NOT EXISTS `srm_tender_expert_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家申请编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `application_no` varchar(64) NOT NULL COMMENT '专家申请单号',
  `project_id` bigint NOT NULL COMMENT '招标项目编号',
  `application_method` varchar(32) NOT NULL COMMENT '专家产生方式',
  `required_specialty_type` varchar(64) NOT NULL COMMENT '要求专业类型',
  `required_expert_count` int NOT NULL COMMENT '要求专家人数',
  `applied_by` bigint DEFAULT NULL COMMENT '申请人用户编号',
  `applied_name` varchar(64) DEFAULT NULL COMMENT '申请人昵称',
  `applied_time` datetime NOT NULL COMMENT '申请时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_tender_expert_application_tenant_no` (`tenant_id`, `application_no`, `deleted`),
  KEY `idx_srm_tender_expert_application_tenant_project` (`tenant_id`, `project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 招标专家申请';

CREATE TABLE IF NOT EXISTS `srm_tender_committee_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评委会成员编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '招标项目编号',
  `application_id` bigint NOT NULL COMMENT '专家申请编号',
  `expert_id` bigint NOT NULL COMMENT '专家编号',
  `expert_name` varchar(64) NOT NULL COMMENT '专家姓名',
  `specialty_type` varchar(64) NOT NULL COMMENT '专业类型',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_tender_committee_tenant_project_expert` (`tenant_id`, `project_id`, `expert_id`, `deleted`),
  KEY `idx_srm_tender_committee_tenant_project` (`tenant_id`, `project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 招标评委会成员';

CREATE TABLE IF NOT EXISTS `srm_tender_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '中标候选编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '招标项目编号',
  `submission_id` bigint NOT NULL COMMENT '投标记录编号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `bid_amount` decimal(18,2) NOT NULL COMMENT '投标金额',
  `rank_no` int NOT NULL COMMENT '候选排名',
  `candidate_status` varchar(32) NOT NULL COMMENT '候选状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_tender_candidate_tenant_project_submission` (`tenant_id`, `project_id`, `submission_id`, `deleted`),
  KEY `idx_srm_tender_candidate_tenant_project` (`tenant_id`, `project_id`, `rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 招标中标候选';

CREATE TABLE IF NOT EXISTS `srm_tender_winning_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '中标结果编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '招标项目编号',
  `candidate_id` bigint NOT NULL COMMENT '中标候选编号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `winning_amount` decimal(18,2) NOT NULL COMMENT '中标金额',
  `winning_remark` varchar(500) NOT NULL COMMENT '中标说明',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人用户编号',
  `confirmed_name` varchar(64) DEFAULT NULL COMMENT '确认人昵称',
  `confirmed_time` datetime NOT NULL COMMENT '确认时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_tender_winning_result_tenant_project` (`tenant_id`, `project_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 招标中标结果';

DROP PROCEDURE IF EXISTS ensure_srm_d10_1_tender;

DELIMITER $$
CREATE PROCEDURE ensure_srm_d10_1_tender()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '缺少 SRM D7-1 基础菜单，禁止安装 D10-1 招标菜单';
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
  SELECT 991060, '招标项目', '', 2, 60, root.id, 'tender-project', 'ep:flag', 'srm/tender-project/index', 'SrmTenderProject', 0, b'1', b'1', b'1', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  FROM `system_menu` root
  WHERE root.`deleted` = b'0'
    AND root.`path` = '/srm'
    AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991060);

  UPDATE `system_menu`
  SET `name` = '招标项目',
      `path` = 'tender-project',
      `component` = 'srm/tender-project/index',
      `component_name` = 'SrmTenderProject',
      `permission` = '',
      `updater` = 'srm-d10-1',
      `update_time` = NOW()
  WHERE `id` = 991060;

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991061, '招标项目查询', 'srm:tender-project:query', 3, 1, 991060, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991061);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991062, '招标项目发布', 'srm:tender-project:publish', 3, 2, 991060, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991062);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991063, '供应商投标', 'srm:tender-project:submit-bid', 3, 3, 991060, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991063);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991064, '专家库维护', 'srm:tender-project:expert', 3, 4, 991060, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991064);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991065, '评委会组建', 'srm:tender-project:committee', 3, 5, 991060, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991065);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991066, '中标候选', 'srm:tender-project:candidate', 3, 6, 991060, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991066);

  INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991067, '中标结果', 'srm:tender-project:winning', 3, 7, 991060, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991067);

  UPDATE `system_menu`
  SET `name` = CASE `id`
      WHEN 991061 THEN '招标项目查询'
      WHEN 991062 THEN '招标项目发布'
      WHEN 991063 THEN '供应商投标'
      WHEN 991064 THEN '专家库维护'
      WHEN 991065 THEN '评委会组建'
      WHEN 991066 THEN '中标候选'
      WHEN 991067 THEN '中标结果'
      ELSE `name`
    END,
    `permission` = CASE `id`
      WHEN 991061 THEN 'srm:tender-project:query'
      WHEN 991062 THEN 'srm:tender-project:publish'
      WHEN 991063 THEN 'srm:tender-project:submit-bid'
      WHEN 991064 THEN 'srm:tender-project:expert'
      WHEN 991065 THEN 'srm:tender-project:committee'
      WHEN 991066 THEN 'srm:tender-project:candidate'
      WHEN 991067 THEN 'srm:tender-project:winning'
      ELSE `permission`
    END,
    `updater` = 'srm-d10-1',
    `update_time` = NOW()
  WHERE `id` BETWEEN 991061 AND 991067;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `name` = '招标项目'
      AND `component` = 'srm/tender-project/index'
      AND `component_name` = 'SrmTenderProject'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM tender route menu for get-permission-info';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d10_1_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d10_1_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991060, 991061, 991062, 991063, 991064, 991065, 991066, 991067);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d10_1_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d10_1_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_d10_1_package_menu_ids` (`package_id`, `menu_id`)
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
  CROSS JOIN `tmp_srm_d10_1_menu_ids` AS `menu`
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
    FROM `tmp_srm_d10_1_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-d10-1',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-d10-1',
    NOW(),
    'srm-d10-1',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_d10_1_menu_ids` AS `menu`
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
  SELECT DISTINCT tenant_source.`tenant_id`, 'SRM_EXPERT_DRAW_APPLICATION', '专家抽取申请编码规则', 'EXPERT_DRAW_APPLICATION', 'EA', 'yyyyMMdd', b'1', 4, 1, 1, 9999, '-', b'1', 'D10-1 招标专家申请规则', 'srm-d10-1', NOW(), 'srm-d10-1', NOW(), b'0'
  FROM (
    SELECT DISTINCT r.`tenant_id`
    FROM `system_role_menu` rm
    JOIN `system_role` r ON r.`id` = rm.`role_id` AND r.`deleted` = b'0'
    WHERE rm.`menu_id` = 991060
      AND rm.`deleted` = b'0'
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1
    FROM `srm_code_rule` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`target_form` = 'EXPERT_DRAW_APPLICATION'
      AND existing.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d10_1_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d10_1_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_d10_1_tender();
DROP PROCEDURE IF EXISTS ensure_srm_d10_1_tender;
