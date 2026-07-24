-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_mes_dv_machinery_extend; type=schema; riskLevel=medium
-- 球囊扩张导管工序设置与设备产能联动：按桌面 Excel Sheet1 导入 tenant_id=1 数据。

SET @target_tenant_id = 1;
SET @process_seed_count = 49;
SET @machinery_seed_count = 31;
SET @machinery_process_seed_count = 83;
SET @balloon_route_process_count = 23;
SET @scoring_balloon_route_process_count = 26;
-- tenant_id = @target_tenant_id

DROP PROCEDURE IF EXISTS intruoyi_add_balloon_process_device_capacity_columns;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_balloon_process_device_capacity_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process'
           AND column_name = 'product_name'
    ) THEN
        ALTER TABLE `mes_pro_process`
            ADD COLUMN `product_name` varchar(128) DEFAULT NULL COMMENT '产品名称' AFTER `id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process'
           AND column_name = 'manual_shift_capacity'
    ) THEN
        ALTER TABLE `mes_pro_process`
            ADD COLUMN `manual_shift_capacity` decimal(18,6) DEFAULT NULL COMMENT '人工班次产能' AFTER `status`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_dv_machinery_process'
           AND column_name = 'process_code'
    ) THEN
        ALTER TABLE `mes_dv_machinery_process`
            ADD COLUMN `process_code` varchar(64) DEFAULT NULL COMMENT '工序编码' AFTER `process_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_dv_machinery_process'
           AND index_name = 'idx_mes_dv_machinery_process_process_id'
    ) THEN
        ALTER TABLE `mes_dv_machinery_process`
            ADD INDEX `idx_mes_dv_machinery_process_process_id` (`process_id`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process'
           AND index_name = 'idx_mes_pro_process_product_code'
    ) THEN
        ALTER TABLE `mes_pro_process`
            ADD INDEX `idx_mes_pro_process_product_code` (`tenant_id`, `product_name`, `code`, `deleted`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process'
           AND index_name = 'idx_mes_pro_process_product_name'
    ) THEN
        ALTER TABLE `mes_pro_process`
            ADD INDEX `idx_mes_pro_process_product_name` (`tenant_id`, `product_name`, `name`, `deleted`);
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_balloon_process_device_capacity_columns();
DROP PROCEDURE IF EXISTS intruoyi_add_balloon_process_device_capacity_columns;

DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_process_seed`;
CREATE TEMPORARY TABLE `tmp_balloon_process_seed` (
    `product_name` varchar(128) NOT NULL,
    `process_code` varchar(64) NOT NULL,
    `process_name` varchar(128) NOT NULL,
    `manual_shift_capacity` decimal(18,6) DEFAULT NULL,
    `remark` varchar(512) DEFAULT NULL,
    PRIMARY KEY (`product_name`, `process_code`, `process_name`)
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tmp_balloon_process_seed`
    (`product_name`, `process_code`, `process_name`, `manual_shift_capacity`, `remark`)
