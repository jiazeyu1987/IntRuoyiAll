-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260623_dcc_view_matrix_independent_source; type=seed; riskLevel=low
-- DCC independent view matrix seed generated from 电子文控系统推进计划及需求表.xlsx, sheet 文件查阅矩阵.
-- This script is fail-fast by design. Set @dcc_view_matrix_seed_tenant_id before sourcing.
-- Example: SET @dcc_view_matrix_seed_tenant_id := 122; SOURCE sql/mysql/20260624_dcc_view_matrix_independent_seed.sql;
-- Tenant 1 requires explicit authorization: SET @dcc_view_matrix_seed_allow_yudao_tenant := 1;
SET @dcc_view_matrix_seed_actor := 'dcc_view_matrix_independent_seed_20260624';

DROP PROCEDURE IF EXISTS apply_dcc_view_matrix_independent_seed_20260624;
DELIMITER $$
CREATE PROCEDURE apply_dcc_view_matrix_independent_seed_20260624()
BEGIN
  DECLARE v_message TEXT DEFAULT NULL;
  DECLARE v_expected_rules BIGINT DEFAULT 243;
  DECLARE v_inserted_rules BIGINT DEFAULT 0;
  DECLARE v_generated_role_count BIGINT DEFAULT 0;
  DECLARE v_generated_role_user_count BIGINT DEFAULT 0;

  IF @dcc_view_matrix_seed_tenant_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIEW_MATRIX_SEED_TENANT_REQUIRED: set @dcc_view_matrix_seed_tenant_id before sourcing this SQL';
  END IF;

  IF @dcc_view_matrix_seed_tenant_id = 1 AND IFNULL(@dcc_view_matrix_seed_allow_yudao_tenant, 0) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIEW_MATRIX_SEED_TENANT1_AUTHORIZATION_REQUIRED: set @dcc_view_matrix_seed_allow_yudao_tenant := 1 only after explicit authorization';
  END IF;

  START TRANSACTION;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_category;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_category (
    category_code varchar(64) NOT NULL,
    file_name varchar(128) NOT NULL,
    matrix_group varchar(8) NOT NULL,
    matrix_sort int NOT NULL,
    file_number_pattern varchar(128) NOT NULL,
    PRIMARY KEY (category_code)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_view_matrix_seed_category
    (category_code, file_name, matrix_group, matrix_sort, file_number_pattern)
  VALUES
('DCC_FVM_DHF_001', '市场调研报告', 'DHF', 1, 'R&D-项目代码-xxx'),
('DCC_FVM_DHF_002', '技术调研报告', 'DHF', 2, 'R&D-项目代码-xxx'),
('DCC_FVM_DHF_003', '临床注册路径分析', 'DHF', 3, 'R&D-项目代码-xxx'),
('DCC_FVM_DHF_004', '项目立项书', 'DHF', 4, 'R&D-项目代码-xxx'),
('DCC_FVM_DHF_005', '项目策划书', 'DHF', 5, 'R&D-项目代码-xxx'),
('DCC_FVM_DHF_006', '专利检索与分析报告', 'DHF', 6, 'R&D-项目代码-xxx'),
('DCC_FVM_DHF_007', '同类产品测试方案、报告', 'DHF', 7, 'R&D-项目代码-xxx'),
('DCC_FVM_DHF_008', '不良事件调研报告', 'DHF', 8, 'R&D-项目代码-xxx'),
('DCC_FVM_DHF_009', '法规、标准清单', 'DHF', 9, ''),
('DCC_FVM_DHF_010', '风险管理计划', 'DHF', 10, 'RM-项目代码-xxx'),
('DCC_FVM_DHF_011', '风险管理报告', 'DHF', 11, 'RM-项目代码-xxx'),
('DCC_FVM_DHF_012', '验证主计划', 'DHF', 12, ''),
('DCC_FVM_DHF_013', '设计验证方案', 'DHF', 13, 'VER-项目代码-xxxa'),
('DCC_FVM_DHF_014', '设计验证报告', 'DHF', 14, 'VER-项目代码-xxxb'),
('DCC_FVM_DHF_015', '通用验证方案', 'DHF', 15, 'VER-CR-XXXa'),
('DCC_FVM_DHF_016', '通用验证报告', 'DHF', 16, 'VER-CR-XXXb'),
('DCC_FVM_DHF_017', '运输包装验证方案/报告', 'DHF', 17, ''),
('DCC_FVM_DHF_018', '货架寿命验证方案/报告', 'DHF', 18, ''),
('DCC_FVM_DHF_019', '设计转移方案/报告', 'DHF', 19, ''),
('DCC_FVM_DHF_020', '性能评价方案和报告', 'DHF', 20, ''),
('DCC_FVM_DHF_021', '产品过程确认主计划', 'DHF', 21, 'PV-项目代码'),
('DCC_FVM_DHF_022', '设备安装确认（IQ）方案', 'DHF', 22, 'PV-设备编号-A-IQ'),
('DCC_FVM_DHF_023', '设备安装确认（IQ）报告', 'DHF', 23, 'PV-设备编号-B-IQ'),
('DCC_FVM_DHF_024', '过程运行确认（OQ）方案', 'DHF', 24, 'PV-项目代码-XXXa-OQ'),
('DCC_FVM_DHF_025', '过程运行确认（OQ）报告', 'DHF', 25, 'PV-项目代码-XXXb-OQ'),
('DCC_FVM_DHF_026', '过程性能确认（PQ）方案', 'DHF', 26, 'PV-项目代码-XXXa-PQ'),
('DCC_FVM_DHF_027', '过程性能确认（PQ）报告', 'DHF', 27, 'PV-项目代码-XXXb-PQ'),
('DCC_FVM_DHF_028', '过程确认总结报告', 'DHF', 28, 'PV-项目代码-XXXb'),
('DCC_FVM_DHF_029', '灭菌确认方案/报告', 'DHF', 29, ''),
('DCC_FVM_DHF_030', '首次注册资料汇编', 'DHF', 30, 'IR-项目代码-XXX'),
('DCC_FVM_DHF_031', '延续注册资料汇编', 'DHF', 31, 'ER-项目代码-XXX'),
('DCC_FVM_DHF_032', '变更注册资料汇编', 'DHF', 32, 'AR-项目代码-XXX'),
('DCC_FVM_DHF_033', '首次备案资料汇编', 'DHF', 33, 'IRD-项目代码-XXX'),
('DCC_FVM_DHF_034', '变更备案资料汇编', 'DHF', 34, 'ARD-项目代码-XXX'),
('DCC_FVM_DHF_035', '生产许可/备案资料汇编', 'DHF', 35, ''),
('DCC_FVM_DMR_001', '产品技术要求', 'DMR', 1, 'DMR-项目代码-XXX'),
('DCC_FVM_DMR_002', '生产用设备清单', 'DMR', 2, 'DMR-项目代码-XXX'),
('DCC_FVM_DMR_003', '检验用设备清单', 'DMR', 3, 'DMR-项目代码-XXX'),
('DCC_FVM_DMR_004', 'BOM表', 'DMR', 4, 'DMR-项目代码-XXX'),
('DCC_FVM_DMR_005', '产品说明书', 'DMR', 5, 'DMR-项目代码-XXX'),
('DCC_FVM_DMR_006', '成品图纸', 'DMR', 6, '/'),
('DCC_FVM_DMR_007', '零配件图纸', 'DMR', 7, ''),
('DCC_FVM_DMR_008', '包装设计', 'DMR', 8, ''),
('DCC_FVM_DMR_009', '标签、合格证', 'DMR', 9, ''),
('DCC_FVM_DMR_010', '物资采购清单', 'DMR', 10, 'P-项目代码'),
('DCC_FVM_DMR_011', '采购技术要求', 'DMR', 11, 'P-项目代码-XXX'),
('DCC_FVM_DMR_012', '工艺流程图', 'DMR', 12, 'PP-项目代码-X'),
('DCC_FVM_DMR_013', '工序卡/作业指导书', 'DMR', 13, 'PP-项目代码-X-YY'),
('DCC_FVM_DMR_014', '项目间通用工序卡/作业指导书', 'DMR', 14, 'PP-CR-XXX'),
('DCC_FVM_DMR_015', '来料检验规程', 'DMR', 15, 'IQC-项目代码-XXX'),
('DCC_FVM_DMR_016', '过程检验规程', 'DMR', 16, 'PQC-项目代码-XXX'),
('DCC_FVM_DMR_017', '成品检验规程', 'DMR', 17, 'FQC-项目代码-XXX'),
('DCC_FVM_DMR_018', '检验记录表单', 'DMR', 18, 'RE-来料/过程/成品sop-XX'),
('DCC_FVM_DMR_019', '生产记录表单', 'DMR', 19, 'INT/RE/XXX-XX'),
('DCC_FVM_DMR_020', '标准测试方法', 'DMR', 20, 'STM-XX-XXX'),
('DCC_FVM_DMR_021', '设备采购技术要求', 'DMR', 21, 'P-EQ-设备编号'),
('DCC_FVM_DMR_022', '生产/检验用工装模具采购技术要求及设计图纸', 'DMR', 22, 'P-项目代码-模具编号'),
('DCC_FVM_DMR_023', '生产/检验用工装模具维护保养规范', 'DMR', 23, 'SOP-M-模具编号'),
('DCC_FVM_DMR_024', '生产/检验用工装模具维护保养记录表', 'DMR', 24, 'RE-SOP-M-模具编号');

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_subject;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_subject (
    subject_label varchar(255) NOT NULL,
    subject_top_header varchar(128) NULL,
    subject_sub_header varchar(128) NULL,
    marker varchar(8) NOT NULL,
    scope_type varchar(32) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_lookup_name varchar(255) NOT NULL,
    dept_name varchar(64) NULL,
    parent_dept_name varchar(64) NULL,
    role_code varchar(100) NULL,
    role_name varchar(64) NULL,
    manager_source varchar(32) NULL,
    remark varchar(255) NULL,
    parent_path_name varchar(64) NULL,
    grand_parent_path_name varchar(64) NULL,
    PRIMARY KEY (subject_label, marker, scope_type)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_view_matrix_seed_subject
    (subject_label, subject_top_header, subject_sub_header, marker, scope_type, subject_type,
     subject_lookup_name, dept_name, parent_dept_name, role_code, role_name, manager_source, remark)
  VALUES
('QA', 'QA', NULL, '●', 'ALL_MEMBERS', 'DEPT', '质量体系中心/QA', 'QA', '质量体系中心', NULL, NULL, NULL, '部门内全员'),
('QC', 'QC', NULL, '▲', 'MANAGER_AND_ABOVE', 'ROLE', 'DCC矩阵-QC主管及以上', 'QC', '质量体系中心', 'dcc_matrix_qc_lead', 'DCC矩阵-QC主管及以上', 'DEPT_LEADER_CHAIN', '部门主管及以上'),
('QC', 'QC', NULL, '●', 'ALL_MEMBERS', 'DEPT', '质量体系中心/QC', 'QC', '质量体系中心', NULL, NULL, NULL, '部门内全员'),
('QMS', 'QMS', NULL, '●', 'ALL_MEMBERS', 'DEPT', '质量体系中心/QMS', 'QMS', '质量体系中心', NULL, NULL, NULL, '部门内全员'),
('包装设计', '包装设计', NULL, '▲', 'MANAGER_AND_ABOVE', 'ROLE', 'DCC矩阵-包装设计主管及以上', '包装设计组', '供应链中心', 'dcc_matrix_packaging_design_lead', 'DCC矩阵-包装设计主管及以上', 'DEPT_LEADER_CHAIN', '部门主管及以上'),
('包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS', 'DEPT', '供应链中心/包装设计组', '包装设计组', '供应链中心', NULL, NULL, NULL, '部门内全员'),
('市场 / 业务+跟单及以上', '市场', '业务+跟单及以上', '●', 'ALL_MEMBERS', 'DEPT', '瑛泰医疗/市场营销中心', '市场营销中心', '瑛泰医疗', NULL, NULL, NULL, '市场营销中心全员'),
('市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS', 'DEPT', '瑛泰医疗/注册服务中心', '注册服务中心', '瑛泰医疗', NULL, NULL, NULL, '注册服务中心全员'),
('新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '新品开发部', '研发创新中心', 'dcc_matrix_new_product_lead', 'DCC矩阵-新品开发部主管及以上', 'DEPT_LEADER_CHAIN', '部门主管及以上'),
('新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS', 'DEPT', '研发创新中心/新品开发部', '新品开发部', '研发创新中心', NULL, NULL, NULL, '部门内全员'),
('检测中心', '检测中心', NULL, '●', 'ALL_MEMBERS', 'DEPT', '瑛泰医疗/检测中心', '检测中心', '瑛泰医疗', NULL, NULL, NULL, '部门内全员'),
('注册', '注册', NULL, '●', 'ALL_MEMBERS', 'DEPT', '瑛泰医疗/注册服务中心/注册部', '注册部', '注册服务中心', NULL, NULL, NULL, '部门内全员'),
('生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '▲', 'MANAGER_AND_ABOVE', 'ROLE', 'DCC矩阵-生产工段长班组长车间主任', '生产制造中心', '瑛泰医疗', 'dcc_matrix_production_line_lead', 'DCC矩阵-生产工段长班组长车间主任', 'DEPT_AND_CHILD_LEADERS', '工段长+班组长+车间主任'),
('生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS', 'DEPT', '瑛泰医疗/生产制造中心', '生产制造中心', '瑛泰医疗', NULL, NULL, NULL, '部门内全员'),
('生产计划', '生产计划', NULL, '●', 'ALL_MEMBERS', 'DEPT', '供应链中心/生产计划', '生产计划', '供应链中心', NULL, NULL, NULL, '部门内全员'),
('生产采购', '生产采购', NULL, '▲', 'MANAGER_AND_ABOVE', 'ROLE', 'DCC矩阵-生产采购主管及以上', '生产采购', '供应链中心', 'dcc_matrix_production_purchase_lead', 'DCC矩阵-生产采购主管及以上', 'DEPT_LEADER_CHAIN', '部门主管及以上'),
('生产采购', '生产采购', NULL, '●', 'ALL_MEMBERS', 'DEPT', '供应链中心/生产采购', '生产采购', '供应链中心', NULL, NULL, NULL, '部门内全员'),
('设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS', 'DEPT', '研发创新中心/设备开发部', '设备开发部', '研发创新中心', NULL, NULL, NULL, '部门内全员');

  UPDATE tmp_dcc_view_matrix_seed_subject
  SET parent_path_name = CASE
      WHEN LENGTH(subject_lookup_name) - LENGTH(REPLACE(subject_lookup_name, '/', '')) >= 2
        THEN SUBSTRING_INDEX(SUBSTRING_INDEX(subject_lookup_name, '/', -2), '/', 1)
      ELSE parent_dept_name
    END,
    grand_parent_path_name = CASE
      WHEN LENGTH(subject_lookup_name) - LENGTH(REPLACE(subject_lookup_name, '/', '')) >= 2
        THEN SUBSTRING_INDEX(subject_lookup_name, '/', 1)
      ELSE NULL
    END;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_grant;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_grant (
    category_code varchar(64) NOT NULL,
    excel_file_name varchar(255) NOT NULL,
    excel_row_no int NOT NULL,
    excel_column_letter varchar(16) NOT NULL,
    subject_label varchar(255) NOT NULL,
    subject_top_header varchar(128) NULL,
    subject_sub_header varchar(128) NULL,
    marker varchar(8) NOT NULL,
    scope_type varchar(32) NOT NULL
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_view_matrix_seed_grant
    (category_code, excel_file_name, excel_row_no, excel_column_letter, subject_label,
     subject_top_header, subject_sub_header, marker, scope_type)
  VALUES
('DCC_FVM_DHF_001', '电子文控系统推进计划及需求表.xlsx', 5, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_001', '电子文控系统推进计划及需求表.xlsx', 5, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_002', '电子文控系统推进计划及需求表.xlsx', 6, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_002', '电子文控系统推进计划及需求表.xlsx', 6, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_003', '电子文控系统推进计划及需求表.xlsx', 7, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_003', '电子文控系统推进计划及需求表.xlsx', 7, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_004', '电子文控系统推进计划及需求表.xlsx', 8, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_004', '电子文控系统推进计划及需求表.xlsx', 8, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_005', '电子文控系统推进计划及需求表.xlsx', 9, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_005', '电子文控系统推进计划及需求表.xlsx', 9, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_006', '电子文控系统推进计划及需求表.xlsx', 10, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_006', '电子文控系统推进计划及需求表.xlsx', 10, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_007', '电子文控系统推进计划及需求表.xlsx', 11, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_007', '电子文控系统推进计划及需求表.xlsx', 11, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_008', '电子文控系统推进计划及需求表.xlsx', 12, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_008', '电子文控系统推进计划及需求表.xlsx', 12, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_009', '电子文控系统推进计划及需求表.xlsx', 13, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_009', '电子文控系统推进计划及需求表.xlsx', 13, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_009', '电子文控系统推进计划及需求表.xlsx', 13, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_010', '电子文控系统推进计划及需求表.xlsx', 14, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_010', '电子文控系统推进计划及需求表.xlsx', 14, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_010', '电子文控系统推进计划及需求表.xlsx', 14, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_010', '电子文控系统推进计划及需求表.xlsx', 14, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_010', '电子文控系统推进计划及需求表.xlsx', 14, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_011', '电子文控系统推进计划及需求表.xlsx', 15, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_011', '电子文控系统推进计划及需求表.xlsx', 15, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_011', '电子文控系统推进计划及需求表.xlsx', 15, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_011', '电子文控系统推进计划及需求表.xlsx', 15, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_011', '电子文控系统推进计划及需求表.xlsx', 15, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_012', '电子文控系统推进计划及需求表.xlsx', 16, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_012', '电子文控系统推进计划及需求表.xlsx', 16, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_013', '电子文控系统推进计划及需求表.xlsx', 17, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_013', '电子文控系统推进计划及需求表.xlsx', 17, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_014', '电子文控系统推进计划及需求表.xlsx', 18, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_014', '电子文控系统推进计划及需求表.xlsx', 18, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_015', '电子文控系统推进计划及需求表.xlsx', 19, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_015', '电子文控系统推进计划及需求表.xlsx', 19, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_016', '电子文控系统推进计划及需求表.xlsx', 20, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_016', '电子文控系统推进计划及需求表.xlsx', 20, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_017', '电子文控系统推进计划及需求表.xlsx', 21, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_017', '电子文控系统推进计划及需求表.xlsx', 21, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_017', '电子文控系统推进计划及需求表.xlsx', 21, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_017', '电子文控系统推进计划及需求表.xlsx', 21, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_018', '电子文控系统推进计划及需求表.xlsx', 22, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_018', '电子文控系统推进计划及需求表.xlsx', 22, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_018', '电子文控系统推进计划及需求表.xlsx', 22, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_018', '电子文控系统推进计划及需求表.xlsx', 22, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_019', '电子文控系统推进计划及需求表.xlsx', 23, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_019', '电子文控系统推进计划及需求表.xlsx', 23, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_020', '电子文控系统推进计划及需求表.xlsx', 24, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_020', '电子文控系统推进计划及需求表.xlsx', 24, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_021', '电子文控系统推进计划及需求表.xlsx', 25, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_021', '电子文控系统推进计划及需求表.xlsx', 25, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_021', '电子文控系统推进计划及需求表.xlsx', 25, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_022', '电子文控系统推进计划及需求表.xlsx', 26, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_022', '电子文控系统推进计划及需求表.xlsx', 26, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_022', '电子文控系统推进计划及需求表.xlsx', 26, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_022', '电子文控系统推进计划及需求表.xlsx', 26, 'I', '设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_023', '电子文控系统推进计划及需求表.xlsx', 27, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_023', '电子文控系统推进计划及需求表.xlsx', 27, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_023', '电子文控系统推进计划及需求表.xlsx', 27, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_023', '电子文控系统推进计划及需求表.xlsx', 27, 'I', '设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_024', '电子文控系统推进计划及需求表.xlsx', 28, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_024', '电子文控系统推进计划及需求表.xlsx', 28, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_024', '电子文控系统推进计划及需求表.xlsx', 28, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_025', '电子文控系统推进计划及需求表.xlsx', 29, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_025', '电子文控系统推进计划及需求表.xlsx', 29, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_025', '电子文控系统推进计划及需求表.xlsx', 29, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_026', '电子文控系统推进计划及需求表.xlsx', 30, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_026', '电子文控系统推进计划及需求表.xlsx', 30, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_026', '电子文控系统推进计划及需求表.xlsx', 30, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_027', '电子文控系统推进计划及需求表.xlsx', 31, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_027', '电子文控系统推进计划及需求表.xlsx', 31, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_027', '电子文控系统推进计划及需求表.xlsx', 31, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_028', '电子文控系统推进计划及需求表.xlsx', 32, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_028', '电子文控系统推进计划及需求表.xlsx', 32, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_028', '电子文控系统推进计划及需求表.xlsx', 32, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_029', '电子文控系统推进计划及需求表.xlsx', 33, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_029', '电子文控系统推进计划及需求表.xlsx', 33, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_029', '电子文控系统推进计划及需求表.xlsx', 33, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_030', '电子文控系统推进计划及需求表.xlsx', 34, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_030', '电子文控系统推进计划及需求表.xlsx', 34, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_030', '电子文控系统推进计划及需求表.xlsx', 34, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_030', '电子文控系统推进计划及需求表.xlsx', 34, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_031', '电子文控系统推进计划及需求表.xlsx', 35, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_031', '电子文控系统推进计划及需求表.xlsx', 35, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_031', '电子文控系统推进计划及需求表.xlsx', 35, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_031', '电子文控系统推进计划及需求表.xlsx', 35, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_032', '电子文控系统推进计划及需求表.xlsx', 36, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_032', '电子文控系统推进计划及需求表.xlsx', 36, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_032', '电子文控系统推进计划及需求表.xlsx', 36, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_032', '电子文控系统推进计划及需求表.xlsx', 36, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_033', '电子文控系统推进计划及需求表.xlsx', 37, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_033', '电子文控系统推进计划及需求表.xlsx', 37, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_033', '电子文控系统推进计划及需求表.xlsx', 37, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_033', '电子文控系统推进计划及需求表.xlsx', 37, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_034', '电子文控系统推进计划及需求表.xlsx', 38, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_034', '电子文控系统推进计划及需求表.xlsx', 38, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_034', '电子文控系统推进计划及需求表.xlsx', 38, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_034', '电子文控系统推进计划及需求表.xlsx', 38, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_035', '电子文控系统推进计划及需求表.xlsx', 39, 'D', '新品开发部', '新品开发部', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DHF_035', '电子文控系统推进计划及需求表.xlsx', 39, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_035', '电子文控系统推进计划及需求表.xlsx', 39, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DHF_035', '电子文控系统推进计划及需求表.xlsx', 39, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_001', '电子文控系统推进计划及需求表.xlsx', 40, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_001', '电子文控系统推进计划及需求表.xlsx', 40, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_001', '电子文控系统推进计划及需求表.xlsx', 40, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_001', '电子文控系统推进计划及需求表.xlsx', 40, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_001', '电子文控系统推进计划及需求表.xlsx', 40, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_002', '电子文控系统推进计划及需求表.xlsx', 41, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_002', '电子文控系统推进计划及需求表.xlsx', 41, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_002', '电子文控系统推进计划及需求表.xlsx', 41, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_002', '电子文控系统推进计划及需求表.xlsx', 41, 'I', '设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_002', '电子文控系统推进计划及需求表.xlsx', 41, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_002', '电子文控系统推进计划及需求表.xlsx', 41, 'M', '包装设计', '包装设计', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_003', '电子文控系统推进计划及需求表.xlsx', 42, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_003', '电子文控系统推进计划及需求表.xlsx', 42, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_003', '电子文控系统推进计划及需求表.xlsx', 42, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_003', '电子文控系统推进计划及需求表.xlsx', 42, 'I', '设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_003', '电子文控系统推进计划及需求表.xlsx', 42, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_003', '电子文控系统推进计划及需求表.xlsx', 42, 'M', '包装设计', '包装设计', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_004', '电子文控系统推进计划及需求表.xlsx', 43, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_004', '电子文控系统推进计划及需求表.xlsx', 43, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_004', '电子文控系统推进计划及需求表.xlsx', 43, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_004', '电子文控系统推进计划及需求表.xlsx', 43, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_004', '电子文控系统推进计划及需求表.xlsx', 43, 'K', '生产计划', '生产计划', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_004', '电子文控系统推进计划及需求表.xlsx', 43, 'L', '生产采购', '生产采购', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_004', '电子文控系统推进计划及需求表.xlsx', 43, 'M', '包装设计', '包装设计', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_004', '电子文控系统推进计划及需求表.xlsx', 43, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'F', 'QC', 'QC', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'H', '注册', '注册', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'K', '生产计划', '生产计划', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'L', '生产采购', '生产采购', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'M', '包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'N', '市场 / 业务+跟单及以上', '市场', '业务+跟单及以上', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_005', '电子文控系统推进计划及需求表.xlsx', 44, 'P', '检测中心', '检测中心', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'F', 'QC', 'QC', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'K', '生产计划', '生产计划', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'L', '生产采购', '生产采购', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'M', '包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'N', '市场 / 业务+跟单及以上', '市场', '业务+跟单及以上', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_006', '电子文控系统推进计划及需求表.xlsx', 45, 'P', '检测中心', '检测中心', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'F', 'QC', 'QC', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'K', '生产计划', '生产计划', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'L', '生产采购', '生产采购', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'M', '包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'N', '市场 / 业务+跟单及以上', '市场', '业务+跟单及以上', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_007', '电子文控系统推进计划及需求表.xlsx', 46, 'P', '检测中心', '检测中心', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_008', '电子文控系统推进计划及需求表.xlsx', 47, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_008', '电子文控系统推进计划及需求表.xlsx', 47, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_008', '电子文控系统推进计划及需求表.xlsx', 47, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_009', '电子文控系统推进计划及需求表.xlsx', 48, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_009', '电子文控系统推进计划及需求表.xlsx', 48, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_009', '电子文控系统推进计划及需求表.xlsx', 48, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_010', '电子文控系统推进计划及需求表.xlsx', 49, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_010', '电子文控系统推进计划及需求表.xlsx', 49, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_010', '电子文控系统推进计划及需求表.xlsx', 49, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_010', '电子文控系统推进计划及需求表.xlsx', 49, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_010', '电子文控系统推进计划及需求表.xlsx', 49, 'L', '生产采购', '生产采购', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_010', '电子文控系统推进计划及需求表.xlsx', 49, 'M', '包装设计', '包装设计', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_010', '电子文控系统推进计划及需求表.xlsx', 49, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_011', '电子文控系统推进计划及需求表.xlsx', 50, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_011', '电子文控系统推进计划及需求表.xlsx', 50, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_011', '电子文控系统推进计划及需求表.xlsx', 50, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_011', '电子文控系统推进计划及需求表.xlsx', 50, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_011', '电子文控系统推进计划及需求表.xlsx', 50, 'L', '生产采购', '生产采购', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_011', '电子文控系统推进计划及需求表.xlsx', 50, 'M', '包装设计', '包装设计', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_011', '电子文控系统推进计划及需求表.xlsx', 50, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'F', 'QC', 'QC', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'I', '设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'K', '生产计划', '生产计划', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'L', '生产采购', '生产采购', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_012', '电子文控系统推进计划及需求表.xlsx', 51, 'M', '包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'F', 'QC', 'QC', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'I', '设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'K', '生产计划', '生产计划', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'L', '生产采购', '生产采购', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_013', '电子文控系统推进计划及需求表.xlsx', 52, 'M', '包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_014', '电子文控系统推进计划及需求表.xlsx', 53, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_014', '电子文控系统推进计划及需求表.xlsx', 53, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'F', 'QC', 'QC', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'K', '生产计划', '生产计划', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'L', '生产采购', '生产采购', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'M', '包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_015', '电子文控系统推进计划及需求表.xlsx', 54, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_016', '电子文控系统推进计划及需求表.xlsx', 55, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_016', '电子文控系统推进计划及需求表.xlsx', 55, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_016', '电子文控系统推进计划及需求表.xlsx', 55, 'F', 'QC', 'QC', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_016', '电子文控系统推进计划及需求表.xlsx', 55, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_016', '电子文控系统推进计划及需求表.xlsx', 55, 'I', '设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_016', '电子文控系统推进计划及需求表.xlsx', 55, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_016', '电子文控系统推进计划及需求表.xlsx', 55, 'M', '包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_016', '电子文控系统推进计划及需求表.xlsx', 55, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'F', 'QC', 'QC', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'I', '设备开发', '设备开发', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'M', '包装设计', '包装设计', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'O', '市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_017', '电子文控系统推进计划及需求表.xlsx', 56, 'P', '检测中心', '检测中心', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_018', '电子文控系统推进计划及需求表.xlsx', 57, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_018', '电子文控系统推进计划及需求表.xlsx', 57, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_018', '电子文控系统推进计划及需求表.xlsx', 57, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_018', '电子文控系统推进计划及需求表.xlsx', 57, 'P', '检测中心', '检测中心', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_019', '电子文控系统推进计划及需求表.xlsx', 58, 'D', '新品开发部', '新品开发部', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_019', '电子文控系统推进计划及需求表.xlsx', 58, 'E', 'QA', 'QA', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_019', '电子文控系统推进计划及需求表.xlsx', 58, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_019', '电子文控系统推进计划及需求表.xlsx', 58, 'J', '生产 / 工段长+班组长+车间主任', '生产', '工段长+班组长+车间主任', '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_019', '电子文控系统推进计划及需求表.xlsx', 58, 'M', '包装设计', '包装设计', NULL, '▲', 'MANAGER_AND_ABOVE'),
('DCC_FVM_DMR_020', '电子文控系统推进计划及需求表.xlsx', 59, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_021', '电子文控系统推进计划及需求表.xlsx', 60, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_022', '电子文控系统推进计划及需求表.xlsx', 61, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_023', '电子文控系统推进计划及需求表.xlsx', 62, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS'),
('DCC_FVM_DMR_024', '电子文控系统推进计划及需求表.xlsx', 63, 'G', 'QMS', 'QMS', NULL, '●', 'ALL_MEMBERS');

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_category_duplicate;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_category_duplicate AS
  SELECT seed.category_code, COUNT(category.id) AS resolved_count
  FROM tmp_dcc_view_matrix_seed_category seed
  JOIN dcc_file_category category
    ON category.tenant_id = @dcc_view_matrix_seed_tenant_id
   AND category.deleted = 0
   AND category.code = seed.category_code
  GROUP BY seed.category_code
  HAVING COUNT(category.id) > 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_view_matrix_seed_category_duplicate) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(category_code, '#', resolved_count) SEPARATOR '; ')
      INTO v_message
    FROM tmp_dcc_view_matrix_seed_category_duplicate;
    SET v_message = 'VIEW_MATRIX_SEED_CATEGORY_AMBIGUOUS';
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
  END IF;

  INSERT INTO dcc_file_category
    (code, name, parent_id, active, sort, source, remark, description,
     distribution_required, training_required, tenant_id, create_time, update_time, creator, updater, deleted)
  SELECT seed.category_code, seed.file_name, NULL, 1, seed.matrix_sort, 'DCC_VIEW_MATRIX_SEED',
         CONCAT(seed.matrix_group, ' ', seed.file_number_pattern), seed.file_name,
         0, 0, @dcc_view_matrix_seed_tenant_id, NOW(), NOW(), @dcc_view_matrix_seed_actor, @dcc_view_matrix_seed_actor, 0
  FROM tmp_dcc_view_matrix_seed_category seed
  LEFT JOIN dcc_file_category category
    ON category.tenant_id = @dcc_view_matrix_seed_tenant_id
   AND category.deleted = 0
   AND category.code = seed.category_code
  WHERE category.id IS NULL;

  UPDATE dcc_file_category category
  JOIN tmp_dcc_view_matrix_seed_category seed ON seed.category_code = category.code
  SET category.name = seed.file_name,
      category.active = 1,
      category.sort = seed.matrix_sort,
      category.source = 'DCC_VIEW_MATRIX_SEED',
      category.remark = CONCAT(seed.matrix_group, ' ', seed.file_number_pattern),
      category.description = seed.file_name,
      category.updater = @dcc_view_matrix_seed_actor,
      category.update_time = NOW()
  WHERE category.tenant_id = @dcc_view_matrix_seed_tenant_id
    AND category.deleted = 0;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_resolved_dept;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_resolved_dept AS
  SELECT subject.subject_label, subject.marker, subject.scope_type, subject.subject_type,
         subject.dept_name, subject.parent_dept_name, subject.manager_source,
         dept.id AS subject_id, dept.leader_user_id, parent_dept.leader_user_id AS parent_leader_user_id
  FROM tmp_dcc_view_matrix_seed_subject subject
  LEFT JOIN system_dept grand_dept
    ON grand_dept.tenant_id = @dcc_view_matrix_seed_tenant_id
   AND grand_dept.deleted = b'0'
   AND grand_dept.status = 0
   AND grand_dept.name = subject.grand_parent_path_name
  JOIN system_dept parent_dept
    ON parent_dept.tenant_id = @dcc_view_matrix_seed_tenant_id
   AND parent_dept.deleted = b'0'
   AND parent_dept.status = 0
   AND parent_dept.name = subject.parent_path_name
   AND (subject.grand_parent_path_name IS NULL OR parent_dept.parent_id = grand_dept.id)
  JOIN system_dept dept
    ON dept.tenant_id = @dcc_view_matrix_seed_tenant_id
   AND dept.deleted = b'0'
   AND dept.status = 0
   AND dept.parent_id = parent_dept.id
   AND dept.name = subject.dept_name
  WHERE subject.subject_type IN ('DEPT', 'ROLE');

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_missing_dept;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_missing_dept AS
  SELECT subject.subject_label, subject.marker, subject.scope_type, subject.subject_lookup_name,
         COALESCE(resolved.resolved_count, 0) AS resolved_count
  FROM tmp_dcc_view_matrix_seed_subject subject
  LEFT JOIN (
    SELECT subject_label, marker, scope_type, COUNT(*) AS resolved_count
    FROM tmp_dcc_view_matrix_seed_resolved_dept
    GROUP BY subject_label, marker, scope_type
  ) resolved ON resolved.subject_label = subject.subject_label
     AND resolved.marker = subject.marker
     AND resolved.scope_type = subject.scope_type
  WHERE subject.subject_type IN ('DEPT', 'ROLE')
    AND COALESCE(resolved.resolved_count, 0) <> 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_view_matrix_seed_missing_dept) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(subject_label, '/', marker, '=>', subject_lookup_name, '#', resolved_count) SEPARATOR '; ')
      INTO v_message
    FROM tmp_dcc_view_matrix_seed_missing_dept;
    SET v_message = 'VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED';
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
  END IF;

  INSERT INTO system_role
    (name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
     tenant_id, creator, create_time, updater, update_time, deleted)
  SELECT subject.role_name, subject.role_code, 6900, 1, '', 0, 2,
         CONCAT('DCC view matrix manager role: ', subject.remark),
         @dcc_view_matrix_seed_tenant_id, @dcc_view_matrix_seed_actor, NOW(), @dcc_view_matrix_seed_actor, NOW(), b'0'
  FROM tmp_dcc_view_matrix_seed_subject subject
  WHERE subject.subject_type = 'ROLE'
    AND NOT EXISTS (
      SELECT 1 FROM system_role existing
      WHERE existing.tenant_id = @dcc_view_matrix_seed_tenant_id
        AND existing.deleted = b'0'
        AND existing.code = subject.role_code
    );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_resolved_role;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_resolved_role AS
  SELECT subject.subject_label, subject.marker, subject.scope_type, subject.role_code, role.id AS subject_id
  FROM tmp_dcc_view_matrix_seed_subject subject
  JOIN system_role role
    ON role.tenant_id = @dcc_view_matrix_seed_tenant_id
   AND role.deleted = b'0'
   AND role.code = subject.role_code
  WHERE subject.subject_type = 'ROLE';

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_role_user;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_role_user (
    role_id bigint NOT NULL,
    user_id bigint NOT NULL,
    PRIMARY KEY (role_id, user_id)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT IGNORE INTO tmp_dcc_view_matrix_seed_role_user (role_id, user_id)
  SELECT role.subject_id AS role_id, dept.leader_user_id AS user_id
  FROM tmp_dcc_view_matrix_seed_subject subject
  JOIN tmp_dcc_view_matrix_seed_resolved_role role
    ON role.subject_label = subject.subject_label
   AND role.marker = subject.marker
   AND role.scope_type = subject.scope_type
  JOIN tmp_dcc_view_matrix_seed_resolved_dept dept
    ON dept.subject_label = subject.subject_label
   AND dept.marker = subject.marker
   AND dept.scope_type = subject.scope_type
  WHERE subject.subject_type = 'ROLE'
    AND subject.manager_source = 'DEPT_LEADER_CHAIN'
    AND dept.leader_user_id IS NOT NULL;

  INSERT IGNORE INTO tmp_dcc_view_matrix_seed_role_user (role_id, user_id)
  SELECT role.subject_id AS role_id, dept.parent_leader_user_id AS user_id
  FROM tmp_dcc_view_matrix_seed_subject subject
  JOIN tmp_dcc_view_matrix_seed_resolved_role role
    ON role.subject_label = subject.subject_label
   AND role.marker = subject.marker
   AND role.scope_type = subject.scope_type
  JOIN tmp_dcc_view_matrix_seed_resolved_dept dept
    ON dept.subject_label = subject.subject_label
   AND dept.marker = subject.marker
   AND dept.scope_type = subject.scope_type
  WHERE subject.subject_type = 'ROLE'
    AND subject.manager_source = 'DEPT_LEADER_CHAIN'
    AND dept.parent_leader_user_id IS NOT NULL;

  INSERT IGNORE INTO tmp_dcc_view_matrix_seed_role_user (role_id, user_id)
  SELECT role.subject_id AS role_id, dept.leader_user_id AS user_id
  FROM tmp_dcc_view_matrix_seed_subject subject
  JOIN tmp_dcc_view_matrix_seed_resolved_role role
    ON role.subject_label = subject.subject_label
   AND role.marker = subject.marker
   AND role.scope_type = subject.scope_type
  JOIN tmp_dcc_view_matrix_seed_resolved_dept dept
    ON dept.subject_label = subject.subject_label
   AND dept.marker = subject.marker
   AND dept.scope_type = subject.scope_type
  WHERE subject.subject_type = 'ROLE'
    AND subject.manager_source = 'DEPT_AND_CHILD_LEADERS'
    AND dept.leader_user_id IS NOT NULL;

  INSERT IGNORE INTO tmp_dcc_view_matrix_seed_role_user (role_id, user_id)
  SELECT role.subject_id AS role_id, child_dept.leader_user_id AS user_id
  FROM tmp_dcc_view_matrix_seed_subject subject
  JOIN tmp_dcc_view_matrix_seed_resolved_role role
    ON role.subject_label = subject.subject_label
   AND role.marker = subject.marker
   AND role.scope_type = subject.scope_type
  JOIN tmp_dcc_view_matrix_seed_resolved_dept dept
    ON dept.subject_label = subject.subject_label
   AND dept.marker = subject.marker
   AND dept.scope_type = subject.scope_type
  JOIN system_dept child_dept
    ON child_dept.tenant_id = @dcc_view_matrix_seed_tenant_id
   AND child_dept.deleted = b'0'
   AND child_dept.status = 0
   AND child_dept.parent_id = dept.subject_id
   AND child_dept.leader_user_id IS NOT NULL
  WHERE subject.subject_type = 'ROLE'
    AND subject.manager_source = 'DEPT_AND_CHILD_LEADERS';

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_missing_role_user;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_missing_role_user AS
  SELECT subject.subject_label, subject.marker, subject.scope_type, subject.role_code,
         COALESCE(user_count.user_count, 0) AS user_count
  FROM tmp_dcc_view_matrix_seed_subject subject
  JOIN tmp_dcc_view_matrix_seed_resolved_role role
    ON role.subject_label = subject.subject_label
   AND role.marker = subject.marker
   AND role.scope_type = subject.scope_type
  LEFT JOIN (
    SELECT role_id, COUNT(DISTINCT user_id) AS user_count
    FROM tmp_dcc_view_matrix_seed_role_user
    GROUP BY role_id
  ) user_count ON user_count.role_id = role.subject_id
  WHERE subject.subject_type = 'ROLE'
    AND COALESCE(user_count.user_count, 0) = 0;

  IF (SELECT COUNT(*) FROM tmp_dcc_view_matrix_seed_missing_role_user) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(subject_label, '/', marker, '=>', role_code) SEPARATOR '; ')
      INTO v_message
    FROM tmp_dcc_view_matrix_seed_missing_role_user;
    SET v_message = 'VIEW_MATRIX_SEED_ROLE_USER_PRECHECK_FAILED';
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
  END IF;

  INSERT INTO system_user_role
    (user_id, role_id, tenant_id, creator, create_time, updater, update_time, deleted)
  SELECT role_user.user_id, role_user.role_id, @dcc_view_matrix_seed_tenant_id,
         @dcc_view_matrix_seed_actor, NOW(), @dcc_view_matrix_seed_actor, NOW(), b'0'
  FROM tmp_dcc_view_matrix_seed_role_user role_user
  WHERE NOT EXISTS (
    SELECT 1 FROM system_user_role existing
    WHERE existing.tenant_id = @dcc_view_matrix_seed_tenant_id
      AND existing.deleted = b'0'
      AND existing.user_id = role_user.user_id
      AND existing.role_id = role_user.role_id
  );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_resolved_subject;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_resolved_subject (
    subject_label varchar(255) NOT NULL,
    marker varchar(8) NOT NULL,
    scope_type varchar(32) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    subject_lookup_name varchar(255) NOT NULL,
    remark varchar(255) NULL
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_view_matrix_seed_resolved_subject
    (subject_label, marker, scope_type, subject_type, subject_id, subject_lookup_name, remark)
  SELECT subject.subject_label, subject.marker, subject.scope_type, subject.subject_type,
         dept.subject_id, subject.subject_lookup_name, subject.remark
  FROM tmp_dcc_view_matrix_seed_subject subject
  JOIN tmp_dcc_view_matrix_seed_resolved_dept dept
    ON dept.subject_label = subject.subject_label
   AND dept.marker = subject.marker
   AND dept.scope_type = subject.scope_type
  WHERE subject.subject_type = 'DEPT';

  INSERT INTO tmp_dcc_view_matrix_seed_resolved_subject
    (subject_label, marker, scope_type, subject_type, subject_id, subject_lookup_name, remark)
  SELECT subject.subject_label, subject.marker, subject.scope_type, subject.subject_type,
         role.subject_id, subject.subject_lookup_name, subject.remark
  FROM tmp_dcc_view_matrix_seed_subject subject
  JOIN tmp_dcc_view_matrix_seed_resolved_role role
    ON role.subject_label = subject.subject_label
   AND role.marker = subject.marker
   AND role.scope_type = subject.scope_type
  WHERE subject.subject_type = 'ROLE';

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_view_matrix_seed_resolved_category;
  CREATE TEMPORARY TABLE tmp_dcc_view_matrix_seed_resolved_category AS
  SELECT seed.category_code, category.id AS category_id
  FROM tmp_dcc_view_matrix_seed_category seed
  JOIN dcc_file_category category
    ON category.tenant_id = @dcc_view_matrix_seed_tenant_id
   AND category.deleted = 0
   AND category.code = seed.category_code;

  UPDATE dcc_category_view_matrix_rule rule_table
  JOIN tmp_dcc_view_matrix_seed_resolved_category category
    ON category.category_id = rule_table.category_id
  SET rule_table.active = 0,
      rule_table.deleted = 1,
      rule_table.updater = @dcc_view_matrix_seed_actor,
      rule_table.update_time = NOW()
  WHERE rule_table.tenant_id = @dcc_view_matrix_seed_tenant_id
    AND rule_table.deleted = 0;

  INSERT INTO dcc_category_view_matrix_rule
    (category_id, excel_file_name, excel_row_no, excel_column_letter,
     subject_label, subject_top_header, subject_sub_header, marker, scope_type,
     subject_type, subject_id, active, remark, tenant_id, create_time, update_time,
     creator, updater, deleted)
  SELECT category.category_id,
         grant_row.excel_file_name,
         grant_row.excel_row_no,
         grant_row.excel_column_letter,
         grant_row.subject_label,
         grant_row.subject_top_header,
         grant_row.subject_sub_header,
         grant_row.marker,
         grant_row.scope_type,
         subject.subject_type,
         subject.subject_id,
         1,
         CONCAT(subject.subject_lookup_name, ' | ', subject.remark),
         @dcc_view_matrix_seed_tenant_id,
         NOW(), NOW(), @dcc_view_matrix_seed_actor, @dcc_view_matrix_seed_actor, 0
  FROM tmp_dcc_view_matrix_seed_grant grant_row
  JOIN tmp_dcc_view_matrix_seed_resolved_category category
    ON category.category_code = grant_row.category_code
  JOIN tmp_dcc_view_matrix_seed_resolved_subject subject
    ON subject.subject_label = grant_row.subject_label
   AND subject.marker = grant_row.marker
   AND subject.scope_type = grant_row.scope_type;

  SELECT COUNT(*) INTO v_inserted_rules
  FROM dcc_category_view_matrix_rule rule_table
  JOIN tmp_dcc_view_matrix_seed_resolved_category category
    ON category.category_id = rule_table.category_id
  WHERE rule_table.tenant_id = @dcc_view_matrix_seed_tenant_id
    AND rule_table.deleted = 0
    AND rule_table.active = 1;

  IF v_inserted_rules <> v_expected_rules THEN
    SET v_message = 'VIEW_MATRIX_SEED_RULE_COUNT_MISMATCH';
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
  END IF;

  COMMIT;

  SELECT COUNT(DISTINCT role_id), COUNT(DISTINCT user_id)
    INTO v_generated_role_count, v_generated_role_user_count
  FROM tmp_dcc_view_matrix_seed_role_user;

  SELECT @dcc_view_matrix_seed_tenant_id AS tenant_id,
         (SELECT COUNT(*) FROM tmp_dcc_view_matrix_seed_category) AS category_count,
         v_inserted_rules AS view_matrix_rule_count,
         v_generated_role_count AS generated_role_count,
         v_generated_role_user_count AS generated_role_user_count;
END$$
DELIMITER ;

CALL apply_dcc_view_matrix_independent_seed_20260624();
DROP PROCEDURE IF EXISTS apply_dcc_view_matrix_independent_seed_20260624;
