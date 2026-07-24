-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Restore the DCC electronic-signature management menu when runtime databases
-- missed the earlier seed patch but frontend/backend signature code already exists.

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6815, 'DCC电子签名管理', 'dcc:controlled-file:signature:manage', 2, 12, 6800,
       'controlled-file/signatures', 'ep:management', 'dcc/controlled-file/signatures/index',
       'DccControlledFileSignatures', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` = 6815 OR `path` = 'controlled-file/signatures'
);

-- Mirror the existing DCC management-role coverage from the directory-management menu.
-- This keeps the signature-management page visible to the same administrator roles
-- that already manage other DCC governance pages.
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT src.`role_id`, 6815, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
WHERE src.`menu_id` = 6801
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = 6815
        AND existing.`deleted` = b'0'
  );