VALUES
('球囊扩张导管', 'Z2630', '吹球囊成型', NULL, NULL),
('球囊扩张导管', 'Z3710', '球囊裁剪', NULL, NULL),
('球囊扩张导管', 'Z2775', '外管拉伸2', NULL, NULL),
('球囊扩张导管', 'Z2772', '内管拉伸2', NULL, NULL),
('球囊扩张导管', 'Z2510', '外管与球囊焊接', NULL, NULL),
('球囊扩张导管', 'Z3810', '外管切缝', NULL, NULL),
('球囊扩张导管', 'Z3720', '裁剪管材', NULL, NULL),
('球囊扩张导管', 'Z5200', '穿显影环', 740.000000, NULL),
('球囊扩张导管', 'Z2520', '尖端管与内管焊接', NULL, NULL),
('球囊扩张导管', 'Z2530', '压显影环', NULL, NULL),
('球囊扩张导管', 'Z2550', '焊接远端锥度', NULL, NULL),
('球囊扩张导管', 'Z3850', '裁剪圆角', NULL, NULL),
('球囊扩张导管', 'Z2560', '焊接圆角', NULL, NULL),
('球囊扩张导管', 'Z2570', '快速交换口焊接', NULL, NULL),
('球囊扩张导管', 'Z2600', 'RX口检测', NULL, NULL),
('球囊扩张导管', 'Z2580', '点胶海波管', NULL, NULL),
('球囊扩张导管', 'Z2480', '球囊涂层', NULL, NULL),
('球囊扩张导管', 'Z2590', '球囊组件与海波管焊接', NULL, NULL),
('球囊扩张导管', 'Z2490', '球囊压握', NULL, NULL),
('球囊扩张导管', 'Z5600', '球囊盘管（机器）', NULL, NULL),
('球囊扩张导管', 'Z2620', '球囊测漏及全检', NULL, NULL),
('球囊扩张导管', 'Z760', '包套装管', 5440.000000, NULL),
('球囊扩张导管', 'Z830', '纸塑袋封口（包装）', NULL, NULL),
('棘突球囊扩张导管', 'Z2630', '吹球囊成型', NULL, NULL),
('棘突球囊扩张导管', 'Z3710', '球囊裁剪', NULL, NULL),
('棘突球囊扩张导管', 'Z2775', '外管拉伸2', NULL, NULL),
('棘突球囊扩张导管', 'Z2772', '内管拉伸2', NULL, NULL),
('棘突球囊扩张导管', 'Z2510', '外管与球囊焊接', NULL, NULL),
('棘突球囊扩张导管', 'Z3810', '外管切缝', NULL, NULL),
('棘突球囊扩张导管', 'Z5200', '穿显影环', 740.000000, NULL),
('棘突球囊扩张导管', 'Z2530', '压显影环', NULL, NULL),
('棘突球囊扩张导管', 'Z2971', '棘突丝拉伸2', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2976', '棘突丝切割', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2972', '棘突远端焊接', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2973', '棘突远端塑型', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2974', '棘突近端焊接', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2975', '棘突TPU 套管粘接', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2550', '焊接远端锥度', NULL, NULL),
('棘突球囊扩张导管', 'Z2570', '快速交换口焊接', NULL, NULL),
('棘突球囊扩张导管', 'Z2600', 'RX口检测', NULL, NULL),
('棘突球囊扩张导管', 'Z2580', '点胶海波管', NULL, NULL),
('棘突球囊扩张导管', 'Z2490', '球囊压握', NULL, NULL),
('棘突球囊扩张导管', 'Z2774', '棘突远端锥度焊接', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2773', '棘突近端粘接', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z3850', '裁剪圆角', NULL, NULL),
('棘突球囊扩张导管', 'Z2560', '焊接圆角', NULL, NULL),
('棘突球囊扩张导管', 'Z2776', '棘突球囊涂层', NULL, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z5600', '球囊盘管（机器）', NULL, NULL),
('棘突球囊扩张导管', 'Z2620', '球囊测漏及全检', NULL, NULL);

DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_route_seed`;
CREATE TEMPORARY TABLE `tmp_balloon_route_seed` (
    `product_name` varchar(128) NOT NULL,
    `route_code` varchar(64) NOT NULL,
    `route_name` varchar(128) NOT NULL,
    `expected_process_count` int NOT NULL,
    PRIMARY KEY (`route_code`),
    UNIQUE KEY `uk_tmp_balloon_route_product` (`product_name`)
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tmp_balloon_route_seed`
    (`product_name`, `route_code`, `route_name`, `expected_process_count`)
VALUES
('球囊扩张导管', 'ROUTE-BALLOON-CATHETER', '球囊扩张导管工艺路线', @balloon_route_process_count),
('棘突球囊扩张导管', 'ROUTE-SCORING-BALLOON-CATHETER', '棘突球囊扩张导管工艺路线', @scoring_balloon_route_process_count);

DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_route_process_next_seed`;
DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_route_process_seed`;
CREATE TEMPORARY TABLE `tmp_balloon_route_process_seed` (
    `product_name` varchar(128) NOT NULL,
    `route_code` varchar(64) NOT NULL,
    `process_code` varchar(64) NOT NULL,
    `process_name` varchar(128) NOT NULL,
    `sort` int NOT NULL,
    PRIMARY KEY (`product_name`, `process_code`, `process_name`),
    UNIQUE KEY `uk_tmp_balloon_route_process_sort` (`route_code`, `sort`)
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @balloon_route_sort = 0;
INSERT INTO `tmp_balloon_route_process_seed`
    (`product_name`, `route_code`, `process_code`, `process_name`, `sort`)
SELECT seed.`product_name`,
       route_seed.`route_code`,
       seed.`process_code`,
       seed.`process_name`,
       @balloon_route_sort := @balloon_route_sort + 1 AS `sort`
  FROM `tmp_balloon_process_seed` seed
 INNER JOIN `tmp_balloon_route_seed` route_seed
    ON route_seed.`product_name` = seed.`product_name`
 WHERE seed.`product_name` = '球囊扩张导管'
 ORDER BY FIELD(seed.`process_code`,
       'Z2630', 'Z3710', 'Z2775', 'Z2772', 'Z2510', 'Z3810', 'Z3720', 'Z5200',
       'Z2520', 'Z2530', 'Z2550', 'Z3850', 'Z2560', 'Z2570', 'Z2600', 'Z2580',
       'Z2480', 'Z2590', 'Z2490', 'Z5600', 'Z2620', 'Z760', 'Z830');

SET @scoring_balloon_route_sort = 0;
INSERT INTO `tmp_balloon_route_process_seed`
    (`product_name`, `route_code`, `process_code`, `process_name`, `sort`)
SELECT seed.`product_name`,
       route_seed.`route_code`,
       seed.`process_code`,
       seed.`process_name`,
       @scoring_balloon_route_sort := @scoring_balloon_route_sort + 1 AS `sort`
  FROM `tmp_balloon_process_seed` seed
 INNER JOIN `tmp_balloon_route_seed` route_seed
    ON route_seed.`product_name` = seed.`product_name`
 WHERE seed.`product_name` = '棘突球囊扩张导管'
 ORDER BY FIELD(seed.`process_code`,
       'Z2630', 'Z3710', 'Z2775', 'Z2772', 'Z2510', 'Z3810', 'Z5200', 'Z2530',
       'Z2971', 'Z2976', 'Z2972', 'Z2973', 'Z2974', 'Z2975', 'Z2550', 'Z2570',
       'Z2600', 'Z2580', 'Z2490', 'Z2774', 'Z2773', 'Z3850', 'Z2560', 'Z2776',
       'Z5600', 'Z2620');

DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_route_process_next_seed`;
CREATE TEMPORARY TABLE `tmp_balloon_route_process_next_seed` LIKE `tmp_balloon_route_process_seed`;
INSERT INTO `tmp_balloon_route_process_next_seed`
SELECT * FROM `tmp_balloon_route_process_seed`;

DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_machinery_seed`;
CREATE TEMPORARY TABLE `tmp_balloon_machinery_seed` (
    `machinery_code` varchar(64) NOT NULL,
    `machinery_name` varchar(255) NOT NULL,
    PRIMARY KEY (`machinery_code`)
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tmp_balloon_machinery_seed`
    (`machinery_code`, `machinery_name`)
VALUES
('A03190', '球囊成型机'),
('A03383', '球囊成型机'),
('A03389', '球囊成型机'),
('A03197', '球囊成型机'),
('B09262', '球囊切管工装'),
('B09222', '球囊切管工装'),
('A03388', '球囊导管拉伸机（三工位）'),
('A03196', '激光焊接机'),
('A03221', '激光焊接机'),
('A03232', '激光焊接机'),
('A03233', '激光焊接机'),
('B09212', '导管切缝工装'),
('B09289', '显影环预压工装+显影环锻打机'),
('A03204', '激光焊接机（RX口焊接专用改造版）'),
('B12046', '球囊扩压工装'),
('B12045', '球囊扩压工装'),
('B09323', '点光源'),
('A03247', '球囊导管浸涂机'),
('B12049', '球囊焊接工装'),
('B09053', '球囊折叠机'),
('B09561', '球囊折叠机'),
('B09326', '导丝盘管设备'),
('C01185', '高精度智能气密测试仪+电子放大镜'),
('G01461', '封口热合机'),
('A03214', '球囊成型机'),
('B25001', '棘突丝切割机'),
('B25002', '棘突丝切割机'),
('B09582', '热风焊接机'),
('B09384', '点光源'),
('A05216', '热熔焊接机'),
('B09133', '点光源');

DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_machinery_process_seed`;
CREATE TEMPORARY TABLE `tmp_balloon_machinery_process_seed` (
    `product_name` varchar(128) NOT NULL,
    `process_code` varchar(64) NOT NULL,
    `process_name` varchar(128) NOT NULL,
    `machinery_code` varchar(64) NOT NULL,
    `device_name` varchar(255) NOT NULL,
    `device_quantity` decimal(18,6) NOT NULL,
    `ten_half_hour_daily_capacity` decimal(18,6) NOT NULL,
    `source_row_no` int NOT NULL,
    `remark` varchar(512) DEFAULT NULL,
    PRIMARY KEY (`product_name`, `process_code`, `process_name`, `machinery_code`, `source_row_no`)
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tmp_balloon_machinery_process_seed`
    (`product_name`, `process_code`, `process_name`, `machinery_code`, `device_name`, `device_quantity`, `ten_half_hour_daily_capacity`, `source_row_no`, `remark`)
VALUES
('球囊扩张导管', 'Z2630', '吹球囊成型', 'A03190', '球囊成型机', 1.000000, 100.000000, 2, NULL),
('球囊扩张导管', 'Z2630', '吹球囊成型', 'A03383', '球囊成型机', 1.000000, 100.000000, 3, NULL),
('球囊扩张导管', 'Z2630', '吹球囊成型', 'A03389', '球囊成型机', 1.000000, 100.000000, 4, NULL),
('球囊扩张导管', 'Z2630', '吹球囊成型', 'A03197', '球囊成型机', 1.000000, 100.000000, 5, NULL),
('球囊扩张导管', 'Z3710', '球囊裁剪', 'B09262', '球囊切管工装', 1.000000, 1550.000000, 6, NULL),
('球囊扩张导管', 'Z3710', '球囊裁剪', 'B09222', '球囊切管工装', 1.000000, 1550.000000, 7, NULL),
('球囊扩张导管', 'Z2775', '外管拉伸2', 'A03388', '球囊导管拉伸机（三工位）', 1.000000, 650.000000, 8, NULL),
('球囊扩张导管', 'Z2772', '内管拉伸2', 'A03388', '球囊导管拉伸机（三工位）', 1.000000, 840.000000, 9, NULL),
('球囊扩张导管', 'Z2510', '外管与球囊焊接', 'A03196', '激光焊接机', 1.000000, 585.000000, 10, NULL),
('球囊扩张导管', 'Z2510', '外管与球囊焊接', 'A03221', '激光焊接机', 1.000000, 585.000000, 11, NULL),
('球囊扩张导管', 'Z2510', '外管与球囊焊接', 'A03232', '激光焊接机', 1.000000, 585.000000, 12, NULL),
('球囊扩张导管', 'Z2510', '外管与球囊焊接', 'A03233', '激光焊接机', 1.000000, 585.000000, 13, NULL),
('球囊扩张导管', 'Z3810', '外管切缝', 'B09212', '导管切缝工装', 1.000000, 1950.000000, 14, NULL),
('球囊扩张导管', 'Z3720', '裁剪管材', 'B09262', '球囊切管工装', 1.000000, 5750.000000, 15, NULL),
('球囊扩张导管', 'Z2520', '尖端管与内管焊接', 'A03196', '激光焊接机', 1.000000, 580.000000, 17, NULL),
('球囊扩张导管', 'Z2520', '尖端管与内管焊接', 'A03221', '激光焊接机', 1.000000, 580.000000, 18, NULL),
('球囊扩张导管', 'Z2520', '尖端管与内管焊接', 'A03232', '激光焊接机', 1.000000, 580.000000, 19, NULL),
('球囊扩张导管', 'Z2520', '尖端管与内管焊接', 'A03233', '激光焊接机', 1.000000, 580.000000, 20, NULL),
('球囊扩张导管', 'Z2530', '压显影环', 'B09289', '显影环预压工装+显影环锻打机', 1.000000, 600.000000, 21, NULL),
('球囊扩张导管', 'Z2550', '焊接远端锥度', 'A03196', '激光焊接机', 1.000000, 390.000000, 22, NULL),
('球囊扩张导管', 'Z2550', '焊接远端锥度', 'A03221', '激光焊接机', 1.000000, 390.000000, 23, NULL),
('球囊扩张导管', 'Z2550', '焊接远端锥度', 'A03232', '激光焊接机', 1.000000, 390.000000, 24, NULL),
('球囊扩张导管', 'Z2550', '焊接远端锥度', 'A03233', '激光焊接机', 1.000000, 390.000000, 25, NULL),
('球囊扩张导管', 'Z3850', '裁剪圆角', 'B09262', '球囊切管工装', 1.000000, 2950.000000, 26, NULL),
('球囊扩张导管', 'Z2560', '焊接圆角', 'A03196', '激光焊接机', 1.000000, 300.000000, 27, NULL),
('球囊扩张导管', 'Z2560', '焊接圆角', 'A03221', '激光焊接机', 1.000000, 300.000000, 28, NULL),
('球囊扩张导管', 'Z2560', '焊接圆角', 'A03232', '激光焊接机', 1.000000, 300.000000, 29, NULL),
('球囊扩张导管', 'Z2560', '焊接圆角', 'A03233', '激光焊接机', 1.000000, 300.000000, 30, NULL),
('球囊扩张导管', 'Z2570', '快速交换口焊接', 'A03204', '激光焊接机（RX口焊接专用改造版）', 1.000000, 310.000000, 31, NULL),
('球囊扩张导管', 'Z2600', 'RX口检测', 'B12046', '球囊扩压工装', 1.000000, 480.000000, 32, NULL),
('球囊扩张导管', 'Z2600', 'RX口检测', 'B12045', '球囊扩压工装', 1.000000, 480.000000, 33, NULL),
('球囊扩张导管', 'Z2580', '点胶海波管', 'B09323', '点光源', 1.000000, 585.000000, 34, NULL),
('球囊扩张导管', 'Z2480', '球囊涂层', 'A03247', '球囊导管浸涂机', 1.000000, 480.000000, 35, NULL),
('球囊扩张导管', 'Z2590', '球囊组件与海波管焊接', 'B12049', '球囊焊接工装', 1.000000, 555.000000, 36, NULL),
('球囊扩张导管', 'Z2490', '球囊压握', 'B09053', '球囊折叠机', 1.000000, 300.000000, 37, NULL),
('球囊扩张导管', 'Z2490', '球囊压握', 'B09561', '球囊折叠机', 1.000000, 200.000000, 38, NULL),
('球囊扩张导管', 'Z5600', '球囊盘管（机器）', 'B09326', '导丝盘管设备', 1.000000, 1650.000000, 39, NULL),
('球囊扩张导管', 'Z2620', '球囊测漏及全检', 'C01185', '高精度智能气密测试仪+电子放大镜', 1.000000, 420.000000, 40, NULL),
('球囊扩张导管', 'Z830', '纸塑袋封口（包装）', 'G01461', '封口热合机', 1.000000, 16000.000000, 42, NULL),
('棘突球囊扩张导管', 'Z2630', '吹球囊成型', 'A03190', '球囊成型机', 1.000000, 100.000000, 43, NULL),
('棘突球囊扩张导管', 'Z2630', '吹球囊成型', 'A03383', '球囊成型机', 1.000000, 100.000000, 44, NULL),
('棘突球囊扩张导管', 'Z2630', '吹球囊成型', 'A03389', '球囊成型机', 1.000000, 100.000000, 45, NULL),
('棘突球囊扩张导管', 'Z2630', '吹球囊成型', 'A03197', '球囊成型机', 1.000000, 100.000000, 46, NULL),
('棘突球囊扩张导管', 'Z2630', '吹球囊成型', 'A03214', '球囊成型机', 1.000000, 100.000000, 47, NULL),
('棘突球囊扩张导管', 'Z3710', '球囊裁剪', 'B09222', '球囊切管工装', 1.000000, 1550.000000, 48, NULL),
('棘突球囊扩张导管', 'Z2775', '外管拉伸2', 'A03388', '球囊导管拉伸机（三工位）', 1.000000, 270.000000, 49, NULL),
('棘突球囊扩张导管', 'Z2772', '内管拉伸2', 'A03388', '球囊导管拉伸机（三工位）', 1.000000, 420.000000, 50, NULL),
('棘突球囊扩张导管', 'Z2510', '外管与球囊焊接', 'A03196', '激光焊接机', 1.000000, 585.000000, 51, NULL),
('棘突球囊扩张导管', 'Z2510', '外管与球囊焊接', 'A03221', '激光焊接机', 1.000000, 585.000000, 52, NULL),
('棘突球囊扩张导管', 'Z2510', '外管与球囊焊接', 'A03232', '激光焊接机', 1.000000, 585.000000, 53, NULL),
('棘突球囊扩张导管', 'Z2510', '外管与球囊焊接', 'A03233', '激光焊接机', 1.000000, 585.000000, 54, NULL),
('棘突球囊扩张导管', 'Z3810', '外管切缝', 'B09212', '导管切缝工装', 1.000000, 1950.000000, 55, NULL),
('棘突球囊扩张导管', 'Z2530', '压显影环', 'B09289', '显影环预压工装+显影环锻打机', 1.000000, 600.000000, 57, NULL),
('棘突球囊扩张导管', 'Z2971', '棘突丝拉伸2', 'A03388', '球囊导管拉伸机（三工位）', 1.000000, 255.000000, 58, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2976', '棘突丝切割', 'B25001', '棘突丝切割机', 1.000000, 480.000000, 59, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2976', '棘突丝切割', 'B25002', '棘突丝切割机', 1.000000, 480.000000, 60, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2972', '棘突远端焊接', 'A03196', '激光焊接机', 1.000000, 130.000000, 61, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2972', '棘突远端焊接', 'A03221', '激光焊接机', 1.000000, 130.000000, 62, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2972', '棘突远端焊接', 'A03232', '激光焊接机', 1.000000, 130.000000, 63, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2972', '棘突远端焊接', 'A03233', '激光焊接机', 1.000000, 130.000000, 64, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2973', '棘突远端塑型', 'B09582', '热风焊接机', 1.000000, 200.000000, 65, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2974', '棘突近端焊接', 'B09582', '热风焊接机', 1.000000, 110.000000, 66, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2975', '棘突TPU 套管粘接', 'B09384', '点光源', 1.000000, 120.000000, 67, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2550', '焊接远端锥度', 'A03196', '激光焊接机', 1.000000, 390.000000, 68, NULL),
('棘突球囊扩张导管', 'Z2550', '焊接远端锥度', 'A03221', '激光焊接机', 1.000000, 390.000000, 69, NULL),
('棘突球囊扩张导管', 'Z2550', '焊接远端锥度', 'A03232', '激光焊接机', 1.000000, 390.000000, 70, NULL),
('棘突球囊扩张导管', 'Z2550', '焊接远端锥度', 'A03233', '激光焊接机', 1.000000, 390.000000, 71, NULL),
('棘突球囊扩张导管', 'Z2570', '快速交换口焊接', 'A03204', '激光焊接机（RX口焊接专用改造版）', 1.000000, 310.000000, 72, NULL),
('棘突球囊扩张导管', 'Z2600', 'RX口检测', 'B12046', '球囊扩压工装', 1.000000, 480.000000, 73, NULL),
('棘突球囊扩张导管', 'Z2600', 'RX口检测', 'B12045', '球囊扩压工装', 1.000000, 480.000000, 74, NULL),
('棘突球囊扩张导管', 'Z2580', '点胶海波管', 'B09323', '点光源', 1.000000, 585.000000, 75, NULL),
('棘突球囊扩张导管', 'Z2490', '球囊压握', 'B09053', '球囊折叠机', 1.000000, 300.000000, 76, NULL),
('棘突球囊扩张导管', 'Z2490', '球囊压握', 'B09561', '球囊折叠机', 1.000000, 200.000000, 77, NULL),
('棘突球囊扩张导管', 'Z2774', '棘突远端锥度焊接', 'A05216', '热熔焊接机', 1.000000, 200.000000, 78, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z2773', '棘突近端粘接', 'B09133', '点光源', 1.000000, 311.000000, 79, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z3850', '裁剪圆角', 'B09262', '球囊切管工装', 1.000000, 2950.000000, 80, NULL),
('棘突球囊扩张导管', 'Z2560', '焊接圆角', 'A03196', '激光焊接机', 1.000000, 300.000000, 81, NULL),
('棘突球囊扩张导管', 'Z2560', '焊接圆角', 'A03221', '激光焊接机', 1.000000, 300.000000, 82, NULL),
('棘突球囊扩张导管', 'Z2560', '焊接圆角', 'A03232', '激光焊接机', 1.000000, 300.000000, 83, NULL),
('棘突球囊扩张导管', 'Z2560', '焊接圆角', 'A03233', '激光焊接机', 1.000000, 300.000000, 84, NULL),
('棘突球囊扩张导管', 'Z2776', '棘突球囊涂层', 'A03247', '球囊导管浸涂机', 1.000000, 480.000000, 85, '棘突球囊专有'),
('棘突球囊扩张导管', 'Z5600', '球囊盘管（机器）', 'B09326', '导丝盘管设备', 1.000000, 1650.000000, 86, NULL),
('棘突球囊扩张导管', 'Z2620', '球囊测漏及全检', 'C01185', '高精度智能气密测试仪+电子放大镜', 1.000000, 420.000000, 87, NULL);

DROP PROCEDURE IF EXISTS intruoyi_seed_balloon_process_device_capacity;
DELIMITER $$
CREATE PROCEDURE intruoyi_seed_balloon_process_device_capacity()
BEGIN
    DECLARE v_default_type_id bigint DEFAULT NULL;
    DECLARE v_default_workshop_id bigint DEFAULT NULL;
    DECLARE v_process_count int DEFAULT 0;
    DECLARE v_machinery_count int DEFAULT 0;
    DECLARE v_machinery_process_count int DEFAULT 0;

    SELECT COUNT(*) INTO v_process_count FROM `tmp_balloon_process_seed`;
    IF v_process_count <> @process_seed_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon process seed count mismatch';
    END IF;

    SELECT COUNT(*) INTO v_machinery_count FROM `tmp_balloon_machinery_seed`;
    IF v_machinery_count <> @machinery_seed_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon machinery seed count mismatch';
    END IF;

    SELECT COUNT(*) INTO v_machinery_process_count FROM `tmp_balloon_machinery_process_seed`;
    IF v_machinery_process_count <> @machinery_process_seed_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon machinery process seed count mismatch';
    END IF;

    SELECT `id` INTO v_default_type_id
      FROM `mes_dv_machinery_type`
     WHERE `tenant_id` = @target_tenant_id
       AND `deleted` = b'0'
     ORDER BY `id`
     LIMIT 1;

    IF v_default_type_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing default machinery type for tenant 1';
    END IF;

    SELECT `id` INTO v_default_workshop_id
      FROM `mes_md_workshop`
     WHERE `tenant_id` = @target_tenant_id
       AND `deleted` = b'0'
     ORDER BY `id`
     LIMIT 1;

    IF v_default_workshop_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing default workshop for tenant 1';
    END IF;

    INSERT INTO `mes_pro_process`
        (`product_name`, `code`, `name`, `attention`, `status`, `manual_shift_capacity`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
    SELECT
        seed.`product_name`,
        seed.`process_code`,
        seed.`process_name`,
        NULL,
        0,
        seed.`manual_shift_capacity`,
        NULLIF(seed.`remark`, ''),
        'balloon-process-device-capacity',
        NOW(),
        'balloon-process-device-capacity',
        NOW(),
        b'0',
        @target_tenant_id
    FROM `tmp_balloon_process_seed` seed
    LEFT JOIN `mes_pro_process` existing
        ON existing.`tenant_id` = @target_tenant_id
       AND existing.`deleted` = b'0'
       AND existing.`product_name` = seed.`product_name`
       AND existing.`code` = seed.`process_code`
       AND existing.`name` = seed.`process_name`
    WHERE existing.`id` IS NULL;

    UPDATE `mes_pro_process` process
    INNER JOIN `tmp_balloon_process_seed` seed
        ON seed.`product_name` = process.`product_name`
       AND seed.`process_code` = process.`code`
       AND seed.`process_name` = process.`name`
    SET process.`manual_shift_capacity` = seed.`manual_shift_capacity`,
        process.`status` = 0,
        process.`remark` = NULLIF(seed.`remark`, ''),
        process.`updater` = 'balloon-process-device-capacity',
        process.`update_time` = NOW()
    WHERE process.`tenant_id` = @target_tenant_id
      AND process.`deleted` = b'0';

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_route_seed` route_seed
         WHERE (
             SELECT COUNT(*)
               FROM `tmp_balloon_route_process_seed` route_process_seed
              WHERE route_process_seed.`route_code` = route_seed.`route_code`
         ) <> route_seed.`expected_process_count`
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon route process seed count mismatch';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_route_process_seed` seed
     LEFT JOIN `mes_pro_process` process
            ON process.`tenant_id` = @target_tenant_id
           AND process.`deleted` = b'0'
           AND process.`product_name` = seed.`product_name`
           AND process.`code` = seed.`process_code`
           AND process.`name` = seed.`process_name`
         WHERE process.`id` IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing balloon route process master data';
    END IF;

    IF EXISTS (
        SELECT route_seed.`route_code`
          FROM `tmp_balloon_route_seed` route_seed
          JOIN `mes_pro_route` route
            ON route.`tenant_id` = @target_tenant_id
           AND route.`deleted` = b'0'
           AND route.`code` = route_seed.`route_code`
         GROUP BY route_seed.`route_code`
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon route code conflict';
    END IF;

    IF EXISTS (
        SELECT route_seed.`route_name`
          FROM `tmp_balloon_route_seed` route_seed
          JOIN `mes_pro_route` route
            ON route.`tenant_id` = @target_tenant_id
           AND route.`deleted` = b'0'
           AND route.`name` = route_seed.`route_name`
         GROUP BY route_seed.`route_name`
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon route name conflict';
    END IF;

    INSERT INTO `mes_pro_route`
        (`code`, `name`, `description`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
    SELECT
        route_seed.`route_code`,
        route_seed.`route_name`,
        CONCAT(route_seed.`product_name`, ' Excel Sheet1 工艺路线'),
        0,
        '球囊工序路线导入',
        'balloon-route-process-import',
        NOW(),
        'balloon-route-process-import',
        NOW(),
        b'0',
        @target_tenant_id
      FROM `tmp_balloon_route_seed` route_seed
 LEFT JOIN `mes_pro_route` existing
        ON existing.`tenant_id` = @target_tenant_id
       AND existing.`deleted` = b'0'
       AND existing.`code` = route_seed.`route_code`
     WHERE existing.`id` IS NULL;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_route_seed` route_seed
     LEFT JOIN `mes_pro_route` route
            ON route.`tenant_id` = @target_tenant_id
           AND route.`deleted` = b'0'
           AND route.`code` = route_seed.`route_code`
           AND route.`name` = route_seed.`route_name`
         WHERE route.`id` IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon route code/name mismatch';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_route_process_seed` seed
          JOIN `tmp_balloon_route_seed` route_seed
            ON route_seed.`route_code` = seed.`route_code`
          JOIN `mes_pro_route` route
            ON route.`tenant_id` = @target_tenant_id
           AND route.`deleted` = b'0'
           AND route.`code` = seed.`route_code`
          JOIN `mes_pro_route_process` route_process
            ON route_process.`tenant_id` = @target_tenant_id
           AND route_process.`deleted` = b'0'
           AND route_process.`route_id` = route.`id`
           AND route_process.`sort` = seed.`sort`
          JOIN `mes_pro_process` process
            ON process.`tenant_id` = @target_tenant_id
           AND process.`deleted` = b'0'
           AND process.`product_name` = seed.`product_name`
           AND process.`code` = seed.`process_code`
           AND process.`name` = seed.`process_name`
         WHERE route_process.`process_id` <> process.`id`
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon route process sort conflict';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `tmp_balloon_route_process_seed` seed
          JOIN `mes_pro_route` route
            ON route.`tenant_id` = @target_tenant_id
           AND route.`deleted` = b'0'
           AND route.`code` = seed.`route_code`
          JOIN `mes_pro_process` process
            ON process.`tenant_id` = @target_tenant_id
           AND process.`deleted` = b'0'
           AND process.`product_name` = seed.`product_name`
           AND process.`code` = seed.`process_code`
           AND process.`name` = seed.`process_name`
          JOIN `mes_pro_route_process` route_process
            ON route_process.`tenant_id` = @target_tenant_id
           AND route_process.`deleted` = b'0'
           AND route_process.`route_id` = route.`id`
           AND route_process.`process_id` = process.`id`
         WHERE route_process.`sort` <> seed.`sort`
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Balloon route process id conflict';
    END IF;

    INSERT INTO `mes_pro_route_process`
        (`route_id`, `process_id`, `sort`, `next_process_id`, `link_type`, `prepare_time`, `wait_time`, `color_code`, `key_flag`, `check_flag`, `batch_record_report_id`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
    SELECT
        route.`id`,
        process.`id`,
        seed.`sort`,
        next_process.`id`,
        0,
        NULL,
        NULL,
        NULL,
        b'0',
        b'0',
        NULL,
        '球囊工序路线导入',
        'balloon-route-process-import',
        NOW(),
        'balloon-route-process-import',
        NOW(),
        b'0',
        @target_tenant_id
      FROM `tmp_balloon_route_process_seed` seed
      JOIN `mes_pro_route` route
        ON route.`tenant_id` = @target_tenant_id
       AND route.`deleted` = b'0'
       AND route.`code` = seed.`route_code`
      JOIN `mes_pro_process` process
        ON process.`tenant_id` = @target_tenant_id
       AND process.`deleted` = b'0'
       AND process.`product_name` = seed.`product_name`
       AND process.`code` = seed.`process_code`
       AND process.`name` = seed.`process_name`
 LEFT JOIN `tmp_balloon_route_process_next_seed` next_seed
        ON next_seed.`route_code` = seed.`route_code`
       AND next_seed.`sort` = seed.`sort` + 1
 LEFT JOIN `mes_pro_process` next_process
        ON next_process.`tenant_id` = @target_tenant_id
       AND next_process.`deleted` = b'0'
       AND next_process.`product_name` = next_seed.`product_name`
       AND next_process.`code` = next_seed.`process_code`
       AND next_process.`name` = next_seed.`process_name`
 LEFT JOIN `mes_pro_route_process` existing
        ON existing.`tenant_id` = @target_tenant_id
       AND existing.`deleted` = b'0'
       AND existing.`route_id` = route.`id`
       AND existing.`process_id` = process.`id`
     WHERE existing.`id` IS NULL;

    UPDATE `mes_pro_route_process` route_process
      JOIN `mes_pro_route` route
        ON route.`id` = route_process.`route_id`
       AND route.`tenant_id` = @target_tenant_id
       AND route.`deleted` = b'0'
      JOIN `tmp_balloon_route_process_seed` seed
        ON seed.`route_code` = route.`code`
       AND seed.`sort` = route_process.`sort`
 LEFT JOIN `tmp_balloon_route_process_next_seed` next_seed
        ON next_seed.`route_code` = seed.`route_code`
       AND next_seed.`sort` = seed.`sort` + 1
 LEFT JOIN `mes_pro_process` next_process
        ON next_process.`tenant_id` = @target_tenant_id
       AND next_process.`deleted` = b'0'
       AND next_process.`product_name` = next_seed.`product_name`
       AND next_process.`code` = next_seed.`process_code`
       AND next_process.`name` = next_seed.`process_name`
       SET route_process.`next_process_id` = next_process.`id`,
           route_process.`link_type` = COALESCE(route_process.`link_type`, 0),
           route_process.`updater` = 'balloon-route-process-import',
           route_process.`update_time` = NOW()
     WHERE route_process.`tenant_id` = @target_tenant_id
       AND route_process.`deleted` = b'0'
       AND route_process.`remark` = '球囊工序路线导入';

    INSERT INTO `mes_dv_machinery`
        (`code`, `name`, `brand`, `specification`, `machinery_type_id`, `workshop_id`, `process_name`, `standard_hourly_capacity`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
    SELECT
        seed.`machinery_code`,
        seed.`machinery_name`,
        NULL,
        NULL,
        v_default_type_id,
        v_default_workshop_id,
        NULL,
        NULL,
        0,
        '球囊扩张导管工序产能导入',
        'balloon-process-device-capacity',
        NOW(),
        'balloon-process-device-capacity',
        NOW(),
        b'0',
        @target_tenant_id
    FROM `tmp_balloon_machinery_seed` seed
    LEFT JOIN `mes_dv_machinery` existing
        ON existing.`tenant_id` = @target_tenant_id
       AND existing.`deleted` = b'0'
       AND existing.`code` = seed.`machinery_code`
    WHERE existing.`id` IS NULL;

    UPDATE `mes_dv_machinery` machinery
    INNER JOIN `tmp_balloon_machinery_seed` seed
        ON seed.`machinery_code` = machinery.`code`
    SET machinery.`name` = seed.`machinery_name`,
        machinery.`machinery_type_id` = COALESCE(machinery.`machinery_type_id`, v_default_type_id),
        machinery.`workshop_id` = COALESCE(machinery.`workshop_id`, v_default_workshop_id),
        machinery.`status` = COALESCE(machinery.`status`, 0),
        machinery.`updater` = 'balloon-process-device-capacity',
        machinery.`update_time` = NOW()
    WHERE machinery.`tenant_id` = @target_tenant_id
      AND machinery.`deleted` = b'0';

    DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_machinery_process_legacy_keep`;
    CREATE TEMPORARY TABLE `tmp_balloon_machinery_process_legacy_keep` AS
    SELECT
        MIN(mp.`id`) AS `keep_id`,
        machinery.`id` AS `machinery_id`,
        process.`id` AS `process_id`,
        seed.`product_name`,
        seed.`process_code`,
        seed.`process_name`,
        seed.`machinery_code`,
        seed.`device_name`,
        seed.`device_quantity`,
        seed.`ten_half_hour_daily_capacity`,
        seed.`source_row_no`,
        seed.`remark`
    FROM `tmp_balloon_machinery_process_seed` seed
    INNER JOIN `mes_pro_process` process
        ON process.`tenant_id` = @target_tenant_id
       AND process.`deleted` = b'0'
       AND process.`product_name` = seed.`product_name`
       AND process.`code` = seed.`process_code`
       AND process.`name` = seed.`process_name`
    INNER JOIN `mes_dv_machinery` machinery
        ON machinery.`tenant_id` = @target_tenant_id
       AND machinery.`deleted` = b'0'
       AND machinery.`code` = seed.`machinery_code`
    INNER JOIN `mes_dv_machinery_process` mp
        ON mp.`tenant_id` = @target_tenant_id
       AND mp.`deleted` = b'0'
       AND mp.`line_name` = seed.`product_name`
       AND mp.`machinery_code` = seed.`machinery_code`
       AND mp.`source_row_no` = seed.`source_row_no`
       AND mp.`process_name` = seed.`process_name`
    GROUP BY
        machinery.`id`,
        process.`id`,
        seed.`product_name`,
        seed.`process_code`,
        seed.`process_name`,
        seed.`machinery_code`,
        seed.`device_name`,
        seed.`device_quantity`,
        seed.`ten_half_hour_daily_capacity`,
        seed.`source_row_no`,
        seed.`remark`;

    UPDATE `mes_dv_machinery_process` mp
    INNER JOIN `tmp_balloon_machinery_process_legacy_keep` legacy_keep
        ON legacy_keep.`keep_id` = mp.`id`
    SET mp.`machinery_id` = legacy_keep.`machinery_id`,
        mp.`process_id` = legacy_keep.`process_id`,
        mp.`process_code` = legacy_keep.`process_code`,
        mp.`line_name` = legacy_keep.`product_name`,
        mp.`process_name` = legacy_keep.`process_name`,
        mp.`device_name` = legacy_keep.`device_name`,
        mp.`device_quantity` = legacy_keep.`device_quantity`,
        mp.`ten_half_hour_daily_capacity` = legacy_keep.`ten_half_hour_daily_capacity`,
        mp.`standard_hourly_capacity` = legacy_keep.`ten_half_hour_daily_capacity` / 10.5,
        mp.`remark` = NULLIF(legacy_keep.`remark`, ''),
        mp.`updater` = 'balloon-process-device-capacity',
        mp.`update_time` = NOW()
    WHERE mp.`tenant_id` = @target_tenant_id
      AND mp.`deleted` = b'0';

    UPDATE `mes_dv_machinery_process` mp
    INNER JOIN `tmp_balloon_machinery_process_seed` seed
        ON mp.`tenant_id` = @target_tenant_id
       AND mp.`deleted` = b'0'
       AND mp.`line_name` = seed.`product_name`
       AND mp.`machinery_code` = seed.`machinery_code`
       AND mp.`source_row_no` = seed.`source_row_no`
       AND mp.`process_name` = seed.`process_name`
    INNER JOIN `tmp_balloon_machinery_process_legacy_keep` legacy_keep
        ON legacy_keep.`product_name` = seed.`product_name`
       AND legacy_keep.`process_code` = seed.`process_code`
       AND legacy_keep.`process_name` = seed.`process_name`
       AND legacy_keep.`machinery_code` = seed.`machinery_code`
       AND legacy_keep.`source_row_no` = seed.`source_row_no`
    SET mp.`deleted` = b'1',
        mp.`updater` = 'balloon-process-device-capacity',
        mp.`update_time` = NOW()
    WHERE mp.`id` <> legacy_keep.`keep_id`;

    UPDATE `mes_dv_machinery_process` mp
    INNER JOIN `tmp_balloon_process_seed` seed
        ON seed.`product_name` = mp.`line_name`
       AND seed.`process_name` = mp.`process_name`
       AND seed.`manual_shift_capacity` IS NOT NULL
    SET mp.`deleted` = b'1',
        mp.`updater` = 'balloon-process-device-capacity',
        mp.`update_time` = NOW()
    WHERE mp.`tenant_id` = @target_tenant_id
      AND mp.`deleted` = b'0'
      AND mp.`machinery_code` = '/';

    UPDATE `mes_dv_machinery_process` mp
    INNER JOIN `mes_pro_process` process
        ON process.`id` = mp.`process_id`
       AND process.`tenant_id` = @target_tenant_id
       AND process.`deleted` = b'0'
    INNER JOIN `tmp_balloon_machinery_process_seed` seed
        ON seed.`product_name` = process.`product_name`
       AND seed.`process_code` = process.`code`
       AND seed.`process_name` = process.`name`
       AND seed.`machinery_code` = mp.`machinery_code`
       AND seed.`source_row_no` = mp.`source_row_no`
    INNER JOIN `mes_dv_machinery` machinery
        ON machinery.`tenant_id` = @target_tenant_id
       AND machinery.`deleted` = b'0'
       AND machinery.`code` = seed.`machinery_code`
    SET mp.`machinery_id` = machinery.`id`,
        mp.`process_code` = seed.`process_code`,
        mp.`process_name` = seed.`process_name`,
        mp.`device_name` = seed.`device_name`,
        mp.`device_quantity` = seed.`device_quantity`,
        mp.`ten_half_hour_daily_capacity` = seed.`ten_half_hour_daily_capacity`,
        mp.`standard_hourly_capacity` = seed.`ten_half_hour_daily_capacity` / 10.5,
        mp.`remark` = NULLIF(seed.`remark`, ''),
        mp.`updater` = 'balloon-process-device-capacity',
        mp.`update_time` = NOW()
    WHERE mp.`tenant_id` = @target_tenant_id
      AND mp.`deleted` = b'0';

    INSERT INTO `mes_dv_machinery_process`
        (`machinery_id`, `process_id`, `process_code`, `machinery_code`, `line_name`, `process_name`, `device_name`, `device_quantity`, `ten_half_hour_daily_capacity`, `standard_hourly_capacity`, `source_row_no`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
    SELECT
        machinery.`id`,
        process.`id`,
        seed.`process_code`,
        seed.`machinery_code`,
        seed.`product_name`,
        seed.`process_name`,
        seed.`device_name`,
        seed.`device_quantity`,
        seed.`ten_half_hour_daily_capacity`,
        seed.`ten_half_hour_daily_capacity` / 10.5,
        seed.`source_row_no`,
        NULLIF(seed.`remark`, ''),
        'balloon-process-device-capacity',
        NOW(),
        'balloon-process-device-capacity',
        NOW(),
        b'0',
        @target_tenant_id
    FROM `tmp_balloon_machinery_process_seed` seed
    INNER JOIN `mes_pro_process` process
        ON process.`tenant_id` = @target_tenant_id
       AND process.`deleted` = b'0'
       AND process.`product_name` = seed.`product_name`
       AND process.`code` = seed.`process_code`
       AND process.`name` = seed.`process_name`
    INNER JOIN `mes_dv_machinery` machinery
        ON machinery.`tenant_id` = @target_tenant_id
       AND machinery.`deleted` = b'0'
       AND machinery.`code` = seed.`machinery_code`
    LEFT JOIN `mes_dv_machinery_process` existing
        ON existing.`tenant_id` = @target_tenant_id
       AND existing.`deleted` = b'0'
       AND existing.`process_id` = process.`id`
       AND existing.`machinery_code` = seed.`machinery_code`
       AND existing.`source_row_no` = seed.`source_row_no`
    WHERE existing.`id` IS NULL;
END$$
DELIMITER ;

CALL intruoyi_seed_balloon_process_device_capacity();
DROP PROCEDURE IF EXISTS intruoyi_seed_balloon_process_device_capacity;

DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_route_process_next_seed`;
DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_route_process_seed`;
DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_route_seed`;
DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_machinery_process_legacy_keep`;
DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_machinery_process_seed`;
DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_machinery_seed`;
DROP TEMPORARY TABLE IF EXISTS `tmp_balloon_process_seed`;
