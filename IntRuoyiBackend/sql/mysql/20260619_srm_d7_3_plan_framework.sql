-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260619_srm_d7_2_supplier_access_risk; type=schema; riskLevel=medium
-- SRM D7-3 procurement plan, sourcing conversion, framework plan and framework agreement.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `srm_procurement_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购计划编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `plan_no` varchar(64) NOT NULL COMMENT '采购计划单号',
  `plan_title` varchar(128) NOT NULL COMMENT '计划标题',
  `procurement_method` varchar(32) NOT NULL COMMENT '采购方式',
  `expected_amount` decimal(18,2) NOT NULL COMMENT '预计金额',
  `plan_status` varchar(32) NOT NULL COMMENT '计划状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人用户编号',
  `submitted_name` varchar(64) DEFAULT NULL COMMENT '提交人昵称',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人用户编号',
  `audit_name` varchar(64) DEFAULT NULL COMMENT '审核人昵称',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `generated_project_id` bigint DEFAULT NULL COMMENT '生成项目编号',
  `generated_project_no` varchar(64) DEFAULT NULL COMMENT '生成项目单号',
  `generated_project_type` varchar(32) DEFAULT NULL COMMENT '生成项目类型',
  `generated_time` datetime DEFAULT NULL COMMENT '生成时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_procurement_plan_tenant_no` (`tenant_id`, `plan_no`, `deleted`),
  KEY `idx_srm_procurement_plan_tenant_status` (`tenant_id`, `plan_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 采购计划';

CREATE TABLE IF NOT EXISTS `srm_procurement_plan_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购计划行编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `plan_id` bigint NOT NULL COMMENT '采购计划编号',
  `line_no` varchar(64) NOT NULL COMMENT '计划行号',
  `material_id` bigint NOT NULL COMMENT '物料编号',
  `material_code` varchar(64) NOT NULL COMMENT '物料编码',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称',
  `quantity` decimal(18,2) NOT NULL COMMENT '数量',
  `unit` varchar(32) NOT NULL COMMENT '单位',
  `required_date` date NOT NULL COMMENT '需求日期',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_procurement_plan_line_tenant_no` (`tenant_id`, `line_no`, `deleted`),
  KEY `idx_srm_procurement_plan_line_tenant_plan` (`tenant_id`, `plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 采购计划行';

CREATE TABLE IF NOT EXISTS `srm_procurement_approval_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批记录编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务编号',
  `action` varchar(32) NOT NULL COMMENT '审批动作',
  `action_label` varchar(32) NOT NULL COMMENT '审批动作名称',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人用户编号',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人昵称',
  `operation_time` datetime NOT NULL COMMENT '操作时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_procurement_approval_tenant_biz` (`tenant_id`, `biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 采购审批记录';

CREATE TABLE IF NOT EXISTS `srm_sourcing_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '寻源项目编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_no` varchar(64) NOT NULL COMMENT '项目单号',
  `project_title` varchar(128) NOT NULL COMMENT '项目标题',
  `project_type` varchar(32) NOT NULL COMMENT '项目类型',
  `project_status` varchar(32) NOT NULL COMMENT '项目状态',
  `source_plan_id` bigint NOT NULL COMMENT '来源采购计划编号',
  `source_plan_no` varchar(64) NOT NULL COMMENT '来源采购计划单号',
  `expected_amount` decimal(18,2) NOT NULL COMMENT '预计金额',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_sourcing_project_tenant_source_plan` (`tenant_id`, `source_plan_id`, `deleted`),
  UNIQUE KEY `uk_srm_sourcing_project_tenant_no` (`tenant_id`, `project_no`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 寻源项目骨架';

CREATE TABLE IF NOT EXISTS `srm_sourcing_project_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '寻源项目行编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `project_id` bigint NOT NULL COMMENT '寻源项目编号',
  `source_plan_id` bigint NOT NULL COMMENT '来源采购计划编号',
  `source_plan_line_id` bigint NOT NULL COMMENT '来源采购计划行编号',
  `line_no` varchar(64) NOT NULL COMMENT '项目行号',
  `material_id` bigint NOT NULL COMMENT '物料编号',
  `material_code` varchar(64) NOT NULL COMMENT '物料编码',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称',
  `quantity` decimal(18,2) NOT NULL COMMENT '数量',
  `unit` varchar(32) NOT NULL COMMENT '单位',
  `required_date` date NOT NULL COMMENT '需求日期',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_sourcing_project_line_tenant_project` (`tenant_id`, `project_id`),
  KEY `idx_srm_sourcing_project_line_tenant_source` (`tenant_id`, `source_plan_id`, `source_plan_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 寻源项目行骨架';

CREATE TABLE IF NOT EXISTS `srm_framework_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '框架计划编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `framework_plan_no` varchar(64) NOT NULL COMMENT '框架计划单号',
  `plan_title` varchar(128) NOT NULL COMMENT '框架计划标题',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `procurement_method` varchar(32) NOT NULL COMMENT '采购方式',
  `budget_amount` decimal(18,2) NOT NULL COMMENT '预算金额',
  `valid_start_date` date NOT NULL COMMENT '有效期开始日期',
  `valid_end_date` date NOT NULL COMMENT '有效期结束日期',
  `plan_status` varchar(32) NOT NULL COMMENT '计划状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交人用户编号',
  `submitted_name` varchar(64) DEFAULT NULL COMMENT '提交人昵称',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人用户编号',
  `audit_name` varchar(64) DEFAULT NULL COMMENT '审核人昵称',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `agreement_id` bigint DEFAULT NULL COMMENT '框架协议编号',
  `agreement_no` varchar(64) DEFAULT NULL COMMENT '框架协议单号',
  `agreement_time` datetime DEFAULT NULL COMMENT '生成协议时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_framework_plan_tenant_no` (`tenant_id`, `framework_plan_no`, `deleted`),
  KEY `idx_srm_framework_plan_tenant_status` (`tenant_id`, `plan_status`, `supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 框架采购计划';

CREATE TABLE IF NOT EXISTS `srm_framework_plan_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '框架计划行编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `framework_plan_id` bigint NOT NULL COMMENT '框架计划编号',
  `material_id` bigint NOT NULL COMMENT '物料编号',
  `material_code` varchar(64) NOT NULL COMMENT '物料编码',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称',
  `quantity` decimal(18,2) NOT NULL COMMENT '数量',
  `unit` varchar(32) NOT NULL COMMENT '单位',
  `budget_amount` decimal(18,2) NOT NULL COMMENT '预算金额',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_framework_plan_line_tenant_plan` (`tenant_id`, `framework_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 框架采购计划行';

CREATE TABLE IF NOT EXISTS `srm_framework_agreement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '框架协议编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `agreement_no` varchar(64) NOT NULL COMMENT '框架协议单号',
  `framework_plan_id` bigint NOT NULL COMMENT '来源框架计划编号',
  `framework_plan_no` varchar(64) NOT NULL COMMENT '来源框架计划单号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `procurement_method` varchar(32) NOT NULL COMMENT '采购方式',
  `budget_amount` decimal(18,2) NOT NULL COMMENT '预算金额',
  `valid_start_date` date NOT NULL COMMENT '有效期开始日期',
  `valid_end_date` date NOT NULL COMMENT '有效期结束日期',
  `agreement_status` varchar(32) NOT NULL COMMENT '协议状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_framework_agreement_tenant_plan` (`tenant_id`, `framework_plan_id`, `deleted`),
  UNIQUE KEY `uk_srm_framework_agreement_tenant_no` (`tenant_id`, `agreement_no`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 框架协议';

CREATE TABLE IF NOT EXISTS `srm_framework_agreement_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '框架协议行编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `agreement_id` bigint NOT NULL COMMENT '框架协议编号',
  `framework_plan_id` bigint NOT NULL COMMENT '来源框架计划编号',
  `framework_plan_line_id` bigint NOT NULL COMMENT '来源框架计划行编号',
  `material_id` bigint NOT NULL COMMENT '物料编号',
  `material_code` varchar(64) NOT NULL COMMENT '物料编码',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称',
  `quantity` decimal(18,2) NOT NULL COMMENT '数量',
  `unit` varchar(32) NOT NULL COMMENT '单位',
  `budget_amount` decimal(18,2) NOT NULL COMMENT '预算金额',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_srm_framework_agreement_line_tenant_agreement` (`tenant_id`, `agreement_id`),
  KEY `idx_srm_framework_agreement_line_tenant_source` (`tenant_id`, `framework_plan_id`, `framework_plan_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 框架协议行';

DROP PROCEDURE IF EXISTS ensure_srm_d7_3_plan_framework;

DELIMITER $$
CREATE PROCEDURE ensure_srm_d7_3_plan_framework()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge SRM plan/framework menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991000
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '缺少 SRM D7-1 基础菜单，不能继续挂载采购计划/框架计划页面';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (
        (`id` = 991030 AND (`path` <> 'procurement-plan' OR `component` <> 'srm/procurement-plan/index' OR `component_name` <> 'SrmProcurementPlan'))
        OR (`id` = 991031 AND `permission` <> 'srm:procurement-plan:query')
        OR (`id` = 991032 AND `permission` <> 'srm:procurement-plan:create')
        OR (`id` = 991033 AND `permission` <> 'srm:procurement-plan:submit')
        OR (`id` = 991034 AND `permission` <> 'srm:procurement-plan:audit')
        OR (`id` = 991035 AND `permission` <> 'srm:procurement-plan:generate')
        OR (`id` = 991040 AND (`path` <> 'framework-plan' OR `component` <> 'srm/framework-plan/index' OR `component_name` <> 'SrmFrameworkPlan'))
        OR (`id` = 991041 AND `permission` <> 'srm:framework-plan:query')
        OR (`id` = 991042 AND `permission` <> 'srm:framework-plan:create')
        OR (`id` = 991043 AND `permission` <> 'srm:framework-plan:submit')
        OR (`id` = 991044 AND `permission` <> 'srm:framework-plan:audit')
        OR (`id` = 991045 AND `permission` <> 'srm:framework-plan:agreement')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM D7-3 clean menu id range; conflicting system_menu rows exist';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991030, '采购计划', 'srm:procurement-plan:query', 2, 3, 991000, 'procurement-plan', 'ep:list', 'srm/procurement-plan/index', 'SrmProcurementPlan', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991030 OR `component` = 'srm/procurement-plan/index' OR `permission` = 'srm:procurement-plan:query')
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991031, '采购计划查询', 'srm:procurement-plan:query', 3, 1, 991030, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991031 OR (`permission` = 'srm:procurement-plan:query' AND `type` = 3)));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991032, '采购计划新增', 'srm:procurement-plan:create', 3, 2, 991030, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991032 OR `permission` = 'srm:procurement-plan:create'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991033, '采购计划提交', 'srm:procurement-plan:submit', 3, 3, 991030, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991033 OR `permission` = 'srm:procurement-plan:submit'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991034, '采购计划审核', 'srm:procurement-plan:audit', 3, 4, 991030, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991034 OR `permission` = 'srm:procurement-plan:audit'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991035, '生成寻源项目', 'srm:procurement-plan:generate', 3, 5, 991030, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991035 OR `permission` = 'srm:procurement-plan:generate'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991040, '框架计划', 'srm:framework-plan:query', 2, 4, 991000, 'framework-plan', 'ep:document-checked', 'srm/framework-plan/index', 'SrmFrameworkPlan', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 991040 OR `component` = 'srm/framework-plan/index' OR `permission` = 'srm:framework-plan:query')
  );

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991041, '框架计划查询', 'srm:framework-plan:query', 3, 1, 991040, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991041 OR (`permission` = 'srm:framework-plan:query' AND `type` = 3)));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991042, '框架计划新增', 'srm:framework-plan:create', 3, 2, 991040, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991042 OR `permission` = 'srm:framework-plan:create'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991043, '框架计划提交', 'srm:framework-plan:submit', 3, 3, 991040, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991043 OR `permission` = 'srm:framework-plan:submit'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991044, '框架计划审核', 'srm:framework-plan:audit', 3, 4, 991040, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991044 OR `permission` = 'srm:framework-plan:audit'));

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT 991045, '生成框架协议', 'srm:framework-plan:agreement', 3, 5, 991040, '', '', '', '', 0, b'1', b'1', b'1', 'srm-d7-3', NOW(), 'srm-d7-3', NOW(), b'0'
  WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND (`id` = 991045 OR `permission` = 'srm:framework-plan:agreement'));

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `component` = 'srm/procurement-plan/index'
      AND `component_name` = 'SrmProcurementPlan'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM procurement-plan route menu for get-permission-info';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `component` = 'srm/framework-plan/index'
      AND `component_name` = 'SrmFrameworkPlan'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM framework-plan route menu for get-permission-info';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_3_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d7_3_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `id` IN (991030, 991031, 991032, 991033, 991034, 991035, 991040, 991041, 991042, 991043, 991044, 991045);

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_3_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_d7_3_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT INTO `tmp_srm_d7_3_package_menu_ids` (`package_id`, `menu_id`)
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
  CROSS JOIN `tmp_srm_d7_3_menu_ids` AS `menu`
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
    FROM `tmp_srm_d7_3_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'srm-d7-3',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
    `role`.`id`,
    `menu`.`id`,
    'srm-d7-3',
    NOW(),
    'srm-d7-3',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_srm_d7_3_menu_ids` AS `menu`
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

  INSERT INTO `srm_code_rule`
  (`tenant_id`, `rule_code`, `rule_name`, `target_form`, `prefix`, `date_pattern`, `date_segment_enabled`, `serial_width`, `step`, `min_serial`, `max_serial`, `separator`, `enabled`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT
    `tenant`.`id`,
    `seed`.`rule_code`,
    `seed`.`rule_name`,
    `seed`.`target_form`,
    `seed`.`prefix`,
    'yyyyMMdd',
    b'1',
    4,
    1,
    1,
    9999,
    '-',
    b'1',
    'SRM D7-3 默认业务编码规则',
    'srm-d7-3',
    NOW(),
    'srm-d7-3',
    NOW(),
    b'0'
  FROM `system_tenant` AS `tenant`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
   AND JSON_CONTAINS(`package`.`menu_ids`, CAST('991000' AS JSON), '$')
  JOIN (
    SELECT 'SRM_PROCUREMENT_PLAN' AS `rule_code`, '采购计划编码规则' AS `rule_name`, 'PROCUREMENT_PLAN' AS `target_form`, 'PP' AS `prefix`
    UNION ALL SELECT 'SRM_PROCUREMENT_PLAN_LINE', '采购计划行编码规则', 'PROCUREMENT_PLAN_LINE', 'PPL'
    UNION ALL SELECT 'SRM_NON_TENDER_PROJECT', '非招标项目编码规则', 'NON_TENDER_PROJECT', 'NB'
    UNION ALL SELECT 'SRM_TENDER_PROJECT', '招标项目编码规则', 'TENDER_PROJECT', 'TP'
    UNION ALL SELECT 'SRM_FRAMEWORK_PLAN', '框架计划编码规则', 'FRAMEWORK_PLAN', 'FP'
    UNION ALL SELECT 'SRM_FRAMEWORK_AGREEMENT', '框架协议编码规则', 'FRAMEWORK_AGREEMENT', 'FA'
  ) AS `seed`
  WHERE `tenant`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `srm_code_rule` AS `existing`
      WHERE `existing`.`tenant_id` = `tenant`.`id`
        AND `existing`.`target_form` = `seed`.`target_form`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_3_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_d7_3_menu_ids`;
END$$
DELIMITER ;

CALL ensure_srm_d7_3_plan_framework();

DROP PROCEDURE IF EXISTS ensure_srm_d7_3_plan_framework;
