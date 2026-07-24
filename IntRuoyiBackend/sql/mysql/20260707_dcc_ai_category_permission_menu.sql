-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260626_dcc_basic_data_global_submenu; type=data; riskLevel=medium
-- DCC 项目代码关联文档 AI 分类权限修复

BEGIN;

SET @dcc_project_code_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`permission` = 'dcc:project-code:query' OR `path` = 'project-code')
    ORDER BY CASE WHEN `permission` = 'dcc:project-code:query' THEN 0 ELSE 1 END, `id`
    LIMIT 1
);

SET @dcc_controlled_file_browser_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'dcc:controlled-file:query'
      AND `path` = 'controlled-file/browser'
    ORDER BY `id`
    LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'DCC项目代码编辑', 'dcc:project-code:update', 3, 2, @dcc_project_code_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', 'dcc-ai-category', NOW(), 'dcc-ai-category', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` = 'dcc:project-code:update'
  );

UPDATE `system_menu`
SET `name` = 'DCC项目代码编辑',
    `parent_id` = @dcc_project_code_menu_id,
    `type` = 3,
    `sort` = 2,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'dcc-ai-category',
    `update_time` = NOW()
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND `deleted` = b'0'
  AND `permission` = 'dcc:project-code:update';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'DCC受控文件编辑', 'dcc:controlled-file:update', 3, 3, @dcc_controlled_file_browser_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', 'dcc-ai-category', NOW(), 'dcc-ai-category', NOW(), b'0'
WHERE @dcc_controlled_file_browser_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` = 'dcc:controlled-file:update'
  );

UPDATE `system_menu`
SET `name` = 'DCC受控文件编辑',
    `parent_id` = @dcc_controlled_file_browser_menu_id,
    `type` = 3,
    `sort` = 3,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'dcc-ai-category',
    `update_time` = NOW()
WHERE @dcc_controlled_file_browser_menu_id IS NOT NULL
  AND `deleted` = b'0'
  AND `permission` = 'dcc:controlled-file:update';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT src.`role_id`, target_menu.`id`, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_menu` source_menu
  ON source_menu.`path` = 'controlled-file/categories'
 AND source_menu.`deleted` = b'0'
JOIN `system_menu` target_menu
  ON target_menu.`permission` IN (
      'dcc:project-code:update',
      'dcc:controlled-file:update'
  )
 AND target_menu.`deleted` = b'0'
WHERE src.`menu_id` = source_menu.`id`
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = target_menu.`id`
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );

COMMIT;
