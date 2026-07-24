-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema,20260515_dcc_governance_split_menu,20260529_dcc_audit_menu_permission; type=menu; riskLevel=low
-- Add the DCC admin full-config package page under 文控中心 and mirror existing
-- 文控权限 role coverage so governance operators can import/export the package.

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6819, '文控管理员', 'dcc:controlled-file:category:manage', 2, 15, 6800,
       'controlled-file/admin', 'ep:upload-filled', 'dcc/controlled-file/admin/index',
       'DccControlledFileAdmin', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 6819
       OR `path` = 'controlled-file/admin'
);

UPDATE `system_menu`
SET `name` = '文控管理员',
    `permission` = 'dcc:controlled-file:category:manage',
    `parent_id` = 6800,
    `path` = 'controlled-file/admin',
    `icon` = 'ep:upload-filled',
    `component` = 'dcc/controlled-file/admin/index',
    `component_name` = 'DccControlledFileAdmin',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 6819
  AND `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT src.`role_id`, 6819, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_menu` source_menu
  ON source_menu.`path` = 'controlled-file/categories'
 AND source_menu.`deleted` = b'0'
WHERE src.`menu_id` = source_menu.`id`
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = 6819
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );
