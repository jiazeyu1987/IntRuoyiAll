-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260610_mes_scheduler_workbench_p7; type=data; riskLevel=medium
-- MES 自动排产：预览、发布、重排权限拆分。

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
    `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
)
SELECT 900180, '自动排产预览', 'mes:pro-auto-schedule:preview', 3, 30, 5590,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5590 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900180);

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
    `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
)
SELECT 900181, '自动排产发布', 'mes:pro-auto-schedule:apply', 3, 40, 5590,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5590 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900181);

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
    `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
)
SELECT 900182, '自动排产重排', 'mes:pro-auto-schedule:replan', 3, 50, 5590,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5590 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900182);

INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT `role`.`role_id`, `menu`.`id`, 'system', NOW(), 'system', NOW(), b'0', `role`.`tenant_id`
FROM (
    SELECT 111 AS `role_id`, 122 AS `tenant_id`
    UNION ALL
    SELECT 1 AS `role_id`, 1 AS `tenant_id`
) `role`
CROSS JOIN `system_menu` `menu`
WHERE `menu`.`id` IN (900180, 900181, 900182)
  AND `menu`.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` `existing`
    WHERE `existing`.`role_id` = `role`.`role_id`
      AND `existing`.`menu_id` = `menu`.`id`
      AND `existing`.`tenant_id` = `role`.`tenant_id`
      AND `existing`.`deleted` = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_auto_schedule_package_menu_ids`;
CREATE TEMPORARY TABLE `tmp_mes_auto_schedule_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
);

INSERT IGNORE INTO `tmp_mes_auto_schedule_package_menu_ids` (`package_id`, `menu_id`)
SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
FROM `system_tenant_package` AS `package`
JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
) AS `existing_menu`
WHERE `package`.`deleted` = b'0'
  AND JSON_VALID(`package`.`menu_ids`)
  AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900120' AS JSON), '$');

INSERT IGNORE INTO `tmp_mes_auto_schedule_package_menu_ids` (`package_id`, `menu_id`)
SELECT
    `package`.`id`,
    `menu`.`id`
FROM `system_tenant_package` AS `package`
CROSS JOIN (
    SELECT 900180 AS `id`
    UNION ALL SELECT 900181
    UNION ALL SELECT 900182
) AS `menu`
WHERE `package`.`deleted` = b'0'
  AND JSON_VALID(`package`.`menu_ids`)
  AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900120' AS JSON), '$');

UPDATE `system_tenant_package` AS `package`
JOIN (
    SELECT DISTINCT
        `package_id`,
        JSON_ARRAYAGG(`menu_id`) OVER (
            PARTITION BY `package_id`
            ORDER BY `menu_id`
            ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
        ) AS `menu_ids`
    FROM `tmp_mes_auto_schedule_package_menu_ids`
) AS `merged`
  ON `merged`.`package_id` = `package`.`id`
SET `package`.`menu_ids` = `merged`.`menu_ids`,
    `package`.`updater` = 'system',
    `package`.`update_time` = NOW();

DROP TEMPORARY TABLE IF EXISTS `tmp_mes_auto_schedule_package_menu_ids`;
