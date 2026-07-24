-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=menu; riskLevel=low
-- Decommission the retired DCC personal-file menu.
-- All controlled-file viewing must use controlled-file/browser or audited detail/view routes.

DELETE role_menu
FROM `system_role_menu` role_menu
JOIN `system_menu` menu ON menu.`id` = role_menu.`menu_id`
WHERE menu.`path` = 'controlled-file/mine'
   OR menu.`component` = 'dcc/controlled-file/mine/index'
   OR menu.`component_name` = 'DccControlledFileMine';

UPDATE `system_menu`
SET `status` = 1,
    `visible` = b'0',
    `deleted` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/mine'
   OR `component` = 'dcc/controlled-file/mine/index'
   OR `component_name` = 'DccControlledFileMine';

UPDATE `system_menu`
SET `status` = 0,
    `visible` = b'1',
    `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/browser'
  AND `component` = 'dcc/controlled-file/browser/index';
