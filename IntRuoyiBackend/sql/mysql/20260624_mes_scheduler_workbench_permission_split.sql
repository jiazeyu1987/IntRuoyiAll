-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260610_mes_scheduler_workbench_p7; type=data; riskLevel=medium
-- MES 排产员工作台：查询、设置写入、冒烟测试权限拆分。

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
    `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
)
SELECT 900170, '排产员工作台设置保存', 'mes:pro-scheduler-workbench:update', 3, 10, 5590,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5590 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900170);

UPDATE `system_menu`
SET `name` = '排产员工作台设置保存',
    `permission` = 'mes:pro-scheduler-workbench:update',
    `type` = 3,
    `sort` = 10,
    `parent_id` = 5590,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `deleted` = b'0',
    `updater` = 'system',
    `update_time` = NOW()
WHERE `id` = 900170;

INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
    `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
)
SELECT 900171, '排产员工作台冒烟测试', 'mes:pro-scheduler-workbench:smoke-test', 3, 20, 5590,
       '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5590 AND `deleted` = b'0')
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900171);

UPDATE `system_menu`
SET `name` = '排产员工作台冒烟测试',
    `permission` = 'mes:pro-scheduler-workbench:smoke-test',
    `type` = 3,
    `sort` = 20,
    `parent_id` = 5590,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `deleted` = b'0',
    `updater` = 'system',
    `update_time` = NOW()
WHERE `id` = 900171;

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
WHERE `menu`.`id` IN (900170, 900171)
  AND `menu`.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` `existing`
    WHERE `existing`.`role_id` = `role`.`role_id`
      AND `existing`.`menu_id` = `menu`.`id`
      AND `existing`.`tenant_id` = `role`.`tenant_id`
      AND `existing`.`deleted` = b'0'
);

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', 900170),
    `updater` = 'system',
    `update_time` = NOW()
WHERE JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5590' AS JSON), '$')
  AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900170' AS JSON), '$');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(CAST(`menu_ids` AS JSON), '$', 900171),
    `updater` = 'system',
    `update_time` = NOW()
WHERE JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5590' AS JSON), '$')
  AND NOT JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900171' AS JSON), '$');
