-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=permission; riskLevel=low
-- Restore eDHR menu and API permissions for tenant-admin roles whose eDHR
-- role-menu rows were soft-deleted by tenant package synchronization.

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_permission_menu_ids`;
CREATE TEMPORARY TABLE `tmp_edhr_permission_menu_ids` AS
WITH RECURSIVE `edhr_menu_tree` AS (
    SELECT `id`, `parent_id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (
        `path` = 'batch-record-template'
        OR `permission` IN (
            'mes:pro-batch-record-template:query',
            'mes:pro-batch-record-template:import',
            'mes:pro-batch-record-template:update',
            'mes:pro-batch-record-template:delete',
            'mes:pro-batch-record-execution:query',
            'mes:pro-batch-record-execution:create',
            'mes:pro-batch-record-execution:update'
        )
      )
    UNION ALL
    SELECT `parent`.`id`, `parent`.`parent_id`
    FROM `system_menu` AS `parent`
    INNER JOIN `edhr_menu_tree` AS `child` ON `child`.`parent_id` = `parent`.`id`
    WHERE `child`.`parent_id` <> 0
      AND `parent`.`deleted` = b'0'
)
SELECT DISTINCT `id`
FROM `edhr_menu_tree`;

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_permission_target_tenants`;
CREATE TEMPORARY TABLE `tmp_edhr_permission_target_tenants` AS
SELECT DISTINCT
    `tenant`.`id` AS `tenant_id`,
    `tenant`.`package_id`,
    `role`.`id` AS `role_id`
FROM `system_tenant` AS `tenant`
INNER JOIN `system_role` AS `role`
        ON `role`.`tenant_id` = `tenant`.`id`
       AND `role`.`code` = 'tenant_admin'
       AND `role`.`deleted` = b'0'
INNER JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`tenant_id` = `tenant`.`id`
       AND `role_menu`.`role_id` = `role`.`id`
       AND `role_menu`.`deleted` = b'1'
INNER JOIN `tmp_edhr_permission_menu_ids` AS `edhr_menu`
        ON `edhr_menu`.`id` = `role_menu`.`menu_id`
WHERE `tenant`.`deleted` = b'0'
  AND `tenant`.`package_id` <> 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_permission_package_menu_ids`;
CREATE TEMPORARY TABLE `tmp_edhr_permission_package_menu_ids` (
    `package_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
);

INSERT IGNORE INTO `tmp_edhr_permission_package_menu_ids` (`package_id`, `menu_id`)
SELECT DISTINCT
    `target`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
FROM `tmp_edhr_permission_target_tenants` AS `target`
INNER JOIN `system_tenant_package` AS `package`
        ON `package`.`id` = `target`.`package_id`
       AND `package`.`deleted` = b'0'
       AND JSON_VALID(`package`.`menu_ids`)
INNER JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
) AS `existing_menu`;

INSERT IGNORE INTO `tmp_edhr_permission_package_menu_ids` (`package_id`, `menu_id`)
SELECT DISTINCT
    `target`.`package_id`,
    `edhr_menu`.`id` AS `menu_id`
FROM `tmp_edhr_permission_target_tenants` AS `target`
CROSS JOIN `tmp_edhr_permission_menu_ids` AS `edhr_menu`;

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_permission_package_menu_json`;
CREATE TEMPORARY TABLE `tmp_edhr_permission_package_menu_json` AS
SELECT
    `package_id`,
    JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
FROM (
    SELECT `package_id`, `menu_id`
    FROM `tmp_edhr_permission_package_menu_ids`
    ORDER BY `package_id`, `menu_id`
) AS `merged_menu_ids`
GROUP BY `package_id`;

UPDATE `system_tenant_package` AS `package`
INNER JOIN `tmp_edhr_permission_package_menu_json` AS `merged`
        ON `merged`.`package_id` = `package`.`id`
SET `package`.`menu_ids` = `merged`.`menu_ids`,
    `package`.`updater` = 'edhr-permission-fix',
    `package`.`update_time` = NOW()
WHERE `package`.`deleted` = b'0';

UPDATE `system_menu`
SET `name` = '电子批记录',
    `visible` = b'1',
    `updater` = 'edhr-permission-fix',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `path` = 'batch-record-template'
  AND `permission` = 'mes:pro-batch-record-template:query';

UPDATE `system_menu`
SET `name` = '电子批记录执行查询',
    `updater` = 'edhr-permission-fix',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `permission` = 'mes:pro-batch-record-execution:query';

UPDATE `system_menu`
SET `name` = '电子批记录执行创建',
    `updater` = 'edhr-permission-fix',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `permission` = 'mes:pro-batch-record-execution:create';

UPDATE `system_menu`
SET `name` = '电子批记录执行更新',
    `updater` = 'edhr-permission-fix',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `permission` = 'mes:pro-batch-record-execution:update';

UPDATE `system_role_menu` AS `role_menu`
INNER JOIN `tmp_edhr_permission_target_tenants` AS `target`
        ON `target`.`tenant_id` = `role_menu`.`tenant_id`
       AND `target`.`role_id` = `role_menu`.`role_id`
INNER JOIN `tmp_edhr_permission_menu_ids` AS `edhr_menu`
        ON `edhr_menu`.`id` = `role_menu`.`menu_id`
SET `role_menu`.`deleted` = b'0',
    `role_menu`.`updater` = 'edhr-permission-fix',
    `role_menu`.`update_time` = NOW()
WHERE `role_menu`.`deleted` = b'1';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
    `target`.`role_id`,
    `edhr_menu`.`id`,
    'edhr-permission-fix',
    NOW(),
    'edhr-permission-fix',
    NOW(),
    b'0',
    `target`.`tenant_id`
FROM `tmp_edhr_permission_target_tenants` AS `target`
CROSS JOIN `tmp_edhr_permission_menu_ids` AS `edhr_menu`
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`tenant_id` = `target`.`tenant_id`
      AND `existing`.`role_id` = `target`.`role_id`
      AND `existing`.`menu_id` = `edhr_menu`.`id`
      AND `existing`.`deleted` = b'0'
);

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_permission_package_menu_json`;
DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_permission_package_menu_ids`;
DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_permission_target_tenants`;
DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_permission_menu_ids`;
