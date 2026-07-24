-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260529_dcc_audit_menu_permission,20260712_dcc_project_code_assignment_audit; type=menu; riskLevel=low
-- Consolidate DCC audit, project-code trace, assignment execution, and training execution records
-- into a single visible DCC controlled-file log menu. Backend permissions and data tables remain.

SET NAMES utf8mb4;
SET @dcc_controlled_file_log_menu_id := 6818;
SET @dcc_controlled_file_audit_permission_id := 990225;
SET @dcc_project_code_audit_permission_id := 990226;
SET @dcc_project_code_execute_permission_id := 990227;

DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_controlled_file_log_source_roles`;
CREATE TEMPORARY TABLE `tmp_dcc_controlled_file_log_source_roles` AS
SELECT DISTINCT
       role_menu.`role_id`,
       role_menu.`tenant_id`,
       role_menu.`creator`,
       role_menu.`updater`
FROM `system_role_menu` role_menu
JOIN `system_menu` source_menu
  ON source_menu.`id` = role_menu.`menu_id`
WHERE role_menu.`deleted` = b'0'
  AND source_menu.`deleted` = b'0'
  AND (
      source_menu.`permission` IN (
          'dcc:controlled-file:audit:query',
          'dcc:project-code-assignment:audit:query',
          'dcc:project-code-assignment:execute'
      )
      OR source_menu.`path` IN (
          'controlled-file/audit',
          'controlled-file/project-code-assignment-audit',
          'controlled-file/project-code-assignments/mine'
      )
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @dcc_controlled_file_log_menu_id, '文控日志', 'dcc:controlled-file:log:query', 2, 13, 6800,
       'controlled-file/logs', 'ep:document-checked', 'dcc/controlled-file/logs/index',
       'DccControlledFileLogs', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` = @dcc_controlled_file_log_menu_id
       OR `path` = 'controlled-file/logs'
       OR `permission` = 'dcc:controlled-file:log:query'
);

UPDATE `system_menu`
SET `name` = '文控日志',
    `permission` = 'dcc:controlled-file:log:query',
    `type` = 2,
    `sort` = 13,
    `parent_id` = 6800,
    `path` = 'controlled-file/logs',
    `icon` = 'ep:document-checked',
    `component` = 'dcc/controlled-file/logs/index',
    `component_name` = 'DccControlledFileLogs',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `update_time` = NOW(),
    `updater` = '1'
WHERE `deleted` = b'0'
  AND (
      `id` = @dcc_controlled_file_log_menu_id
      OR `path` = 'controlled-file/audit'
      OR `permission` = 'dcc:controlled-file:log:query'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @dcc_controlled_file_audit_permission_id, 'DCC受控文件审计查询权限', 'dcc:controlled-file:audit:query', 3, 1, @dcc_controlled_file_log_menu_id,
       '', '', '', '', 0, b'0', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` = @dcc_controlled_file_audit_permission_id
       OR `permission` = 'dcc:controlled-file:audit:query'
);

UPDATE `system_menu`
SET `name` = 'DCC受控文件审计查询权限',
    `type` = 3,
    `sort` = 1,
    `parent_id` = @dcc_controlled_file_log_menu_id,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `visible` = b'0',
    `update_time` = NOW(),
    `updater` = '1'
WHERE `deleted` = b'0'
  AND `permission` = 'dcc:controlled-file:audit:query';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @dcc_project_code_audit_permission_id, 'DCC项目代码修正追溯查询权限', 'dcc:project-code-assignment:audit:query', 3, 2, @dcc_controlled_file_log_menu_id,
       '', '', '', '', 0, b'0', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` = @dcc_project_code_audit_permission_id
       OR `permission` = 'dcc:project-code-assignment:audit:query'
);

UPDATE `system_menu`
SET `name` = 'DCC项目代码修正追溯查询权限',
    `type` = 3,
    `sort` = 2,
    `parent_id` = @dcc_controlled_file_log_menu_id,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `visible` = b'0',
    `update_time` = NOW(),
    `updater` = '1'
