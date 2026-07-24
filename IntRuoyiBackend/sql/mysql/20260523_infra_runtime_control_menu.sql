-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- 运行控制台菜单与权限
SET @runtime_control_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'infra:runtime-control:query'
      AND `path` = 'runtime-control'
      AND `component` = 'infra/runtime-control/index'
    ORDER BY `id`
    LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900100, '运行控制台', 'infra:runtime-control:query', 2, 4, 2740, 'runtime-control', 'ep:monitor', 'infra/runtime-control/index', 'InfraRuntimeControl', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @runtime_control_menu_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900100);

SET @runtime_control_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'infra:runtime-control:query'
      AND `path` = 'runtime-control'
      AND `component` = 'infra/runtime-control/index'
    ORDER BY `id`
    LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900101, '运行控制台查询', 'infra:runtime-control:query', 3, 1, @runtime_control_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @runtime_control_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900101)
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `parent_id` = @runtime_control_menu_id
        AND `permission` = 'infra:runtime-control:query'
        AND `type` = 3
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900102, '运行控制台重启', 'infra:runtime-control:restart', 3, 2, @runtime_control_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @runtime_control_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900102)
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `parent_id` = @runtime_control_menu_id
        AND `permission` = 'infra:runtime-control:restart'
        AND `type` = 3
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900103, '运行控制台运维操作', 'infra:runtime-control:operate', 3, 3, @runtime_control_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @runtime_control_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900103)
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `parent_id` = @runtime_control_menu_id
        AND `permission` = 'infra:runtime-control:operate'
        AND `type` = 3
  );
