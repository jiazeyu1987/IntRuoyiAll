-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Ensure showroom company-version menu exists in the live MySQL runtime
-- and inherits the same role bindings as the existing company menu.

UPDATE `system_menu`
SET `sort` = 3,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980102
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 4,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980103
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 5,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980104
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 6,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980105
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 7,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980106
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 8,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980107
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 9,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980119
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 10,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980108
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 11,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980109
  AND `parent_id` = 980100
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
    980118,
    '公司版本',
    '',
    2,
    2,
    980100,
    'company-version',
    'ep:clock',
    'showroom-admin/index',
    'ShowroomAdminCompanyVersion',
    0,
    b'1',
    b'1',
    b'1',
    'showroom-menu-fix',
    NOW(),
    'showroom-menu-fix',
    NOW(),
    b'0'
FROM `system_menu` AS `source`
WHERE `source`.`id` = 980101
  AND `source`.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    WHERE `existing`.`id` = 980118
      AND `existing`.`deleted` = b'0'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    WHERE `existing`.`parent_id` = 980100
      AND `existing`.`path` = 'company-version'
      AND `existing`.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `name` = '公司版本',
    `permission` = '',
    `type` = 2,
    `sort` = 2,
    `parent_id` = 980100,
    `path` = 'company-version',
    `icon` = 'ep:clock',
    `component` = 'showroom-admin/index',
    `component_name` = 'ShowroomAdminCompanyVersion',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 980118
  AND `parent_id` = 980100;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
    `source`.`role_id`,
    980118,
    'showroom-menu-fix',
    NOW(),
    'showroom-menu-fix',
    NOW(),
    b'0',
    `source`.`tenant_id`
FROM `system_role_menu` AS `source`
WHERE `source`.`menu_id` = 980101
  AND `source`.`deleted` = b'0'
  AND EXISTS (
    SELECT 1
    FROM `system_menu` AS `target_menu`
    WHERE `target_menu`.`id` = 980118
      AND `target_menu`.`parent_id` = 980100
      AND `target_menu`.`path` = 'company-version'
      AND `target_menu`.`component_name` = 'ShowroomAdminCompanyVersion'
      AND `target_menu`.`deleted` = b'0'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `target`
    WHERE `target`.`role_id` = `source`.`role_id`
      AND `target`.`menu_id` = 980118
      AND `target`.`tenant_id` = `source`.`tenant_id`
      AND `target`.`deleted` = b'0'
  );