WHERE `deleted` = b'0'
  AND (
      `permission` = 'dcc:project-code-assignment:audit:query'
      OR `path` = 'controlled-file/project-code-assignment-audit'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @dcc_project_code_execute_permission_id, 'DCC项目代码修正执行权限', 'dcc:project-code-assignment:execute', 3, 3, @dcc_controlled_file_log_menu_id,
       '', '', '', '', 0, b'0', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` = @dcc_project_code_execute_permission_id
       OR `permission` = 'dcc:project-code-assignment:execute'
);

UPDATE `system_menu`
SET `name` = 'DCC项目代码修正执行权限',
    `type` = 3,
    `sort` = 3,
    `parent_id` = @dcc_controlled_file_log_menu_id,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = '',
    `visible` = b'0',
    `update_time` = NOW(),
    `updater` = '1'
WHERE `deleted` = b'0'
  AND (
      `permission` = 'dcc:project-code-assignment:execute'
      OR `path` = 'controlled-file/project-code-assignments/mine'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT source_role.`role_id`, @dcc_controlled_file_log_menu_id, source_role.`creator`, source_role.`updater`, source_role.`tenant_id`
FROM `tmp_dcc_controlled_file_log_source_roles` source_role
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing
    WHERE existing.`role_id` = source_role.`role_id`
      AND existing.`menu_id` = @dcc_controlled_file_log_menu_id
      AND existing.`tenant_id` = source_role.`tenant_id`
      AND existing.`deleted` = b'0'
);

DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_controlled_file_log_source_roles`;

DROP PROCEDURE IF EXISTS `ensure_dcc_controlled_file_logs_consolidation`;
DELIMITER //
CREATE PROCEDURE `ensure_dcc_controlled_file_logs_consolidation`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM `system_menu`
        WHERE `id` = @dcc_controlled_file_log_menu_id
          AND `name` = '文控日志'
          AND `permission` = 'dcc:controlled-file:log:query'
          AND `type` = 2
          AND `parent_id` = 6800
          AND `path` = 'controlled-file/logs'
          AND `component` = 'dcc/controlled-file/logs/index'
          AND `component_name` = 'DccControlledFileLogs'
          AND `visible` = b'1'
          AND `deleted` = b'0'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing DCC controlled-file log menu 6818';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM `system_menu`
        WHERE `deleted` = b'0'
          AND `type` = 2
          AND (
              `path` IN (
                  'controlled-file/audit',
                  'controlled-file/project-code-assignment-audit',
                  'controlled-file/project-code-assignments/mine'
              )
              OR `component` IN (
                  'dcc/controlled-file/audit/index',
                  'dcc/controlled-file/project-code-assignment-audit/index',
                  'dcc/controlled-file/project-code-assignments/mine/index'
              )
          )
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Legacy DCC audit or assignment page menu still visible';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM `system_menu`
        WHERE `permission` = 'dcc:controlled-file:audit:query'
          AND `type` = 3
          AND `parent_id` = @dcc_controlled_file_log_menu_id
          AND `deleted` = b'0'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing legacy DCC controlled-file audit permission';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM `system_menu`
        WHERE `permission` = 'dcc:project-code-assignment:audit:query'
          AND `type` = 3
          AND `parent_id` = @dcc_controlled_file_log_menu_id
          AND `deleted` = b'0'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing legacy DCC project-code assignment audit permission';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM `system_menu`
        WHERE `permission` = 'dcc:project-code-assignment:execute'
          AND `type` = 3
          AND `parent_id` = @dcc_controlled_file_log_menu_id
          AND `deleted` = b'0'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing legacy DCC project-code assignment execute permission';
    END IF;
END//
DELIMITER ;

CALL `ensure_dcc_controlled_file_logs_consolidation`();
DROP PROCEDURE IF EXISTS `ensure_dcc_controlled_file_logs_consolidation`;
