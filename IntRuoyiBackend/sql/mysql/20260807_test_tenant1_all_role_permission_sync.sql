-- release-migration: allowedEnvironments=test; dependsOn=20260728_mes_scheduler_route_flow_list_permission; type=data; riskLevel=high
-- Purpose: align every active tenant-1 local role and effective permission with the test environment by stable keys.
-- source-active-role-count: 60
-- source-role-permission-count: 1676
-- source-missing-permission-count: 12
-- Target-only roles and all user-role bindings must remain unchanged.
-- Other-tenant role-menu rows must remain unchanged.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS sync_test_tenant1_all_role_permissions;

DELIMITER //
CREATE PROCEDURE sync_test_tenant1_all_role_permissions()
BEGIN
  DECLARE previous_menu_resolution_count int DEFAULT -1;
  DECLARE current_menu_resolution_count int DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_source`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_source` (
    `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `sort` int NOT NULL,
    `category_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `data_scope` tinyint NOT NULL,
    `data_scope_dept_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `status` tinyint NOT NULL,
    `type` tinyint NOT NULL,
    `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    PRIMARY KEY (`code`)
  );

  INSERT INTO `tmp_test_tenant1_role_source`
    (`code`, `name`, `sort`, `category_code`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`)
  VALUES
  ('体系工程师', '体系工程师', 1, 'dcc', 1, '', 0, 2, ''),
  ('approval_admin', '审批管理员', 910296, 'menu', 1, '', 0, 2, '审批中心全量可见管理员角色'),
  ('approval_center_entry', '审批中心入口', 910295, 'menu', 1, '', 0, 2, '审批中心最小入口角色'),
  ('BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_RECORD_UPLOAD_1', '成品检记录上传1', 904, 'batch-record', 1, '', 0, 2, '工序开始批记录附件默认上传角色'),
  ('BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_REPORT_UPLOAD_1', '成品检报告上传1', 903, 'batch-record', 1, '', 0, 2, '工序开始批记录附件默认上传角色'),
  ('BATCH_ATTACHMENT_INCOMING_INSPECTION_REPORT_UPLOAD_1', '来料检报告上传1', 901, 'batch-record', 1, '', 0, 2, '工序开始批记录附件默认上传角色'),
  ('BATCH_ATTACHMENT_STERILIZATION_REPORT_UPLOAD_1', '灭菌报告上传1', 902, 'batch-record', 1, '', 0, 2, '工序开始批记录附件默认上传角色'),
  ('bpm_admin', 'BPM管理员', 910311, 'menu', 1, '', 0, 2, '审批中心流程管理菜单及 BPM 配置维护权限'),
  ('codex_test_admin', '测试管理员', 910320, 'menu', 1, '', 0, 2, '测试管理角色；可维护自然语言测试项并发起、查看 Codex Playwright 自动测试。'),
  ('common', '普通角色', 2, 'menu', 2, '', 0, 1, '普通角色'),
  ('crm_admin', 'CRM 管理员', 2, 'menu', 1, '', 0, 1, 'CRM 专属角色'),
  ('dcc_action_distribute_independent', 'DCC Action Distribute', 7104, NULL, 1, '', 0, 2, 'Independent DCC formal-distribute role'),
  ('dcc_action_download_independent', 'DCC Action Download', 7102, NULL, 1, '', 0, 2, 'Independent DCC download role'),
  ('dcc_action_training_independent', 'DCC Action Training', 7103, NULL, 1, '', 0, 2, 'Independent DCC training/read-confirm role'),
  ('dcc_action_view_independent', 'DCC Action View', 7101, NULL, 1, '', 0, 2, 'Independent DCC view/preview role'),
  ('dcc_dhf_dmr_uploader', 'DCC DHF/DMR上传员', 45, 'dcc', 1, '', 0, 2, 'Codex local E2E role: DCC DHF/DMR category upload permission'),
  ('dcc_distribute_e2e', 'DCC Distribute E2E', 910431, NULL, 1, '', 0, 2, 'TASK-20260802-DCC-DISTRIBUTE-PERMISSION'),
  ('dcc_project_code_admin', '项目代码管理员', 40, 'dcc', 1, '', 0, 2, 'DCC项目代码管理：查询、新增、编辑、删除、导入、导出、分配与追溯。'),
  ('dcc_training_mine_e2e', 'DCC Training Mine E2E', 910419, NULL, 1, '', 0, 2, 'TASK-20260802-DCC-TRAINING-MINE-PERMISSION'),
  ('doc_control', '文控', 910217, 'dcc', 1, '', 0, 2, '本机 admin 文控预览编辑授权'),
  ('edhr_batch_record_admin', '批记录管理员', 65, 'batch-record', 1, '', 0, 2, 'eDHR批次记录全览管理员'),
  ('edhr_batch_void_admin', '批次执行作废管理员', 66, 'batch-record', 1, '', 0, 2, 'eDHR批次执行作废 BPM 审批角色'),
  ('edhr_golden_finger_admin', '批记录金手指管理员', 910399, 'batch-record', 1, '', 0, 2, '临时测试权限：放行前可代填并直接提交当前 eDHR 表单，绕过填写人和普通检查；不得绕过放行、关闭、作废或审批锁定，所有提交必须审计。'),
  ('edhr_route_922067_save', '批记录工艺路线保存', 999, 'batch-record', 1, '', 0, 2, 'Grant eDHR ROUTE_EDIT for route 922067'),
  ('EDITOR', '展厅编辑', 1, 'showroom', 1, '', 0, 2, '提供展厅编辑'),
  ('electronic_signature_admin', '电子签名管理员', 910418, 'dcc', 1, '', 0, 1, '仅允许查看电子签名用户授权并管理个人签名权限'),
  ('form_template_obsolete_approver', '表单模板作废审批员', 71, 'form-center', 1, '', 0, 2, '表单模板作废审批角色'),
  ('form_template_upgrade_approver', '表单模板升版审批员', 70, 'form-center', 1, '', 0, 2, '表单模板升版审批角色'),
  ('mes_route_version_admin', '工艺路线版本管理员', 65, 'batch-record', 1, '', 0, 2, '工艺路线版本审批最终审核角色'),
  ('mes_schedule_replan_approver', '排产重排审批人', 67, 'batch-record', 1, '', 0, 2, 'MES排产手动重排 BPM 审批角色'),
  ('mes_scheduler', '排产员', 5600, 'scheduling', 1, '', 0, 2, 'smart scheduling tab permission'),
  ('mes_team_leader', '班组长', 239, 'scheduling', 2, '', 0, 2, 'MES 智能排产报工班组长'),
  ('mes_workshop_director', '车间主任', 238, 'scheduling', 2, '', 0, 2, 'MES 智能排产车间主任'),
  ('pqc_leader_permission', 'PQC组长权限角色', 900435, 'menu', 1, '', 0, 2, 'PQC组长页签可见性和运行权限'),
  ('pqc_permission', 'PQC权限角色', 900435, 'batch-record', 1, '', 0, 2, 'Created for local PQC assignment task 20260806'),
  ('pressure_pump_equipment_filler', '压力泵设备填写员', 902, 'batch-record', 1, '', 0, 2, '压力泵工序填写角色'),
  ('pressure_pump_filler_01', '产品信息填写者角色', 1001, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 产品信息 默认填写人'),
  ('pressure_pump_filler_02', '粗洗工序填写者角色', 1002, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 粗洗工序生产记录 默认填写人'),
  ('pressure_pump_filler_03', '精洗工序填写者角色', 1003, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 精洗工序生产记录 默认填写人'),
  ('pressure_pump_filler_04', '清洗工序填写者角色', 1004, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 清洗工序生产记录 默认填写人'),
  ('pressure_pump_filler_05', '清洁工序填写者角色', 1005, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 清洁工序生产记录 默认填写人'),
  ('pressure_pump_filler_06', '组装Ⅰ工序填写者角色', 1006, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 组装Ⅰ工序生产记录 默认填写人'),
  ('pressure_pump_filler_07', '光固Ⅰ工序填写者角色', 1007, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 光固Ⅰ工序生产记录 默认填写人'),
  ('pressure_pump_filler_08', '硅化Ⅰ工序填写者角色', 1008, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 硅化Ⅰ工序生产记录 默认填写人'),
  ('pressure_pump_filler_09', '硅化Ⅱ工序填写者角色', 1009, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 硅化Ⅱ工序生产记录 默认填写人'),
  ('pressure_pump_filler_10', '组装Ⅱ工序填写者角色', 1010, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 组装Ⅱ工序生产记录 默认填写人'),
  ('pressure_pump_filler_11', '检测工序填写者角色', 1011, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 检测工序生产记录 默认填写人'),
  ('pressure_pump_filler_12', '光固Ⅱ工序填写者角色', 1012, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 光固Ⅱ工序生产记录 默认填写人'),
  ('pressure_pump_filler_13', '单包装工序填写者角色', 1013, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 单包装工序生产记录 默认填写人'),
  ('pressure_pump_filler_14', '中包装工序填写者角色', 1014, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 中包装工序生产记录 默认填写人'),
  ('pressure_pump_filler_15', '大包装工序填写者角色', 1015, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V14.0 大包装工序生产记录 默认填写人'),
  ('pressure_pump_production_1', '压力泵生产1', 904, 'batch-record', 1, '', 0, 2, '球囊扩张压力泵 V13.0 表单填写员角色'),
  ('pressure_pump_production_filler', '压力泵生产填写员', 901, 'batch-record', 1, '', 0, 2, '压力泵工序填写角色'),
  ('pressure_pump_quality_filler', '压力泵质量填写员', 903, 'batch-record', 1, '', 0, 2, '压力泵工序填写角色'),
  ('rd_doc_corrector', '研发文档校正员', 910350, 'dcc', 1, '', 0, 2, 'DCC 项目代码被分配文件的研发文档校正角色：项目代码只读、文件查阅依赖、我的DCC修正'),
  ('showroom_publicity', '企宣', 10, 'showroom', 1, '', 0, 2, '展厅公司信息直发角色'),
  ('srm_admin', 'SRM管理员', 910240, 'srm', 1, '', 0, 1, '仅允许查看和处理 SRM 菜单及 SRM 审批模块'),
  ('super_admin', '超级管理员', 1, 'menu', 1, '', 0, 1, '超级管理员'),
  ('wenkong', '文控', 6800, 'dcc', 1, '', 0, 2, 'DCC文控中心全操作权限'),
  ('wenkong_download', '文控下载', 6811, 'dcc', 1, '', 0, 2, '备份服芋道源码 DCC 下载最小权限角色：查询、预览、下载、审计只读');

  IF (SELECT COUNT(*) FROM `tmp_test_tenant1_role_source`) <> 60 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected tenant-1 source role count';
  END IF;

  IF EXISTS (
    SELECT `role`.`code`
    FROM `system_role` AS `role`
    JOIN `tmp_test_tenant1_role_source` AS `source`
      ON `role`.`code` = `source`.`code`
    WHERE `role`.`tenant_id` = 1 AND `role`.`deleted` = b'0'
    GROUP BY `role`.`code`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate active target role code in tenant 1';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_category_target`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_category_target` (
    `category_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `category_id` bigint NOT NULL,
    PRIMARY KEY (`category_code`)
  );

  INSERT INTO `tmp_test_tenant1_role_category_target` (`category_code`, `category_id`)
  SELECT `source`.`category_code`, MIN(`category`.`id`)
  FROM (
    SELECT DISTINCT `category_code`
    FROM `tmp_test_tenant1_role_source`
    WHERE `category_code` IS NOT NULL
  ) AS `source`
  JOIN `system_role_category` AS `category`
    ON `category`.`code` = `source`.`category_code`
   AND `category`.`tenant_id` = 1
   AND `category`.`deleted` = b'0'
   AND `category`.`status` = 0
  GROUP BY `source`.`category_code`
  HAVING COUNT(*) = 1;

  IF EXISTS (
    SELECT 1
    FROM `tmp_test_tenant1_role_source` AS `source`
    LEFT JOIN `tmp_test_tenant1_role_category_target` AS `category`
      ON `category`.`category_code` = `source`.`category_code`
    WHERE `source`.`category_code` IS NOT NULL
      AND `category`.`category_id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or duplicate target role category code';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_source`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_missing_menu_source` (
    `source_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `type` tinyint NOT NULL,
    `sort` int NOT NULL,
    `parent_source_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `parent_target_id_hint` bigint NOT NULL,
    `parent_permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `parent_type` tinyint NOT NULL,
    `parent_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `parent_component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `parent_component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `status` tinyint NOT NULL,
    `visible` tinyint NOT NULL,
    PRIMARY KEY (`source_key`)
  );

  INSERT INTO `tmp_test_tenant1_missing_menu_source`
    (`source_key`, `name`, `permission`, `type`, `sort`, `parent_source_key`, `parent_target_id_hint`,
     `parent_permission`, `parent_type`, `parent_path`, `parent_component`, `parent_component_name`,
     `path`, `component`, `component_name`, `status`, `visible`)
  VALUES
  ('local-menu-6810', 'DCC受控预览', 'dcc:controlled-file:preview', 3, 1, NULL, 6807, 'dcc:controlled-file:query', 2, 'controlled-file/browser', 'dcc/controlled-file/browser/index', 'DccControlledFileBrowser', '', '', '', 0, 1),
  ('local-menu-900304', 'eDHR版本治理确认', 'mes:pro-batch-record-version:confirm', 3, 1, NULL, 900220, 'mes:pro-edhr-batch-processing:query', 1, 'edhr-batch-processing', '', '', '', '', '', 0, 0),
  ('local-menu-900305', 'eDHR版本草稿重传', 'mes:pro-batch-record-version:import', 3, 2, NULL, 900220, 'mes:pro-edhr-batch-processing:query', 1, 'edhr-batch-processing', '', '', '', '', '', 0, 0),
  ('local-menu-900306', 'eDHR版本受控回滚', 'mes:pro-batch-record-version:rollback-request', 3, 3, NULL, 900220, 'mes:pro-edhr-batch-processing:query', 1, 'edhr-batch-processing', '', '', '', '', '', 0, 0),
  ('local-menu-900312', '班组长提交复核', 'mes:pro-process-pool-team-leader:review', 3, 2, NULL, 900436, 'mes:pro-process-pool-team-leader:query', 2, '/mes/pro/process-pool/production-leader', 'mes/pro/processpool/ProductionLeaderWorkbenchPage', 'MesProProcessPoolProductionLeaderWorkbench', '', '', '', 0, 1),
  ('local-menu-900313', '生产工单异常上报', 'mes:pro-process-pool-team-leader:abnormal', 3, 3, NULL, 900436, 'mes:pro-process-pool-team-leader:query', 2, '/mes/pro/process-pool/production-leader', 'mes/pro/processpool/ProductionLeaderWorkbenchPage', 'MesProProcessPoolProductionLeaderWorkbench', '', '', '', 0, 1),
  ('local-menu-900314', '班组基础维护', 'mes:pro-process-pool-team-leader:maintain', 3, 4, NULL, 900436, 'mes:pro-process-pool-team-leader:query', 2, '/mes/pro/process-pool/production-leader', 'mes/pro/processpool/ProductionLeaderWorkbenchPage', 'MesProProcessPoolProductionLeaderWorkbench', '', '', '', 0, 1),
  ('local-menu-991200', '分贝通凭证', 'erp:fenbeitong-voucher:query', 2, 90, NULL, 2645, '', 1, 'finance', '', '', 'fenbeitong-voucher', 'erp/finance/fenbeitong-voucher/index', 'ErpFenbeitongVoucher', 0, 1),
  ('local-menu-991201', '分贝通凭证查询', 'erp:fenbeitong-voucher:query', 3, 1, 'local-menu-991200', 991200, 'erp:fenbeitong-voucher:query', 2, 'fenbeitong-voucher', 'erp/finance/fenbeitong-voucher/index', 'ErpFenbeitongVoucher', '', '', NULL, 0, 1),
  ('local-menu-991202', '分贝通凭证配置', 'erp:fenbeitong-voucher:config', 3, 2, 'local-menu-991200', 991200, 'erp:fenbeitong-voucher:query', 2, 'fenbeitong-voucher', 'erp/finance/fenbeitong-voucher/index', 'ErpFenbeitongVoucher', '', '', NULL, 0, 1),
  ('local-menu-991203', '分贝通凭证保存', 'erp:fenbeitong-voucher:save', 3, 3, 'local-menu-991200', 991200, 'erp:fenbeitong-voucher:query', 2, 'fenbeitong-voucher', 'erp/finance/fenbeitong-voucher/index', 'ErpFenbeitongVoucher', '', '', NULL, 0, 1),
  ('local-menu-605071305', 'DCC项目代码新增', 'dcc:project-code:create', 3, 42, NULL, 990210, 'dcc:project-code:query', 2, 'project-code', 'dcc/controlled-file/basic-data/project-code/index', 'DccProjectCodeBasicDataPage', '', '', '', 0, 1),
  ('local-menu-605071306', 'DCC项目代码删除', 'dcc:project-code:delete', 3, 44, NULL, 990210, 'dcc:project-code:query', 2, 'project-code', 'dcc/controlled-file/basic-data/project-code/index', 'DccProjectCodeBasicDataPage', '', '', '', 0, 1);

  IF (SELECT COUNT(DISTINCT `permission`) FROM `tmp_test_tenant1_missing_menu_source`) <> 12 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected missing source permission count';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    WHERE EXISTS (
      SELECT 1 FROM `system_menu` AS `permission_menu`
      WHERE `permission_menu`.`permission` = `source`.`permission`
        AND `permission_menu`.`deleted` = b'0'
        AND `permission_menu`.`status` = 0
    )
    AND NOT EXISTS (
      SELECT 1 FROM `system_menu` AS `exact_menu`
      WHERE `exact_menu`.`permission` = `source`.`permission`
        AND `exact_menu`.`type` = `source`.`type`
        AND (`exact_menu`.`path` <=> `source`.`path`)
        AND (`exact_menu`.`component` <=> `source`.`component`)
        AND (`exact_menu`.`component_name` <=> `source`.`component_name`)
        AND `exact_menu`.`name` COLLATE utf8mb4_unicode_ci = `source`.`name`
        AND `exact_menu`.`deleted` = b'0'
        AND `exact_menu`.`status` = 0
    )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Conflicting target menu for source missing permission';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    LEFT JOIN `system_menu` AS `parent`
      ON `parent`.`id` = `source`.`parent_target_id_hint`
     AND `parent`.`deleted` = b'0'
     AND `parent`.`status` = 0
     AND `parent`.`type` = `source`.`parent_type`
     AND `parent`.`permission` = `source`.`parent_permission`
     AND (`parent`.`path` <=> `source`.`parent_path`)
     AND (`parent`.`component` <=> `source`.`parent_component`)
     AND (`parent`.`component_name` <=> `source`.`parent_component_name`)
    WHERE `source`.`parent_source_key` IS NULL
      AND `parent`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing stable parent contract for source menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_permission_source`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_permission_source` (
    `role_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `source_menu_type` tinyint NOT NULL,
    `source_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `source_component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `source_component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    PRIMARY KEY (`role_code`, `permission`)
  );

  INSERT INTO `tmp_test_tenant1_role_permission_source`
    (`role_code`, `permission`, `source_menu_type`, `source_path`, `source_component`, `source_component_name`)
  VALUES
  ('体系工程师', 'dcc:controlled-file:category:manage', 2, 'controlled-file/categories', 'dcc/controlled-file/categories/index', 'DccControlledFileCategories'),
  ('体系工程师', 'dcc:controlled-file:directory:manage', 2, 'controlled-file/directories', 'dcc/controlled-file/directories/index', 'DccControlledFileDirectories'),
  ('体系工程师', 'dcc:controlled-file:download', 3, '', '', ''),
  ('体系工程师', 'dcc:controlled-file:log:query', 2, 'controlled-file/logs', 'dcc/controlled-file/logs/index', 'DccControlledFileLogs'),
  ('体系工程师', 'dcc:controlled-file:position:manage', 2, 'approval-role', 'dcc/controlled-file/positions/index', 'DccControlledFilePositions'),
  ('体系工程师', 'dcc:controlled-file:preview', 3, '', '', ''),
  ('体系工程师', 'dcc:controlled-file:query', 2, 'controlled-file/browser', 'dcc/controlled-file/browser/index', 'DccControlledFileBrowser'),
  ('体系工程师', 'dcc:controlled-file:route:manage', 2, 'controlled-file/routes', 'dcc/controlled-file/routes/index', 'DccControlledFileRoutes'),
  ('体系工程师', 'dcc:controlled-file:stamp:retry', 3, '', '', ''),
  ('体系工程师', 'dcc:controlled-file:submit', 2, 'controlled-file/upload', 'dcc/controlled-file/upload/index', 'DccControlledFileUpload'),
  ('体系工程师', 'dcc:project-code-assignment:execute', 3, '', '', ''),
  ('体系工程师', 'dcc:project-code:export', 3, '', '', ''),
  ('体系工程师', 'dcc:project-code:import', 3, '', '', ''),
  ('体系工程师', 'dcc:project-code:query', 2, 'project-code', 'dcc/controlled-file/basic-data/project-code/index', 'DccProjectCodeBasicDataPage'),
  ('体系工程师', 'signature-governance:policy:query', 2, '/signature-governance', 'signature-governance/index', 'SignatureGovernanceWorkbench'),
  ('approval_admin', 'bpm:process-instance-cc:query', 2, 'cc', 'approval-center/index', 'ApprovalCenterCc'),
  ('approval_admin', 'bpm:task:query', 3, '', '', NULL),
  ('approval_admin', 'bpm:task:update', 3, '', '', NULL),
  ('approval_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('approval_center_entry', 'bpm:process-instance-cc:query', 2, 'cc', 'approval-center/index', 'ApprovalCenterCc'),
  ('approval_center_entry', 'bpm:process-instance:query', 3, '', '', NULL),
  ('approval_center_entry', 'bpm:task:query', 3, '', '', NULL),
  ('approval_center_entry', 'bpm:task:update', 3, '', '', NULL),
  ('approval_center_entry', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_RECORD_UPLOAD_1', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_REPORT_UPLOAD_1', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('BATCH_ATTACHMENT_INCOMING_INSPECTION_REPORT_UPLOAD_1', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('BATCH_ATTACHMENT_STERILIZATION_REPORT_UPLOAD_1', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('bpm_admin', 'bpm:business-approval-policy:create', 3, '', '', ''),
  ('bpm_admin', 'bpm:business-approval-policy:disable', 3, '', '', ''),
  ('bpm_admin', 'bpm:business-approval-policy:publish', 3, '', '', ''),
  ('bpm_admin', 'bpm:business-approval-policy:query', 2, 'business-approval-policy', 'bpm/businessApprovalPolicy/index', 'BpmBusinessApprovalPolicy'),
  ('bpm_admin', 'bpm:category:create', 3, '', '', ''),
  ('bpm_admin', 'bpm:category:delete', 3, '', '', ''),
  ('bpm_admin', 'bpm:category:query', 3, '', '', ''),
  ('bpm_admin', 'bpm:category:update', 3, '', '', ''),
  ('bpm_admin', 'bpm:form:create', 3, '', '', NULL),
  ('bpm_admin', 'bpm:form:delete', 3, '', '', NULL),
  ('bpm_admin', 'bpm:form:export', 3, '', '', NULL),
  ('bpm_admin', 'bpm:form:query', 3, '', '', NULL),
  ('bpm_admin', 'bpm:form:update', 3, '', '', NULL),
  ('bpm_admin', 'bpm:model:clean', 3, '', '', ''),
  ('bpm_admin', 'bpm:model:create', 3, '', '', NULL),
  ('bpm_admin', 'bpm:model:delete', 3, '', '', NULL),
  ('bpm_admin', 'bpm:model:deploy', 3, '', '', NULL),
  ('bpm_admin', 'bpm:model:query', 3, '', '', NULL),
  ('bpm_admin', 'bpm:model:update', 3, '', '', NULL),
  ('bpm_admin', 'bpm:process-expression:create', 3, '', '', NULL),
  ('bpm_admin', 'bpm:process-expression:delete', 3, '', '', NULL),
  ('bpm_admin', 'bpm:process-expression:query', 3, '', '', NULL),
  ('bpm_admin', 'bpm:process-expression:update', 3, '', '', NULL),
  ('bpm_admin', 'bpm:process-instance-cc:query', 2, 'cc', 'approval-center/index', 'ApprovalCenterCc'),
  ('bpm_admin', 'bpm:task:query', 3, '', '', NULL),
  ('bpm_admin', 'bpm:user-group:create', 3, '', '', NULL),
  ('bpm_admin', 'bpm:user-group:delete', 3, '', '', NULL),
  ('bpm_admin', 'bpm:user-group:query', 3, '', '', NULL),
  ('bpm_admin', 'bpm:user-group:update', 3, '', '', NULL),
  ('bpm_admin', 'form:instance:abandon', 3, '', '', ''),
  ('bpm_admin', 'form:instance:create', 3, '', '', ''),
  ('bpm_admin', 'form:instance:submit', 3, '', '', ''),
  ('bpm_admin', 'form:instance:update', 3, '', '', ''),
  ('bpm_admin', 'form:policy:create', 3, '', '', ''),
  ('bpm_admin', 'form:policy:publish', 3, '', '', ''),
  ('bpm_admin', 'form:policy:query', 2, 'policy', 'form-center/policy/index', 'FormCenterPolicy'),
  ('bpm_admin', 'form:template-source:download', 3, '', '', ''),
  ('bpm_admin', 'form:template:create', 3, '', '', ''),
  ('bpm_admin', 'form:template:disable', 3, '', '', ''),
  ('bpm_admin', 'form:template:obsolete', 3, '', '', ''),
  ('bpm_admin', 'form:template:publish', 3, '', '', ''),
  ('bpm_admin', 'form:template:query', 2, 'template', 'form-center/template/index', 'FormCenterTemplate'),
  ('bpm_admin', 'form:template:update', 3, '', '', ''),
  ('bpm_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('codex_test_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('codex_test_admin', 'system:codex-test:artifact', 3, '', '', ''),
  ('codex_test_admin', 'system:codex-test:cancel', 3, '', '', ''),
  ('codex_test_admin', 'system:codex-test:create', 3, '', '', ''),
  ('codex_test_admin', 'system:codex-test:delete', 3, '', '', ''),
  ('codex_test_admin', 'system:codex-test:execute', 3, '', '', ''),
  ('codex_test_admin', 'system:codex-test:query', 2, 'codex-test-record', 'system/codex-test-record/index', 'SystemCodexTestRecord'),
  ('codex_test_admin', 'system:codex-test:update', 3, '', '', ''),
  ('common', 'bpm:oa-leave:create', 3, '', '', NULL),
  ('common', 'bpm:oa-leave:query', 3, '', '', NULL),
  ('common', 'bpm:process-definition:query', 3, '', '', NULL),
  ('common', 'bpm:process-instance-cc:query', 2, 'cc', 'approval-center/index', 'ApprovalCenterCc'),
  ('common', 'bpm:process-instance:cancel', 3, '', '', NULL),
  ('common', 'bpm:process-instance:cancel-by-admin', 3, '', '', ''),
  ('common', 'bpm:process-instance:create', 3, '', '', NULL),
  ('common', 'bpm:process-instance:manager-query', 3, '', '', ''),
  ('common', 'bpm:process-instance:query', 3, '', '', NULL),
  ('common', 'bpm:process-listener:create', 3, '', '', NULL),
  ('common', 'bpm:process-listener:delete', 3, '', '', NULL),
  ('common', 'bpm:process-listener:query', 3, '', '', NULL),
  ('common', 'bpm:process-listener:update', 3, '', '', NULL),
  ('common', 'bpm:task-assign-rule:create', 3, '', '', NULL),
  ('common', 'bpm:task-assign-rule:query', 3, '', '', NULL),
  ('common', 'bpm:task-assign-rule:update', 3, '', '', NULL),
  ('common', 'bpm:task:manager-query', 3, '', '', ''),
  ('common', 'bpm:task:query', 3, '', '', NULL),
  ('common', 'bpm:task:update', 3, '', '', NULL),
  ('common', 'infra:api-access-log:export', 3, '', '', NULL),
  ('common', 'infra:api-access-log:query', 3, '', '', NULL),
  ('common', 'infra:api-error-log:export', 3, '', '', NULL),
  ('common', 'infra:api-error-log:query', 2, 'api-error-log', 'infra/apiErrorLog/index', 'InfraApiErrorLog'),
  ('common', 'infra:api-error-log:update-status', 3, '', '', NULL),
  ('common', 'infra:build:list', 2, 'build', 'infra/build/index', 'InfraBuild'),
  ('common', 'infra:codegen:create', 3, '', '', NULL),
  ('common', 'infra:codegen:delete', 3, '', '', NULL),
  ('common', 'infra:codegen:download', 3, '', '', NULL),
  ('common', 'infra:codegen:preview', 3, '', '', NULL),
  ('common', 'infra:codegen:query', 2, 'codegen', 'infra/codegen/index', 'InfraCodegen'),
  ('common', 'infra:codegen:update', 3, '', '', NULL),
  ('common', 'infra:config:create', 3, '', '', NULL),
  ('common', 'infra:config:delete', 3, '', '', NULL),
  ('common', 'infra:config:export', 3, '', '', NULL),
  ('common', 'infra:config:query', 3, '', '', NULL),
  ('common', 'infra:config:update', 3, '', '', NULL),
  ('common', 'infra:data-source-config:create', 3, '', '', NULL),
  ('common', 'infra:data-source-config:delete', 3, '', '', NULL),
  ('common', 'infra:data-source-config:export', 3, '', '', NULL),
  ('common', 'infra:data-source-config:query', 3, '', '', NULL),
  ('common', 'infra:data-source-config:update', 3, '', '', NULL),
  ('common', 'infra:file-config:create', 3, '', '', NULL),
  ('common', 'infra:file-config:delete', 3, '', '', NULL),
  ('common', 'infra:file-config:export', 3, '', '', NULL),
  ('common', 'infra:file-config:query', 3, '', '', NULL),
  ('common', 'infra:file-config:update', 3, '', '', NULL),
  ('common', 'infra:file:delete', 3, '', '', NULL),
  ('common', 'infra:file:query', 3, '', '', NULL),
  ('common', 'infra:job:create', 3, '', '', NULL),
  ('common', 'infra:job:delete', 3, '', '', NULL),
  ('common', 'infra:job:export', 3, '', '', NULL),
  ('common', 'infra:job:query', 3, '', '', NULL),
  ('common', 'infra:job:trigger', 3, '', '', NULL),
  ('common', 'infra:job:update', 3, '', '', NULL),
  ('common', 'infra:redis:get-key-list', 3, '', '', NULL),
  ('common', 'infra:redis:get-monitor-info', 3, '', '', NULL),
  ('common', 'infra:swagger:list', 2, 'swagger', 'infra/swagger/index', 'InfraSwagger'),
  ('common', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('common', 'mes:wm-product-issue:finish', 3, '', '', NULL),
  ('common', 'pay:app:create', 3, '', '', NULL),
  ('common', 'pay:app:delete', 3, '', '', NULL),
  ('common', 'pay:app:query', 3, '', '', NULL),
  ('common', 'pay:app:update', 3, '', '', NULL),
  ('common', 'pay:channel:parsing', 3, '', '', NULL),
  ('common', 'pay:merchant:create', 3, '', '', NULL),
  ('common', 'pay:merchant:delete', 3, '', '', NULL),
  ('common', 'pay:merchant:export', 3, '', '', NULL),
  ('common', 'pay:merchant:query', 3, '', '', NULL),
  ('common', 'pay:merchant:update', 3, '', '', NULL),
  ('common', 'pay:order:export', 3, '', '', NULL),
  ('common', 'pay:order:query', 3, '', '', NULL),
  ('common', 'pay:refund:export', 3, '', '', NULL),
  ('common', 'pay:refund:query', 3, '', '', NULL),
  ('common', 'product:brand:create', 3, '', '', NULL),
  ('common', 'product:brand:delete', 3, '', '', NULL),
  ('common', 'product:brand:query', 3, '', '', NULL),
  ('common', 'product:brand:update', 3, '', '', NULL),
  ('common', 'product:category:create', 3, '', '', NULL),
  ('common', 'product:category:delete', 3, '', '', NULL),
  ('common', 'product:category:query', 3, '', '', NULL),
  ('common', 'product:category:update', 3, '', '', NULL),
  ('common', 'product:property:create', 3, '', '', NULL),
  ('common', 'product:property:delete', 3, '', '', NULL),
  ('common', 'product:property:query', 3, '', '', NULL),
  ('common', 'product:property:update', 3, '', '', NULL),
  ('common', 'product:spu:create', 3, '', '', NULL),
  ('common', 'product:spu:delete', 3, '', '', NULL),
  ('common', 'product:spu:query', 3, '', '', NULL),
  ('common', 'product:spu:update', 3, '', '', NULL),
  ('common', 'promotion:banner:create', 3, '', '', ''),
  ('common', 'promotion:banner:delete', 3, '', '', ''),
  ('common', 'promotion:banner:update', 3, '', '', ''),
  ('common', 'system:dept:create', 3, '', '', NULL),
  ('common', 'system:dept:delete', 3, '', '', NULL),
  ('common', 'system:dept:query', 3, '', '', NULL),
  ('common', 'system:dept:update', 3, '', '', NULL),
  ('common', 'system:dict:create', 3, '', '', NULL),
  ('common', 'system:dict:delete', 3, '', '', NULL),
  ('common', 'system:dict:export', 3, '#', '', NULL),
  ('common', 'system:dict:query', 3, '#', '', NULL),
  ('common', 'system:dict:update', 3, '', '', NULL),
  ('common', 'system:login-log:export', 3, '#', '', NULL),
  ('common', 'system:login-log:query', 3, '#', '', NULL),
  ('common', 'system:menu:query', 3, '', '', NULL),
  ('common', 'system:menu:update', 3, '', '', NULL),
  ('common', 'system:notice:create', 3, '', '', NULL),
  ('common', 'system:notice:delete', 3, '', '', NULL),
  ('common', 'system:notice:query', 3, '#', '', NULL),
  ('common', 'system:notice:update', 3, '', '', NULL),
  ('common', 'system:oauth2-client:create', 3, '', '', NULL),
  ('common', 'system:oauth2-client:delete', 3, '', '', NULL),
  ('common', 'system:oauth2-client:query', 3, '', '', NULL),
  ('common', 'system:oauth2-client:update', 3, '', '', NULL),
  ('common', 'system:oauth2-token:delete', 3, '', '', NULL),
  ('common', 'system:oauth2-token:page', 3, '', '', NULL),
  ('common', 'system:operate-log:export', 3, '', '', NULL),
  ('common', 'system:operate-log:query', 3, '', '', NULL),
  ('common', 'system:permission:assign-role-data-scope', 3, '', '', NULL),
  ('common', 'system:permission:assign-role-menu', 3, '', '', NULL),
  ('common', 'system:permission:assign-user-role', 3, '', '', NULL),
  ('common', 'system:post:create', 3, '', '', NULL),
  ('common', 'system:post:delete', 3, '', '', NULL),
  ('common', 'system:post:export', 3, '', '', NULL),
  ('common', 'system:post:query', 3, '', '', NULL),
  ('common', 'system:post:update', 3, '', '', NULL),
  ('common', 'system:role:create', 3, '', '', NULL),
  ('common', 'system:role:delete', 3, '', '', NULL),
  ('common', 'system:role:export', 3, '', '', NULL),
  ('common', 'system:role:query', 3, '', '', NULL),
  ('common', 'system:role:update', 3, '', '', NULL),
  ('common', 'system:sms-channel:create', 3, '', '', NULL),
  ('common', 'system:sms-channel:delete', 3, '', '', NULL),
  ('common', 'system:sms-channel:query', 3, '', '', NULL),
  ('common', 'system:sms-channel:update', 3, '', '', NULL),
  ('common', 'system:sms-log:export', 3, '', '', NULL),
  ('common', 'system:sms-log:query', 3, '', '', NULL),
  ('common', 'system:sms-template:create', 3, '', '', NULL),
  ('common', 'system:sms-template:delete', 3, '', '', NULL),
  ('common', 'system:sms-template:export', 3, '', '', NULL),
  ('common', 'system:sms-template:query', 3, '', '', NULL),
  ('common', 'system:sms-template:send-sms', 3, '', '', NULL),
  ('common', 'system:sms-template:update', 3, '', '', NULL),
  ('common', 'system:tenant-package:create', 3, '', '', NULL),
  ('common', 'system:tenant-package:delete', 3, '', '', NULL),
  ('common', 'system:tenant-package:query', 3, '', '', NULL),
  ('common', 'system:tenant-package:update', 3, '', '', NULL),
  ('common', 'system:tenant:create', 3, '', '', NULL),
  ('common', 'system:tenant:delete', 3, '', '', NULL),
  ('common', 'system:tenant:export', 3, '', '', NULL),
  ('common', 'system:tenant:query', 3, '', '', NULL),
  ('common', 'system:tenant:update', 3, '', '', NULL),
  ('common', 'system:user:create', 3, '', '', NULL),
  ('common', 'system:user:delete', 3, '', '', NULL),
  ('common', 'system:user:export', 3, '', '', NULL),
  ('common', 'system:user:import', 3, '', '', NULL),
  ('common', 'system:user:list', 2, 'user', 'system/user/index', 'SystemUser'),
  ('common', 'system:user:query', 3, '', '', NULL),
  ('common', 'system:user:update', 3, '', '', NULL),
  ('common', 'system:user:update-password', 3, '', '', NULL),
  ('crm_admin', 'mes:wm-product-issue:finish', 3, '', '', NULL),
  ('dcc_action_download_independent', 'dcc:controlled-file:download', 3, '', '', ''),
  ('dcc_action_training_independent', 'dcc:controlled-file:training:mine', 2, 'controlled-file/training-mine', 'dcc/controlled-file/training/mine/index', 'DccControlledFileTrainingMine'),
  ('dcc_action_view_independent', 'dcc:controlled-file:preview', 3, '', '', ''),
  ('dcc_action_view_independent', 'dcc:controlled-file:query', 2, 'controlled-file/browser', 'dcc/controlled-file/browser/index', 'DccControlledFileBrowser'),
  ('dcc_dhf_dmr_uploader', 'dcc:controlled-file:submit', 2, 'controlled-file/upload', 'dcc/controlled-file/upload/index', 'DccControlledFileUpload'),
  ('dcc_dhf_dmr_uploader', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:controlled-file:log:query', 2, 'controlled-file/logs', 'dcc/controlled-file/logs/index', 'DccControlledFileLogs'),
  ('dcc_project_code_admin', 'dcc:project-code-assignment:assign', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code-assignment:audit:query', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code-assignment:execute', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code-assignment:query', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code-assignment:revoke', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code:create', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code:delete', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code:export', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code:import', 3, '', '', ''),
  ('dcc_project_code_admin', 'dcc:project-code:query', 2, 'project-code', 'dcc/controlled-file/basic-data/project-code/index', 'DccProjectCodeBasicDataPage'),
  ('dcc_project_code_admin', 'dcc:project-code:update', 3, '', '', ''),
  ('dcc_project_code_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('dcc_training_mine_e2e', 'dcc:controlled-file:training:mine', 2, 'controlled-file/training-mine', 'dcc/controlled-file/training/mine/index', 'DccControlledFileTrainingMine'),
  ('doc_control', 'dcc:controlled-file:print', 3, '', '', ''),
  ('doc_control', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('edhr_batch_record_admin', 'bpm:task:query', 3, '', '', NULL),
  ('edhr_batch_record_admin', 'bpm:task:update', 3, '', '', NULL),
  ('edhr_batch_record_admin', 'mes:pro-edhr-batch-execution:overview', 3, '', '', ''),
  ('edhr_batch_record_admin', 'mes:pro-edhr-operation-audit:query', 2, '/mes/pro/feedback/edhr-operation-audit', 'mes/pro/edhr/OperationAuditPage', 'MesProEdhrOperationAuditPage'),
  ('edhr_batch_record_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('edhr_batch_void_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('edhr_golden_finger_admin', 'mes:pro-batch-record-execution:golden-finger', 3, '', '', ''),
  ('edhr_golden_finger_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('edhr_route_922067_save', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('electronic_signature_admin', 'dcc:controlled-file:signature:manage', 2, 'authorizations', 'signature-governance/index', 'SignatureGovernanceAuthorizations'),
  ('electronic_signature_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('electronic_signature_admin', 'signature-governance:policy:query', 2, '/signature-governance', 'signature-governance/index', 'SignatureGovernanceWorkbench'),
  ('form_template_obsolete_approver', 'bpm:task:query', 3, '', '', NULL),
  ('form_template_obsolete_approver', 'bpm:task:update', 3, '', '', NULL),
  ('form_template_obsolete_approver', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('form_template_upgrade_approver', 'bpm:task:query', 3, '', '', NULL),
  ('form_template_upgrade_approver', 'bpm:task:update', 3, '', '', NULL),
  ('form_template_upgrade_approver', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('mes_route_version_admin', 'bpm:task:query', 3, '', '', NULL),
  ('mes_route_version_admin', 'bpm:task:update', 3, '', '', NULL),
  ('mes_route_version_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('mes_schedule_replan_approver', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('mes_scheduler', 'mes:dv-machinery-type:query', 3, '', '', ''),
  ('mes_scheduler', 'mes:dv-machinery:query', 3, '', '', ''),
  ('mes_scheduler', 'mes:home:query', 2, '/mes/home/index', 'mes/home/index', 'MesHome'),
  ('mes_scheduler', 'mes:md-workshop:query', 3, '', '', ''),
  ('mes_scheduler', 'mes:md-workstation:query', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-auto-schedule:apply', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-auto-schedule:preview', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-auto-schedule:replan', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-feedback:approve', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-feedback:create', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-feedback:export', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-feedback:query', 2, '/mes/pro/feedback', 'mes/pro/feedback/index', 'MesProFeedback'),
  ('mes_scheduler', 'mes:pro-feedback:update', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-mes-process:query', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-route:query', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-route:schedule-config:query', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-route:schedule-config:update', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-route:update', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-route:version-query', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-schedule-order:admission-diff', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-schedule-order:create', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-schedule-order:manual-finish', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-schedule-order:preflight', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-schedule-order:query', 2, '/mes/pro/schedule-order', 'mes/pro/scheduleorder/index', 'MesProScheduleOrder'),
  ('mes_scheduler', 'mes:pro-schedule-order:update', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-scheduler-workbench:query', 2, '/mes/pro/scheduler-workbench', 'mes/pro/scheduler-workbench/index', 'MesProSchedulerWorkbench'),
  ('mes_scheduler', 'mes:pro-scheduler-workbench:update', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-smart-scheduling:query', 1, 'smart-scheduling', '', ''),
  ('mes_scheduler', 'mes:pro-task:query', 2, '/mes/pro/schedule-calendar', 'mes/pro/task/calendar/index', 'MesProScheduleCalendar'),
  ('mes_scheduler', 'mes:pro-work-order:create', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-work-order:create-erp', 3, '', '', NULL),
  ('mes_scheduler', 'mes:pro-work-order:export', 3, '', '', ''),
  ('mes_scheduler', 'mes:pro-work-order:query', 3, '', '', ''),
  ('mes_team_leader', 'mes:pro-feedback:create', 3, '', '', ''),
  ('mes_team_leader', 'mes:pro-feedback:query', 2, '/mes/pro/feedback', 'mes/pro/feedback/index', 'MesProFeedback'),
  ('mes_team_leader', 'mes:pro-feedback:update', 3, '', '', ''),
  ('mes_team_leader', 'mes:pro-mes-process:query', 3, '', '', ''),
  ('mes_team_leader', 'mes:pro-route:query', 3, '', '', ''),
  ('mes_team_leader', 'mes:pro-route:version-query', 3, '', '', ''),
  ('mes_team_leader', 'mes:pro-smart-scheduling:query', 1, 'smart-scheduling', '', ''),
  ('mes_team_leader', 'mes:pro-work-order:query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:dv-machinery-type:query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:dv-machinery:query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:md-workshop:query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:md-workstation:query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:pro-feedback:query', 2, '/mes/pro/feedback', 'mes/pro/feedback/index', 'MesProFeedback'),
  ('mes_workshop_director', 'mes:pro-mes-process:query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:pro-route:query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:pro-route:schedule-config:query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:pro-route:version-query', 3, '', '', ''),
  ('mes_workshop_director', 'mes:pro-schedule-order:query', 2, '/mes/pro/schedule-order', 'mes/pro/scheduleorder/index', 'MesProScheduleOrder'),
  ('mes_workshop_director', 'mes:pro-smart-scheduling:query', 1, 'smart-scheduling', '', ''),
  ('mes_workshop_director', 'mes:pro-work-order:query', 3, '', '', ''),
  ('pqc_leader_permission', 'mes:pro-edhr-batch-processing:query', 1, 'edhr-batch-processing', '', ''),
  ('pqc_leader_permission', 'mes:pro-edhr-nonconformance-review:create', 3, '', '', ''),
  ('pqc_leader_permission', 'mes:pro-edhr-nonconformance-review:query', 3, '', '', ''),
  ('pqc_leader_permission', 'mes:pro-process-pool-pqc-leader:query', 2, '/mes/pro/process-pool/pqc-leader', 'mes/pro/processpool/PqcLeaderWorkbenchPage', 'MesProProcessPoolPqcLeaderWorkbench'),
  ('pqc_leader_permission', 'mes:pro-process-pool-team-leader:abnormal', 3, '', '', ''),
  ('pqc_leader_permission', 'mes:pro-process-pool-team-leader:maintain', 3, '', '', ''),
  ('pqc_leader_permission', 'mes:pro-process-pool-team-leader:query', 3, '', '', ''),
  ('pqc_leader_permission', 'mes:pro-process-pool-team-leader:review', 3, '', '', ''),
  ('pqc_permission', 'mes:pro-edhr-batch-execution:query', 2, '/mes/pro/feedback/edhr-batch-pqc-fill', 'mes/pro/edhr-batch/BatchPqcFillPage', 'MesProEdhrBatchPqcFill'),
  ('pqc_permission', 'mes:pro-edhr-batch-processing:query', 1, 'edhr-batch-processing', '', ''),
  ('rd_doc_corrector', 'dcc:controlled-file:log:query', 2, 'controlled-file/logs', 'dcc/controlled-file/logs/index', 'DccControlledFileLogs'),
  ('rd_doc_corrector', 'dcc:controlled-file:query', 2, 'controlled-file/browser', 'dcc/controlled-file/browser/index', 'DccControlledFileBrowser'),
  ('rd_doc_corrector', 'dcc:project-code-assignment:execute', 3, '', '', ''),
  ('rd_doc_corrector', 'dcc:project-code:query', 2, 'project-code', 'dcc/controlled-file/basic-data/project-code/index', 'DccProjectCodeBasicDataPage'),
  ('showroom_publicity', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('srm_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('srm_admin', 'srm:code-rule:query', 2, 'code-rule', 'srm/code-rule/index', 'SrmCodeRule'),
  ('srm_admin', 'srm:framework-plan:agreement', 3, '', '', ''),
  ('srm_admin', 'srm:framework-plan:audit', 3, '', '', ''),
  ('srm_admin', 'srm:framework-plan:create', 3, '', '', ''),
  ('srm_admin', 'srm:framework-plan:query', 2, 'framework-plan', 'srm/framework-plan/index', 'SrmFrameworkPlan'),
  ('srm_admin', 'srm:framework-plan:submit', 3, '', '', ''),
  ('srm_admin', 'srm:nas-locator:config', 3, '', '', ''),
  ('srm_admin', 'srm:nas-locator:download', 3, '', '', ''),
  ('srm_admin', 'srm:nas-locator:query', 3, '', '', ''),
  ('srm_admin', 'srm:nas-locator:refresh', 3, '', '', ''),
  ('srm_admin', 'srm:non-bidding-project:contract', 3, '', '', ''),
  ('srm_admin', 'srm:non-bidding-project:deal', 3, '', '', ''),
  ('srm_admin', 'srm:non-bidding-project:publish', 3, '', '', ''),
  ('srm_admin', 'srm:non-bidding-project:query', 2, 'non-bidding-project', 'srm/non-bidding-project/index', 'SrmNonBiddingProject'),
  ('srm_admin', 'srm:non-bidding-project:quote', 3, '', '', ''),
  ('srm_admin', 'srm:outsource-execution:create', 3, '', '', ''),
  ('srm_admin', 'srm:outsource-execution:query', 3, '', '', ''),
  ('srm_admin', 'srm:outsource-execution:update', 3, '', '', ''),
  ('srm_admin', 'srm:payment-execution:approve', 3, '', '', ''),
  ('srm_admin', 'srm:payment-execution:create', 3, '', '', ''),
  ('srm_admin', 'srm:payment-execution:query', 3, '', '', ''),
  ('srm_admin', 'srm:procurement-contract:cancel', 3, '', '', ''),
  ('srm_admin', 'srm:procurement-contract:create', 3, '', '', ''),
  ('srm_admin', 'srm:procurement-contract:delete', 3, '', '', ''),
  ('srm_admin', 'srm:procurement-contract:query', 3, '', '', ''),
  ('srm_admin', 'srm:procurement-plan:audit', 3, '', '', ''),
  ('srm_admin', 'srm:procurement-plan:create', 3, '', '', ''),
  ('srm_admin', 'srm:procurement-plan:generate', 3, '', '', ''),
  ('srm_admin', 'srm:procurement-plan:query', 2, 'procurement-plan', 'srm/procurement-plan/index', 'SrmProcurementPlan'),
  ('srm_admin', 'srm:procurement-plan:submit', 3, '', '', ''),
  ('srm_admin', 'srm:purchase-order:create', 3, '', '', ''),
  ('srm_admin', 'srm:purchase-order:query', 3, '', '', ''),
  ('srm_admin', 'srm:supplier-access:query', 2, 'access', 'srm/supplier-access/index', 'SrmSupplierAccess'),
  ('srm_admin', 'srm:supplier-portal:audit', 3, '', '', ''),
  ('srm_admin', 'srm:supplier-portal:review', 2, 'supplier-portal-review', 'srm/supplier-portal/review/index', 'SrmSupplierPortalReview'),
  ('srm_admin', 'srm:supplier-profile:query', 2, 'profile', 'srm/supplier-profile/index', 'SrmSupplierProfile'),
  ('srm_admin', 'srm:supplier-risk:query', 2, 'risk', 'srm/supplier-risk/index', 'SrmSupplierRisk'),
  ('srm_admin', 'srm:tender-project:candidate', 3, '', '', ''),
  ('srm_admin', 'srm:tender-project:committee', 3, '', '', ''),
  ('srm_admin', 'srm:tender-project:expert', 3, '', '', ''),
  ('srm_admin', 'srm:tender-project:publish', 3, '', '', ''),
  ('srm_admin', 'srm:tender-project:query', 3, '', '', ''),
  ('srm_admin', 'srm:tender-project:submit-bid', 3, '', '', ''),
  ('srm_admin', 'srm:tender-project:winning', 3, '', '', ''),
  ('super_admin', 'ai:api-key:create', 3, '', '', ''),
  ('super_admin', 'ai:api-key:delete', 3, '', '', ''),
  ('super_admin', 'ai:api-key:query', 3, '', '', ''),
  ('super_admin', 'ai:api-key:update', 3, '', '', ''),
  ('super_admin', 'ai:chat-conversation:delete', 3, '', '', ''),
  ('super_admin', 'ai:chat-conversation:query', 3, '', '', ''),
  ('super_admin', 'ai:chat-message:delete', 3, '', '', ''),
  ('super_admin', 'ai:chat-message:query', 3, '', '', ''),
  ('super_admin', 'ai:chat-role:create', 3, '', '', NULL),
  ('super_admin', 'ai:chat-role:delete', 3, '', '', ''),
  ('super_admin', 'ai:chat-role:query', 3, '', '', NULL),
  ('super_admin', 'ai:chat-role:update', 3, '', '', NULL),
  ('super_admin', 'ai:image:delete', 3, '', '', ''),
  ('super_admin', 'ai:image:query', 3, '', '', ''),
  ('super_admin', 'ai:image:update', 3, '', '', ''),
  ('super_admin', 'ai:knowledge:create', 3, '', '', NULL),
  ('super_admin', 'ai:knowledge:delete', 3, '', '', NULL),
  ('super_admin', 'ai:knowledge:query', 3, '', '', NULL),
  ('super_admin', 'ai:knowledge:update', 3, '', '', NULL),
  ('super_admin', 'ai:mind-map:delete', 3, '', '', NULL),
  ('super_admin', 'ai:mind-map:query', 3, '', '', NULL),
  ('super_admin', 'ai:model:create', 3, '', '', ''),
  ('super_admin', 'ai:model:delete', 3, '', '', ''),
  ('super_admin', 'ai:model:query', 3, '', '', ''),
  ('super_admin', 'ai:model:update', 3, '', '', ''),
  ('super_admin', 'ai:music:delete', 3, '', '', NULL),
  ('super_admin', 'ai:music:query', 3, '', '', NULL),
  ('super_admin', 'ai:music:update', 3, '', '', NULL),
  ('super_admin', 'ai:tool:create', 3, '', '', NULL),
  ('super_admin', 'ai:tool:delete', 3, '', '', NULL),
  ('super_admin', 'ai:tool:query', 3, '', '', NULL),
  ('super_admin', 'ai:tool:update', 3, '', '', NULL),
  ('super_admin', 'ai:workflow:create', 3, '', '', ''),
  ('super_admin', 'ai:workflow:delete', 3, '', '', ''),
  ('super_admin', 'ai:workflow:query', 3, '', '', ''),
  ('super_admin', 'ai:workflow:test', 3, '', '', ''),
  ('super_admin', 'ai:workflow:update', 3, '', '', ''),
  ('super_admin', 'ai:write:delete', 3, '', '', NULL),
  ('super_admin', 'ai:write:query', 3, '', '', NULL),
  ('super_admin', 'bpm:business-approval-policy:create', 3, '', '', ''),
  ('super_admin', 'bpm:business-approval-policy:disable', 3, '', '', ''),
  ('super_admin', 'bpm:business-approval-policy:publish', 3, '', '', ''),
  ('super_admin', 'bpm:business-approval-policy:query', 2, 'business-approval-policy', 'bpm/businessApprovalPolicy/index', 'BpmBusinessApprovalPolicy'),
  ('super_admin', 'bpm:category:create', 3, '', '', ''),
  ('super_admin', 'bpm:category:delete', 3, '', '', ''),
  ('super_admin', 'bpm:category:query', 3, '', '', ''),
  ('super_admin', 'bpm:category:update', 3, '', '', ''),
  ('super_admin', 'bpm:form:create', 3, '', '', NULL),
  ('super_admin', 'bpm:form:delete', 3, '', '', NULL),
  ('super_admin', 'bpm:form:export', 3, '', '', NULL),
  ('super_admin', 'bpm:form:query', 3, '', '', NULL),
  ('super_admin', 'bpm:form:update', 3, '', '', NULL),
  ('super_admin', 'bpm:model:clean', 3, '', '', ''),
  ('super_admin', 'bpm:model:create', 3, '', '', NULL),
  ('super_admin', 'bpm:model:delete', 3, '', '', NULL),
  ('super_admin', 'bpm:model:deploy', 3, '', '', NULL),
  ('super_admin', 'bpm:model:query', 3, '', '', NULL),
  ('super_admin', 'bpm:model:update', 3, '', '', NULL),
  ('super_admin', 'bpm:oa-leave:create', 3, '', '', NULL),
  ('super_admin', 'bpm:oa-leave:query', 3, '', '', NULL),
  ('super_admin', 'bpm:process-definition:query', 3, '', '', NULL),
  ('super_admin', 'bpm:process-expression:create', 3, '', '', NULL),
  ('super_admin', 'bpm:process-expression:delete', 3, '', '', NULL),
  ('super_admin', 'bpm:process-expression:query', 3, '', '', NULL),
  ('super_admin', 'bpm:process-expression:update', 3, '', '', NULL),
  ('super_admin', 'bpm:process-instance-cc:query', 2, 'cc', 'approval-center/index', 'ApprovalCenterCc'),
  ('super_admin', 'bpm:process-instance:cancel', 3, '', '', NULL),
  ('super_admin', 'bpm:process-instance:cancel-by-admin', 3, '', '', ''),
  ('super_admin', 'bpm:process-instance:create', 3, '', '', NULL),
  ('super_admin', 'bpm:process-instance:manager-query', 3, '', '', ''),
  ('super_admin', 'bpm:process-instance:query', 3, '', '', NULL),
  ('super_admin', 'bpm:process-listener:create', 3, '', '', NULL),
  ('super_admin', 'bpm:process-listener:delete', 3, '', '', NULL),
  ('super_admin', 'bpm:process-listener:query', 3, '', '', NULL),
  ('super_admin', 'bpm:process-listener:update', 3, '', '', NULL),
  ('super_admin', 'bpm:task-assign-rule:create', 3, '', '', NULL),
  ('super_admin', 'bpm:task-assign-rule:query', 3, '', '', NULL),
  ('super_admin', 'bpm:task-assign-rule:update', 3, '', '', NULL),
  ('super_admin', 'bpm:task:manager-query', 3, '', '', ''),
  ('super_admin', 'bpm:task:query', 3, '', '', NULL),
  ('super_admin', 'bpm:task:update', 3, '', '', NULL),
  ('super_admin', 'bpm:user-group:create', 3, '', '', NULL),
  ('super_admin', 'bpm:user-group:delete', 3, '', '', NULL),
  ('super_admin', 'bpm:user-group:query', 3, '', '', NULL),
  ('super_admin', 'bpm:user-group:update', 3, '', '', NULL),
  ('super_admin', 'crm:business-status:create', 3, '', '', ''),
  ('super_admin', 'crm:business-status:delete', 3, '', '', ''),
  ('super_admin', 'crm:business-status:query', 3, '', '', ''),
  ('super_admin', 'crm:business-status:update', 3, '', '', ''),
  ('super_admin', 'crm:business:create', 3, '', '', NULL),
  ('super_admin', 'crm:business:delete', 3, '', '', NULL),
  ('super_admin', 'crm:business:export', 3, '', '', NULL),
  ('super_admin', 'crm:business:query', 3, '', '', NULL),
  ('super_admin', 'crm:business:update', 3, '', '', NULL),
  ('super_admin', 'crm:clue:create', 3, '', '', NULL),
  ('super_admin', 'crm:clue:delete', 3, '', '', NULL),
  ('super_admin', 'crm:clue:export', 3, '', '', NULL),
  ('super_admin', 'crm:clue:query', 3, '', '', NULL),
  ('super_admin', 'crm:clue:update', 3, '', '', NULL),
  ('super_admin', 'crm:contact:create', 3, '', '', NULL),
  ('super_admin', 'crm:contact:create-business', 3, '', '', ''),
  ('super_admin', 'crm:contact:delete', 3, '', '', NULL),
  ('super_admin', 'crm:contact:delete-business', 3, '', '', ''),
  ('super_admin', 'crm:contact:export', 3, '', '', NULL),
  ('super_admin', 'crm:contact:query', 3, '', '', NULL),
  ('super_admin', 'crm:contact:update', 3, '', '', NULL),
  ('super_admin', 'crm:contract-config:query', 3, '', '', ''),
  ('super_admin', 'crm:contract-config:update', 3, '', '', ''),
  ('super_admin', 'crm:contract:create', 3, '', '', NULL),
  ('super_admin', 'crm:contract:delete', 3, '', '', NULL),
  ('super_admin', 'crm:contract:export', 3, '', '', NULL),
  ('super_admin', 'crm:contract:query', 3, '', '', NULL),
  ('super_admin', 'crm:contract:update', 3, '', '', NULL),
  ('super_admin', 'crm:customer-limit-config:create', 3, '', '', NULL),
  ('super_admin', 'crm:customer-limit-config:delete', 3, '', '', NULL),
  ('super_admin', 'crm:customer-limit-config:export', 3, '', '', NULL),
  ('super_admin', 'crm:customer-limit-config:query', 3, '', '', NULL),
  ('super_admin', 'crm:customer-limit-config:update', 3, '', '', NULL),
  ('super_admin', 'crm:customer-pool-config:query', 3, '', '', ''),
  ('super_admin', 'crm:customer-pool-config:update', 3, '', '', NULL),
  ('super_admin', 'crm:customer:create', 3, '', '', NULL),
  ('super_admin', 'crm:customer:delete', 3, '', '', NULL),
  ('super_admin', 'crm:customer:distribute', 3, '', '', ''),
  ('super_admin', 'crm:customer:export', 3, '', '', NULL),
  ('super_admin', 'crm:customer:import', 3, '', '', ''),
  ('super_admin', 'crm:customer:query', 3, '', '', NULL),
  ('super_admin', 'crm:customer:receive', 3, '', '', ''),
  ('super_admin', 'crm:customer:update', 3, '', '', NULL),
  ('super_admin', 'crm:product-category:create', 3, '', '', ''),
  ('super_admin', 'crm:product-category:delete', 3, '', '', ''),
  ('super_admin', 'crm:product-category:query', 3, '', '', ''),
  ('super_admin', 'crm:product-category:update', 3, '', '', ''),
  ('super_admin', 'crm:product:create', 3, '', '', ''),
  ('super_admin', 'crm:product:delete', 3, '', '', ''),
  ('super_admin', 'crm:product:export', 3, '', '', ''),
  ('super_admin', 'crm:product:query', 3, '', '', ''),
  ('super_admin', 'crm:product:update', 3, '', '', ''),
  ('super_admin', 'crm:receivable-plan:create', 3, '', '', NULL),
  ('super_admin', 'crm:receivable-plan:delete', 3, '', '', NULL),
  ('super_admin', 'crm:receivable-plan:export', 3, '', '', NULL),
  ('super_admin', 'crm:receivable-plan:query', 3, '', '', NULL),
  ('super_admin', 'crm:receivable-plan:update', 3, '', '', NULL),
  ('super_admin', 'crm:receivable:create', 3, '', '', NULL),
  ('super_admin', 'crm:receivable:delete', 3, '', '', NULL),
  ('super_admin', 'crm:receivable:export', 3, '', '', NULL),
  ('super_admin', 'crm:receivable:query', 3, '', '', NULL),
  ('super_admin', 'crm:receivable:update', 3, '', '', NULL),
  ('super_admin', 'crm:statistics-customer:query', 2, 'customer', 'crm/statistics/customer/index.vue', 'CrmStatisticsCustomer'),
  ('super_admin', 'crm:statistics-funnel:query', 2, 'funnel', 'crm/statistics/funnel/index', 'CrmStatisticsFunnel'),
  ('super_admin', 'crm:statistics-performance:query', 2, 'performance', 'crm/statistics/performance/index', 'CrmStatisticsPerformance'),
  ('super_admin', 'crm:statistics-portrait:query', 2, 'portrait', 'crm/statistics/portrait/index', 'CrmStatisticsPortrait'),
  ('super_admin', 'crm:statistics-rank:query', 2, 'ranking', 'crm/statistics/rank/index', 'CrmStatisticsRank'),
  ('super_admin', 'dcc:controlled-file:approve', 3, '', '', ''),
  ('super_admin', 'dcc:controlled-file:category:manage', 2, 'controlled-file/categories', 'dcc/controlled-file/categories/index', 'DccControlledFileCategories'),
  ('super_admin', 'dcc:controlled-file:directory:manage', 2, 'controlled-file/directories', 'dcc/controlled-file/directories/index', 'DccControlledFileDirectories'),
  ('super_admin', 'dcc:controlled-file:download', 3, '', '', ''),
  ('super_admin', 'dcc:controlled-file:log:query', 2, 'controlled-file/logs', 'dcc/controlled-file/logs/index', 'DccControlledFileLogs'),
  ('super_admin', 'dcc:controlled-file:position:manage', 2, 'approval-role', 'dcc/controlled-file/positions/index', 'DccControlledFilePositions'),
  ('super_admin', 'dcc:controlled-file:preview', 3, '', '', ''),
  ('super_admin', 'dcc:controlled-file:print-template:manage', 2, 'controlled-file/print-template', 'dcc/controlled-file/print-template/index', 'DccApprovalPrintTemplate'),
  ('super_admin', 'dcc:controlled-file:query', 2, 'controlled-file/browser', 'dcc/controlled-file/browser/index', 'DccControlledFileBrowser'),
  ('super_admin', 'dcc:controlled-file:review', 3, '', '', ''),
  ('super_admin', 'dcc:controlled-file:route:manage', 2, 'controlled-file/routes', 'dcc/controlled-file/routes/index', 'DccControlledFileRoutes'),
  ('super_admin', 'dcc:controlled-file:stamp:retry', 3, '', '', ''),
  ('super_admin', 'dcc:controlled-file:submit', 2, 'controlled-file/upload', 'dcc/controlled-file/upload/index', 'DccControlledFileUpload'),
  ('super_admin', 'dcc:controlled-file:training:mine', 2, 'controlled-file/training-mine', 'dcc/controlled-file/training/mine/index', 'DccControlledFileTrainingMine'),
  ('super_admin', 'dcc:project-code-assignment:assign', 3, '', '', ''),
  ('super_admin', 'dcc:project-code-assignment:audit:query', 3, '', '', ''),
  ('super_admin', 'dcc:project-code-assignment:execute', 3, '', '', ''),
  ('super_admin', 'dcc:project-code-assignment:query', 3, '', '', ''),
  ('super_admin', 'dcc:project-code-assignment:revoke', 3, '', '', ''),
  ('super_admin', 'dcc:project-code:export', 3, '', '', ''),
  ('super_admin', 'dcc:project-code:import', 3, '', '', ''),
  ('super_admin', 'dcc:project-code:query', 2, 'project-code', 'dcc/controlled-file/basic-data/project-code/index', 'DccProjectCodeBasicDataPage'),
  ('super_admin', 'erp:account:create', 3, '', '', NULL),
  ('super_admin', 'erp:account:delete', 3, '', '', NULL),
  ('super_admin', 'erp:account:export', 3, '', '', NULL),
  ('super_admin', 'erp:account:query', 3, '', '', NULL),
  ('super_admin', 'erp:account:update', 3, '', '', NULL),
  ('super_admin', 'erp:bom-list:query', 3, '', '', ''),
  ('super_admin', 'erp:customer:create', 3, '', '', NULL),
  ('super_admin', 'erp:customer:delete', 3, '', '', NULL),
  ('super_admin', 'erp:customer:export', 3, '', '', NULL),
  ('super_admin', 'erp:customer:query', 3, '', '', NULL),
  ('super_admin', 'erp:customer:update', 3, '', '', NULL),
  ('super_admin', 'erp:fenbeitong-voucher:config', 3, '', '', NULL),
  ('super_admin', 'erp:fenbeitong-voucher:query', 2, 'fenbeitong-voucher', 'erp/finance/fenbeitong-voucher/index', 'ErpFenbeitongVoucher'),
  ('super_admin', 'erp:fenbeitong-voucher:save', 3, '', '', NULL),
  ('super_admin', 'erp:finance-payment:create', 3, '', '', NULL),
  ('super_admin', 'erp:finance-payment:delete', 3, '', '', NULL),
  ('super_admin', 'erp:finance-payment:export', 3, '', '', NULL),
  ('super_admin', 'erp:finance-payment:query', 3, '', '', NULL),
  ('super_admin', 'erp:finance-payment:update', 3, '', '', NULL),
  ('super_admin', 'erp:finance-payment:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:finance-receipt:create', 3, '', '', NULL),
  ('super_admin', 'erp:finance-receipt:delete', 3, '', '', NULL),
  ('super_admin', 'erp:finance-receipt:export', 3, '', '', NULL),
  ('super_admin', 'erp:finance-receipt:query', 3, '', '', NULL),
  ('super_admin', 'erp:finance-receipt:update', 3, '', '', NULL),
  ('super_admin', 'erp:finance-receipt:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:inventory-list:query', 3, '', '', ''),
  ('super_admin', 'erp:kingdee-config:query', 3, '', '', NULL),
  ('super_admin', 'erp:kingdee-config:save', 3, '', '', NULL),
  ('super_admin', 'erp:kingdee-sync:query', 3, '', '', NULL),
  ('super_admin', 'erp:product-category:create', 3, '', '', NULL),
  ('super_admin', 'erp:product-category:delete', 3, '', '', NULL),
  ('super_admin', 'erp:product-category:export', 3, '', '', NULL),
  ('super_admin', 'erp:product-category:query', 3, '', '', NULL),
  ('super_admin', 'erp:product-category:update', 3, '', '', NULL),
  ('super_admin', 'erp:product-unit:create', 3, '', '', NULL),
  ('super_admin', 'erp:product-unit:delete', 3, '', '', NULL),
  ('super_admin', 'erp:product-unit:export', 3, '', '', NULL),
  ('super_admin', 'erp:product-unit:query', 3, '', '', NULL),
  ('super_admin', 'erp:product-unit:update', 3, '', '', NULL),
  ('super_admin', 'erp:product:create', 3, '', '', ''),
  ('super_admin', 'erp:product:delete', 3, '', '', ''),
  ('super_admin', 'erp:product:export', 3, '', '', ''),
  ('super_admin', 'erp:product:query', 3, '', '', ''),
  ('super_admin', 'erp:product:update', 3, '', '', ''),
  ('super_admin', 'erp:production-material-list:query', 3, '', '', ''),
  ('super_admin', 'erp:purchase-in:create', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-in:delete', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-in:export', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-in:query', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-in:update', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-in:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-order:create', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-order:delete', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-order:export', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-order:query', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-order:sync-kingdee', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-order:update', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-order:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-return:create', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-return:delete', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-return:export', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-return:query', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-return:update', 3, '', '', NULL),
  ('super_admin', 'erp:purchase-return:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:sale-order:create', 3, '', '', NULL),
  ('super_admin', 'erp:sale-order:delete', 3, '', '', NULL),
  ('super_admin', 'erp:sale-order:export', 3, '', '', NULL),
  ('super_admin', 'erp:sale-order:query', 3, '', '', NULL),
  ('super_admin', 'erp:sale-order:update', 3, '', '', NULL),
  ('super_admin', 'erp:sale-order:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:sale-out:create', 3, '', '', NULL),
  ('super_admin', 'erp:sale-out:delete', 3, '', '', NULL),
  ('super_admin', 'erp:sale-out:export', 3, '', '', NULL),
  ('super_admin', 'erp:sale-out:query', 3, '', '', NULL),
  ('super_admin', 'erp:sale-out:update', 3, '', '', NULL),
  ('super_admin', 'erp:sale-out:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:sale-return:create', 3, '', '', NULL),
  ('super_admin', 'erp:sale-return:delete', 3, '', '', NULL),
  ('super_admin', 'erp:sale-return:export', 3, '', '', NULL),
  ('super_admin', 'erp:sale-return:query', 3, '', '', NULL),
  ('super_admin', 'erp:sale-return:update', 3, '', '', NULL),
  ('super_admin', 'erp:sale-return:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:statistics:query', 2, 'home', 'erp/home/index.vue', 'ErpHome'),
  ('super_admin', 'erp:stock-check:create', 3, '', '', NULL),
  ('super_admin', 'erp:stock-check:delete', 3, '', '', NULL),
  ('super_admin', 'erp:stock-check:export', 3, '', '', NULL),
  ('super_admin', 'erp:stock-check:query', 3, '', '', NULL),
  ('super_admin', 'erp:stock-check:update', 3, '', '', NULL),
  ('super_admin', 'erp:stock-check:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:stock-in:create', 3, '', '', NULL),
  ('super_admin', 'erp:stock-in:delete', 3, '', '', NULL),
  ('super_admin', 'erp:stock-in:export', 3, '', '', NULL),
  ('super_admin', 'erp:stock-in:query', 3, '', '', NULL),
  ('super_admin', 'erp:stock-in:update', 3, '', '', NULL),
  ('super_admin', 'erp:stock-in:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:stock-move:create', 3, '', '', NULL),
  ('super_admin', 'erp:stock-move:delete', 3, '', '', NULL),
  ('super_admin', 'erp:stock-move:export', 3, '', '', NULL),
  ('super_admin', 'erp:stock-move:query', 3, '', '', NULL),
  ('super_admin', 'erp:stock-move:update', 3, '', '', NULL),
  ('super_admin', 'erp:stock-move:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:stock-out:create', 3, '', '', NULL),
  ('super_admin', 'erp:stock-out:delete', 3, '', '', NULL),
  ('super_admin', 'erp:stock-out:export', 3, '', '', NULL),
  ('super_admin', 'erp:stock-out:query', 3, '', '', NULL),
  ('super_admin', 'erp:stock-out:update', 3, '', '', NULL),
  ('super_admin', 'erp:stock-out:update-status', 3, '', '', NULL),
  ('super_admin', 'erp:stock-record:export', 3, '', '', NULL),
  ('super_admin', 'erp:stock-record:query', 3, '', '', NULL),
  ('super_admin', 'erp:stock:export', 3, '', '', NULL),
  ('super_admin', 'erp:stock:query', 3, '', '', NULL),
  ('super_admin', 'erp:supplier:create', 3, '', '', NULL),
  ('super_admin', 'erp:supplier:delete', 3, '', '', NULL),
  ('super_admin', 'erp:supplier:export', 3, '', '', NULL),
  ('super_admin', 'erp:supplier:query', 3, '', '', NULL),
  ('super_admin', 'erp:supplier:update', 3, '', '', NULL),
  ('super_admin', 'erp:warehouse:create', 3, '', '', NULL),
  ('super_admin', 'erp:warehouse:delete', 3, '', '', NULL),
  ('super_admin', 'erp:warehouse:export', 3, '', '', NULL),
  ('super_admin', 'erp:warehouse:query', 3, '', '', NULL),
  ('super_admin', 'erp:warehouse:update', 3, '', '', NULL),
  ('super_admin', 'form:bpm-callback:handle', 3, '', '', ''),
  ('super_admin', 'form:effect:query', 2, 'effect', 'form-center/effect/index', 'FormCenterEffect'),
  ('super_admin', 'form:effect:retry', 3, '', '', ''),
  ('super_admin', 'form:instance:abandon', 3, '', '', ''),
  ('super_admin', 'form:instance:create', 3, '', '', ''),
  ('super_admin', 'form:instance:snapshot:query', 3, '', '', ''),
  ('super_admin', 'form:instance:submit', 3, '', '', ''),
  ('super_admin', 'form:instance:update', 3, '', '', ''),
  ('super_admin', 'form:policy:create', 3, '', '', ''),
  ('super_admin', 'form:policy:publish', 3, '', '', ''),
  ('super_admin', 'form:policy:query', 2, 'policy', 'form-center/policy/index', 'FormCenterPolicy'),
  ('super_admin', 'form:template-source:download', 3, '', '', ''),
  ('super_admin', 'form:template:create', 3, '', '', ''),
  ('super_admin', 'form:template:disable', 3, '', '', ''),
  ('super_admin', 'form:template:obsolete', 3, '', '', ''),
  ('super_admin', 'form:template:publish', 3, '', '', ''),
  ('super_admin', 'form:template:query', 2, 'template', 'form-center/template/index', 'FormCenterTemplate'),
  ('super_admin', 'form:template:update', 3, '', '', ''),
  ('super_admin', 'infra:api-access-log:export', 3, '', '', NULL),
  ('super_admin', 'infra:api-access-log:query', 3, '', '', NULL),
  ('super_admin', 'infra:api-error-log:export', 3, '', '', NULL),
  ('super_admin', 'infra:api-error-log:query', 2, 'api-error-log', 'infra/apiErrorLog/index', 'InfraApiErrorLog'),
  ('super_admin', 'infra:api-error-log:update-status', 3, '', '', NULL),
  ('super_admin', 'infra:build:list', 2, 'build', 'infra/build/index', 'InfraBuild'),
  ('super_admin', 'infra:codegen:create', 3, '', '', NULL),
  ('super_admin', 'infra:codegen:delete', 3, '', '', NULL),
  ('super_admin', 'infra:codegen:download', 3, '', '', NULL),
  ('super_admin', 'infra:codegen:preview', 3, '', '', NULL),
  ('super_admin', 'infra:codegen:query', 2, 'codegen', 'infra/codegen/index', 'InfraCodegen'),
  ('super_admin', 'infra:codegen:update', 3, '', '', NULL),
  ('super_admin', 'infra:config:create', 3, '', '', NULL),
  ('super_admin', 'infra:config:delete', 3, '', '', NULL),
  ('super_admin', 'infra:config:export', 3, '', '', NULL),
  ('super_admin', 'infra:config:query', 3, '', '', NULL),
  ('super_admin', 'infra:config:update', 3, '', '', NULL),
  ('super_admin', 'infra:data-source-config:create', 3, '', '', NULL),
  ('super_admin', 'infra:data-source-config:delete', 3, '', '', NULL),
  ('super_admin', 'infra:data-source-config:export', 3, '', '', NULL),
  ('super_admin', 'infra:data-source-config:query', 3, '', '', NULL),
  ('super_admin', 'infra:data-source-config:update', 3, '', '', NULL),
  ('super_admin', 'infra:demo01-contact:create', 3, '', '', NULL),
  ('super_admin', 'infra:demo01-contact:delete', 3, '', '', NULL),
  ('super_admin', 'infra:demo01-contact:export', 3, '', '', NULL),
  ('super_admin', 'infra:demo01-contact:query', 3, '', '', NULL),
  ('super_admin', 'infra:demo01-contact:update', 3, '', '', NULL),
  ('super_admin', 'infra:demo02-category:create', 3, '', '', NULL),
  ('super_admin', 'infra:demo02-category:delete', 3, '', '', NULL),
  ('super_admin', 'infra:demo02-category:export', 3, '', '', NULL),
  ('super_admin', 'infra:demo02-category:query', 3, '', '', NULL),
  ('super_admin', 'infra:demo02-category:update', 3, '', '', NULL),
  ('super_admin', 'infra:demo03-student:create', 3, '', '', NULL),
  ('super_admin', 'infra:demo03-student:delete', 3, '', '', NULL),
  ('super_admin', 'infra:demo03-student:export', 3, '', '', NULL),
  ('super_admin', 'infra:demo03-student:query', 3, '', '', NULL),
  ('super_admin', 'infra:demo03-student:update', 3, '', '', NULL),
  ('super_admin', 'infra:file-config:create', 3, '', '', NULL),
  ('super_admin', 'infra:file-config:delete', 3, '', '', NULL),
  ('super_admin', 'infra:file-config:export', 3, '', '', NULL),
  ('super_admin', 'infra:file-config:query', 3, '', '', NULL),
  ('super_admin', 'infra:file-config:update', 3, '', '', NULL),
  ('super_admin', 'infra:file:delete', 3, '', '', NULL),
  ('super_admin', 'infra:file:query', 3, '', '', NULL),
  ('super_admin', 'infra:job:create', 3, '', '', NULL),
  ('super_admin', 'infra:job:delete', 3, '', '', NULL),
  ('super_admin', 'infra:job:export', 3, '', '', NULL),
  ('super_admin', 'infra:job:query', 3, '', '', NULL),
  ('super_admin', 'infra:job:trigger', 3, '', '', NULL),
  ('super_admin', 'infra:job:update', 3, '', '', NULL),
  ('super_admin', 'infra:nas:query', 3, '', '', ''),
  ('super_admin', 'infra:nas:test', 3, '', '', ''),
  ('super_admin', 'infra:nas:update', 3, '', '', ''),
  ('super_admin', 'infra:redis:get-key-list', 3, '', '', NULL),
  ('super_admin', 'infra:redis:get-monitor-info', 3, '', '', NULL),
  ('super_admin', 'infra:runtime-control:operate', 3, '', '', ''),
  ('super_admin', 'infra:runtime-control:query', 2, 'runtime-control', 'infra/runtime-control/index', 'InfraRuntimeControl'),
  ('super_admin', 'infra:runtime-control:restart', 3, '', '', ''),
  ('super_admin', 'infra:swagger:list', 2, 'swagger', 'infra/swagger/index', 'InfraSwagger'),
  ('super_admin', 'iot:alert-config:create', 3, '', '', ''),
  ('super_admin', 'iot:alert-config:delete', 3, '', '', ''),
  ('super_admin', 'iot:alert-config:query', 3, '', '', ''),
  ('super_admin', 'iot:alert-config:update', 3, '', '', ''),
  ('super_admin', 'iot:alert-record:process', 3, '', '', ''),
  ('super_admin', 'iot:alert-record:query', 3, '', '', ''),
  ('super_admin', 'iot:data-rule:create', 3, '', '', ''),
  ('super_admin', 'iot:data-rule:delete', 3, '', '', ''),
  ('super_admin', 'iot:data-rule:query', 3, '', '', ''),
  ('super_admin', 'iot:data-rule:update', 3, '', '', ''),
  ('super_admin', 'iot:data-sink:create', 3, '', '', ''),
  ('super_admin', 'iot:data-sink:delete', 3, '', '', ''),
  ('super_admin', 'iot:data-sink:query', 3, '', '', ''),
  ('super_admin', 'iot:data-sink:update', 3, '', '', ''),
  ('super_admin', 'iot:device-group:create', 3, '', '', NULL),
  ('super_admin', 'iot:device-group:delete', 3, '', '', NULL),
  ('super_admin', 'iot:device-group:query', 3, '', '', NULL),
  ('super_admin', 'iot:device-group:update', 3, '', '', NULL),
  ('super_admin', 'iot:device:create', 3, '', '', ''),
  ('super_admin', 'iot:device:delete', 3, '', '', ''),
  ('super_admin', 'iot:device:export', 3, '', '', ''),
  ('super_admin', 'iot:device:import', 3, '', '', ''),
  ('super_admin', 'iot:device:message-query', 3, '', '', ''),
  ('super_admin', 'iot:device:message-send', 3, '', '', ''),
  ('super_admin', 'iot:device:property-query', 3, '', '', ''),
  ('super_admin', 'iot:device:query', 3, '', '', ''),
  ('super_admin', 'iot:device:update', 3, '', '', ''),
  ('super_admin', 'iot:ota-firmware:create', 3, '', '', ''),
  ('super_admin', 'iot:ota-firmware:delete', 3, '', '', ''),
  ('super_admin', 'iot:ota-firmware:query', 3, '', '', ''),
  ('super_admin', 'iot:ota-firmware:update', 3, '', '', ''),
  ('super_admin', 'iot:ota-task-record:cancel', 3, '', '', ''),
  ('super_admin', 'iot:ota-task-record:query', 3, '', '', ''),
  ('super_admin', 'iot:ota-task:cancel', 3, '', '', ''),
  ('super_admin', 'iot:ota-task:create', 3, '', '', ''),
  ('super_admin', 'iot:product-category:create', 3, '', '', NULL),
  ('super_admin', 'iot:product-category:delete', 3, '', '', NULL),
  ('super_admin', 'iot:product-category:query', 3, '', '', NULL),
  ('super_admin', 'iot:product-category:update', 3, '', '', NULL),
  ('super_admin', 'iot:product:create', 3, '', '', NULL),
  ('super_admin', 'iot:product:delete', 3, '', '', NULL),
  ('super_admin', 'iot:product:export', 3, '', '', NULL),
  ('super_admin', 'iot:product:query', 3, '', '', NULL),
  ('super_admin', 'iot:product:update', 3, '', '', NULL),
  ('super_admin', 'iot:rule-scene:create', 3, '', '', ''),
  ('super_admin', 'iot:rule-scene:delete', 3, '', '', ''),
  ('super_admin', 'iot:rule-scene:export', 3, '', '', ''),
  ('super_admin', 'iot:rule-scene:query', 3, '', '', ''),
  ('super_admin', 'iot:rule-scene:update', 3, '', '', ''),
  ('super_admin', 'iot:thing-model:create', 3, '', '', NULL),
  ('super_admin', 'iot:thing-model:delete', 3, '', '', NULL),
  ('super_admin', 'iot:thing-model:export', 3, '', '', NULL),
  ('super_admin', 'iot:thing-model:query', 3, '', '', NULL),
  ('super_admin', 'iot:thing-model:update', 3, '', '', NULL),
  ('super_admin', 'mdm:product:create', 3, '', '', ''),
  ('super_admin', 'mdm:product:delete', 3, '', '', ''),
  ('super_admin', 'mdm:product:export', 3, '', '', ''),
  ('super_admin', 'mdm:product:import', 3, '', '', ''),
  ('super_admin', 'mdm:product:map-showroom', 3, '', '', ''),
  ('super_admin', 'mdm:product:query', 2, 'product', 'mdm/product/index', 'MdmProduct'),
  ('super_admin', 'mdm:product:update', 3, '', '', ''),
  ('super_admin', 'member:config:query', 3, '', '', ''),
  ('super_admin', 'member:config:save', 3, '', '', ''),
  ('super_admin', 'member:experience-record:query', 3, '', '', ''),
  ('super_admin', 'member:group:create', 3, '', '', NULL),
  ('super_admin', 'member:group:delete', 3, '', '', NULL),
  ('super_admin', 'member:group:query', 3, '', '', NULL),
  ('super_admin', 'member:group:update', 3, '', '', NULL),
  ('super_admin', 'member:level-record:query', 3, '', '', ''),
  ('super_admin', 'member:level:create', 3, '', '', NULL),
  ('super_admin', 'member:level:delete', 3, '', '', NULL),
  ('super_admin', 'member:level:query', 3, '', '', NULL),
  ('super_admin', 'member:level:update', 3, '', '', NULL),
  ('super_admin', 'member:tag:create', 3, '', '', NULL),
  ('super_admin', 'member:tag:delete', 3, '', '', NULL),
  ('super_admin', 'member:tag:query', 3, '', '', NULL),
  ('super_admin', 'member:tag:update', 3, '', '', NULL),
  ('super_admin', 'member:user:query', 3, '', '', NULL),
  ('super_admin', 'member:user:update', 3, '', '', NULL),
  ('super_admin', 'member:user:update-level', 3, '', '', NULL),
  ('super_admin', 'member:user:update-point', 3, '', '', NULL),
  ('super_admin', 'mes:auto-code-rule:create', 3, '', '', NULL),
  ('super_admin', 'mes:auto-code-rule:delete', 3, '', '', NULL),
  ('super_admin', 'mes:auto-code-rule:export', 3, '', '', NULL),
  ('super_admin', 'mes:auto-code-rule:query', 3, '', '', NULL),
  ('super_admin', 'mes:auto-code-rule:update', 3, '', '', NULL),
  ('super_admin', 'mes:cal-holiday:create', 3, '', '', ''),
  ('super_admin', 'mes:cal-holiday:delete', 3, '', '', ''),
  ('super_admin', 'mes:cal-holiday:export', 3, '', '', ''),
  ('super_admin', 'mes:cal-holiday:query', 3, '', '', ''),
  ('super_admin', 'mes:cal-holiday:update', 3, '', '', ''),
  ('super_admin', 'mes:cal-plan:create', 3, '', '', ''),
  ('super_admin', 'mes:cal-plan:delete', 3, '', '', ''),
  ('super_admin', 'mes:cal-plan:export', 3, '', '', ''),
  ('super_admin', 'mes:cal-plan:query', 3, '', '', ''),
  ('super_admin', 'mes:cal-plan:update', 3, '', '', ''),
  ('super_admin', 'mes:cal-team-shift:query', 3, '', '', ''),
  ('super_admin', 'mes:cal-team:create', 3, '', '', ''),
  ('super_admin', 'mes:cal-team:delete', 3, '', '', ''),
  ('super_admin', 'mes:cal-team:export', 3, '', '', ''),
  ('super_admin', 'mes:cal-team:query', 3, '', '', ''),
  ('super_admin', 'mes:cal-team:update', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-plan:create', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-plan:delete', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-plan:export', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-plan:query', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-plan:update', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-record:create', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-record:delete', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-record:export', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-record:query', 3, '', '', ''),
  ('super_admin', 'mes:dv-check-record:update', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery-type:create', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery-type:delete', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery-type:export', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery-type:query', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery-type:update', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery:create', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery:delete', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery:export', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery:import', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery:query', 3, '', '', ''),
  ('super_admin', 'mes:dv-machinery:update', 3, '', '', ''),
  ('super_admin', 'mes:dv-mainten-record:create', 3, '', '', ''),
  ('super_admin', 'mes:dv-mainten-record:delete', 3, '', '', ''),
  ('super_admin', 'mes:dv-mainten-record:export', 3, '', '', ''),
  ('super_admin', 'mes:dv-mainten-record:query', 3, '', '', ''),
  ('super_admin', 'mes:dv-mainten-record:update', 3, '', '', ''),
  ('super_admin', 'mes:dv-repair:create', 3, '', '', ''),
  ('super_admin', 'mes:dv-repair:delete', 3, '', '', ''),
  ('super_admin', 'mes:dv-repair:export', 3, '', '', ''),
  ('super_admin', 'mes:dv-repair:query', 3, '', '', ''),
  ('super_admin', 'mes:dv-repair:update', 3, '', '', ''),
  ('super_admin', 'mes:dv-subject:create', 3, '', '', ''),
  ('super_admin', 'mes:dv-subject:delete', 3, '', '', ''),
  ('super_admin', 'mes:dv-subject:export', 3, '', '', ''),
  ('super_admin', 'mes:dv-subject:query', 3, '', '', ''),
  ('super_admin', 'mes:dv-subject:update', 3, '', '', ''),
  ('super_admin', 'mes:home:query', 2, '/mes/home/index', 'mes/home/index', 'MesHome'),
  ('super_admin', 'mes:md-client:create', 3, '', '', ''),
  ('super_admin', 'mes:md-client:delete', 3, '', '', ''),
  ('super_admin', 'mes:md-client:export', 3, '', '', ''),
  ('super_admin', 'mes:md-client:import', 3, '', '', ''),
  ('super_admin', 'mes:md-client:query', 3, '', '', ''),
  ('super_admin', 'mes:md-client:update', 3, '', '', ''),
  ('super_admin', 'mes:md-item-type:create', 3, '', '', ''),
  ('super_admin', 'mes:md-item-type:delete', 3, '', '', ''),
  ('super_admin', 'mes:md-item-type:query', 3, '', '', ''),
  ('super_admin', 'mes:md-item-type:update', 3, '', '', ''),
  ('super_admin', 'mes:md-item:create', 3, '', '', ''),
  ('super_admin', 'mes:md-item:delete', 3, '', '', ''),
  ('super_admin', 'mes:md-item:export', 3, '', '', ''),
  ('super_admin', 'mes:md-item:import', 3, '', '', ''),
  ('super_admin', 'mes:md-item:query', 3, '', '', ''),
  ('super_admin', 'mes:md-item:update', 3, '', '', ''),
  ('super_admin', 'mes:md-unit-measure:create', 3, '', '', ''),
  ('super_admin', 'mes:md-unit-measure:delete', 3, '', '', ''),
  ('super_admin', 'mes:md-unit-measure:export', 3, '', '', ''),
  ('super_admin', 'mes:md-unit-measure:query', 3, '', '', ''),
  ('super_admin', 'mes:md-unit-measure:update', 3, '', '', ''),
  ('super_admin', 'mes:md-vendor:create', 3, '', '', ''),
  ('super_admin', 'mes:md-vendor:delete', 3, '', '', ''),
  ('super_admin', 'mes:md-vendor:export', 3, '', '', ''),
  ('super_admin', 'mes:md-vendor:import', 3, '', '', ''),
  ('super_admin', 'mes:md-vendor:query', 3, '', '', ''),
  ('super_admin', 'mes:md-vendor:update', 3, '', '', ''),
  ('super_admin', 'mes:md-workshop:create', 3, '', '', ''),
  ('super_admin', 'mes:md-workshop:delete', 3, '', '', ''),
  ('super_admin', 'mes:md-workshop:query', 3, '', '', ''),
  ('super_admin', 'mes:md-workshop:update', 3, '', '', ''),
  ('super_admin', 'mes:md-workstation:create', 3, '', '', ''),
  ('super_admin', 'mes:md-workstation:delete', 3, '', '', ''),
  ('super_admin', 'mes:md-workstation:export', 3, '', '', ''),
  ('super_admin', 'mes:md-workstation:query', 3, '', '', ''),
  ('super_admin', 'mes:md-workstation:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-config:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-config:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-config:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-config:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-record:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-record:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-record:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-record:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-andon-record:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-auto-schedule:apply', 3, '', '', ''),
  ('super_admin', 'mes:pro-auto-schedule:preview', 3, '', '', ''),
  ('super_admin', 'mes:pro-auto-schedule:replan', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution-archive:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution-archive:download', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution-archive:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:approve', 2, '/mes/pro/feedback/edhr-approval', 'mes/pro/edhr/ApprovalPage', 'MesProFeedbackEdhrApproval'),
  ('super_admin', 'mes:pro-batch-record-execution:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:domain-trace-query', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:domain-trace-verify', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:field-audit-export', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:field-audit-query', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:field-audit-update', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:field-audit-verify', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:signature-query', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-execution:track', 2, '/mes/pro/feedback/edhr-form-trace', 'mes/pro/edhr/FormTracePage', 'MesProFeedbackEdhrFormTrace'),
  ('super_admin', 'mes:pro-batch-record-execution:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-template:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-template:import', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-template:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-template:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-version:confirm', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-version:import', 3, '', '', ''),
  ('super_admin', 'mes:pro-batch-record-version:rollback-request', 3, '', '', ''),
  ('super_admin', 'mes:pro-card:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-card:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-card:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-card:finish', 3, '', '', ''),
  ('super_admin', 'mes:pro-card:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-card:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-batch-execution-archive:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-batch-execution-archive:download', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-batch-execution-archive:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-batch-execution:close', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-batch-execution:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-batch-execution:quality-reject', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-batch-execution:query', 2, '/mes/pro/feedback/edhr-batch-execution', 'mes/pro/edhr-batch/BatchExecutionListPage', 'MesProEdhrBatchExecutionListPage'),
  ('super_admin', 'mes:pro-edhr-batch-execution:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-batch-processing:query', 1, 'edhr-batch-processing', '', ''),
  ('super_admin', 'mes:pro-edhr-change:approve', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-change:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-change:reopen', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-change:supplement', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-change:void', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-delivery:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-delivery:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-deployment:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-deployment:precheck', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-deployment:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-deployment:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-init-batch:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-init-batch:import', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-init-batch:precheck', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-init-batch:query', 2, '/mes/pro/feedback/edhr-init-batch', 'mes/pro/edhr-init-batch/InitBatchPage', 'MesProEdhrInitBatchPage'),
  ('super_admin', 'mes:pro-edhr-init-batch:signoff', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-operation-audit:query', 2, '/mes/pro/feedback/edhr-operation-audit', 'mes/pro/edhr/OperationAuditPage', 'MesProEdhrOperationAuditPage'),
  ('super_admin', 'mes:pro-edhr-oq-pq:close', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-oq-pq:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-oq-pq:execute', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-oq-pq:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-oq-pq:retest', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-permission-scope:evaluate', 2, '/mes/pro/feedback/edhr-permission-matrix', 'mes/pro/edhr/PermissionMatrixPage', 'MesProEdhrPermissionMatrixPage'),
  ('super_admin', 'mes:pro-edhr-permission-scope:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-permission-scope:save', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-release:approve', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-release:intervene', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-release:precheck', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-release:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-release:submit', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-report:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-report:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-traveler-template:activate', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-traveler-template:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-traveler-template:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-traveler:generate', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-traveler:query', 2, '/mes/pro/feedback/edhr-traveler', 'mes/pro/edhr-traveler/TravelerPage', 'MesProFeedbackEdhrTraveler'),
  ('super_admin', 'mes:pro-edhr-validation:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-validation:evaluate-trace', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-validation:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-work-task-rule:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-work-task-rule:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-edhr-work-task:query', 2, '/mes/pro/feedback/edhr-work-task', 'mes/pro/edhr-work-task/WorkTaskBoardPage', 'MesProEdhrWorkTaskBoardPage'),
  ('super_admin', 'mes:pro-edhr-work-task:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-feedback:approve', 3, '', '', ''),
  ('super_admin', 'mes:pro-feedback:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-feedback:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-feedback:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('super_admin', 'mes:pro-feedback:query', 2, '/mes/pro/feedback', 'mes/pro/feedback/index', 'MesProFeedback'),
  ('super_admin', 'mes:pro-feedback:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-mes-process:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-process-pool-team-leader:query', 2, '/mes/pro/process-pool/qa-regulation', 'mes/pro/processpool/QaRegulationPage', 'MesProProcessPoolQaRegulation'),
  ('super_admin', 'mes:pro-process:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-process:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-process:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-process:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-process:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-puhui-schedule:query', 2, '/mes/pro/puhui-schedule', 'mes/pro/puhui-schedule/index', 'MesProPuhuiSchedule'),
  ('super_admin', 'mes:pro-route:batch-record-config:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:batch-record-config:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:schedule-config:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:schedule-config:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:version-cancel', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:version-create', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:version-publish', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:version-query', 3, '', '', ''),
  ('super_admin', 'mes:pro-route:version-submit', 3, '', '', ''),
  ('super_admin', 'mes:pro-schedule-order:admission-diff', 3, '', '', ''),
  ('super_admin', 'mes:pro-schedule-order:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-schedule-order:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-schedule-order:manual-finish', 3, '', '', ''),
  ('super_admin', 'mes:pro-schedule-order:preflight', 3, '', '', ''),
  ('super_admin', 'mes:pro-schedule-order:query', 2, '/mes/pro/schedule-order', 'mes/pro/scheduleorder/index', 'MesProScheduleOrder'),
  ('super_admin', 'mes:pro-schedule-order:revoke-complete', 3, '', '', ''),
  ('super_admin', 'mes:pro-schedule-order:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-scheduler-workbench:query', 2, '/mes/pro/scheduler-workbench', 'mes/pro/scheduler-workbench/index', 'MesProSchedulerWorkbench'),
  ('super_admin', 'mes:pro-scheduler-workbench:smoke-test', 3, '', '', ''),
  ('super_admin', 'mes:pro-scheduler-workbench:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-smart-scheduling:query', 1, 'smart-scheduling', '', ''),
  ('super_admin', 'mes:pro-task:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-task:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-task:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-task:query', 2, '/mes/pro/schedule-calendar', 'mes/pro/task/calendar/index', 'MesProScheduleCalendar'),
  ('super_admin', 'mes:pro-task:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-work-order:create', 3, '', '', ''),
  ('super_admin', 'mes:pro-work-order:create-erp', 3, '', '', NULL),
  ('super_admin', 'mes:pro-work-order:delete', 3, '', '', ''),
  ('super_admin', 'mes:pro-work-order:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-work-order:query', 3, '', '', ''),
  ('super_admin', 'mes:pro-work-order:update', 3, '', '', ''),
  ('super_admin', 'mes:pro-workrecord:clock', 3, '', '', ''),
  ('super_admin', 'mes:pro-workrecord:export', 3, '', '', ''),
  ('super_admin', 'mes:pro-workrecord:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-defect:create', 3, '', '', ''),
  ('super_admin', 'mes:qc-defect:delete', 3, '', '', ''),
  ('super_admin', 'mes:qc-defect:export', 3, '', '', ''),
  ('super_admin', 'mes:qc-defect:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-defect:update', 3, '', '', ''),
  ('super_admin', 'mes:qc-indicator:create', 3, '', '', ''),
  ('super_admin', 'mes:qc-indicator:delete', 3, '', '', ''),
  ('super_admin', 'mes:qc-indicator:export', 3, '', '', ''),
  ('super_admin', 'mes:qc-indicator:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-indicator:update', 3, '', '', ''),
  ('super_admin', 'mes:qc-ipqc:create', 3, '', '', ''),
  ('super_admin', 'mes:qc-ipqc:delete', 3, '', '', ''),
  ('super_admin', 'mes:qc-ipqc:export', 3, '', '', ''),
  ('super_admin', 'mes:qc-ipqc:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-ipqc:update', 3, '', '', ''),
  ('super_admin', 'mes:qc-iqc:create', 3, '', '', ''),
  ('super_admin', 'mes:qc-iqc:delete', 3, '', '', ''),
  ('super_admin', 'mes:qc-iqc:export', 3, '', '', ''),
  ('super_admin', 'mes:qc-iqc:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-iqc:update', 3, '', '', ''),
  ('super_admin', 'mes:qc-oqc:create', 3, '', '', ''),
  ('super_admin', 'mes:qc-oqc:delete', 3, '', '', ''),
  ('super_admin', 'mes:qc-oqc:export', 3, '', '', ''),
  ('super_admin', 'mes:qc-oqc:finish', 3, '', '', NULL),
  ('super_admin', 'mes:qc-oqc:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-oqc:update', 3, '', '', ''),
  ('super_admin', 'mes:qc-pending-inspect:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-rqc:create', 3, '', '', ''),
  ('super_admin', 'mes:qc-rqc:delete', 3, '', '', ''),
  ('super_admin', 'mes:qc-rqc:export', 3, '', '', ''),
  ('super_admin', 'mes:qc-rqc:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-rqc:update', 3, '', '', ''),
  ('super_admin', 'mes:qc-template:create', 3, '', '', ''),
  ('super_admin', 'mes:qc-template:delete', 3, '', '', ''),
  ('super_admin', 'mes:qc-template:export', 3, '', '', ''),
  ('super_admin', 'mes:qc-template:query', 3, '', '', ''),
  ('super_admin', 'mes:qc-template:update', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool-type:create', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool-type:delete', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool-type:export', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool-type:query', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool-type:update', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool:create', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool:delete', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool:export', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool:query', 3, '', '', ''),
  ('super_admin', 'mes:tm-tool:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-arrival-notice:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-arrival-notice:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-arrival-notice:export', 3, '', '', ''),
  ('super_admin', 'mes:wm-arrival-notice:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-arrival-notice:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode-config:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode-config:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode-config:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode-config:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode:export', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-barcode:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-batch:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-item-receipt:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-item-receipt:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-item-receipt:export', 3, '', '', ''),
  ('super_admin', 'mes:wm-item-receipt:finish', 3, '', '', ''),
  ('super_admin', 'mes:wm-item-receipt:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-item-receipt:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-material-stock:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-material-stock:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-material-stock:export', 3, '', '', ''),
  ('super_admin', 'mes:wm-material-stock:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-material-stock:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-misc-issue:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-misc-issue:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-misc-issue:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-misc-issue:finish', 3, '', '', NULL),
  ('super_admin', 'mes:wm-misc-issue:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-misc-issue:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-issue:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-issue:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-issue:execute', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-issue:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-issue:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-issue:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-receipt:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-receipt:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-receipt:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-receipt:finish', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-receipt:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-outsource-receipt:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-package:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-package:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-package:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-package:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-issue:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-issue:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-issue:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-issue:finish', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-issue:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-issue:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-receipt:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-receipt:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-receipt:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-receipt:finish', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-receipt:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-receipt:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-product-sales:cancel', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:export', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:finish', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:shipping', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:stock', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:submit', 3, '', '', ''),
  ('super_admin', 'mes:wm-product-sales:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-return-issue:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-issue:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-issue:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-issue:finish', 3, '', '', ''),
  ('super_admin', 'mes:wm-return-issue:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-issue:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:cancel', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:finish', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:stock', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:submit', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-sales:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-vendor:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-vendor:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-vendor:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-vendor:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-vendor:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-return-vendor:update-status', 3, '', '', NULL),
  ('super_admin', 'mes:wm-sales-notice:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-sales-notice:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-sales-notice:export', 3, '', '', ''),
  ('super_admin', 'mes:wm-sales-notice:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-sales-notice:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-sn:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-sn:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-stock-taking-plan:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-stock-taking-plan:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-stock-taking-plan:export', 3, '', '', ''),
  ('super_admin', 'mes:wm-stock-taking-plan:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-stock-taking-plan:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-stock-taking-task:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm-stock-taking-task:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm-stock-taking-task:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm-stock-taking-task:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm-stock-taking-task:update', 3, '', '', NULL),
  ('super_admin', 'mes:wm-transfer:cancel', 3, '', '', ''),
  ('super_admin', 'mes:wm-transfer:confirm', 3, '', '', ''),
  ('super_admin', 'mes:wm-transfer:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-transfer:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-transfer:export', 3, '', '', ''),
  ('super_admin', 'mes:wm-transfer:finish', 3, '', '', ''),
  ('super_admin', 'mes:wm-transfer:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-transfer:submit', 3, '', '', ''),
  ('super_admin', 'mes:wm-transfer:update', 3, '', '', ''),
  ('super_admin', 'mes:wm-warehouse:create', 3, '', '', ''),
  ('super_admin', 'mes:wm-warehouse:delete', 3, '', '', ''),
  ('super_admin', 'mes:wm-warehouse:query', 3, '', '', ''),
  ('super_admin', 'mes:wm-warehouse:update', 3, '', '', ''),
  ('super_admin', 'mes:wm:misc-receipt:cancel', 3, '', '', NULL),
  ('super_admin', 'mes:wm:misc-receipt:create', 3, '', '', NULL),
  ('super_admin', 'mes:wm:misc-receipt:delete', 3, '', '', NULL),
  ('super_admin', 'mes:wm:misc-receipt:export', 3, '', '', NULL),
  ('super_admin', 'mes:wm:misc-receipt:finish', 3, '', '', NULL),
  ('super_admin', 'mes:wm:misc-receipt:query', 3, '', '', NULL),
  ('super_admin', 'mes:wm:misc-receipt:submit', 3, '', '', NULL),
  ('super_admin', 'mes:wm:misc-receipt:update', 3, '', '', NULL),
  ('super_admin', 'mp:account:clear-quota', 3, '', '', NULL),
  ('super_admin', 'mp:account:create', 3, '', '', NULL),
  ('super_admin', 'mp:account:delete', 3, '', '', NULL),
  ('super_admin', 'mp:account:qr-code', 3, '', '', NULL),
  ('super_admin', 'mp:account:query', 3, '', '', NULL),
  ('super_admin', 'mp:account:update', 3, '', '', NULL),
  ('super_admin', 'mp:auto-reply:create', 3, '', '', NULL),
  ('super_admin', 'mp:auto-reply:delete', 3, '', '', NULL),
  ('super_admin', 'mp:auto-reply:query', 3, '', '', NULL),
  ('super_admin', 'mp:auto-reply:update', 3, '', '', NULL),
  ('super_admin', 'mp:draft:create', 3, '', '', NULL),
  ('super_admin', 'mp:draft:delete', 3, '', '', NULL),
  ('super_admin', 'mp:draft:query', 3, '', '', NULL),
  ('super_admin', 'mp:draft:update', 3, '', '', NULL),
  ('super_admin', 'mp:free-publish:delete', 3, '', '', NULL),
  ('super_admin', 'mp:free-publish:query', 3, '', '', NULL),
  ('super_admin', 'mp:free-publish:submit', 3, '', '', NULL),
  ('super_admin', 'mp:material:delete', 3, '', '', NULL),
  ('super_admin', 'mp:material:query', 3, '', '', NULL),
  ('super_admin', 'mp:material:upload-news-image', 3, '', '', NULL),
  ('super_admin', 'mp:material:upload-permanent', 3, '', '', NULL),
  ('super_admin', 'mp:material:upload-temporary', 3, '', '', NULL),
  ('super_admin', 'mp:menu:delete', 3, '', '', NULL),
  ('super_admin', 'mp:menu:query', 3, '', '', NULL),
  ('super_admin', 'mp:menu:save', 3, '', '', NULL),
  ('super_admin', 'mp:message-template:delete', 3, '', '', ''),
  ('super_admin', 'mp:message-template:query', 3, '', '', ''),
  ('super_admin', 'mp:message-template:send', 3, '', '', ''),
  ('super_admin', 'mp:message-template:sync', 3, '', '', ''),
  ('super_admin', 'mp:message:query', 3, '', '', NULL),
  ('super_admin', 'mp:message:send', 3, '', '', NULL),
  ('super_admin', 'mp:statistics:query', 2, 'statistics', 'mp/statistics/index', 'MpStatistics'),
  ('super_admin', 'mp:tag:create', 3, '', '', NULL),
  ('super_admin', 'mp:tag:delete', 3, '', '', NULL),
  ('super_admin', 'mp:tag:query', 3, '', '', NULL),
  ('super_admin', 'mp:tag:sync', 3, '', '', NULL),
  ('super_admin', 'mp:tag:update', 3, '', '', NULL),
  ('super_admin', 'mp:user:query', 3, '', '', NULL),
  ('super_admin', 'mp:user:sync', 3, '', '', NULL),
  ('super_admin', 'mp:user:update', 3, '', '', NULL),
  ('super_admin', 'pay:app:create', 3, '', '', NULL),
  ('super_admin', 'pay:app:delete', 3, '', '', NULL),
  ('super_admin', 'pay:app:query', 3, '', '', NULL),
  ('super_admin', 'pay:app:update', 3, '', '', NULL),
  ('super_admin', 'pay:channel:create', 3, '', '', ''),
  ('super_admin', 'pay:channel:delete', 3, '', '', ''),
  ('super_admin', 'pay:channel:parsing', 3, '', '', NULL),
  ('super_admin', 'pay:channel:query', 3, '', '', ''),
  ('super_admin', 'pay:channel:update', 3, '', '', ''),
  ('super_admin', 'pay:merchant:create', 3, '', '', NULL),
  ('super_admin', 'pay:merchant:delete', 3, '', '', NULL),
  ('super_admin', 'pay:merchant:export', 3, '', '', NULL),
  ('super_admin', 'pay:merchant:query', 3, '', '', NULL),
  ('super_admin', 'pay:merchant:update', 3, '', '', NULL),
  ('super_admin', 'pay:notify:query', 3, '', '', NULL),
  ('super_admin', 'pay:order:export', 3, '', '', NULL),
  ('super_admin', 'pay:order:query', 3, '', '', NULL),
  ('super_admin', 'pay:refund:export', 3, '', '', NULL),
  ('super_admin', 'pay:refund:query', 3, '', '', NULL),
  ('super_admin', 'pay:transfer:export', 3, '', '', ''),
  ('super_admin', 'pay:transfer:query', 3, '', '', ''),
  ('super_admin', 'pay:wallet-recharge-package:create', 3, '', '', NULL),
  ('super_admin', 'pay:wallet-recharge-package:delete', 3, '', '', NULL),
  ('super_admin', 'pay:wallet-recharge-package:query', 3, '', '', NULL),
  ('super_admin', 'pay:wallet-recharge-package:update', 3, '', '', NULL),
  ('super_admin', 'pay:wallet:query', 3, '', '', NULL),
  ('super_admin', 'pay:wallet:update-balance', 3, '', '', ''),
  ('super_admin', 'point:record:query', 3, '', '', NULL),
  ('super_admin', 'point:sign-in-config:create', 3, '', '', NULL),
  ('super_admin', 'point:sign-in-config:delete', 3, '', '', NULL),
  ('super_admin', 'point:sign-in-config:query', 3, '', '', NULL),
  ('super_admin', 'point:sign-in-config:update', 3, '', '', NULL),
  ('super_admin', 'point:sign-in-record:delete', 3, '', '', NULL),
  ('super_admin', 'point:sign-in-record:query', 3, '', '', NULL),
  ('super_admin', 'product:brand:create', 3, '', '', NULL),
  ('super_admin', 'product:brand:delete', 3, '', '', NULL),
  ('super_admin', 'product:brand:query', 3, '', '', NULL),
  ('super_admin', 'product:brand:update', 3, '', '', NULL),
  ('super_admin', 'product:browse-history:query', 3, '', '', ''),
  ('super_admin', 'product:category:create', 3, '', '', NULL),
  ('super_admin', 'product:category:delete', 3, '', '', NULL),
  ('super_admin', 'product:category:query', 3, '', '', NULL),
  ('super_admin', 'product:category:update', 3, '', '', NULL),
  ('super_admin', 'product:comment:create', 3, '', '', ''),
  ('super_admin', 'product:comment:query', 3, '', '', ''),
  ('super_admin', 'product:comment:update', 3, '', '', ''),
  ('super_admin', 'product:favorite:query', 3, '', '', ''),
  ('super_admin', 'product:property:create', 3, '', '', NULL),
  ('super_admin', 'product:property:delete', 3, '', '', NULL),
  ('super_admin', 'product:property:query', 3, '', '', NULL),
  ('super_admin', 'product:property:update', 3, '', '', NULL),
  ('super_admin', 'product:spu:create', 3, '', '', NULL),
  ('super_admin', 'product:spu:delete', 3, '', '', NULL),
  ('super_admin', 'product:spu:export', 3, '', '', NULL),
  ('super_admin', 'product:spu:query', 3, '', '', NULL),
  ('super_admin', 'product:spu:update', 3, '', '', NULL),
  ('super_admin', 'promotion:article-category:create', 3, '', '', NULL),
  ('super_admin', 'promotion:article-category:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:article-category:query', 3, '', '', NULL),
  ('super_admin', 'promotion:article-category:update', 3, '', '', NULL),
  ('super_admin', 'promotion:article:create', 3, '', '', NULL),
  ('super_admin', 'promotion:article:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:article:query', 3, '', '', NULL),
  ('super_admin', 'promotion:article:update', 3, '', '', NULL),
  ('super_admin', 'promotion:banner:create', 3, '', '', ''),
  ('super_admin', 'promotion:banner:delete', 3, '', '', ''),
  ('super_admin', 'promotion:banner:query', 3, '', '', ''),
  ('super_admin', 'promotion:banner:update', 3, '', '', ''),
  ('super_admin', 'promotion:bargain-activity:close', 3, '', '', ''),
  ('super_admin', 'promotion:bargain-activity:create', 3, '', '', ''),
  ('super_admin', 'promotion:bargain-activity:delete', 3, '', '', ''),
  ('super_admin', 'promotion:bargain-activity:query', 3, '', '', ''),
  ('super_admin', 'promotion:bargain-activity:update', 3, '', '', ''),
  ('super_admin', 'promotion:bargain-help:query', 3, '', '', ''),
  ('super_admin', 'promotion:bargain-record:query', 3, '', '', NULL),
  ('super_admin', 'promotion:combination-activity:close', 3, '', '', ''),
  ('super_admin', 'promotion:combination-activity:create', 3, '', '', ''),
  ('super_admin', 'promotion:combination-activity:delete', 3, '', '', ''),
  ('super_admin', 'promotion:combination-activity:query', 3, '', '', ''),
  ('super_admin', 'promotion:combination-activity:update', 3, '', '', ''),
  ('super_admin', 'promotion:combination-record:query', 2, 'record', 'mall/promotion/combination/record/index.vue', 'PromotionCombinationRecord'),
  ('super_admin', 'promotion:coupon-template:create', 3, '', '', NULL),
  ('super_admin', 'promotion:coupon-template:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:coupon-template:query', 3, '', '', NULL),
  ('super_admin', 'promotion:coupon-template:update', 3, '', '', NULL),
  ('super_admin', 'promotion:coupon:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:coupon:query', 3, '', '', NULL),
  ('super_admin', 'promotion:coupon:send', 3, '', '', ''),
  ('super_admin', 'promotion:discount-activity:close', 3, '', '', NULL),
  ('super_admin', 'promotion:discount-activity:create', 3, '', '', NULL),
  ('super_admin', 'promotion:discount-activity:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:discount-activity:query', 3, '', '', NULL),
  ('super_admin', 'promotion:discount-activity:update', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-page:create', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-page:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-page:query', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-page:update', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-template:create', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-template:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-template:query', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-template:update', 3, '', '', NULL),
  ('super_admin', 'promotion:diy-template:use', 3, '', '', NULL),
  ('super_admin', 'promotion:kefu-conversation:delete', 3, '', '', ''),
  ('super_admin', 'promotion:kefu-conversation:query', 3, '', '', ''),
  ('super_admin', 'promotion:kefu-conversation:update', 3, '', '', ''),
  ('super_admin', 'promotion:kefu-message:query', 3, '', '', ''),
  ('super_admin', 'promotion:kefu-message:send', 3, '', '', ''),
  ('super_admin', 'promotion:kefu-message:update', 3, '', '', ''),
  ('super_admin', 'promotion:point-activity:close', 3, '', '', ''),
  ('super_admin', 'promotion:point-activity:create', 3, '', '', ''),
  ('super_admin', 'promotion:point-activity:delete', 3, '', '', ''),
  ('super_admin', 'promotion:point-activity:export', 3, '', '', ''),
  ('super_admin', 'promotion:point-activity:query', 3, '', '', ''),
  ('super_admin', 'promotion:point-activity:update', 3, '', '', ''),
  ('super_admin', 'promotion:reward-activity:close', 3, '', '', NULL),
  ('super_admin', 'promotion:reward-activity:create', 3, '', '', NULL),
  ('super_admin', 'promotion:reward-activity:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:reward-activity:query', 3, '', '', NULL),
  ('super_admin', 'promotion:reward-activity:update', 3, '', '', NULL),
  ('super_admin', 'promotion:seckill-activity:close', 3, '', '', ''),
  ('super_admin', 'promotion:seckill-activity:create', 3, '', '', NULL),
  ('super_admin', 'promotion:seckill-activity:delete', 3, '', '', NULL),
  ('super_admin', 'promotion:seckill-activity:query', 3, '', '', NULL),
  ('super_admin', 'promotion:seckill-activity:update', 3, '', '', NULL),
  ('super_admin', 'promotion:seckill-config:create', 3, '', '', ''),
  ('super_admin', 'promotion:seckill-config:delete', 3, '', '', ''),
  ('super_admin', 'promotion:seckill-config:query', 3, '', '', ''),
  ('super_admin', 'promotion:seckill-config:update', 3, '', '', ''),
  ('super_admin', 'report:go-view-data:get-by-http', 3, '', '', NULL),
  ('super_admin', 'report:go-view-data:get-by-sql', 3, '', '', NULL),
  ('super_admin', 'report:go-view-project:create', 3, '', '', NULL),
  ('super_admin', 'report:go-view-project:delete', 3, '', '', ''),
  ('super_admin', 'report:go-view-project:query', 3, '', '', NULL),
  ('super_admin', 'report:go-view-project:update', 3, '', '', ''),
  ('super_admin', 'signature-governance:csv-package:manage', 3, '', '', ''),
  ('super_admin', 'signature-governance:csv-package:query', 2, 'csv-package', 'signature-governance/index', 'SignatureGovernanceCsvPackage'),
  ('super_admin', 'signature-governance:periodic-review:manage', 3, '', '', ''),
  ('super_admin', 'signature-governance:periodic-review:query', 2, 'periodic-review', 'signature-governance/index', 'SignatureGovernancePeriodicReview'),
  ('super_admin', 'signature-governance:policy:manage', 3, '', '', ''),
  ('super_admin', 'signature-governance:policy:query', 2, '/signature-governance', 'signature-governance/index', 'SignatureGovernanceWorkbench'),
  ('super_admin', 'signature-governance:retention:manage', 3, '', '', ''),
  ('super_admin', 'signature-governance:retention:query', 2, 'retention', 'signature-governance/index', 'SignatureGovernanceRetention'),
  ('super_admin', 'srm:code-rule:create', 3, '', '', ''),
  ('super_admin', 'srm:code-rule:enable', 3, '', '', ''),
  ('super_admin', 'srm:code-rule:query', 2, 'code-rule', 'srm/code-rule/index', 'SrmCodeRule'),
  ('super_admin', 'srm:code-rule:update', 3, '', '', ''),
  ('super_admin', 'srm:framework-plan:agreement', 3, '', '', ''),
  ('super_admin', 'srm:framework-plan:audit', 3, '', '', ''),
  ('super_admin', 'srm:framework-plan:create', 3, '', '', ''),
  ('super_admin', 'srm:framework-plan:query', 2, 'framework-plan', 'srm/framework-plan/index', 'SrmFrameworkPlan'),
  ('super_admin', 'srm:framework-plan:submit', 3, '', '', ''),
  ('super_admin', 'srm:nas-locator:config', 3, '', '', ''),
  ('super_admin', 'srm:nas-locator:download', 3, '', '', ''),
  ('super_admin', 'srm:nas-locator:query', 3, '', '', ''),
  ('super_admin', 'srm:nas-locator:refresh', 3, '', '', ''),
  ('super_admin', 'srm:non-bidding-project:contract', 3, '', '', ''),
  ('super_admin', 'srm:non-bidding-project:deal', 3, '', '', ''),
  ('super_admin', 'srm:non-bidding-project:publish', 3, '', '', ''),
  ('super_admin', 'srm:non-bidding-project:query', 2, 'non-bidding-project', 'srm/non-bidding-project/index', 'SrmNonBiddingProject'),
  ('super_admin', 'srm:non-bidding-project:quote', 3, '', '', ''),
  ('super_admin', 'srm:outsource-execution:create', 3, '', '', ''),
  ('super_admin', 'srm:outsource-execution:query', 3, '', '', ''),
  ('super_admin', 'srm:outsource-execution:update', 3, '', '', ''),
  ('super_admin', 'srm:payment-execution:approve', 3, '', '', ''),
  ('super_admin', 'srm:payment-execution:create', 3, '', '', ''),
  ('super_admin', 'srm:payment-execution:query', 3, '', '', ''),
  ('super_admin', 'srm:procurement-contract:cancel', 3, '', '', ''),
  ('super_admin', 'srm:procurement-contract:create', 3, '', '', ''),
  ('super_admin', 'srm:procurement-contract:delete', 3, '', '', ''),
  ('super_admin', 'srm:procurement-contract:query', 3, '', '', ''),
  ('super_admin', 'srm:procurement-plan:audit', 3, '', '', ''),
  ('super_admin', 'srm:procurement-plan:create', 3, '', '', ''),
  ('super_admin', 'srm:procurement-plan:generate', 3, '', '', ''),
  ('super_admin', 'srm:procurement-plan:query', 2, 'procurement-plan', 'srm/procurement-plan/index', 'SrmProcurementPlan'),
  ('super_admin', 'srm:procurement-plan:submit', 3, '', '', ''),
  ('super_admin', 'srm:purchase-order:create', 3, '', '', ''),
  ('super_admin', 'srm:purchase-order:query', 3, '', '', ''),
  ('super_admin', 'srm:supplier-access:audit', 3, '', '', ''),
  ('super_admin', 'srm:supplier-access:check', 3, '', '', ''),
  ('super_admin', 'srm:supplier-access:create', 3, '', '', ''),
  ('super_admin', 'srm:supplier-access:delete', 3, '', '', ''),
  ('super_admin', 'srm:supplier-access:enable', 3, '', '', ''),
  ('super_admin', 'srm:supplier-access:query', 2, 'access', 'srm/supplier-access/index', 'SrmSupplierAccess'),
  ('super_admin', 'srm:supplier-access:update', 3, '', '', ''),
  ('super_admin', 'srm:supplier-portal:audit', 3, '', '', ''),
  ('super_admin', 'srm:supplier-portal:review', 2, 'supplier-portal-review', 'srm/supplier-portal/review/index', 'SrmSupplierPortalReview'),
  ('super_admin', 'srm:supplier-profile:query', 2, 'profile', 'srm/supplier-profile/index', 'SrmSupplierProfile'),
  ('super_admin', 'srm:supplier-risk:create', 3, '', '', ''),
  ('super_admin', 'srm:supplier-risk:query', 2, 'risk', 'srm/supplier-risk/index', 'SrmSupplierRisk'),
  ('super_admin', 'srm:supplier-risk:resolve', 3, '', '', ''),
  ('super_admin', 'srm:tender-project:candidate', 3, '', '', ''),
  ('super_admin', 'srm:tender-project:committee', 3, '', '', ''),
  ('super_admin', 'srm:tender-project:expert', 3, '', '', ''),
  ('super_admin', 'srm:tender-project:publish', 3, '', '', ''),
  ('super_admin', 'srm:tender-project:query', 3, '', '', ''),
  ('super_admin', 'srm:tender-project:submit-bid', 3, '', '', ''),
  ('super_admin', 'srm:tender-project:winning', 3, '', '', ''),
  ('super_admin', 'statistics:member:query', 3, '', '', NULL),
  ('super_admin', 'statistics:product:export', 3, '', '', ''),
  ('super_admin', 'statistics:product:query', 3, '', '', ''),
  ('super_admin', 'statistics:trade:export', 3, '', '', NULL),
  ('super_admin', 'statistics:trade:query', 3, '', '', NULL),
  ('super_admin', 'system:backup-plan:execute', 3, '', '', ''),
  ('super_admin', 'system:backup-plan:query', 2, 'backup-plan', 'system/backup-plan/index', 'SystemBackupPlan'),
  ('super_admin', 'system:backup-plan:update', 3, '', '', ''),
  ('super_admin', 'system:codex-test:query', 2, 'codex-test-record', 'system/codex-test-record/index', 'SystemCodexTestRecord'),
  ('super_admin', 'system:config-package:export', 3, '', '', ''),
  ('super_admin', 'system:config-package:import', 3, '', '', ''),
  ('super_admin', 'system:config-package:query', 2, 'config-package', 'system/config-package/index', 'SystemConfigPackage'),
  ('super_admin', 'system:dept:create', 3, '', '', NULL),
  ('super_admin', 'system:dept:delete', 3, '', '', NULL),
  ('super_admin', 'system:dept:query', 3, '', '', NULL),
  ('super_admin', 'system:dept:update', 3, '', '', NULL),
  ('super_admin', 'system:dict:create', 3, '', '', NULL),
  ('super_admin', 'system:dict:delete', 3, '', '', NULL),
  ('super_admin', 'system:dict:export', 3, '#', '', NULL),
  ('super_admin', 'system:dict:query', 3, '#', '', NULL),
  ('super_admin', 'system:dict:update', 3, '', '', NULL),
  ('super_admin', 'system:login-log:export', 3, '#', '', NULL),
  ('super_admin', 'system:login-log:query', 3, '#', '', NULL),
  ('super_admin', 'system:mail-account:create', 3, '', '', NULL),
  ('super_admin', 'system:mail-account:delete', 3, '', '', NULL),
  ('super_admin', 'system:mail-account:query', 3, '', '', NULL),
  ('super_admin', 'system:mail-account:update', 3, '', '', NULL),
  ('super_admin', 'system:mail-log:query', 3, '', '', NULL),
  ('super_admin', 'system:mail-template:create', 3, '', '', NULL),
  ('super_admin', 'system:mail-template:delete', 3, '', '', NULL),
  ('super_admin', 'system:mail-template:query', 3, '', '', NULL),
  ('super_admin', 'system:mail-template:send-mail', 3, '', '', NULL),
  ('super_admin', 'system:mail-template:update', 3, '', '', NULL),
  ('super_admin', 'system:menu:create', 3, '', '', NULL),
  ('super_admin', 'system:menu:delete', 3, '', '', NULL),
  ('super_admin', 'system:menu:query', 3, '', '', NULL),
  ('super_admin', 'system:menu:update', 3, '', '', NULL),
  ('super_admin', 'system:notice:create', 3, '', '', NULL),
  ('super_admin', 'system:notice:delete', 3, '', '', NULL),
  ('super_admin', 'system:notice:query', 3, '#', '', NULL),
  ('super_admin', 'system:notice:update', 3, '', '', NULL),
  ('super_admin', 'system:notify-message:query', 3, '', '', NULL),
  ('super_admin', 'system:notify-template:create', 3, '', '', NULL),
  ('super_admin', 'system:notify-template:delete', 3, '', '', NULL),
  ('super_admin', 'system:notify-template:query', 3, '', '', NULL),
  ('super_admin', 'system:notify-template:send-notify', 3, '', '', NULL),
  ('super_admin', 'system:notify-template:update', 3, '', '', NULL),
  ('super_admin', 'system:oauth2-client:create', 3, '', '', NULL),
  ('super_admin', 'system:oauth2-client:delete', 3, '', '', NULL),
  ('super_admin', 'system:oauth2-client:query', 3, '', '', NULL),
  ('super_admin', 'system:oauth2-client:update', 3, '', '', NULL),
  ('super_admin', 'system:oauth2-token:delete', 3, '', '', NULL),
  ('super_admin', 'system:oauth2-token:page', 3, '', '', NULL),
  ('super_admin', 'system:operate-log:export', 3, '', '', NULL),
  ('super_admin', 'system:operate-log:query', 3, '', '', NULL),
  ('super_admin', 'system:permission:assign-role-data-scope', 3, '', '', NULL),
  ('super_admin', 'system:permission:assign-role-menu', 3, '', '', NULL),
  ('super_admin', 'system:permission:assign-user-role', 3, '', '', NULL),
  ('super_admin', 'system:post:create', 3, '', '', NULL),
  ('super_admin', 'system:post:delete', 3, '', '', NULL),
  ('super_admin', 'system:post:export', 3, '', '', NULL),
  ('super_admin', 'system:post:query', 3, '', '', NULL),
  ('super_admin', 'system:post:update', 3, '', '', NULL),
  ('super_admin', 'system:role:create', 3, '', '', NULL),
  ('super_admin', 'system:role:delete', 3, '', '', NULL),
  ('super_admin', 'system:role:export', 3, '', '', NULL),
  ('super_admin', 'system:role:query', 3, '', '', NULL),
  ('super_admin', 'system:role:update', 3, '', '', NULL),
  ('super_admin', 'system:sms-channel:create', 3, '', '', NULL),
  ('super_admin', 'system:sms-channel:delete', 3, '', '', NULL),
  ('super_admin', 'system:sms-channel:query', 3, '', '', NULL),
  ('super_admin', 'system:sms-channel:update', 3, '', '', NULL),
  ('super_admin', 'system:sms-log:export', 3, '', '', NULL),
  ('super_admin', 'system:sms-log:query', 3, '', '', NULL),
  ('super_admin', 'system:sms-template:create', 3, '', '', NULL),
  ('super_admin', 'system:sms-template:delete', 3, '', '', NULL),
  ('super_admin', 'system:sms-template:export', 3, '', '', NULL),
  ('super_admin', 'system:sms-template:query', 3, '', '', NULL),
  ('super_admin', 'system:sms-template:send-sms', 3, '', '', NULL),
  ('super_admin', 'system:sms-template:update', 3, '', '', NULL),
  ('super_admin', 'system:social-client:create', 3, '', '', ''),
  ('super_admin', 'system:social-client:delete', 3, '', '', ''),
  ('super_admin', 'system:social-client:query', 3, '', '', ''),
  ('super_admin', 'system:social-client:update', 3, '', '', ''),
  ('super_admin', 'system:social-user:query', 2, 'user', 'system/social/user/index.vue', 'SocialUser'),
  ('super_admin', 'system:tenant-package:create', 3, '', '', NULL),
  ('super_admin', 'system:tenant-package:delete', 3, '', '', NULL),
  ('super_admin', 'system:tenant-package:query', 3, '', '', NULL),
  ('super_admin', 'system:tenant-package:update', 3, '', '', NULL),
  ('super_admin', 'system:tenant:create', 3, '', '', NULL),
  ('super_admin', 'system:tenant:delete', 3, '', '', NULL),
  ('super_admin', 'system:tenant:export', 3, '', '', NULL),
  ('super_admin', 'system:tenant:query', 3, '', '', NULL),
  ('super_admin', 'system:tenant:update', 3, '', '', NULL),
  ('super_admin', 'system:tenant:visit', 3, '', '', ''),
  ('super_admin', 'system:user:create', 3, '', '', NULL),
  ('super_admin', 'system:user:delete', 3, '', '', NULL),
  ('super_admin', 'system:user:export', 3, '', '', NULL),
  ('super_admin', 'system:user:import', 3, '', '', NULL),
  ('super_admin', 'system:user:list', 2, 'user', 'system/user/index', 'SystemUser'),
  ('super_admin', 'system:user:query', 3, '', '', NULL),
  ('super_admin', 'system:user:update', 3, '', '', NULL),
  ('super_admin', 'system:user:update-password', 3, '', '', NULL),
  ('super_admin', 'trade:after-sale:agree', 3, '', '', ''),
  ('super_admin', 'trade:after-sale:disagree', 3, '', '', ''),
  ('super_admin', 'trade:after-sale:query', 3, '', '', NULL),
  ('super_admin', 'trade:after-sale:receive', 3, '', '', ''),
  ('super_admin', 'trade:after-sale:refund', 3, '', '', ''),
  ('super_admin', 'trade:brokerage-record:query', 3, '', '', NULL),
  ('super_admin', 'trade:brokerage-user:clear-bind-user', 3, '', '', ''),
  ('super_admin', 'trade:brokerage-user:create', 3, '', '', ''),
  ('super_admin', 'trade:brokerage-user:order-query', 3, '', '', NULL),
  ('super_admin', 'trade:brokerage-user:query', 3, '', '', NULL),
  ('super_admin', 'trade:brokerage-user:update-bind-user', 3, '', '', ''),
  ('super_admin', 'trade:brokerage-user:update-brokerage-enable', 3, '', '', NULL),
  ('super_admin', 'trade:brokerage-user:user-query', 3, '', '', NULL),
  ('super_admin', 'trade:brokerage-withdraw:audit', 3, '', '', NULL),
  ('super_admin', 'trade:brokerage-withdraw:query', 3, '', '', NULL),
  ('super_admin', 'trade:config:query', 3, '', '', NULL),
  ('super_admin', 'trade:config:save', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express-template:create', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express-template:delete', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express-template:export', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express-template:query', 2, 'express-template', 'mall/trade/delivery/expressTemplate/index', 'ExpressTemplate'),
  ('super_admin', 'trade:delivery:express-template:update', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express:create', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express:delete', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express:export', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express:query', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:express:update', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:pick-up-store:create', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:pick-up-store:delete', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:pick-up-store:export', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:pick-up-store:query', 3, '', '', NULL),
  ('super_admin', 'trade:delivery:pick-up-store:update', 3, '', '', NULL),
  ('super_admin', 'trade:order:pick-up', 3, '', '', ''),
  ('super_admin', 'trade:order:query', 3, '', '', ''),
  ('super_admin', 'trade:order:update', 3, '', '', ''),
  ('wenkong', 'dcc:controlled-file:approve', 3, '', '', ''),
  ('wenkong', 'dcc:controlled-file:category:manage', 2, 'controlled-file/categories', 'dcc/controlled-file/categories/index', 'DccControlledFileCategories'),
  ('wenkong', 'dcc:controlled-file:directory:manage', 2, 'controlled-file/directories', 'dcc/controlled-file/directories/index', 'DccControlledFileDirectories'),
  ('wenkong', 'dcc:controlled-file:download', 3, '', '', ''),
  ('wenkong', 'dcc:controlled-file:log:query', 2, 'controlled-file/logs', 'dcc/controlled-file/logs/index', 'DccControlledFileLogs'),
  ('wenkong', 'dcc:controlled-file:position:manage', 2, 'approval-role', 'dcc/controlled-file/positions/index', 'DccControlledFilePositions'),
  ('wenkong', 'dcc:controlled-file:preview', 3, '', '', ''),
  ('wenkong', 'dcc:controlled-file:print-template:manage', 2, 'controlled-file/print-template', 'dcc/controlled-file/print-template/index', 'DccApprovalPrintTemplate'),
  ('wenkong', 'dcc:controlled-file:query', 2, 'controlled-file/browser', 'dcc/controlled-file/browser/index', 'DccControlledFileBrowser'),
  ('wenkong', 'dcc:controlled-file:review', 3, '', '', ''),
  ('wenkong', 'dcc:controlled-file:route:manage', 2, 'controlled-file/routes', 'dcc/controlled-file/routes/index', 'DccControlledFileRoutes'),
  ('wenkong', 'dcc:controlled-file:stamp:retry', 3, '', '', ''),
  ('wenkong', 'dcc:controlled-file:submit', 2, 'controlled-file/upload', 'dcc/controlled-file/upload/index', 'DccControlledFileUpload'),
  ('wenkong', 'dcc:controlled-file:training:mine', 2, 'controlled-file/training-mine', 'dcc/controlled-file/training/mine/index', 'DccControlledFileTrainingMine'),
  ('wenkong', 'dcc:project-code-assignment:assign', 3, '', '', ''),
  ('wenkong', 'dcc:project-code-assignment:audit:query', 3, '', '', ''),
  ('wenkong', 'dcc:project-code-assignment:execute', 3, '', '', ''),
  ('wenkong', 'dcc:project-code-assignment:query', 3, '', '', ''),
  ('wenkong', 'dcc:project-code-assignment:revoke', 3, '', '', ''),
  ('wenkong', 'dcc:project-code:export', 3, '', '', ''),
  ('wenkong', 'dcc:project-code:import', 3, '', '', ''),
  ('wenkong', 'dcc:project-code:query', 2, 'project-code', 'dcc/controlled-file/basic-data/project-code/index', 'DccProjectCodeBasicDataPage'),
  ('wenkong', 'form:instance:create', 3, '', '', ''),
  ('wenkong', 'form:instance:submit', 3, '', '', ''),
  ('wenkong', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', ''),
  ('wenkong', 'signature-governance:policy:query', 2, '/signature-governance', 'signature-governance/index', 'SignatureGovernanceWorkbench'),
  ('wenkong', 'system:user:list', 2, 'user', 'system/user/index', 'SystemUser'),
  ('wenkong', 'system:user:query', 3, '', '', NULL),
  ('wenkong_download', 'dcc:controlled-file:download', 3, '', '', ''),
  ('wenkong_download', 'dcc:controlled-file:log:query', 2, 'controlled-file/logs', 'dcc/controlled-file/logs/index', 'DccControlledFileLogs'),
  ('wenkong_download', 'dcc:controlled-file:preview', 3, '', '', ''),
  ('wenkong_download', 'dcc:controlled-file:query', 2, 'controlled-file/browser', 'dcc/controlled-file/browser/index', 'DccControlledFileBrowser'),
  ('wenkong_download', 'mes:pro-feedback:frontline-pressure-pump:all-processes', 3, '', '', '');

  IF (SELECT COUNT(*) FROM `tmp_test_tenant1_role_permission_source`) <> 1676 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected source role-permission count';
  END IF;

  START TRANSACTION;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_target`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_missing_menu_target` (
    `source_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`source_key`)
  );

  WHILE (SELECT COUNT(*) FROM `tmp_test_tenant1_missing_menu_target`) <
        (SELECT COUNT(*) FROM `tmp_test_tenant1_missing_menu_source`) DO
    SET previous_menu_resolution_count = (SELECT COUNT(*) FROM `tmp_test_tenant1_missing_menu_target`);

    DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_parent_snapshot`;
    CREATE TEMPORARY TABLE `tmp_test_tenant1_missing_menu_parent_snapshot`
      LIKE `tmp_test_tenant1_missing_menu_target`;
    INSERT INTO `tmp_test_tenant1_missing_menu_parent_snapshot` (`source_key`, `menu_id`)
    SELECT `source_key`, `menu_id` FROM `tmp_test_tenant1_missing_menu_target`;

    INSERT IGNORE INTO `tmp_test_tenant1_missing_menu_target` (`source_key`, `menu_id`)
    SELECT `source`.`source_key`, MIN(`menu`.`id`)
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    LEFT JOIN `tmp_test_tenant1_missing_menu_parent_snapshot` AS `parent_target`
      ON `parent_target`.`source_key` = `source`.`parent_source_key`
    JOIN `system_menu` AS `menu`
      ON `menu`.`permission` = `source`.`permission`
     AND `menu`.`type` = `source`.`type`
     AND (`menu`.`path` <=> `source`.`path`)
     AND (`menu`.`component` <=> `source`.`component`)
     AND (`menu`.`component_name` <=> `source`.`component_name`)
     AND `menu`.`name` COLLATE utf8mb4_unicode_ci = `source`.`name`
     AND `menu`.`parent_id` = COALESCE(`parent_target`.`menu_id`, `source`.`parent_target_id_hint`)
     AND `menu`.`deleted` = b'0'
     AND `menu`.`status` = 0
    WHERE `source`.`parent_source_key` IS NULL OR `parent_target`.`menu_id` IS NOT NULL
    GROUP BY `source`.`source_key`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_parent_snapshot`;
    CREATE TEMPORARY TABLE `tmp_test_tenant1_missing_menu_parent_snapshot`
      LIKE `tmp_test_tenant1_missing_menu_target`;
    INSERT INTO `tmp_test_tenant1_missing_menu_parent_snapshot` (`source_key`, `menu_id`)
    SELECT `source_key`, `menu_id` FROM `tmp_test_tenant1_missing_menu_target`;

    INSERT INTO `system_menu`
      (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
       `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
    SELECT `source`.`name`, `source`.`permission`, `source`.`type`, `source`.`sort`,
           COALESCE(`parent_target`.`menu_id`, `source`.`parent_target_id_hint`),
           `source`.`path`, '', `source`.`component`, `source`.`component_name`,
           `source`.`status`, CAST(`source`.`visible` AS UNSIGNED), b'1', b'1',
           'test-tenant1-role-permission-sync', NOW(), 'test-tenant1-role-permission-sync', NOW(), b'0'
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    LEFT JOIN `tmp_test_tenant1_missing_menu_target` AS `existing_target`
      ON `existing_target`.`source_key` = `source`.`source_key`
    LEFT JOIN `tmp_test_tenant1_missing_menu_parent_snapshot` AS `parent_target`
      ON `parent_target`.`source_key` = `source`.`parent_source_key`
    WHERE `existing_target`.`menu_id` IS NULL
      AND (`source`.`parent_source_key` IS NULL OR `parent_target`.`menu_id` IS NOT NULL)
      AND NOT EXISTS (
        SELECT 1 FROM `system_menu` AS `existing_menu`
        WHERE `existing_menu`.`permission` = `source`.`permission`
          AND `existing_menu`.`type` = `source`.`type`
          AND (`existing_menu`.`path` <=> `source`.`path`)
          AND (`existing_menu`.`component` <=> `source`.`component`)
          AND (`existing_menu`.`component_name` <=> `source`.`component_name`)
          AND `existing_menu`.`name` COLLATE utf8mb4_unicode_ci = `source`.`name`
          AND `existing_menu`.`parent_id` = COALESCE(`parent_target`.`menu_id`, `source`.`parent_target_id_hint`)
          AND `existing_menu`.`deleted` = b'0'
      );

    INSERT IGNORE INTO `tmp_test_tenant1_missing_menu_target` (`source_key`, `menu_id`)
    SELECT `source`.`source_key`, MIN(`menu`.`id`)
    FROM `tmp_test_tenant1_missing_menu_source` AS `source`
    LEFT JOIN `tmp_test_tenant1_missing_menu_parent_snapshot` AS `parent_target`
      ON `parent_target`.`source_key` = `source`.`parent_source_key`
    JOIN `system_menu` AS `menu`
      ON `menu`.`permission` = `source`.`permission`
     AND `menu`.`type` = `source`.`type`
     AND (`menu`.`path` <=> `source`.`path`)
     AND (`menu`.`component` <=> `source`.`component`)
     AND (`menu`.`component_name` <=> `source`.`component_name`)
     AND `menu`.`name` COLLATE utf8mb4_unicode_ci = `source`.`name`
     AND `menu`.`parent_id` = COALESCE(`parent_target`.`menu_id`, `source`.`parent_target_id_hint`)
     AND `menu`.`deleted` = b'0'
     AND `menu`.`status` = 0
    WHERE `source`.`parent_source_key` IS NULL OR `parent_target`.`menu_id` IS NOT NULL
    GROUP BY `source`.`source_key`;

    SET current_menu_resolution_count = (SELECT COUNT(*) FROM `tmp_test_tenant1_missing_menu_target`);
    IF current_menu_resolution_count = previous_menu_resolution_count THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing source permissions after target menu resolution';
    END IF;
  END WHILE;

  INSERT INTO `system_role` (`name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`,
     `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `source`.`name`, `source`.`code`, `source`.`sort`, `category`.`category_id`,
         `source`.`data_scope`, `source`.`data_scope_dept_ids`, `source`.`status`, `source`.`type`, `source`.`remark`,
         'test-tenant1-role-permission-sync', NOW(), 'test-tenant1-role-permission-sync', NOW(), b'0', 1
  FROM `tmp_test_tenant1_role_source` AS `source`
  LEFT JOIN `tmp_test_tenant1_role_category_target` AS `category`
    ON `category`.`category_code` = `source`.`category_code`
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role` AS `existing`
    WHERE `existing`.`tenant_id` = 1
      AND `existing`.`code` = `source`.`code`
      AND `existing`.`deleted` = b'0'
  );

  UPDATE `system_role` AS `role`
  JOIN `tmp_test_tenant1_role_source` AS `source`
    ON `role`.`code` = `source`.`code`
  LEFT JOIN `tmp_test_tenant1_role_category_target` AS `category`
    ON `category`.`category_code` = `source`.`category_code`
  SET `role`.`name` = `source`.`name`,
      `role`.`sort` = `source`.`sort`,
      `role`.`category_id` = `category`.`category_id`,
      `role`.`data_scope` = `source`.`data_scope`,
      `role`.`data_scope_dept_ids` = `source`.`data_scope_dept_ids`,
      `role`.`status` = `source`.`status`,
      `role`.`type` = `source`.`type`,
      `role`.`remark` = `source`.`remark`,
      `role`.`updater` = 'test-tenant1-role-permission-sync',
      `role`.`update_time` = NOW()
  WHERE `role`.`tenant_id` = 1 AND `role`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_target`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_target` (
    `role_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `role_id` bigint NOT NULL,
    PRIMARY KEY (`role_code`),
    UNIQUE KEY (`role_id`)
  );

  INSERT INTO `tmp_test_tenant1_role_target` (`role_code`, `role_id`)
  SELECT `source`.`code`, `role`.`id`
  FROM `tmp_test_tenant1_role_source` AS `source`
  JOIN `system_role` AS `role`
    ON `role`.`code` = `source`.`code`
   AND `role`.`tenant_id` = 1
   AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_test_tenant1_role_target`) <> 60 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target role resolution did not produce 60 roles';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_permission_menu_target`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_permission_menu_target` (
    `role_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_code`, `permission`)
  );

  INSERT INTO `tmp_test_tenant1_permission_menu_target` (`role_code`, `permission`, `menu_id`)
  SELECT `ranked`.`role_code`, `ranked`.`permission`, `ranked`.`menu_id`
  FROM (
    SELECT `desired`.`role_code`, `desired`.`permission`, `menu`.`id` AS `menu_id`,
           ROW_NUMBER() OVER (
             PARTITION BY `desired`.`role_code`, `desired`.`permission`
             ORDER BY
               CASE
                 WHEN `menu`.`type` = `desired`.`source_menu_type`
                  AND (`menu`.`path` <=> `desired`.`source_path`)
                  AND (`menu`.`component` <=> `desired`.`source_component`)
                  AND (`menu`.`component_name` <=> `desired`.`source_component_name`) THEN 0
                 WHEN `menu`.`type` = `desired`.`source_menu_type` THEN 1
                 ELSE 2
               END,
               `menu`.`id`
           ) AS `row_number`
    FROM `tmp_test_tenant1_role_permission_source` AS `desired`
    JOIN `system_menu` AS `menu`
      ON `menu`.`permission` = `desired`.`permission`
     AND `menu`.`deleted` = b'0'
     AND `menu`.`status` = 0
  ) AS `ranked`
  WHERE `ranked`.`row_number` = 1;

  IF (SELECT COUNT(*) FROM `tmp_test_tenant1_permission_menu_target`) <> 1676 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing source permissions after target menu resolution';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_role` AS `role`
    ON `role`.`id` = `role_menu`.`role_id`
   AND `role`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `tmp_test_tenant1_role_source` AS `source_role`
    ON `role`.`code` = `source_role`.`code`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `role_menu`.`menu_id`
   AND `menu`.`deleted` = b'0'
   AND `menu`.`status` = 0
  LEFT JOIN `tmp_test_tenant1_role_permission_source` AS `desired`
    ON `desired`.`role_code` = `source_role`.`code`
   AND `desired`.`permission` = `menu`.`permission`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'test-tenant1-role-permission-sync',
      `role_menu`.`update_time` = NOW()
  WHERE `role`.`tenant_id` = 1
    AND `role_menu`.`tenant_id` = 1
    AND `role_menu`.`deleted` = b'0'
    AND `menu`.`permission` <> ''
    AND `desired`.`permission` IS NULL;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_desired`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_menu_desired` (
    `role_id` bigint NOT NULL,
    `role_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `permission`)
  );

  INSERT INTO `tmp_test_tenant1_role_menu_desired` (`role_id`, `role_code`, `permission`, `menu_id`)
  SELECT `role`.`role_id`, `permission_menu`.`role_code`, `permission_menu`.`permission`, `permission_menu`.`menu_id`
  FROM `tmp_test_tenant1_permission_menu_target` AS `permission_menu`
  JOIN `tmp_test_tenant1_role_target` AS `role`
    ON `role`.`role_code` = `permission_menu`.`role_code`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_missing`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_menu_missing` AS
  SELECT `desired`.`role_id`, `desired`.`role_code`, `desired`.`permission`, `desired`.`menu_id`
  FROM `tmp_test_tenant1_role_menu_desired` AS `desired`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    JOIN `system_menu` AS `existing_menu`
      ON `existing_menu`.`id` = `existing`.`menu_id`
     AND `existing_menu`.`deleted` = b'0'
     AND `existing_menu`.`status` = 0
    WHERE `existing`.`role_id` = `desired`.`role_id`
      AND `existing`.`tenant_id` = 1
      AND `existing`.`deleted` = b'0'
      AND `existing_menu`.`permission` = `desired`.`permission`
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_restore`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_menu_restore` (
    `id` bigint NOT NULL,
    PRIMARY KEY (`id`)
  );

  INSERT INTO `tmp_test_tenant1_role_menu_restore` (`id`)
  SELECT MIN(`existing`.`id`)
  FROM `tmp_test_tenant1_role_menu_missing` AS `desired`
  JOIN `system_role_menu` AS `existing`
    ON `existing`.`role_id` = `desired`.`role_id`
   AND `existing`.`menu_id` = `desired`.`menu_id`
   AND `existing`.`tenant_id` = 1
   AND `existing`.`deleted` = b'1'
  GROUP BY `desired`.`role_id`, `desired`.`menu_id`;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_test_tenant1_role_menu_restore` AS `restore`
    ON `restore`.`id` = `role_menu`.`id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'test-tenant1-role-permission-sync',
      `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `desired`.`role_id`, `desired`.`menu_id`,
         'test-tenant1-role-permission-sync', NOW(), 'test-tenant1-role-permission-sync', NOW(), b'0', 1
  FROM `tmp_test_tenant1_role_menu_missing` AS `desired`
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `desired`.`role_id`
      AND `existing`.`menu_id` = `desired`.`menu_id`
      AND `existing`.`tenant_id` = 1
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_ancestor`;
  CREATE TEMPORARY TABLE `tmp_test_tenant1_role_menu_ancestor` (
    `role_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_test_tenant1_role_menu_ancestor` (`role_id`, `menu_id`)
  WITH RECURSIVE `ancestor_tree` AS (
    SELECT `desired`.`role_id`, `parent`.`id` AS `menu_id`, `parent`.`parent_id`, `parent`.`permission`, 1 AS `depth`
    FROM `tmp_test_tenant1_role_menu_desired` AS `desired`
    JOIN `system_menu` AS `menu` ON `menu`.`id` = `desired`.`menu_id`
    JOIN `system_menu` AS `parent`
      ON `parent`.`id` = `menu`.`parent_id`
     AND `parent`.`deleted` = b'0'
     AND `parent`.`status` = 0
    UNION ALL
    SELECT `tree`.`role_id`, `parent`.`id`, `parent`.`parent_id`, `parent`.`permission`, `tree`.`depth` + 1
    FROM `ancestor_tree` AS `tree`
    JOIN `system_menu` AS `parent`
      ON `parent`.`id` = `tree`.`parent_id`
     AND `parent`.`deleted` = b'0'
     AND `parent`.`status` = 0
    WHERE `tree`.`depth` < 20
  )
  SELECT DISTINCT `tree`.`role_id`, `tree`.`menu_id`
  FROM `ancestor_tree` AS `tree`
  WHERE `tree`.`permission` = '' OR `tree`.`permission` IS NULL;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_test_tenant1_role_menu_ancestor` AS `ancestor`
    ON `ancestor`.`role_id` = `role_menu`.`role_id`
   AND `ancestor`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'test-tenant1-role-permission-sync',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`tenant_id` = 1 AND `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `ancestor`.`role_id`, `ancestor`.`menu_id`,
         'test-tenant1-role-permission-sync', NOW(), 'test-tenant1-role-permission-sync', NOW(), b'0', 1
  FROM `tmp_test_tenant1_role_menu_ancestor` AS `ancestor`
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `ancestor`.`role_id`
      AND `existing`.`menu_id` = `ancestor`.`menu_id`
      AND `existing`.`tenant_id` = 1
      AND `existing`.`deleted` = b'0'
  );

  IF EXISTS (
    SELECT 1
    FROM `tmp_test_tenant1_role_permission_source` AS `desired`
    JOIN `tmp_test_tenant1_role_target` AS `target_role`
      ON `target_role`.`role_code` = `desired`.`role_code`
    WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `system_menu` AS `menu`
        ON `menu`.`id` = `role_menu`.`menu_id`
       AND `menu`.`deleted` = b'0'
       AND `menu`.`status` = 0
      WHERE `role_menu`.`role_id` = `target_role`.`role_id`
        AND `role_menu`.`tenant_id` = 1
        AND `role_menu`.`deleted` = b'0'
        AND `menu`.`permission` = `desired`.`permission`
    )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target role is missing a source permission after sync';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_role` AS `role`
    JOIN `tmp_test_tenant1_role_source` AS `source_role`
      ON `role`.`code` = `source_role`.`code`
    JOIN `system_role_menu` AS `role_menu`
      ON `role_menu`.`role_id` = `role`.`id`
     AND `role_menu`.`tenant_id` = `role`.`tenant_id`
     AND `role_menu`.`deleted` = b'0'
    JOIN `system_menu` AS `menu`
      ON `menu`.`id` = `role_menu`.`menu_id`
     AND `menu`.`deleted` = b'0'
     AND `menu`.`status` = 0
    LEFT JOIN `tmp_test_tenant1_role_permission_source` AS `desired`
      ON `desired`.`role_code` = `source_role`.`code`
     AND `desired`.`permission` = `menu`.`permission`
    WHERE `role`.`tenant_id` = 1
      AND `role`.`deleted` = b'0'
      AND `menu`.`permission` <> ''
      AND `desired`.`permission` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target role retains an extra effective permission after sync';
  END IF;

  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_ancestor`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_restore`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_missing`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_menu_desired`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_permission_menu_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_parent_snapshot`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_permission_source`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_missing_menu_source`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_category_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_test_tenant1_role_source`;
END//
DELIMITER ;

CALL sync_test_tenant1_all_role_permissions();

DROP PROCEDURE IF EXISTS sync_test_tenant1_all_role_permissions;
