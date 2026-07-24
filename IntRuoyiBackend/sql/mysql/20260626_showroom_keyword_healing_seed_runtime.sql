-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260626_showroom_keyword_bu_seed_runtime; type=seed; riskLevel=low
-- Add runtime-scanned glossary override for 翰凌 -> Healing so sub-term translation matches showroom keyword rules.

INSERT INTO `showroom_keyword`
(`tenant_id`, `name_zh`, `name_en`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
    `seed_scope`.`tenant_id`,
    `seed_data`.`name_zh`,
    `seed_data`.`name_en`,
    'showroom-keyword-healing-seed',
    '2026-06-26 00:00:00',
    'showroom-keyword-healing-seed',
    '2026-06-26 00:00:00',
    b'0'
FROM (
    SELECT DISTINCT `menu_scope`.`tenant_id`
    FROM `system_role_menu` AS `menu_scope`
    WHERE `menu_scope`.`menu_id` = 980102
      AND `menu_scope`.`deleted` = b'0'
) AS `seed_scope`
JOIN (
    SELECT 1 AS `seed_order`, '翰凌' AS `name_zh`, 'Healing' AS `name_en`
) AS `seed_data`
WHERE NOT EXISTS (
    SELECT 1
    FROM `showroom_keyword` AS `existing`
    WHERE `existing`.`tenant_id` = `seed_scope`.`tenant_id`
      AND `existing`.`name_zh` = `seed_data`.`name_zh`
      AND `existing`.`deleted` = b'0'
)
ORDER BY `tenant_id` ASC, `seed_order` ASC;
