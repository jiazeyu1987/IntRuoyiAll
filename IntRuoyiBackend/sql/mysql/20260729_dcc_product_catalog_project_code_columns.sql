-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_dcc_product_catalog_database; type=data; riskLevel=medium
-- DCC 产品目录增加项目名称/项目代码，并仅回填前置分析确认的“完全对应”瑛泰产品目录行。
-- Rollback: UPDATE dcc_product_catalog SET project_name = NULL, project_code = NULL WHERE HEX(data_source) = 'E7919BE6B3B0E4BAA7E59381'; ALTER TABLE dcc_product_catalog DROP COLUMN project_name, DROP COLUMN project_code;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @dcc_product_catalog_project_name_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dcc_product_catalog'
    AND column_name = 'project_name'
);
SET @dcc_product_catalog_project_name_column_sql := IF(
  @dcc_product_catalog_project_name_column_exists = 0,
  'ALTER TABLE `dcc_product_catalog` ADD COLUMN `project_name` varchar(255) DEFAULT NULL COMMENT ''项目名称'' AFTER `product_code`',
  'SELECT 1'
);
PREPARE dcc_product_catalog_project_name_column_stmt FROM @dcc_product_catalog_project_name_column_sql;
EXECUTE dcc_product_catalog_project_name_column_stmt;
DEALLOCATE PREPARE dcc_product_catalog_project_name_column_stmt;

SET @dcc_product_catalog_project_code_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dcc_product_catalog'
    AND column_name = 'project_code'
);
SET @dcc_product_catalog_project_code_column_sql := IF(
  @dcc_product_catalog_project_code_column_exists = 0,
  'ALTER TABLE `dcc_product_catalog` ADD COLUMN `project_code` varchar(64) DEFAULT NULL COMMENT ''项目代码'' AFTER `project_name`',
  'SELECT 1'
);
PREPARE dcc_product_catalog_project_code_column_stmt FROM @dcc_product_catalog_project_code_column_sql;
EXECUTE dcc_product_catalog_project_code_column_stmt;
DEALLOCATE PREPARE dcc_product_catalog_project_code_column_stmt;

DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_product_catalog_project_match`;
CREATE TEMPORARY TABLE `tmp_dcc_product_catalog_project_match` (
  `original_row_no` int NOT NULL PRIMARY KEY,
  `project_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tmp_dcc_product_catalog_project_match` (`original_row_no`, `project_name`, `project_code`) VALUES
  (2, '一次性使用血管鞘', 'VS'),
  (3, '血管鞘 (CE准备资料）', 'VS(CE)'),
  (4, '一次性使用亲水涂层导管鞘', 'HGIK'),
  (5, '一次性使用导管鞘套装', 'IK'),
  (6, '一次性使用导管鞘套装', 'IK'),
  (7, '一次性使用导管鞘套装', 'IK'),
  (9, '一次性使用导管鞘套装', 'IK'),
  (10, '无菌导管鞘组', 'ACS'),
  (12, 'mini鞘', 'MI'),
  (13, 'mini鞘', 'MI'),
  (15, '导引导丝（血管指引导丝）', 'GW（BGGW）'),
  (16, '造影导丝（国内）', 'AGW'),
  (17, '造影导丝（国内）', 'AGW'),
  (19, '造影导丝（国内）', 'AGW'),
  (20, '一次性使用亲水涂层导丝', 'HGGW'),
  (21, '一次性使用亲水性导丝', 'HGW'),
  (22, '导引导丝（血管指引导丝）', 'GW（BGGW）'),
  (23, '一次性使用指引导丝', 'MGW'),
  (26, '指引导丝', 'CEMGW'),
  (27, '硬导丝', 'SSGW'),
  (28, '一次性使用硬导丝', 'SGW'),
  (33, '一次性使用指引导管', 'GC'),
  (34, '指引导管（CE）', 'GC(CE)'),
  (35, '指引导管（CE）', 'GC(CE)'),
  (36, '导引导管', 'NGC'),
  (37, '亲水涂层导引导管', 'HGC'),
  (38, '指引延长导管', 'GEC'),
  (39, '指引延长导管', 'GEC'),
  (40, '指引延长导管', 'GEC'),
  (41, '支撑导管（TUV)', 'SC(TUV)'),
  (42, '支撑导管（TUV)', 'SC(TUV)'),
  (46, '输送导管', 'DC'),
  (48, '一次性使用造影导管(俄罗斯审核）', 'AC'),
  (49, '一次性使用造影导管(俄罗斯审核）', 'AC'),
  (50, '一次性使用造影导管(俄罗斯审核）', 'AC'),
  (51, '一次性使用造影导管(俄罗斯审核）', 'AC'),
  (52, '一次性使用造影导管(俄罗斯审核）', 'AC'),
  (53, '亲水涂层造影导管', 'HAC'),
  (54, '亲水涂层血管造影导管', 'HVAC'),
  (56, '亲水涂层血管造影导管', 'HVAC'),
  (57, '球囊扩张压力泵', 'ID'),
  (58, '球囊扩张压力泵', 'ID'),
  (59, '球囊扩张压力泵', 'ID'),
  (60, '按压式球囊扩张压力泵', 'IDPR'),
  (61, '按压式球囊扩充压力泵', 'IDI'),
  (62, '数显球囊扩张压力泵（FDA)', 'IDE(FDA)'),
  (64, 'PTCA球囊扩张导管', 'PTCABC'),
  (66, 'PTCA球囊扩张导管', 'PTCABC'),
  (67, '非顺应性球囊扩张导管', 'NTPTCA'),
  (68, '冠状动脉棘突球囊扩张导管', 'SDC'),
  (76, '一次性使用压力延长管（CE)', 'CEPMT'),
  (77, '一次性使用压力延长管（CE)', 'CEPMT'),
  (78, '一次性使用压力延长管（CE)', 'CEPMT'),
  (79, '一次性使用高压延长管（CE)', 'CEHPMT'),
  (80, '一次性使用高压延长管（CE)', 'CEHPMT'),
  (81, '一次性使用微导管', 'MC'),
  (82, '微导管', 'IMC'),
  (83, '微导管', 'IMC'),
  (84, '微导管', 'IMC'),
  (85, '微导管', 'IMC'),
  (86, '一次性使用微导管', 'MC'),
  (87, '微导管', 'IMC'),
  (91, '多环测量灌注导管', 'FC'),
  (93, '一次性使用血栓抽吸系统（CE+巴西）', 'XSCX(CE)'),
  (98, '按压式Y型连接器', 'YCKPR'),
  (102, 'Y型连接阀套件', 'HV'),
  (104, '一次性使用三通旋塞', 'MN'),
  (105, '一次性使用三通旋塞', 'MN'),
  (106, '三连三通套装（CEMK)', 'CEMK'),
  (109, '造影剂推入器', 'CEACS'),
  (110, '造影剂推入器', 'CEACS'),
  (111, '气囊式止血带', 'PBAP'),
  (112, '气囊式止血带', 'PBAP'),
  (113, '气囊式止血带', 'PBAP'),
  (114, '气囊式股动脉止血带', 'BFAT'),
  (115, '一次性使用动脉压迫止血带', 'PB'),
  (117, '股动脉压迫止血带', 'PBF'),
  (118, '无菌敷贴止血带', 'PBWA'),
  (120, '动脉留置导管', 'ACK'),
  (121, '一次性使用输液装置', 'IP'),
  (122, '输液连接管路', 'YYZY'),
  (123, '输液接头及附件', 'ICA'),
  (124, '输液接头及附件', 'ICA'),
  (126, '肠道球囊导管', 'IBC'),
  (127, '输尿管球囊导管', 'UC'),
  (128, '咽鼓管球囊扩张导管', 'EC'),
  (130, '医用电动吸引器', 'VAP'),
  (131, '负压锁定抽吸器', 'NLS'),
  (132, '负压锁定抽吸器', 'NLS'),
  (133, '负压锁定抽吸器', 'NLS'),
  (135, '医用外科口罩', 'SM'),
  (136, '医用口罩(CE)', 'MFM'),
  (137, '医用防护口罩', 'FM'),
  (144, '亲水润滑导尿管', 'HC'),
  (146, '一次性使用有创压力传感器', 'IBPT'),
  (147, '一次性使用有创压力传感器', 'IBPT'),
  (148, '一次性使用有创血压传感器', 'IBPTR'),
  (152, 'β-磷酸三钙人工骨', 'AB'),
  (161, '一次性使用椎体工具包', 'VTK'),
  (163, '骨髓血穿刺抽吸循环器械', 'SCF'),
  (164, '骨髓血穿刺抽吸循环动力泵', 'CEP'),
  (165, '输卵管导管套件', 'OC'),
  (166, '输卵管导管及附件', 'FTC'),
  (167, '脐带剪夹器', 'UCC'),
  (169, '一次性使用阴道扩张器', 'VD'),
  (170, '微创筋膜闭合器（CE)', 'MSN（CE)'),
  (172, '斑马导丝', 'UG'),
  (173, '一次性使用取石网篮', 'QSW'),
  (174, '一次性使用取石网篮', 'QSW'),
  (175, '一次性使用取石球囊', 'QSN'),
  (176, '穿刺针', 'SN'),
  (177, '穿刺针', 'SN'),
  (179, '电子内窥镜图像处理器', 'IPE'),
  (180, '医用控压冲吸系统', 'IPS'),
  (181, '一次性使用影像定位材料', 'ILM');

UPDATE `dcc_product_catalog` catalog
JOIN `tmp_dcc_product_catalog_project_match` project_match
  ON project_match.`original_row_no` = catalog.`original_row_no`
SET catalog.`project_name` = project_match.`project_name`,
    catalog.`project_code` = project_match.`project_code`,
    catalog.`updater` = 'dcc-project-code-backfill'
WHERE HEX(catalog.`data_source`) = 'E7919BE6B3B0E4BAA7E59381'
  AND catalog.`deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_product_catalog_project_match`;
