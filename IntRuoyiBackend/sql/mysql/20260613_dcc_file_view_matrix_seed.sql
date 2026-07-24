-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260614_dcc_product_visibility_group; type=seed; riskLevel=low
-- DCC file view matrix seed generated from C:/Users/BJB110/Desktop/电子文控系统推进计划及需求表.xlsx, sheet 文件查阅矩阵.
-- Matrix shape: 59 categories, DHF 35, DMR 24, deduped VIEW grants 231.
-- Safety: idempotent seed, no controlled-file reclassification, no download permission creation.

SET @dcc_fvm_tenant_id := 1;
SET @dcc_fvm_actor := 'dcc_file_view_matrix_20260613';

DROP PROCEDURE IF EXISTS apply_dcc_file_view_matrix_seed;
DELIMITER $$
CREATE PROCEDURE apply_dcc_file_view_matrix_seed()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_category;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_category (
    matrix_group varchar(8) NOT NULL,
    matrix_sort int NOT NULL,
    file_number_pattern varchar(128) NOT NULL,
    file_name varchar(128) NOT NULL,
    category_code varchar(64) NOT NULL,
    PRIMARY KEY (category_code)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_file_view_matrix_category
    (matrix_group, matrix_sort, file_number_pattern, file_name, category_code)
  VALUES
  ('DHF', 1, 'R&D-项目代码-xxx', '市场调研报告', 'DCC_FVM_DHF_001'),
  ('DHF', 2, 'R&D-项目代码-xxx', '技术调研报告', 'DCC_FVM_DHF_002'),
  ('DHF', 3, 'R&D-项目代码-xxx', '临床注册路径分析', 'DCC_FVM_DHF_003'),
  ('DHF', 4, 'R&D-项目代码-xxx', '项目立项书', 'DCC_FVM_DHF_004'),
  ('DHF', 5, 'R&D-项目代码-xxx', '项目策划书', 'DCC_FVM_DHF_005'),
  ('DHF', 6, 'R&D-项目代码-xxx', '专利检索与分析报告', 'DCC_FVM_DHF_006'),
  ('DHF', 7, 'R&D-项目代码-xxx', '同类产品测试方案、报告', 'DCC_FVM_DHF_007'),
  ('DHF', 8, 'R&D-项目代码-xxx', '不良事件调研报告', 'DCC_FVM_DHF_008'),
  ('DHF', 9, '', '法规、标准清单', 'DCC_FVM_DHF_009'),
  ('DHF', 10, 'RM-项目代码-xxx', '风险管理计划', 'DCC_FVM_DHF_010'),
  ('DHF', 11, 'RM-项目代码-xxx', '风险管理报告', 'DCC_FVM_DHF_011'),
  ('DHF', 12, '', '验证主计划', 'DCC_FVM_DHF_012'),
  ('DHF', 13, 'VER-项目代码-xxxa', '设计验证方案', 'DCC_FVM_DHF_013'),
  ('DHF', 14, 'VER-项目代码-xxxb', '设计验证报告', 'DCC_FVM_DHF_014'),
  ('DHF', 15, 'VER-CR-XXXa', '通用验证方案', 'DCC_FVM_DHF_015'),
  ('DHF', 16, 'VER-CR-XXXb', '通用验证报告', 'DCC_FVM_DHF_016'),
  ('DHF', 17, '', '运输包装验证方案/报告', 'DCC_FVM_DHF_017'),
  ('DHF', 18, '', '货架寿命验证方案/报告', 'DCC_FVM_DHF_018'),
  ('DHF', 19, '', '设计转移方案/报告', 'DCC_FVM_DHF_019'),
  ('DHF', 20, '', '性能评价方案和报告', 'DCC_FVM_DHF_020'),
  ('DHF', 21, 'PV-项目代码', '产品过程确认主计划', 'DCC_FVM_DHF_021'),
  ('DHF', 22, 'PV-设备编号-A-IQ', '设备安装确认（IQ）方案', 'DCC_FVM_DHF_022'),
  ('DHF', 23, 'PV-设备编号-B-IQ', '设备安装确认（IQ）报告', 'DCC_FVM_DHF_023'),
  ('DHF', 24, 'PV-项目代码-XXXa-OQ', '过程运行确认（OQ）方案', 'DCC_FVM_DHF_024'),
  ('DHF', 25, 'PV-项目代码-XXXb-OQ', '过程运行确认（OQ）报告', 'DCC_FVM_DHF_025'),
  ('DHF', 26, 'PV-项目代码-XXXa-PQ', '过程性能确认（PQ）方案', 'DCC_FVM_DHF_026'),
  ('DHF', 27, 'PV-项目代码-XXXb-PQ', '过程性能确认（PQ）报告', 'DCC_FVM_DHF_027'),
  ('DHF', 28, 'PV-项目代码-XXXb', '过程确认总结报告', 'DCC_FVM_DHF_028'),
  ('DHF', 29, '', '灭菌确认方案/报告', 'DCC_FVM_DHF_029'),
  ('DHF', 30, 'IR-项目代码-XXX', '首次注册资料汇编', 'DCC_FVM_DHF_030'),
  ('DHF', 31, 'ER-项目代码-XXX', '延续注册资料汇编', 'DCC_FVM_DHF_031'),
  ('DHF', 32, 'AR-项目代码-XXX', '变更注册资料汇编', 'DCC_FVM_DHF_032'),
  ('DHF', 33, 'IRD-项目代码-XXX', '首次备案资料汇编', 'DCC_FVM_DHF_033'),
  ('DHF', 34, 'ARD-项目代码-XXX', '变更备案资料汇编', 'DCC_FVM_DHF_034'),
  ('DHF', 35, '', '生产许可/备案资料汇编', 'DCC_FVM_DHF_035'),
  ('DMR', 1, 'DMR-项目代码-XXX', '产品技术要求', 'DCC_FVM_DMR_001'),
  ('DMR', 2, 'DMR-项目代码-XXX', '生产用设备清单', 'DCC_FVM_DMR_002'),
  ('DMR', 3, 'DMR-项目代码-XXX', '检验用设备清单', 'DCC_FVM_DMR_003'),
  ('DMR', 4, 'DMR-项目代码-XXX', 'BOM表', 'DCC_FVM_DMR_004'),
  ('DMR', 5, 'DMR-项目代码-XXX', '产品说明书', 'DCC_FVM_DMR_005'),
  ('DMR', 6, '/', '成品图纸', 'DCC_FVM_DMR_006'),
  ('DMR', 7, '', '零配件图纸', 'DCC_FVM_DMR_007'),
  ('DMR', 8, '', '包装设计', 'DCC_FVM_DMR_008'),
  ('DMR', 9, '', '标签、合格证', 'DCC_FVM_DMR_009'),
  ('DMR', 10, 'P-项目代码', '物资采购清单', 'DCC_FVM_DMR_010'),
  ('DMR', 11, 'P-项目代码-XXX', '采购技术要求', 'DCC_FVM_DMR_011'),
  ('DMR', 12, 'PP-项目代码-X', '工艺流程图', 'DCC_FVM_DMR_012'),
  ('DMR', 13, 'PP-项目代码-X-YY', '工序卡/作业指导书', 'DCC_FVM_DMR_013'),
  ('DMR', 14, 'PP-CR-XXX', '项目间通用工序卡/作业指导书', 'DCC_FVM_DMR_014'),
  ('DMR', 15, 'IQC-项目代码-XXX', '来料检验规程', 'DCC_FVM_DMR_015'),
  ('DMR', 16, 'PQC-项目代码-XXX', '过程检验规程', 'DCC_FVM_DMR_016'),
  ('DMR', 17, 'FQC-项目代码-XXX', '成品检验规程', 'DCC_FVM_DMR_017'),
  ('DMR', 18, 'RE-来料/过程/成品sop-XX', '检验记录表单', 'DCC_FVM_DMR_018'),
  ('DMR', 19, 'INT/RE/XXX-XX', '生产记录表单', 'DCC_FVM_DMR_019'),
  ('DMR', 20, 'STM-XX-XXX', '标准测试方法', 'DCC_FVM_DMR_020'),
  ('DMR', 21, 'P-EQ-设备编号', '设备采购技术要求', 'DCC_FVM_DMR_021'),
  ('DMR', 22, 'P-项目代码-模具编号', '生产/检验用工装模具采购技术要求及设计图纸', 'DCC_FVM_DMR_022'),
  ('DMR', 23, 'SOP-M-模具编号', '生产/检验用工装模具维护保养规范', 'DCC_FVM_DMR_023'),
  ('DMR', 24, 'RE-SOP-M-模具编号', '生产/检验用工装模具维护保养记录表', 'DCC_FVM_DMR_024');

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_department;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_department (
    matrix_department varchar(64) NOT NULL,
    dept_name varchar(64) NOT NULL,
    parent_dept_name varchar(64) NOT NULL,
    subject_lookup_name varchar(128) NOT NULL,
    PRIMARY KEY (matrix_department)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_file_view_matrix_department
    (matrix_department, dept_name, parent_dept_name, subject_lookup_name)
  VALUES
  ('新品开发部', '新品开发部', '研发创新中心', '研发创新中心/新品开发部'),
  ('QA', 'QA', '质量体系中心', '质量体系中心/QA'),
  ('QC', 'QC', '质量体系中心', '质量体系中心/QC'),
  ('QMS', 'QMS', '质量体系中心', '质量体系中心/QMS'),
  ('注册', '注册部', '注册服务中心', '注册服务中心/注册部'),
  ('设备开发', '设备开发部', '研发创新中心', '研发创新中心/设备开发部'),
  ('生产', '生产制造中心', '瑛泰医疗', '瑛泰医疗/生产制造中心'),
  ('生产计划', '生产计划', '供应链中心', '供应链中心/生产计划'),
  ('生产采购', '生产采购', '供应链中心', '供应链中心/生产采购'),
  ('包装设计', '包装设计组', '供应链中心', '供应链中心/包装设计组'),
  ('市场', '市场营销中心', '瑛泰医疗', '瑛泰医疗/市场营销中心'),
  ('检测中心', '检测中心', '瑛泰医疗', '瑛泰医疗/检测中心');

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_role;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_role (
    matrix_department varchar(64) NOT NULL,
    role_name varchar(30) NOT NULL,
    role_code varchar(100) NOT NULL,
    role_remark varchar(500) NOT NULL,
    PRIMARY KEY (role_code)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_file_view_matrix_role
    (matrix_department, role_name, role_code, role_remark)
  VALUES
  ('新品开发部', 'DCC矩阵-新品开发部主管及以上', 'dcc_matrix_new_product_lead', '部门主管及以上'),
  ('包装设计', 'DCC矩阵-包装设计主管及以上', 'dcc_matrix_packaging_design_lead', '部门主管及以上'),
  ('生产', 'DCC矩阵-生产工段长班组长车间主任', 'dcc_matrix_production_line_lead', '工段长+班组长+车间主任'),
  ('生产采购', 'DCC矩阵-生产采购主管及以上', 'dcc_matrix_production_purchase_lead', '部门主管及以上'),
  ('QC', 'DCC矩阵-QC主管及以上', 'dcc_matrix_qc_lead', '部门主管及以上');

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_grant;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_grant (
    category_code varchar(64) NOT NULL,
    matrix_department varchar(64) NOT NULL,
    marker varchar(4) NOT NULL,
    action_type varchar(32) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_lookup_name varchar(128) NOT NULL,
    access_note varchar(128) NOT NULL,
    PRIMARY KEY (category_code, matrix_department)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_file_view_matrix_grant
    (category_code, matrix_department, marker, action_type, subject_type, subject_lookup_name, access_note)
  VALUES
  ('DCC_FVM_DHF_001', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_001', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_002', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_002', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_003', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_003', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_004', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_004', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_005', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_005', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_006', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_006', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_007', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_007', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_008', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_008', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_009', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_009', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_009', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_010', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_010', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_010', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_010', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_011', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_011', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_011', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_011', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_012', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_012', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_013', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_013', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_014', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_014', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_015', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_015', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_016', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_016', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_017', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_017', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_017', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_018', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_018', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_018', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_019', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_019', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_020', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_020', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_021', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_021', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_021', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_022', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_022', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_022', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_022', '设备开发', '●', 'VIEW', 'DEPT', '研发创新中心/设备开发部', '部门内全员'),  ('DCC_FVM_DHF_023', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_023', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_023', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_023', '设备开发', '●', 'VIEW', 'DEPT', '研发创新中心/设备开发部', '部门内全员'),  ('DCC_FVM_DHF_024', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_024', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_024', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_025', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_025', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_025', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_026', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_026', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_026', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_027', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_027', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_027', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_028', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_028', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_028', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_029', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_029', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DHF_029', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_030', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_030', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_030', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_031', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_031', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_031', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_032', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_032', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_032', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_033', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_033', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_033', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_034', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_034', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_034', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DHF_035', '新品开发部', '▲', 'VIEW', 'ROLE', 'DCC矩阵-新品开发部主管及以上', '部门主管及以上'),  ('DCC_FVM_DHF_035', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DHF_035', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_001', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_001', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_001', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_001', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_002', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_002', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_002', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_002', '设备开发', '●', 'VIEW', 'DEPT', '研发创新中心/设备开发部', '部门内全员'),  ('DCC_FVM_DMR_002', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_002', '包装设计', '▲', 'VIEW', 'ROLE', 'DCC矩阵-包装设计主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_003', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_003', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_003', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_003', '设备开发', '●', 'VIEW', 'DEPT', '研发创新中心/设备开发部', '部门内全员'),  ('DCC_FVM_DMR_003', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_003', '包装设计', '▲', 'VIEW', 'ROLE', 'DCC矩阵-包装设计主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_004', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_004', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_004', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_004', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_004', '生产计划', '●', 'VIEW', 'DEPT', '供应链中心/生产计划', '部门内全员'),  ('DCC_FVM_DMR_004', '生产采购', '●', 'VIEW', 'DEPT', '供应链中心/生产采购', '部门内全员'),  ('DCC_FVM_DMR_004', '包装设计', '▲', 'VIEW', 'ROLE', 'DCC矩阵-包装设计主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_004', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_005', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_005', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_005', 'QC', '●', 'VIEW', 'DEPT', '质量体系中心/QC', '部门内全员'),  ('DCC_FVM_DMR_005', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_005', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_005', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_005', '生产计划', '●', 'VIEW', 'DEPT', '供应链中心/生产计划', '部门内全员'),  ('DCC_FVM_DMR_005', '生产采购', '●', 'VIEW', 'DEPT', '供应链中心/生产采购', '部门内全员'),  ('DCC_FVM_DMR_005', '包装设计', '●', 'VIEW', 'DEPT', '供应链中心/包装设计组', '部门内全员'),  ('DCC_FVM_DMR_005', '市场', '●', 'VIEW', 'DEPT', '瑛泰医疗/市场营销中心', '业务+跟单及以上'),  ('DCC_FVM_DMR_005', '检测中心', '●', 'VIEW', 'DEPT', '瑛泰医疗/检测中心', '部门内全员'),  ('DCC_FVM_DMR_006', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_006', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_006', 'QC', '●', 'VIEW', 'DEPT', '质量体系中心/QC', '部门内全员'),  ('DCC_FVM_DMR_006', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_006', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_006', '生产计划', '●', 'VIEW', 'DEPT', '供应链中心/生产计划', '部门内全员'),  ('DCC_FVM_DMR_006', '生产采购', '●', 'VIEW', 'DEPT', '供应链中心/生产采购', '部门内全员'),  ('DCC_FVM_DMR_006', '包装设计', '●', 'VIEW', 'DEPT', '供应链中心/包装设计组', '部门内全员'),  ('DCC_FVM_DMR_006', '市场', '●', 'VIEW', 'DEPT', '瑛泰医疗/市场营销中心', '业务+跟单及以上'),  ('DCC_FVM_DMR_006', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_006', '检测中心', '●', 'VIEW', 'DEPT', '瑛泰医疗/检测中心', '部门内全员'),  ('DCC_FVM_DMR_007', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_007', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_007', 'QC', '●', 'VIEW', 'DEPT', '质量体系中心/QC', '部门内全员'),  ('DCC_FVM_DMR_007', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_007', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_007', '生产计划', '●', 'VIEW', 'DEPT', '供应链中心/生产计划', '部门内全员'),  ('DCC_FVM_DMR_007', '生产采购', '●', 'VIEW', 'DEPT', '供应链中心/生产采购', '部门内全员'),  ('DCC_FVM_DMR_007', '包装设计', '●', 'VIEW', 'DEPT', '供应链中心/包装设计组', '部门内全员'),  ('DCC_FVM_DMR_007', '市场', '●', 'VIEW', 'DEPT', '瑛泰医疗/市场营销中心', '业务+跟单及以上'),  ('DCC_FVM_DMR_007', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_007', '检测中心', '●', 'VIEW', 'DEPT', '瑛泰医疗/检测中心', '部门内全员'),  ('DCC_FVM_DMR_008', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_008', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_008', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_009', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_009', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_009', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_010', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_010', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_010', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_010', '生产', '▲', 'VIEW', 'ROLE', 'DCC矩阵-生产工段长班组长车间主任', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_010', '生产采购', '●', 'VIEW', 'DEPT', '供应链中心/生产采购', '部门内全员'),  ('DCC_FVM_DMR_010', '包装设计', '▲', 'VIEW', 'ROLE', 'DCC矩阵-包装设计主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_010', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_011', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_011', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_011', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_011', '生产', '▲', 'VIEW', 'ROLE', 'DCC矩阵-生产工段长班组长车间主任', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_011', '生产采购', '●', 'VIEW', 'DEPT', '供应链中心/生产采购', '部门内全员'),  ('DCC_FVM_DMR_011', '包装设计', '▲', 'VIEW', 'ROLE', 'DCC矩阵-包装设计主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_011', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_012', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_012', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_012', 'QC', '●', 'VIEW', 'DEPT', '质量体系中心/QC', '部门内全员'),  ('DCC_FVM_DMR_012', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_012', '设备开发', '●', 'VIEW', 'DEPT', '研发创新中心/设备开发部', '部门内全员'),  ('DCC_FVM_DMR_012', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_012', '生产计划', '●', 'VIEW', 'DEPT', '供应链中心/生产计划', '部门内全员'),  ('DCC_FVM_DMR_012', '生产采购', '▲', 'VIEW', 'ROLE', 'DCC矩阵-生产采购主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_012', '包装设计', '●', 'VIEW', 'DEPT', '供应链中心/包装设计组', '部门内全员'),  ('DCC_FVM_DMR_013', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_013', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_013', 'QC', '●', 'VIEW', 'DEPT', '质量体系中心/QC', '部门内全员'),  ('DCC_FVM_DMR_013', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_013', '设备开发', '●', 'VIEW', 'DEPT', '研发创新中心/设备开发部', '部门内全员'),  ('DCC_FVM_DMR_013', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_013', '生产计划', '●', 'VIEW', 'DEPT', '供应链中心/生产计划', '部门内全员'),  ('DCC_FVM_DMR_013', '生产采购', '▲', 'VIEW', 'ROLE', 'DCC矩阵-生产采购主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_013', '包装设计', '●', 'VIEW', 'DEPT', '供应链中心/包装设计组', '部门内全员'),  ('DCC_FVM_DMR_014', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_014', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_015', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_015', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_015', 'QC', '●', 'VIEW', 'DEPT', '质量体系中心/QC', '部门内全员'),  ('DCC_FVM_DMR_015', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_015', '生产', '▲', 'VIEW', 'ROLE', 'DCC矩阵-生产工段长班组长车间主任', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_015', '生产计划', '●', 'VIEW', 'DEPT', '供应链中心/生产计划', '部门内全员'),  ('DCC_FVM_DMR_015', '生产采购', '●', 'VIEW', 'DEPT', '供应链中心/生产采购', '部门内全员'),  ('DCC_FVM_DMR_015', '包装设计', '●', 'VIEW', 'DEPT', '供应链中心/包装设计组', '部门内全员'),  ('DCC_FVM_DMR_015', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_016', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_016', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_016', 'QC', '●', 'VIEW', 'DEPT', '质量体系中心/QC', '部门内全员'),  ('DCC_FVM_DMR_016', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_016', '设备开发', '●', 'VIEW', 'DEPT', '研发创新中心/设备开发部', '部门内全员'),  ('DCC_FVM_DMR_016', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_016', '包装设计', '●', 'VIEW', 'DEPT', '供应链中心/包装设计组', '部门内全员'),  ('DCC_FVM_DMR_016', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_017', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_017', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_017', 'QC', '▲', 'VIEW', 'ROLE', 'DCC矩阵-QC主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_017', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_017', '设备开发', '●', 'VIEW', 'DEPT', '研发创新中心/设备开发部', '部门内全员'),  ('DCC_FVM_DMR_017', '生产', '▲', 'VIEW', 'ROLE', 'DCC矩阵-生产工段长班组长车间主任', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_017', '包装设计', '●', 'VIEW', 'DEPT', '供应链中心/包装设计组', '部门内全员'),  ('DCC_FVM_DMR_017', '注册', '●', 'VIEW', 'DEPT', '注册服务中心/注册部', '部门内全员'),  ('DCC_FVM_DMR_017', '检测中心', '●', 'VIEW', 'DEPT', '瑛泰医疗/检测中心', '部门内全员'),  ('DCC_FVM_DMR_018', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_018', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_018', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_018', '检测中心', '●', 'VIEW', 'DEPT', '瑛泰医疗/检测中心', '部门内全员'),  ('DCC_FVM_DMR_019', '新品开发部', '●', 'VIEW', 'DEPT', '研发创新中心/新品开发部', '部门内全员'),  ('DCC_FVM_DMR_019', 'QA', '●', 'VIEW', 'DEPT', '质量体系中心/QA', '部门内全员'),  ('DCC_FVM_DMR_019', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_019', '生产', '●', 'VIEW', 'DEPT', '瑛泰医疗/生产制造中心', '工段长+班组长+车间主任'),  ('DCC_FVM_DMR_019', '包装设计', '▲', 'VIEW', 'ROLE', 'DCC矩阵-包装设计主管及以上', '部门主管及以上'),  ('DCC_FVM_DMR_020', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_021', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_022', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_023', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员'),  ('DCC_FVM_DMR_024', 'QMS', '●', 'VIEW', 'DEPT', '质量体系中心/QMS', '部门内全员');

  INSERT INTO `system_role`
    (name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
     tenant_id, creator, create_time, updater, update_time, deleted)
  SELECT role.role_name, role.role_code, 6900, 1, '', 0, 2,
         CONCAT('DCC 文件查阅矩阵主管级角色：', role.role_remark),
         @dcc_fvm_tenant_id, @dcc_fvm_actor, NOW(), @dcc_fvm_actor, NOW(), b'0'
  FROM tmp_dcc_file_view_matrix_role role
  WHERE NOT EXISTS (
    SELECT 1 FROM system_role existing
    WHERE existing.tenant_id = @dcc_fvm_tenant_id
      AND existing.deleted = b'0'
      AND existing.code = role.role_code
  );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_resolved_dept;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_resolved_dept AS
  SELECT mapping.matrix_department,
         mapping.subject_lookup_name,
         dept.id AS subject_id
  FROM tmp_dcc_file_view_matrix_department mapping
  JOIN system_dept dept
    ON dept.tenant_id = @dcc_fvm_tenant_id
   AND dept.deleted = b'0'
   AND dept.status = 0
   AND dept.name = mapping.dept_name
  JOIN system_dept parent_dept
    ON parent_dept.tenant_id = @dcc_fvm_tenant_id
   AND parent_dept.deleted = b'0'
   AND parent_dept.id = dept.parent_id
   AND parent_dept.name = mapping.parent_dept_name;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_missing_dept;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_missing_dept AS
  SELECT mapping.matrix_department, mapping.subject_lookup_name, COALESCE(resolved.resolved_count, 0) AS resolved_count
  FROM tmp_dcc_file_view_matrix_department mapping
  LEFT JOIN (
    SELECT matrix_department, COUNT(*) AS resolved_count
    FROM tmp_dcc_file_view_matrix_resolved_dept
    GROUP BY matrix_department
  ) resolved ON resolved.matrix_department = mapping.matrix_department
  WHERE COALESCE(resolved.resolved_count, 0) <> 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_file_view_matrix_missing_dept) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(matrix_department, '=>', subject_lookup_name, '#', resolved_count) SEPARATOR '; ')
      INTO @dcc_fvm_missing_dept
    FROM tmp_dcc_file_view_matrix_missing_dept;
    SET @dcc_fvm_missing_dept = CONCAT('DCC_FILE_VIEW_MATRIX_SUBJECT_PRECHECK_FAILED: dept=', @dcc_fvm_missing_dept);
    SET @dcc_fvm_missing_dept_signal_message = LEFT(@dcc_fvm_missing_dept, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @dcc_fvm_missing_dept_signal_message;
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_resolved_role;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_resolved_role AS
  SELECT role_map.matrix_department,
         role_map.role_name AS subject_lookup_name,
         role_map.role_code,
         role.id AS subject_id
  FROM tmp_dcc_file_view_matrix_role role_map
  JOIN system_role role
    ON role.tenant_id = @dcc_fvm_tenant_id
   AND role.deleted = b'0'
   AND role.code = role_map.role_code;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_missing_role;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_missing_role AS
  SELECT role_map.matrix_department, role_map.role_name, role_map.role_code, COALESCE(resolved.resolved_count, 0) AS resolved_count
  FROM tmp_dcc_file_view_matrix_role role_map
  LEFT JOIN (
    SELECT role_code, COUNT(*) AS resolved_count
    FROM tmp_dcc_file_view_matrix_resolved_role
    GROUP BY role_code
  ) resolved ON resolved.role_code = role_map.role_code
  WHERE COALESCE(resolved.resolved_count, 0) <> 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_file_view_matrix_missing_role) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(matrix_department, '=>', role_code, '#', resolved_count) SEPARATOR '; ')
      INTO @dcc_fvm_missing_role
    FROM tmp_dcc_file_view_matrix_missing_role;
    SET @dcc_fvm_missing_role = CONCAT('DCC_FILE_VIEW_MATRIX_SUBJECT_PRECHECK_FAILED: role=', @dcc_fvm_missing_role);
    SET @dcc_fvm_missing_role_signal_message = LEFT(@dcc_fvm_missing_role, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @dcc_fvm_missing_role_signal_message;
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_category_code_match;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_category_code_match AS
  SELECT matrix_category.category_code, COUNT(code_category.id) AS code_count
  FROM tmp_dcc_file_view_matrix_category matrix_category
  LEFT JOIN dcc_file_category code_category
    ON code_category.tenant_id = @dcc_fvm_tenant_id
   AND code_category.deleted = 0
   AND code_category.code = matrix_category.category_code
  GROUP BY matrix_category.category_code;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_category_name_match;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_category_name_match AS
  SELECT matrix_category.category_code, COUNT(name_category.id) AS name_count
  FROM tmp_dcc_file_view_matrix_category matrix_category
  LEFT JOIN dcc_file_category name_category
    ON name_category.tenant_id = @dcc_fvm_tenant_id
   AND name_category.deleted = 0
   AND name_category.name = matrix_category.file_name
  GROUP BY matrix_category.category_code;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_category_ambiguous;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_category_ambiguous AS
  SELECT matrix_category.category_code,
         matrix_category.file_name,
         CASE
           WHEN code_match.code_count > 1 THEN code_match.code_count
           ELSE name_match.name_count
         END AS resolved_count
  FROM tmp_dcc_file_view_matrix_category matrix_category
  JOIN tmp_dcc_file_view_matrix_category_code_match code_match
    ON code_match.category_code = matrix_category.category_code
  JOIN tmp_dcc_file_view_matrix_category_name_match name_match
    ON name_match.category_code = matrix_category.category_code
  WHERE code_match.code_count > 1
     OR (COALESCE(code_match.code_count, 0) = 0 AND name_match.name_count > 1);

  IF (SELECT COUNT(*) FROM tmp_dcc_file_view_matrix_category_ambiguous) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(category_code, '=>', file_name, '#', resolved_count) SEPARATOR '; ')
      INTO @dcc_fvm_ambiguous_category
    FROM tmp_dcc_file_view_matrix_category_ambiguous;
    SET @dcc_fvm_ambiguous_category = CONCAT('DCC_FILE_VIEW_MATRIX_CATEGORY_PRECHECK_FAILED: ', @dcc_fvm_ambiguous_category);
    SET @dcc_fvm_ambiguous_category_signal_message = LEFT(@dcc_fvm_ambiguous_category, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @dcc_fvm_ambiguous_category_signal_message;
  END IF;

  INSERT INTO `dcc_file_category`
    (code, name, parent_id, active, sort, source, remark, description,
     distribution_required, training_required, tenant_id, create_time, update_time, creator, updater, deleted)
  SELECT matrix_category.category_code,
         matrix_category.file_name,
         NULL,
         1,
         CASE matrix_category.matrix_group WHEN 'DHF' THEN matrix_category.matrix_sort ELSE 100 + matrix_category.matrix_sort END,
         'VIEW_MATRIX',
         CONCAT('文件查阅矩阵 ', matrix_category.matrix_group, '-', LPAD(matrix_category.matrix_sort, 3, '0')),
         CONCAT('编号模板：', COALESCE(NULLIF(matrix_category.file_number_pattern, ''), '/')),
         0,
         0,
         @dcc_fvm_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_actor,
         @dcc_fvm_actor,
         0
  FROM tmp_dcc_file_view_matrix_category matrix_category
  WHERE NOT EXISTS (
    SELECT 1 FROM dcc_file_category existing
    WHERE existing.tenant_id = @dcc_fvm_tenant_id
      AND existing.deleted = 0
      AND (existing.code = matrix_category.category_code OR existing.name = matrix_category.file_name)
  );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_category_resolved;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_category_resolved AS
  SELECT matrix_category.category_code,
         matrix_category.matrix_group,
         matrix_category.matrix_sort,
         matrix_category.file_name,
         category.id AS category_id
  FROM tmp_dcc_file_view_matrix_category matrix_category
  JOIN tmp_dcc_file_view_matrix_category_code_match code_match
    ON code_match.category_code = matrix_category.category_code
  JOIN dcc_file_category category
    ON category.tenant_id = @dcc_fvm_tenant_id
   AND category.deleted = 0
   AND (
     category.code = matrix_category.category_code
     OR (COALESCE(code_match.code_count, 0) = 0 AND category.name = matrix_category.file_name)
   );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_category_missing;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_category_missing AS
  SELECT matrix_category.category_code, matrix_category.file_name, COALESCE(resolved.resolved_count, 0) AS resolved_count
  FROM tmp_dcc_file_view_matrix_category matrix_category
  LEFT JOIN (
    SELECT category_code, COUNT(*) AS resolved_count
    FROM tmp_dcc_file_view_matrix_category_resolved
    GROUP BY category_code
  ) resolved ON resolved.category_code = matrix_category.category_code
  WHERE COALESCE(resolved.resolved_count, 0) <> 1;

  IF (SELECT COUNT(*) FROM tmp_dcc_file_view_matrix_category_missing) > 0 THEN
    SELECT GROUP_CONCAT(CONCAT(category_code, '=>', file_name, '#', resolved_count) SEPARATOR '; ')
      INTO @dcc_fvm_missing_category
    FROM tmp_dcc_file_view_matrix_category_missing;
    SET @dcc_fvm_missing_category = CONCAT('DCC_FILE_VIEW_MATRIX_CATEGORY_PRECHECK_FAILED: ', @dcc_fvm_missing_category);
    SET @dcc_fvm_missing_category_signal_message = LEFT(@dcc_fvm_missing_category, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @dcc_fvm_missing_category_signal_message;
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_resolved_subject;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_resolved_subject (
    category_code varchar(64) NOT NULL,
    matrix_department varchar(64) NOT NULL,
    marker varchar(4) NOT NULL,
    action_type varchar(32) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    subject_lookup_name varchar(128) NOT NULL,
    access_note varchar(128) NOT NULL,
    scope_type varchar(32) NOT NULL
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO tmp_dcc_file_view_matrix_resolved_subject
    (category_code, matrix_department, marker, action_type, subject_type, subject_id, subject_lookup_name, access_note, scope_type)
  SELECT grant_rule.category_code,
         grant_rule.matrix_department,
         grant_rule.marker,
         grant_rule.action_type,
         grant_rule.subject_type,
         resolved_dept.subject_id,
         grant_rule.subject_lookup_name,
         grant_rule.access_note,
         CASE
           WHEN grant_rule.matrix_department = '新品开发部'
             AND grant_rule.subject_type = 'DEPT'
             AND grant_rule.marker = '●'
           THEN 'PRODUCT_GROUP'
           ELSE 'GLOBAL'
         END AS scope_type
  FROM tmp_dcc_file_view_matrix_grant grant_rule
  JOIN tmp_dcc_file_view_matrix_resolved_dept resolved_dept
    ON grant_rule.subject_type = 'DEPT'
   AND resolved_dept.matrix_department = grant_rule.matrix_department;

  INSERT INTO tmp_dcc_file_view_matrix_resolved_subject
    (category_code, matrix_department, marker, action_type, subject_type, subject_id, subject_lookup_name, access_note, scope_type)
  SELECT grant_rule.category_code,
         grant_rule.matrix_department,
         grant_rule.marker,
         grant_rule.action_type,
         grant_rule.subject_type,
         resolved_role.subject_id,
         grant_rule.subject_lookup_name,
         grant_rule.access_note,
         'GLOBAL' AS scope_type
  FROM tmp_dcc_file_view_matrix_grant grant_rule
  JOIN tmp_dcc_file_view_matrix_resolved_role resolved_role
    ON grant_rule.subject_type = 'ROLE'
   AND resolved_role.subject_lookup_name = grant_rule.subject_lookup_name;

  INSERT INTO `dcc_file_category_permission_rule`
    (category_id, action_type, subject_type, subject_id, scope_type, active, remark,
     tenant_id, create_time, update_time, creator, updater, deleted)
  SELECT category_resolved.category_id,
         subject_resolved.action_type,
         subject_resolved.subject_type,
         subject_resolved.subject_id,
         subject_resolved.scope_type,
         1,
         CONCAT('文件查阅矩阵 ', subject_resolved.matrix_department, ' ', subject_resolved.marker, ' ', subject_resolved.access_note),
         @dcc_fvm_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_actor,
         @dcc_fvm_actor,
         0
  FROM tmp_dcc_file_view_matrix_resolved_subject subject_resolved
  JOIN tmp_dcc_file_view_matrix_category_resolved category_resolved
    ON category_resolved.category_code = subject_resolved.category_code
  ON DUPLICATE KEY UPDATE
    active = VALUES(active),
    scope_type = VALUES(scope_type),
    remark = VALUES(remark),
    update_time = VALUES(update_time),
    updater = VALUES(updater),
    deleted = VALUES(deleted);

  UPDATE `dcc_file_category_permission_rule` legacy_rule
  JOIN tmp_dcc_file_view_matrix_category_resolved category_resolved
    ON category_resolved.category_id = legacy_rule.category_id
  LEFT JOIN tmp_dcc_file_view_matrix_resolved_subject subject_resolved
    ON subject_resolved.category_code = category_resolved.category_code
   AND subject_resolved.action_type = legacy_rule.action_type
   AND subject_resolved.subject_type = legacy_rule.subject_type
   AND subject_resolved.subject_id = legacy_rule.subject_id
   AND subject_resolved.scope_type = legacy_rule.scope_type
  SET legacy_rule.active = 0,
      legacy_rule.deleted = 1,
      legacy_rule.remark = LEFT(CONCAT('DCC_FILE_VIEW_MATRIX_LEGACY_PERMISSION_DISABLED: ', COALESCE(legacy_rule.remark, '')), 255),
      legacy_rule.update_time = NOW(),
      legacy_rule.updater = @dcc_fvm_actor
  WHERE legacy_rule.tenant_id = @dcc_fvm_tenant_id
    AND legacy_rule.deleted = 0
    AND legacy_rule.active = 1
    AND (
      legacy_rule.action_type = 'DOWNLOAD'
      OR legacy_rule.subject_type = 'USER'
      OR (
        legacy_rule.action_type = 'VIEW'
        AND subject_resolved.category_code IS NULL
      )
    );

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_directory_subject;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_directory_subject AS
  SELECT DISTINCT directory_subject.`subject_type`, directory_subject.`subject_id`
  FROM tmp_dcc_file_view_matrix_resolved_subject directory_subject;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_missing_directory;
  CREATE TEMPORARY TABLE tmp_dcc_file_view_matrix_missing_directory AS
  SELECT 'NO_ACTIVE_DCC_DIRECTORY' AS missing_reason
  WHERE NOT EXISTS (
    SELECT 1
    FROM dcc_file_directory directory_record
    WHERE directory_record.tenant_id = @dcc_fvm_tenant_id
      AND directory_record.deleted = 0
      AND directory_record.active = 1
  );

  IF (SELECT COUNT(*) FROM tmp_dcc_file_view_matrix_missing_directory) > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC_FILE_VIEW_MATRIX_DIRECTORY_PRECHECK_FAILED: no active DCC directory for tenant';
  END IF;

  UPDATE dcc_directory_access_rule access_rule
  JOIN dcc_file_directory directory_record
    ON directory_record.id = access_rule.directory_id
   AND directory_record.tenant_id = @dcc_fvm_tenant_id
   AND directory_record.deleted = 0
   AND directory_record.active = 1
  JOIN tmp_dcc_file_view_matrix_directory_subject subject_resolved
    ON subject_resolved.subject_type = access_rule.subject_type
   AND subject_resolved.subject_id = access_rule.subject_id
  SET access_rule.can_query = 1,
      access_rule.can_preview = 1,
      access_rule.can_download = 0,
      access_rule.active = 1,
      access_rule.change_reason = '文件查阅矩阵：允许查询与预览，不自动开放下载',
      access_rule.update_time = NOW(),
      access_rule.updater = @dcc_fvm_actor,
      access_rule.deleted = 0
  WHERE access_rule.tenant_id = @dcc_fvm_tenant_id;

  INSERT INTO `dcc_directory_access_rule`
    (`directory_id`, `subject_type`, `subject_id`, `can_query`, `can_preview`, `can_download`, `active`, `change_reason`,
     `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`)
  SELECT directory_record.id,
         directory_subject.subject_type,
         directory_subject.subject_id,
         1,
         1,
         0,
         1,
         '文件查阅矩阵：允许查询与预览，不自动开放下载',
         @dcc_fvm_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_actor,
         @dcc_fvm_actor,
         0
  FROM dcc_file_directory directory_record
  CROSS JOIN tmp_dcc_file_view_matrix_directory_subject directory_subject
  WHERE directory_record.tenant_id = @dcc_fvm_tenant_id
    AND directory_record.deleted = 0
    AND directory_record.active = 1
    AND NOT EXISTS (
      SELECT 1
      FROM dcc_directory_access_rule existing
      WHERE existing.tenant_id = @dcc_fvm_tenant_id
        AND existing.deleted = 0
        AND existing.directory_id = directory_record.id
        AND existing.subject_type = directory_subject.subject_type
        AND existing.subject_id = directory_subject.subject_id
    );

  SELECT 'DCC_FILE_VIEW_MATRIX_UNCLASSIFIED_AUDIT' AS audit_code,
         controlled_file.id,
         controlled_file.file_number,
         controlled_file.title,
         controlled_file.file_name,
         controlled_file.product_name,
         controlled_file.category_id
  FROM dcc_controlled_file controlled_file
  JOIN dcc_file_category category_record ON category_record.id = controlled_file.category_id
  WHERE controlled_file.tenant_id = @dcc_fvm_tenant_id
    AND controlled_file.deleted = 0
    AND category_record.name = CONVERT(UNHEX('E585B6E4BB96') USING utf8mb4) COLLATE utf8mb4_unicode_ci
  ORDER BY controlled_file.id;
END$$
DELIMITER ;

CALL apply_dcc_file_view_matrix_seed();
DROP PROCEDURE IF EXISTS apply_dcc_file_view_matrix_seed;
