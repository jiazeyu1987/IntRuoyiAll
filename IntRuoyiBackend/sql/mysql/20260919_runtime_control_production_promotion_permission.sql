-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260523_infra_runtime_control_menu; type=permission; riskLevel=medium
-- 为已测试发布包正式晋级按钮增加独立生产权限。
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
SELECT 900104, '运行控制台发布正式服', 'infra:runtime-control:promote-prod', 3, 4, @runtime_control_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @runtime_control_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900104)
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `parent_id` = @runtime_control_menu_id
        AND `permission` = 'infra:runtime-control:promote-prod'
        AND `type` = 3
  );
