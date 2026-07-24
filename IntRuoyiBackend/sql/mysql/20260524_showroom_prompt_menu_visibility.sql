-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Ensure showroom prompt menu exists in the live MySQL runtime
-- and inherits the same role bindings as the existing product menu.

UPDATE `system_menu`
SET `sort` = 4,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980119
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 5,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980103
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 6,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980104
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 7,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980105
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 8,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980106
  AND `parent_id` = 980100
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 9,
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW()
WHERE `id` = 980107
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
    980119,
    '提示管理',
    '',
    2,
    4,
    980100,
    'prompt',
    'ep:edit-pen',
    'showroom-admin/index',
    'ShowroomAdminPrompt',
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
WHERE `source`.`id` = 980102
  AND `source`.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    WHERE `existing`.`id` = 980119
      AND `existing`.`deleted` = b'0'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    WHERE `existing`.`parent_id` = 980100
      AND `existing`.`path` = 'prompt'
      AND `existing`.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `name` = '提示管理',
    `permission` = '',
    `type` = 2,
    `sort` = 4,
    `parent_id` = 980100,
    `path` = 'prompt',
    `icon` = 'ep:edit-pen',
    `component` = 'showroom-admin/index',
    `component_name` = 'ShowroomAdminPrompt',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'showroom-menu-fix',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 980119
  AND `parent_id` = 980100;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
    `source`.`role_id`,
    980119,
    'showroom-menu-fix',
    NOW(),
    'showroom-menu-fix',
    NOW(),
    b'0',
    `source`.`tenant_id`
FROM `system_role_menu` AS `source`
WHERE `source`.`menu_id` = 980102
  AND `source`.`deleted` = b'0'
  AND EXISTS (
    SELECT 1
    FROM `system_menu` AS `target_menu`
    WHERE `target_menu`.`id` = 980119
      AND `target_menu`.`parent_id` = 980100
      AND `target_menu`.`path` = 'prompt'
      AND `target_menu`.`component_name` = 'ShowroomAdminPrompt'
      AND `target_menu`.`deleted` = b'0'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `target`
    WHERE `target`.`role_id` = `source`.`role_id`
      AND `target`.`menu_id` = 980119
      AND `target`.`tenant_id` = `source`.`tenant_id`
      AND `target`.`deleted` = b'0'
  );
