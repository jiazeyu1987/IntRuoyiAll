-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260712_dcc_project_code_assignment_audit; type=menu; riskLevel=medium
-- Separate DCC operation capabilities from explicit global data visibility.

SET NAMES utf8mb4;

SET @dcc_project_code_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `path` = 'project-code'
      AND `deleted` = b'0'
    LIMIT 1
);

SET @dcc_controlled_file_browser_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `permission` = 'dcc:controlled-file:query'
      AND `deleted` = b'0'
    ORDER BY `id`
    LIMIT 1
);

DROP PROCEDURE IF EXISTS add_dcc_explicit_data_scope_permissions;
DELIMITER //
CREATE PROCEDURE add_dcc_explicit_data_scope_permissions()
BEGIN
  IF @dcc_project_code_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing DCC project-code menu';
  END IF;
  IF @dcc_controlled_file_browser_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing DCC controlled-file query menu';
  END IF;

  INSERT INTO `system_menu`
  (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT CONVERT(UNHEX('444343e9a1b9e79baee4bba3e7a081e585a8e9878fe88c83e59bb4') USING utf8mb4),
         'dcc:project-code:scope:all', 3, 90,
         @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'codex', NOW(), 'codex', NOW(), b'0'
  WHERE NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `permission` = 'dcc:project-code:scope:all' AND `deleted` = b'0'
  );

  INSERT INTO `system_menu`
  (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT CONVERT(UNHEX('444343e58f97e68ea7e69687e4bbb6e585a8e9878fe88c83e59bb4') USING utf8mb4),
         'dcc:controlled-file:scope:all', 3, 91,
         @dcc_controlled_file_browser_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'codex', NOW(), 'codex', NOW(), b'0'
  WHERE NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `permission` = 'dcc:controlled-file:scope:all' AND `deleted` = b'0'
  );

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
  SELECT role_admin.`id`, scope_menu.`id`, 'codex', 'codex', role_admin.`tenant_id`
  FROM `system_role` role_admin
  JOIN `system_menu` scope_menu
    ON scope_menu.`permission` IN ('dcc:project-code:scope:all', 'dcc:controlled-file:scope:all')
   AND scope_menu.`deleted` = b'0'
  WHERE role_admin.`code` IN ('super_admin', 'admin')
    AND role_admin.`deleted` = b'0'
    AND NOT EXISTS (
        SELECT 1
        FROM `system_role_menu` existing
        WHERE existing.`role_id` = role_admin.`id`
          AND existing.`menu_id` = scope_menu.`id`
          AND existing.`tenant_id` = role_admin.`tenant_id`
          AND existing.`deleted` = b'0'
    );
END//
DELIMITER ;

CALL add_dcc_explicit_data_scope_permissions();
DROP PROCEDURE IF EXISTS add_dcc_explicit_data_scope_permissions;
