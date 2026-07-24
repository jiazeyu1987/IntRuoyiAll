-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_showroom_award_and_hall_item_schema; type=data; riskLevel=medium
SET @showroom_company_honor_target_tenant_id = IFNULL(@showroom_company_honor_target_tenant_id, 0);

SET @showroom_company_honor_award_count := (
    SELECT COUNT(*)
    FROM `showroom_award`
    WHERE `deleted` = b'0'
      AND `current_revision_id` IS NOT NULL
      AND (@showroom_company_honor_target_tenant_id = 0
           OR `tenant_id` = @showroom_company_honor_target_tenant_id)
);

DROP PROCEDURE IF EXISTS `_showroom_company_honor_hall_requirements`;
DELIMITER //
CREATE PROCEDURE `_showroom_company_honor_hall_requirements`()
BEGIN
    IF @showroom_company_honor_award_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'SHOWROOM_ENTERPRISE_HONOR_AWARD_MISSING: no published awards are available for enterprise honor hall mapping';
    END IF;
END//
DELIMITER ;
CALL `_showroom_company_honor_hall_requirements`();
DROP PROCEDURE `_showroom_company_honor_hall_requirements`;

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_hall_definitions`;
CREATE TEMPORARY TABLE `tmp_showroom_company_honor_hall_definitions` (
    `hall_code` varchar(64) NOT NULL,
    `name` varchar(128) NOT NULL,
    `name_en` varchar(128) NOT NULL,
    `description` text NOT NULL,
    `description_en` text NOT NULL,
    `display_order` int NOT NULL,
    PRIMARY KEY (`hall_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tmp_showroom_company_honor_hall_definitions` (
    `hall_code`, `name`, `name_en`, `description`, `description_en`, `display_order`
)
VALUES
    (
        'hall_09',
        '企业荣誉展柜1',
        'Corporate Honors Showcase 1',
        '企业荣誉展柜1集中呈现公司荣誉体系的第一组奖项，涵盖社会贡献、总部认定、专精特新、创新总部、商业单项冠军、高新技术、知识产权与质量体系等代表性成果，展示企业在规范经营、技术创新和行业认可方面的持续积累。',
        'Corporate Honors Showcase 1 presents the first group of enterprise awards, covering social contribution, headquarters recognition, specialized and innovative enterprise honors, innovation headquarters, single-champion recognition, high-tech capability, intellectual property, and quality-system achievements. It highlights the company''s sustained progress in compliant operations, technology innovation, and industry recognition.',
        9
    ),
    (
        'hall_10',
        '企业荣誉展柜2',
        'Corporate Honors Showcase 2',
        '企业荣誉展柜2集中呈现公司荣誉体系的第二组奖项，延续展示品牌影响力、技术创新、产品质量、行业资质、社会责任和市场信任等成果，承接 Excel 奖项页签后半部分奖项信息，体现企业长期稳健发展的综合实力。',
        'Corporate Honors Showcase 2 presents the second group of enterprise awards, continuing the record of brand influence, technology innovation, product quality, industry qualifications, social responsibility, and market trust. It carries the latter half of the Excel Awards sheet and reflects the company''s sustained and balanced growth.',
        10
    );

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_tenants`;
CREATE TEMPORARY TABLE `tmp_showroom_company_honor_tenants` AS
SELECT `tenant_id`
FROM `showroom_award`
WHERE `deleted` = b'0'
  AND `current_revision_id` IS NOT NULL
  AND (@showroom_company_honor_target_tenant_id = 0
       OR `tenant_id` = @showroom_company_honor_target_tenant_id)
GROUP BY `tenant_id`;

UPDATE `showroom_hall` h
JOIN `tmp_showroom_company_honor_tenants` t
  ON t.`tenant_id` = h.`tenant_id`
JOIN `tmp_showroom_company_honor_hall_definitions` d
  ON d.`hall_code` = h.`hall_code`
SET h.`name` = d.`name`,
    h.`name_en` = d.`name_en`,
    h.`description` = d.`description`,
    h.`description_en` = d.`description_en`,
    h.`display_order` = d.`display_order`,
    h.`status` = 'ACTIVE',
    h.`deleted` = b'0',
    h.`updater` = 'system',
    h.`update_time` = NOW()
WHERE h.`hall_code` IN ('hall_09', 'hall_10');

