-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_readonly_catalog_menu; type=schema; riskLevel=medium
-- Source: C:\Users\BJB110\Desktop\文档\压力泵工序.xlsx / 二代压力泵

CREATE TABLE IF NOT EXISTS `mes_pro_mes_process_catalog` (
  `id` bigint NOT NULL,
  `source_file_name` varchar(128) NOT NULL COMMENT '来源文件名',
  `source_sheet_name` varchar(64) NOT NULL COMMENT '来源工作表',
  `source_row_no` int NOT NULL COMMENT 'Excel 源行号',
  `sort_no` int NOT NULL COMMENT '列表排序',
  `catalog_code` varchar(64) NOT NULL COMMENT '目录编码',
  `product_name` varchar(128) NOT NULL COMMENT '产品名称',
  `source_machinery_codes` varchar(128) NOT NULL COMMENT '源表设备编码',
  `mes_process_name` varchar(128) NOT NULL COMMENT '工序名称',
  `source_machinery_name` varchar(128) NOT NULL COMMENT '源表设备名称',
  `source_machinery_quantity` varchar(32) NOT NULL COMMENT '源表设备数量',
  `daily_capacity_10_5` varchar(32) NOT NULL COMMENT '10.5小时日产能',
  `daily_worker_quantity` varchar(32) NOT NULL COMMENT '日常工序人力',
  `mes_process_code` varchar(64) NOT NULL COMMENT '工序编码',
  `process_price` varchar(32) NOT NULL COMMENT '工序单价',
  `feedback_flag` varchar(32) NOT NULL COMMENT '工序是否报工',
  `batch_record_flag` varchar(64) NOT NULL COMMENT '工序是否形成批记录',
  `batch_record_process_name` varchar(128) NOT NULL COMMENT '批记录工序名称',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_process_catalog_source_row` (`source_file_name`, `source_sheet_name`, `source_row_no`, `tenant_id`),
  UNIQUE KEY `uk_mes_process_catalog_code` (`catalog_code`, `tenant_id`),
  KEY `idx_mes_process_catalog_sort` (`tenant_id`, `sort_no`, `source_row_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序 Excel 只读目录';

CREATE TABLE IF NOT EXISTS `mes_pro_mes_process_catalog_machinery` (
  `id` bigint NOT NULL,
  `catalog_id` bigint NOT NULL COMMENT 'MES 工序目录 ID',
  `machinery_sort_no` int NOT NULL COMMENT '设备排序',
  `machinery_code` varchar(64) NOT NULL COMMENT '拆分后的设备编码',
  `machinery_name` varchar(128) NOT NULL COMMENT '源表设备名称',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_process_catalog_machinery_code` (`catalog_id`, `machinery_sort_no`, `tenant_id`),
  KEY `idx_mes_process_catalog_machinery_catalog` (`tenant_id`, `catalog_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序 Excel 只读目录设备明细';

DELETE cm
FROM `mes_pro_mes_process_catalog_machinery` cm
JOIN `mes_pro_mes_process_catalog` c ON c.`id` = cm.`catalog_id`
WHERE c.`source_file_name` = '压力泵工序.xlsx'
  AND c.`source_sheet_name` = '二代压力泵'
  AND c.`tenant_id` = 0;

DELETE FROM `mes_pro_mes_process_catalog`
WHERE `source_file_name` = '压力泵工序.xlsx'
  AND `source_sheet_name` = '二代压力泵'
  AND `tenant_id` = 0;

INSERT INTO `mes_pro_mes_process_catalog` (
  `id`, `source_file_name`, `source_sheet_name`, `source_row_no`, `sort_no`, `catalog_code`,
  `product_name`, `source_machinery_codes`, `mes_process_name`, `source_machinery_name`,
  `source_machinery_quantity`, `daily_capacity_10_5`, `daily_worker_quantity`,
  `mes_process_code`, `process_price`, `feedback_flag`, `batch_record_flag`,
  `batch_record_process_name`, `creator`, `updater`, `deleted`, `tenant_id`
) VALUES
-- source_row_no=2
(9003131001, '压力泵工序.xlsx', '二代压力泵', 2, 1, 'PUMP2-MES-0001', '二代压力泵', 'B09393', '粗洗', '超声波清洗机', '1', '/', '/', '/', '/', '/', '是', '粗洗', 'codex', 'codex', b'0', 0),
-- source_row_no=3
(9003131002, '压力泵工序.xlsx', '二代压力泵', 3, 2, 'PUMP2-MES-0002', '二代压力泵', 'B09353', '精洗', '超声波清洗机', '1', '/', '/', '/', '/', '/', '是', '精洗', 'codex', 'codex', b'0', 0),
-- source_row_no=4
(9003131003, '压力泵工序.xlsx', '二代压力泵', 4, 3, 'PUMP2-MES-0003', '二代压力泵', 'B09353', '清洗', '超声波清洗机', '1', '/', '/', '/', '/', '/', '是（两道合并）', '清洗', 'codex', 'codex', b'0', 0),
-- source_row_no=5
(9003131004, '压力泵工序.xlsx', '二代压力泵', 5, 4, 'PUMP2-MES-0004', '二代压力泵', 'B09041', '烘干', '箱型干燥机', '1', '/', '/', '/', '/', '/', '是（两道合并）', '清洗', 'codex', 'codex', b'0', 0),
-- source_row_no=6
(9003131005, '压力泵工序.xlsx', '二代压力泵', 6, 5, 'PUMP2-MES-0005', '二代压力泵', '/', '清洁', '无尘布/75%酒精', '/', '/', '/', '/', '/', '/', '是', '清洁', 'codex', 'codex', b'0', 0),
-- source_row_no=7
(9003131006, '压力泵工序.xlsx', '二代压力泵', 7, 6, 'PUMP2-MES-0006', '二代压力泵', 'B09340', '组装', '杠杆架自动组装机', '1', '5800', '/', '/', '/', '/', '是', '组装Ⅰ', 'codex', 'codex', b'0', 0),
-- source_row_no=8
(9003131007, '压力泵工序.xlsx', '二代压力泵', 8, 7, 'PUMP2-MES-0007', '二代压力泵', 'A03378/A03377', '编织管自动抽芯点胶', '编织管自动抽芯点胶', '1', '7000', '/', '/', '/', '/', '/', '/', 'codex', 'codex', b'0', 0),
-- source_row_no=9
(9003131008, '压力泵工序.xlsx', '二代压力泵', 9, 8, 'PUMP2-MES-0008', '二代压力泵', 'A05059', '点胶二代编织管表', '光固机', '1', '3500', '3', 'Z1500', '0.2224', '是', '是', '光固', 'codex', 'codex', b'0', 0),
-- source_row_no=10
(9003131009, '压力泵工序.xlsx', '二代压力泵', 10, 9, 'PUMP2-MES-0009', '二代压力泵', 'B09026', '硅化', '喷套筒', '1', '9000', '1', 'Z1520', '0.0259', '是', '是', '硅化Ⅱ', 'codex', 'codex', b'0', 0),
-- source_row_no=11
(9003131010, '压力泵工序.xlsx', '二代压力泵', 11, 10, 'PUMP2-MES-0010', '二代压力泵', '/', '硅化胶塞环', '/', '/', '/', '/', '/', '/', '/', '是', '硅化Ⅲ', 'codex', 'codex', b'0', 0),
-- source_row_no=12
(9003131011, '压力泵工序.xlsx', '二代压力泵', 12, 11, 'PUMP2-MES-0011', '二代压力泵', '/', '螺杆硅化', '/', '/', '9000', '1', 'Z1530', '0.0259', '是', '是', '硅化Ⅰ', 'codex', 'codex', b'0', 0),
-- source_row_no=13
(9003131012, '压力泵工序.xlsx', '二代压力泵', 13, 12, 'PUMP2-MES-0012', '二代压力泵', '/', '组装后盖', '/', '/', '4000', '1', 'Z1490', '0.0597', '是', '是', '组装Ⅱ', 'codex', 'codex', b'0', 0),
-- source_row_no=14
(9003131013, '压力泵工序.xlsx', '二代压力泵', 14, 13, 'PUMP2-MES-0013', '二代压力泵', '/', '二代压力泵手柄耐压检测', '/', '/', '4200', '1', 'Z1570', '0.0738', '是', '/', '/', 'codex', 'codex', b'0', 0),
-- source_row_no=15
(9003131014, '压力泵工序.xlsx', '二代压力泵', 15, 14, 'PUMP2-MES-0014', '二代压力泵', 'G01034', '压活塞', '/', '1', '10000', '1', 'Z1510', '0.0254', '是', '/', '/', 'codex', 'codex', b'0', 0),
-- source_row_no=16
(9003131015, '压力泵工序.xlsx', '二代压力泵', 16, 15, 'PUMP2-MES-0015', '二代压力泵', '/', '套外套', '/', '/', '4000', '1', 'Z1650', '0.0701', '是', '/', '/', 'codex', 'codex', b'0', 0),
-- source_row_no=17
(9003131016, '压力泵工序.xlsx', '二代压力泵', 17, 16, 'PUMP2-MES-0016', '二代压力泵', '/', '目测二代异物', '/', '/', '1900', '1', 'Z1550', '0.1911', '是', '/', '/', 'codex', 'codex', b'0', 0),
-- source_row_no=18
(9003131017, '压力泵工序.xlsx', '二代压力泵', 18, 17, 'PUMP2-MES-0017', '二代压力泵', '/', '点胶二代压力泵', '/', '/', '4000', '3', 'Z1560', '0.2272', '是', '是', '组装Ⅲ', 'codex', 'codex', b'0', 0),
-- source_row_no=19
(9003131018, '压力泵工序.xlsx', '二代压力泵', 19, 18, 'PUMP2-MES-0018', '二代压力泵', 'B09032/G01160', '二代压力泵负压检测', '/', '2', '4000', '1', 'Z1610', '0.0834', '是', '是（两道合并）', '检测', 'codex', 'codex', b'0', 0),
-- source_row_no=20
(9003131019, '压力泵工序.xlsx', '二代压力泵', 20, 19, 'PUMP2-MES-0019', '二代压力泵', 'G01143', '测二代压力泵全套', '小气压检测', '1', '2000', '1', 'Z1580', '0.1509', '是', '是（两道合并）', '检测', 'codex', 'codex', b'0', 0),
-- source_row_no=21
(9003131020, '压力泵工序.xlsx', '二代压力泵', 21, 20, 'PUMP2-MES-0020', '二代压力泵', '/', '全检压力泵（内）', '/', '/', '2500', '1', 'Z1590', '0.1406', '是', '/', '/', 'codex', 'codex', b'0', 0),
-- source_row_no=22
(9003131021, '压力泵工序.xlsx', '二代压力泵', 22, 21, 'PUMP2-MES-0021', '二代压力泵', 'A05199/A05203', '压力泵热合(顶头袋封口）', '封口热合机', '2', '7600', '8', 'Z560', '0.3338', '是', '是（两道合并）', '单包装', 'codex', 'codex', b'0', 0),
-- source_row_no=23
(9003131022, '压力泵工序.xlsx', '二代压力泵', 23, 22, 'PUMP2-MES-0022', '二代压力泵', 'A05048/A03274', '压力泵热合（吸塑盒面纸热合）', '封口热合机', '2', '7600', '8', 'Z560', '0.3338', '是', '是（两道合并）', '单包装', 'codex', 'codex', b'0', 0),
-- source_row_no=24
(9003131023, '压力泵工序.xlsx', '二代压力泵', 24, 23, 'PUMP2-MES-0023', '二代压力泵', '/', '全检压力泵', '/', '/', '3800', '1', 'Z5623', '0.0834', '是', '/', '/', 'codex', 'codex', b'0', 0),
-- source_row_no=25
(9003131024, '压力泵工序.xlsx', '二代压力泵', 25, 24, 'PUMP2-MES-0024', '二代压力泵', 'G01235', '贴条形码', '贴标机', '1', '10000', '1', 'Z6133', '0.0235', '是', '/', '/', 'codex', 'codex', b'0', 0),
-- source_row_no=26
(9003131025, '压力泵工序.xlsx', '二代压力泵', 26, 25, 'PUMP2-MES-0025', '压力泵（硬吸塑）', '/', 'W贴产品标签（大标签）', '/', '/', '9581', '', '', '', '', '', '中包装', 'codex', 'codex', b'0', 0),
-- source_row_no=27
(9003131026, '压力泵工序.xlsx', '二代压力泵', 27, 26, 'PUMP2-MES-0026', '压力泵（硬吸塑）', '/', 'W贴产品标签（小标签）', '/', '/', '37405', '', '', '', '', '', '大包装', 'codex', 'codex', b'0', 0),
-- source_row_no=28
(9003131027, '压力泵工序.xlsx', '二代压力泵', 28, 27, 'PUMP2-MES-0027', '压力泵（硬吸塑）', '/', '压力泵中盒（说明书）', '/', '/', '3412', '', '', '', '', '', '', 'codex', 'codex', b'0', 0),
-- source_row_no=29
(9003131028, '压力泵工序.xlsx', '二代压力泵', 29, 28, 'PUMP2-MES-0028', '压力泵（硬吸塑）', 'G01248', 'W包装打包', '包装线', '1', '8180', '', '', '', '', '', '', 'codex', 'codex', b'0', 0),
-- source_row_no=30
(9003131029, '压力泵工序.xlsx', '二代压力泵', 30, 29, 'PUMP2-MES-0029', '压力泵（散装套袋）', '/', '散装压力泵（套袋）', '/', '/', '1838', '', '', '', '', '', '', 'codex', 'codex', b'0', 0),
-- source_row_no=31
(9003131030, '压力泵工序.xlsx', '二代压力泵', 31, 30, 'PUMP2-MES-0030', '压力泵（散装套袋）', '/', 'W包装打包', '/', '/', '20450', '', '', '', '', '', '', 'codex', 'codex', b'0', 0),
-- source_row_no=32
(9003131031, '压力泵工序.xlsx', '二代压力泵', 32, 31, 'PUMP2-MES-0031', '压力泵（散装不套袋）', '/', '散装压力泵', '/', '/', '3937', '', '', '', '', '', '', 'codex', 'codex', b'0', 0),
-- source_row_no=33
(9003131032, '压力泵工序.xlsx', '二代压力泵', 33, 32, 'PUMP2-MES-0032', '压力泵（散装不套袋）', '/', 'W包装打包', '/', '/', '24540', '', '', '', '', '', '', 'codex', 'codex', b'0', 0);

INSERT INTO `mes_pro_mes_process_catalog_machinery` (
  `id`, `catalog_id`, `machinery_sort_no`, `machinery_code`, `machinery_name`,
  `creator`, `updater`, `deleted`, `tenant_id`
) VALUES
(9003132001, 9003131001, 1, 'B09393', '超声波清洗机', 'codex', 'codex', b'0', 0),
(9003132002, 9003131002, 1, 'B09353', '超声波清洗机', 'codex', 'codex', b'0', 0),
(9003132003, 9003131003, 1, 'B09353', '超声波清洗机', 'codex', 'codex', b'0', 0),
(9003132004, 9003131004, 1, 'B09041', '箱型干燥机', 'codex', 'codex', b'0', 0),
(9003132005, 9003131006, 1, 'B09340', '杠杆架自动组装机', 'codex', 'codex', b'0', 0),
(9003132006, 9003131007, 1, 'A03378', '编织管自动抽芯点胶', 'codex', 'codex', b'0', 0),
(9003132007, 9003131007, 2, 'A03377', '编织管自动抽芯点胶', 'codex', 'codex', b'0', 0),
(9003132008, 9003131008, 1, 'A05059', '光固机', 'codex', 'codex', b'0', 0),
(9003132009, 9003131009, 1, 'B09026', '喷套筒', 'codex', 'codex', b'0', 0),
(9003132010, 9003131014, 1, 'G01034', '/', 'codex', 'codex', b'0', 0),
(9003132011, 9003131018, 1, 'B09032', '/', 'codex', 'codex', b'0', 0),
(9003132012, 9003131018, 2, 'G01160', '/', 'codex', 'codex', b'0', 0),
(9003132013, 9003131019, 1, 'G01143', '小气压检测', 'codex', 'codex', b'0', 0),
(9003132014, 9003131021, 1, 'A05199', '封口热合机', 'codex', 'codex', b'0', 0),
(9003132015, 9003131021, 2, 'A05203', '封口热合机', 'codex', 'codex', b'0', 0),
(9003132016, 9003131022, 1, 'A05048', '封口热合机', 'codex', 'codex', b'0', 0),
(9003132017, 9003131022, 2, 'A03274', '封口热合机', 'codex', 'codex', b'0', 0),
(9003132018, 9003131024, 1, 'G01235', '贴标机', 'codex', 'codex', b'0', 0),
(9003132019, 9003131028, 1, 'G01248', '包装线', 'codex', 'codex', b'0', 0);
