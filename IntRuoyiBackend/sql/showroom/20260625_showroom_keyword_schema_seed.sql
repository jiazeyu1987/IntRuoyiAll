CREATE TABLE IF NOT EXISTS `showroom_keyword` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name_zh` varchar(255) NOT NULL,
    `name_en` varchar(255) NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_showroom_keyword_tenant_name_zh` (`tenant_id`, `name_zh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='展厅关键词中英对照';

INSERT INTO `showroom_keyword`
(`tenant_id`, `name_zh`, `name_en`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
    `seed_scope`.`tenant_id`,
    `seed_data`.`name_zh`,
    `seed_data`.`name_en`,
    'showroom-keyword-seed',
    '2026-06-25 00:00:00',
    'showroom-keyword-seed',
    '2026-06-25 00:00:00',
    b'0'
FROM (
    SELECT DISTINCT `menu_scope`.`tenant_id`
    FROM `system_role_menu` AS `menu_scope`
    WHERE `menu_scope`.`menu_id` = 980102
      AND `menu_scope`.`deleted` = b'0'
) AS `seed_scope`
JOIN (
    SELECT 1 AS `seed_order`, '上海瑛泰医疗器械自动化有限公司' AS `name_zh`, 'Shanghai INT Medical Instruments Automation Co., Ltd.' AS `name_en`
    UNION ALL SELECT 2, '珠海德瑞医疗器械有限公司', 'Zhuhai Derui Medical Instruments Co., Ltd.'
    UNION ALL SELECT 3, '上海璞康医疗器械有限公司', 'Shanghai Pukon Medical Instruments Co., Ltd.'
    UNION ALL SELECT 4, '上海七木医疗器械有限公司', 'Shanghai Qimu Medical Instruments Co., Ltd.'
    UNION ALL SELECT 5, '上海璞慧医疗器械有限公司', 'Shanghai Puhui Medical Instruments Co., Ltd.'
    UNION ALL SELECT 6, '上海翰凌医疗器械有限公司', 'Shanghai Healing Medical Instruments Co., Ltd.'
    UNION ALL SELECT 7, '香港瑛泰医疗器械有限公司', 'Hongkong INT Medical Instruments Company Limited'
    UNION ALL SELECT 8, '上海璞镁医疗器械有限公司', 'Shanghai Pumei Medical Instruments Co., Ltd.'
    UNION ALL SELECT 9, '山东瑛泰医疗器械有限公司', 'Shandong INT Medical Instruments Co., Ltd.'
    UNION ALL SELECT 10, '上海璞霖医疗器械有限公司', 'Shanghai Pulin Medical Instruments Co., Ltd.'
    UNION ALL SELECT 11, '上海璞跃医疗器械有限公司', 'Shanghai Puyue Medical Instruments Co., Ltd.'
    UNION ALL SELECT 12, '上海益凯医疗器械有限公司', 'Shanghai Yikai Medical Instruments Co., Ltd.'
    UNION ALL SELECT 13, '上海瑛泰生物科技有限公司', 'Shanghai INT Biotechnology Co., Ltd.'
    UNION ALL SELECT 14, '上海瑛泰璞润医疗器械有限公司', 'Shanghai INT Pureray Medical Instruments Co., Ltd.'
    UNION ALL SELECT 15, '山东瑛盛新材料有限公司', 'Shandong Insant New Materials Co., Ltd.'
    UNION ALL SELECT 16, '珠海璞跃医疗器械有限公司', 'Zhuhai Puyue Medical Instruments Co., Ltd.'
    UNION ALL SELECT 17, '上海泰嘉瑞医疗科技有限公司', 'Shanghai Techarray Medical Technology Co., Ltd.'
    UNION ALL SELECT 18, '山东瑛泰医疗科技有限公司', 'Shandong INT Medical Technology Co., Ltd.'
    UNION ALL SELECT 19, '上海瑛泰昇活商贸有限公司', 'Shanghai INT Life Co., Ltd.'
    UNION ALL SELECT 20, '珠海璞瑞智能制造有限公司', 'Zhuhai Purui Intelligent Manufacturing Co., Ltd.'
    UNION ALL SELECT 21, '上海瑛泰投资管理有限公司', 'Shanghai INT Investment Management Co., Ltd.'
    UNION ALL SELECT 22, '上海瑛泰实业有限公司', 'Shanghai INT Property Management Co., Ltd.'
    UNION ALL SELECT 23, '杭州唯强医疗科技有限公司', 'Hangzhou Endonom Medtech Co., Ltd.'
    UNION ALL SELECT 24, '杭州唯淅医疗科技有限公司', 'Hangzhou Weixi Medical Technology Co., Ltd.'
    UNION ALL SELECT 25, '上海瑛泰企业管理有限公司', 'Shanghai INT Enterprise Management Co., Ltd.'
    UNION ALL SELECT 26, '上海吉尔邦医学科技有限公司', 'Shanghai GelBond Medtech Co., Ltd.'
    UNION ALL SELECT 27, '上海瑛泰医疗科技有限公司', 'Shanghai INT Medical Technology Co., Ltd.'
) AS `seed_data`
WHERE NOT EXISTS (
    SELECT 1
    FROM `showroom_keyword` AS `existing`
    WHERE `existing`.`tenant_id` = `seed_scope`.`tenant_id`
      AND `existing`.`name_zh` = `seed_data`.`name_zh`
      AND `existing`.`deleted` = b'0'
)
ORDER BY `tenant_id` ASC, `seed_order` ASC;