INSERT INTO `showroom_hall` (
    `hall_code`, `name`, `name_en`, `description`, `description_en`, `display_order`, `status`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT d.`hall_code`,
       d.`name`,
       d.`name_en`,
       d.`description`,
       d.`description_en`,
       d.`display_order`,
       'ACTIVE',
       'system',
       NOW(),
       'system',
       NOW(),
       b'0',
       t.`tenant_id`
FROM `tmp_showroom_company_honor_tenants` t
JOIN `tmp_showroom_company_honor_hall_definitions` d
LEFT JOIN `showroom_hall` h
       ON h.`tenant_id` = t.`tenant_id`
      AND h.`hall_code` = d.`hall_code`
WHERE h.`id` IS NULL;

DELETE hp
FROM `showroom_hall_product` hp
JOIN `showroom_hall` h
  ON h.`tenant_id` = hp.`tenant_id`
 AND h.`id` = hp.`hall_id`
JOIN `tmp_showroom_company_honor_tenants` t
  ON t.`tenant_id` = hp.`tenant_id`
WHERE h.`hall_code` IN ('hall_09', 'hall_10');

DELETE hi
FROM `showroom_hall_item` hi
JOIN `showroom_hall` h
  ON h.`tenant_id` = hi.`tenant_id`
 AND h.`id` = hi.`hall_id`
JOIN `tmp_showroom_company_honor_tenants` t
  ON t.`tenant_id` = hi.`tenant_id`
WHERE h.`hall_code` IN ('hall_09', 'hall_10');

DELETE hi
FROM `showroom_hall_item` hi
JOIN `showroom_hall` h
  ON h.`tenant_id` = hi.`tenant_id`
 AND h.`id` = hi.`hall_id`
JOIN `tmp_showroom_company_honor_tenants` t
  ON t.`tenant_id` = hi.`tenant_id`
WHERE hi.`item_type` = 'AWARD'
  AND h.`hall_code` NOT IN ('hall_09', 'hall_10');

UPDATE `showroom_hall` h
JOIN `tmp_showroom_company_honor_tenants` t
  ON t.`tenant_id` = h.`tenant_id`
SET h.`deleted` = b'1',
    h.`updater` = 'system',
    h.`update_time` = NOW()
WHERE h.`hall_code` = 'company_honor';

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_award_routes`;
CREATE TEMPORARY TABLE `tmp_showroom_company_honor_award_routes` AS
SELECT ranked.`tenant_id`,
       ranked.`award_id`,
       ranked.`award_sort_no`,
       ranked.`total_count`,
       CASE WHEN ranked.`award_sort_no` <= CEIL(ranked.`total_count` / 2)
            THEN CAST('hall_09' AS CHAR(64) CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
            ELSE CAST('hall_10' AS CHAR(64) CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
       END AS `target_hall_code`
FROM (
    SELECT a.`tenant_id`,
           a.`id` AS `award_id`,
           ROW_NUMBER() OVER (PARTITION BY a.`tenant_id` ORDER BY a.`award_code`, a.`id`) AS `award_sort_no`,
           COUNT(*) OVER (PARTITION BY a.`tenant_id`) AS `total_count`
    FROM `showroom_award` a
    JOIN `tmp_showroom_company_honor_tenants` t
      ON t.`tenant_id` = a.`tenant_id`
    WHERE a.`deleted` = b'0'
      AND a.`current_revision_id` IS NOT NULL
) ranked;

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_awards`;
CREATE TEMPORARY TABLE `tmp_showroom_company_honor_awards` AS
SELECT ranked.`tenant_id`,
       ranked.`target_hall_code`,
       h.`id` AS `hall_id`,
       ranked.`award_id`,
       ROW_NUMBER() OVER (PARTITION BY ranked.`tenant_id`, ranked.`target_hall_code` ORDER BY ranked.`award_sort_no`) AS `display_order`,
       COUNT(*) OVER (PARTITION BY ranked.`tenant_id`, ranked.`target_hall_code`) AS `item_count`
FROM `tmp_showroom_company_honor_award_routes` ranked
JOIN `showroom_hall` h
  ON h.`tenant_id` = ranked.`tenant_id`
 AND h.`hall_code` = ranked.`target_hall_code`
 AND h.`deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_layout`;
CREATE TEMPORARY TABLE `tmp_showroom_company_honor_layout` AS
SELECT base.`tenant_id`,
       base.`hall_id`,
       base.`award_id`,
       base.`display_order`,
       base.`row_total`,
       FLOOR((base.`display_order` - 1) / base.`column_total`) AS `row_index`,
       MOD(base.`display_order` - 1, base.`column_total`) AS `column_index`,
       LEAST(
           base.`column_total`,
           base.`item_count` - FLOOR((base.`display_order` - 1) / base.`column_total`) * base.`column_total`
       ) AS `row_count`
FROM (
    SELECT a.`tenant_id`,
           a.`hall_id`,
           a.`award_id`,
           a.`display_order`,
           a.`item_count`,
           GREATEST(1, FLOOR(SQRT(a.`item_count`))) AS `row_total`,
           CEIL(a.`item_count` / GREATEST(1, FLOOR(SQRT(a.`item_count`)))) AS `column_total`
    FROM `tmp_showroom_company_honor_awards` a
) base;

INSERT INTO `showroom_hall_item` (
    `hall_id`, `item_type`, `item_id`, `display_order`, `layout_x`, `layout_y`, `layout_width`, `layout_height`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT l.`hall_id`,
       'AWARD',
       l.`award_id`,
       l.`display_order`,
       CAST(ROUND(l.`column_index` / l.`row_count`, 6) AS DECIMAL(8,6)) AS `layout_x`,
       CAST(ROUND(l.`row_index` / l.`row_total`, 6) AS DECIMAL(8,6)) AS `layout_y`,
       CAST(ROUND(
           ROUND((l.`column_index` + 1) / l.`row_count`, 6)
           - ROUND(l.`column_index` / l.`row_count`, 6),
           6
       ) AS DECIMAL(8,6)) AS `layout_width`,
       CAST(ROUND(
           ROUND((l.`row_index` + 1) / l.`row_total`, 6)
           - ROUND(l.`row_index` / l.`row_total`, 6),
           6
       ) AS DECIMAL(8,6)) AS `layout_height`,
       'system',
       NOW(),
       'system',
       NOW(),
       b'0',
       l.`tenant_id`
FROM `tmp_showroom_company_honor_layout` l;

DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_layout`;
DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_awards`;
DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_award_routes`;
DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_tenants`;
DROP TEMPORARY TABLE IF EXISTS `tmp_showroom_company_honor_hall_definitions`;
