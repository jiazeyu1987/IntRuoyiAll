-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Restore the DCC my-training menu when runtime databases missed the closed-loop
-- training seed but frontend/backend training-task code already exists.

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的培训', 'dcc:controlled-file:training:mine', 2, 13, 6800,
       'controlled-file/training-mine', 'ep:reading', 'dcc/controlled-file/training/mine/index',
       'DccControlledFileTrainingMine', 0, b'0', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `path` = 'controlled-file/training-mine'
);

UPDATE `system_menu`
SET `name` = '我的培训',
    `permission` = 'dcc:controlled-file:training:mine',
    `component` = 'dcc/controlled-file/training/mine/index',
    `component_name` = 'DccControlledFileTrainingMine',
    `visible` = b'0',
    `always_show` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = 'controlled-file/training-mine'
  AND `deleted` = b'0';

-- Mirror the existing DCC training-menu role coverage so users who can enter
-- DCC training can also open their own assigned training tasks.
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT src.`role_id`, mine_menu.`id`, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_menu` training_menu
  ON training_menu.`path` = 'controlled-file/training'
 AND training_menu.`deleted` = b'0'
JOIN `system_menu` mine_menu
  ON mine_menu.`path` = 'controlled-file/training-mine'
 AND mine_menu.`deleted` = b'0'
WHERE src.`menu_id` = training_menu.`id`
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = mine_menu.`id`
        AND existing.`deleted` = b'0'
  );
