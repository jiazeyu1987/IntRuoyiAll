-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 排产 P7：排产员工作台菜单

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
    `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
)
SELECT 5590, '排产员工作台', 'mes:pro-scheduler-workbench:query', 2, 0, 5700,
       'scheduler-workbench', 'ep:monitor', 'mes/pro/scheduler-workbench/index',
       'MesProSchedulerWorkbench', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5590);

UPDATE `system_menu`
SET `name` = '排产员工作台',
    `permission` = 'mes:pro-scheduler-workbench:query',
    `type` = 2,
    `sort` = 0,
    `parent_id` = 5700,
    `path` = 'scheduler-workbench',
    `icon` = 'ep:monitor',
    `component` = 'mes/pro/scheduler-workbench/index',
    `component_name` = 'MesProSchedulerWorkbench',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `deleted` = b'0',
    `updater` = 'system',
    `update_time` = NOW()
WHERE `id` = 5590;

INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 111, 5590, 'system', NOW(), 'system', NOW(), b'0', 122
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu`
    WHERE `role_id` = 111
      AND `menu_id` = 5590
      AND `tenant_id` = 122
      AND `deleted` = b'0'
);

INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 111, 5985, 'system', NOW(), 'system', NOW(), b'0', 122
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5985 AND `deleted` = b'0')
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu`
    WHERE `role_id` = 111
      AND `menu_id` = 5985
      AND `tenant_id` = 122
      AND `deleted` = b'0'
);

INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 1, 5590, 'system', NOW(), 'system', NOW(), b'0', 1
WHERE EXISTS (SELECT 1 FROM `system_role` WHERE `id` = 1 AND `deleted` = b'0')
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu`
    WHERE `role_id` = 1
      AND `menu_id` = 5590
      AND `tenant_id` = 1
      AND `deleted` = b'0'
);
