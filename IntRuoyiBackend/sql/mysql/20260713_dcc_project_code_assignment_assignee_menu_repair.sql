-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260712_dcc_project_code_assignment_audit; type=menu; riskLevel=low
-- Repair DCC project-code assignment assignee menu visibility without granting project-code management permissions.

-- Existing assignments created before the execute-permission guard may point to users whose roles
-- do not yet have the controlled "my DCC correction" entry. Grant only that task entry to the
-- assignee user's current roles; the service still enforces assignment ownership and file scope.
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT user_role.`role_id`, target_menu.`id`, '1', '1', assignment.`tenant_id`
FROM `dcc_project_code_assignment` assignment
JOIN `system_user_role` user_role
  ON user_role.`user_id` = assignment.`assignee_user_id`
 AND user_role.`deleted` = b'0'
JOIN `system_role` role_item
  ON role_item.`id` = user_role.`role_id`
 AND role_item.`tenant_id` = assignment.`tenant_id`
 AND role_item.`deleted` = b'0'
 AND role_item.`status` = 0
JOIN `system_menu` target_menu
  ON target_menu.`path` = 'controlled-file/project-code-assignments/mine'
 AND target_menu.`deleted` = b'0'
WHERE assignment.`deleted` = b'0'
  AND assignment.`status` = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = user_role.`role_id`
        AND existing.`menu_id` = target_menu.`id`
        AND existing.`deleted` = b'0'
  );

-- A leaf menu is not visible unless its root menu is also in the login permission tree.
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT leaf_grant.`role_id`, root_menu.`id`, leaf_grant.`creator`, leaf_grant.`updater`, leaf_grant.`tenant_id`
FROM `system_role_menu` leaf_grant
JOIN `system_menu` leaf_menu
  ON leaf_menu.`id` = leaf_grant.`menu_id`
 AND leaf_menu.`path` = 'controlled-file/project-code-assignments/mine'
 AND leaf_menu.`deleted` = b'0'
JOIN `system_menu` root_menu
  ON root_menu.`path` = '/dcc'
 AND root_menu.`deleted` = b'0'
WHERE leaf_grant.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = leaf_grant.`role_id`
        AND existing.`menu_id` = root_menu.`id`
        AND existing.`deleted` = b'0'
  );

-- Repair roles that already have the DCC project-code leaf but missed the /mdm parent,
-- which makes the existing DCC project-code tab disappear in the dynamic route tree.
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT DISTINCT leaf_grant.`role_id`, parent_menu.`id`, leaf_grant.`creator`, leaf_grant.`updater`, leaf_grant.`tenant_id`
FROM `system_role_menu` leaf_grant
JOIN `system_menu` leaf_menu
  ON leaf_menu.`id` = leaf_grant.`menu_id`
 AND leaf_menu.`path` = 'project-code'
 AND leaf_menu.`deleted` = b'0'
JOIN `system_menu` parent_menu
  ON parent_menu.`path` = '/mdm'
 AND parent_menu.`deleted` = b'0'
WHERE leaf_grant.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = leaf_grant.`role_id`
        AND existing.`menu_id` = parent_menu.`id`
        AND existing.`deleted` = b'0'
  );
