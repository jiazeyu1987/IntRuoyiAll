-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260625_showroom_keyword_schema_seed; type=seed; riskLevel=low
-- Add BU keyword bilingual rows for showroom keyword list.

INSERT INTO `showroom_keyword`
(`tenant_id`, `name_zh`, `name_en`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
    `seed_scope`.`tenant_id`,
    `seed_data`.`name_zh`,
    `seed_data`.`name_en`,
    'showroom-keyword-bu-seed',
    '2026-06-26 00:00:00',
    'showroom-keyword-bu-seed',
    '2026-06-26 00:00:00',
    b'0'
FROM (
    SELECT DISTINCT `menu_scope`.`tenant_id`
    FROM `system_role_menu` AS `menu_scope`
    WHERE `menu_scope`.`menu_id` = 980102
      AND `menu_scope`.`deleted` = b'0'
) AS `seed_scope`
JOIN (
    SELECT 1 AS `seed_order`, '心脏电生理BU' AS `name_zh`, 'Cardiac Electrophysiology BU' AS `name_en`
    UNION ALL SELECT 2, '神经血管BU', 'Neurovascular BU'
    UNION ALL SELECT 3, '心血管BU', 'Cardiovascular BU'
    UNION ALL SELECT 4, '结构心BU', 'Structural Heart BU'
    UNION ALL SELECT 5, '外周血管BU', 'Peripheral Vascular BU'
    UNION ALL SELECT 6, '非血管BU', 'Non-vascular BU'
) AS `seed_data`
WHERE NOT EXISTS (
    SELECT 1
    FROM `showroom_keyword` AS `existing`
    WHERE `existing`.`tenant_id` = `seed_scope`.`tenant_id`
      AND `existing`.`name_zh` = `seed_data`.`name_zh`
      AND `existing`.`deleted` = b'0'
)
ORDER BY `tenant_id` ASC, `seed_order` ASC;
