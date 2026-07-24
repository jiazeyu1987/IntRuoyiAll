-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Restore the DCC controlled-file audit menu permission.
-- The backend audit API requires dcc:controlled-file:audit:query; this patch
-- makes the permission assignable and mirrors existing DCC browse-role coverage.

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6818, '文件审计', 'dcc:controlled-file:audit:query', 2, 13, 6800,
       'controlled-file/audit', 'ep:document-checked', 'dcc/controlled-file/audit/index',
       'DccControlledFileAudit', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` = 6818
       OR `path` = 'controlled-file/audit'
       OR `permission` = 'dcc:controlled-file:audit:query'
);

UPDATE `system_menu`
SET `permission` = 'dcc:controlled-file:audit:query',
    `component` = 'dcc/controlled-file/audit/index',
    `component_name` = 'DccControlledFileAudit',
    `status` = 0,
    `visible` = b'1',
    `update_time` = NOW(),
    `updater` = '1'
WHERE `path` = 'controlled-file/audit'
  AND `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT src.`role_id`, 6818, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
WHERE src.`menu_id` = 6807
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = 6818
        AND existing.`tenant_id` = src.`tenant_id`
        AND existing.`deleted` = b'0'
  );
