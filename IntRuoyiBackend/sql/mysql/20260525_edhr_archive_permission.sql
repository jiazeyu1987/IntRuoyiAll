-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=permission; riskLevel=low
-- Restore eDHR archive menu and API permissions for tenant-admin roles and
-- tenant packages that already carry eDHR menu access.

UPDATE `system_menu`
SET `name` = '电子批记录归档查询',
    `type` = 3,
    `sort` = 7,
    `parent_id` = 900010,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'edhr-archive-permission',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `permission` = 'mes:pro-batch-record-execution-archive:query';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900017, '电子批记录归档查询', 'mes:pro-batch-record-execution-archive:query', 3, 7, 900010, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-archive-permission', NOW(), 'edhr-archive-permission', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'mes:pro-batch-record-execution-archive:query'
);

UPDATE `system_menu`
SET `name` = '电子批记录归档生成',
    `type` = 3,
    `sort` = 8,
    `parent_id` = 900010,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'edhr-archive-permission',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `permission` = 'mes:pro-batch-record-execution-archive:create';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900018, '电子批记录归档生成', 'mes:pro-batch-record-execution-archive:create', 3, 8, 900010, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-archive-permission', NOW(), 'edhr-archive-permission', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'mes:pro-batch-record-execution-archive:create'
);

UPDATE `system_menu`
SET `name` = '电子批记录归档下载',
    `type` = 3,
    `sort` = 9,
    `parent_id` = 900010,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'edhr-archive-permission',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `permission` = 'mes:pro-batch-record-execution-archive:download';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900019, '电子批记录归档下载', 'mes:pro-batch-record-execution-archive:download', 3, 9, 900010, '', '', '', '', 0, b'1', b'1', b'1', 'edhr-archive-permission', NOW(), 'edhr-archive-permission', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'mes:pro-batch-record-execution-archive:download'
);

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_archive_permission_menu_ids`;
CREATE TEMPORARY TABLE `tmp_edhr_archive_permission_menu_ids` AS
SELECT `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `permission` IN (
      'mes:pro-batch-record-execution-archive:query',
      'mes:pro-batch-record-execution-archive:create',
      'mes:pro-batch-record-execution-archive:download'
  );

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_archive_permission_target_tenants`;
CREATE TEMPORARY TABLE `tmp_edhr_archive_permission_target_tenants` AS
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
INNER JOIN `system_menu` AS `edhr_menu`
        ON `edhr_menu`.`id` = `role_menu`.`menu_id`
       AND `edhr_menu`.`deleted` = b'0'
       AND (
           `edhr_menu`.`path` = 'batch-record-template'
           OR `edhr_menu`.`permission` IN (
               'mes:pro-batch-record-template:query',
               'mes:pro-batch-record-execution:query',
               'mes:pro-batch-record-execution:create',
               'mes:pro-batch-record-execution:update'
           )
       )
WHERE `tenant`.`deleted` = b'0'
  AND `tenant`.`package_id` <> 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_archive_permission_package_menu_ids`;
CREATE TEMPORARY TABLE `tmp_edhr_archive_permission_package_menu_ids` (
    `package_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
);

INSERT IGNORE INTO `tmp_edhr_archive_permission_package_menu_ids` (`package_id`, `menu_id`)
SELECT DISTINCT
    `target`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED) AS `menu_id`
FROM `tmp_edhr_archive_permission_target_tenants` AS `target`
INNER JOIN `system_tenant_package` AS `package`
        ON `package`.`id` = `target`.`package_id`
       AND `package`.`deleted` = b'0'
       AND JSON_VALID(`package`.`menu_ids`)
INNER JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
) AS `existing_menu`;

INSERT IGNORE INTO `tmp_edhr_archive_permission_package_menu_ids` (`package_id`, `menu_id`)
SELECT DISTINCT
    `target`.`package_id`,
    `archive_menu`.`id` AS `menu_id`
FROM `tmp_edhr_archive_permission_target_tenants` AS `target`
CROSS JOIN `tmp_edhr_archive_permission_menu_ids` AS `archive_menu`;

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_archive_permission_package_menu_json`;
CREATE TEMPORARY TABLE `tmp_edhr_archive_permission_package_menu_json` AS
SELECT
    `package_id`,
    JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
FROM (
    SELECT `package_id`, `menu_id`
    FROM `tmp_edhr_archive_permission_package_menu_ids`
    ORDER BY `package_id`, `menu_id`
) AS `merged_menu_ids`
GROUP BY `package_id`;

UPDATE `system_tenant_package` AS `package`
INNER JOIN `tmp_edhr_archive_permission_package_menu_json` AS `merged`
        ON `merged`.`package_id` = `package`.`id`
SET `package`.`menu_ids` = `merged`.`menu_ids`,
    `package`.`updater` = 'edhr-archive-permission',
    `package`.`update_time` = NOW()
WHERE `package`.`deleted` = b'0';

UPDATE `system_role_menu` AS `role_menu`
INNER JOIN `tmp_edhr_archive_permission_target_tenants` AS `target`
        ON `target`.`tenant_id` = `role_menu`.`tenant_id`
       AND `target`.`role_id` = `role_menu`.`role_id`
INNER JOIN `tmp_edhr_archive_permission_menu_ids` AS `archive_menu`
        ON `archive_menu`.`id` = `role_menu`.`menu_id`
SET `role_menu`.`deleted` = b'0',
    `role_menu`.`updater` = 'edhr-archive-permission',
    `role_menu`.`update_time` = NOW()
WHERE `role_menu`.`deleted` = b'1';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
    `target`.`role_id`,
    `archive_menu`.`id`,
    'edhr-archive-permission',
    NOW(),
    'edhr-archive-permission',
    NOW(),
    b'0',
    `target`.`tenant_id`
FROM `tmp_edhr_archive_permission_target_tenants` AS `target`
CROSS JOIN `tmp_edhr_archive_permission_menu_ids` AS `archive_menu`
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`tenant_id` = `target`.`tenant_id`
      AND `existing`.`role_id` = `target`.`role_id`
      AND `existing`.`menu_id` = `archive_menu`.`id`
      AND `existing`.`deleted` = b'0'
);

DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_archive_permission_package_menu_json`;
DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_archive_permission_package_menu_ids`;
DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_archive_permission_target_tenants`;
DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_archive_permission_menu_ids`;
