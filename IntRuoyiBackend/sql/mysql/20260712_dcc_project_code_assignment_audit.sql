-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_dcc_product_catalog_database; type=schema; riskLevel=medium

CREATE TABLE IF NOT EXISTS `dcc_project_code_assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `assignment_no` VARCHAR(64) NOT NULL,
  `project_code_id` BIGINT NOT NULL,
  `scope_mode` VARCHAR(32) NOT NULL,
  `assignee_user_id` BIGINT NOT NULL,
  `assigned_by` BIGINT NOT NULL,
  `assigned_time` DATETIME NOT NULL,
  `expire_time` DATETIME NULL,
  `status` VARCHAR(32) NOT NULL,
  `assignment_reason` VARCHAR(512) NULL,
  `file_count` INT NOT NULL DEFAULT 0,
  `changed_file_count` INT NOT NULL DEFAULT 0,
  `changed_field_count` INT NOT NULL DEFAULT 0,
  `revoked_by` BIGINT NULL,
  `revoked_time` DATETIME NULL,
  `revoke_reason` VARCHAR(512) NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pc_assignment_no` (`tenant_id`, `assignment_no`, `deleted`),
  KEY `idx_dcc_pc_assignment_project_status` (`tenant_id`, `project_code_id`, `status`, `deleted`),
  KEY `idx_dcc_pc_assignment_assignee_status` (`tenant_id`, `assignee_user_id`, `status`, `deleted`),
  KEY `idx_dcc_pc_assignment_assigned_time` (`tenant_id`, `assigned_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC project code correction assignment';

CREATE TABLE IF NOT EXISTS `dcc_project_code_assignment_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT NOT NULL,
  `project_code_id` BIGINT NOT NULL,
  `controlled_file_id` BIGINT NOT NULL,
  `master_id` BIGINT NULL,
  `file_number_snapshot` VARCHAR(128) NULL,
  `file_name_snapshot` VARCHAR(512) NULL,
  `category_id_snapshot` BIGINT NULL,
  `directory_id_snapshot` BIGINT NULL,
  `initial_file_type_level1` VARCHAR(64) NULL,
  `initial_file_type_level2` VARCHAR(128) NULL,
  `initial_file_type_level3` VARCHAR(128) NULL,
  `initial_file_type_level4` VARCHAR(128) NULL,
  `initial_file_type_level5` VARCHAR(128) NULL,
  `changed` BIT NOT NULL DEFAULT b'0',
  `changed_field_count` INT NOT NULL DEFAULT 0,
  `last_changed_time` DATETIME NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_pc_assignment_file` (`tenant_id`, `assignment_id`, `controlled_file_id`, `deleted`),
  KEY `idx_dcc_pc_assignment_file_lookup` (`tenant_id`, `controlled_file_id`, `deleted`),
  KEY `idx_dcc_pc_assignment_project_file` (`tenant_id`, `project_code_id`, `controlled_file_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC project code assignment file snapshot';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_metadata_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT NULL,
  `project_code_id` BIGINT NULL,
  `controlled_file_id` BIGINT NOT NULL,
  `master_id` BIGINT NULL,
  `operator_user_id` BIGINT NOT NULL,
  `source` VARCHAR(32) NOT NULL,
  `request_id` VARCHAR(64) NULL,
  `change_reason` VARCHAR(512) NULL,
  `changed_field_count` INT NOT NULL DEFAULT 0,
  `before_snapshot_json` LONGTEXT NULL,
  `after_snapshot_json` LONGTEXT NULL,
  `changed_time` DATETIME NOT NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_metadata_change_assignment_time` (`tenant_id`, `assignment_id`, `changed_time`, `deleted`),
  KEY `idx_dcc_metadata_change_file_time` (`tenant_id`, `controlled_file_id`, `changed_time`, `deleted`),
  KEY `idx_dcc_metadata_change_operator_time` (`tenant_id`, `operator_user_id`, `changed_time`, `deleted`),
  KEY `idx_dcc_metadata_change_project_time` (`tenant_id`, `project_code_id`, `changed_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file metadata change group';

CREATE TABLE IF NOT EXISTS `dcc_controlled_file_metadata_change_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `change_id` BIGINT NOT NULL,
  `assignment_id` BIGINT NULL,
  `project_code_id` BIGINT NULL,
  `controlled_file_id` BIGINT NOT NULL,
  `operator_user_id` BIGINT NOT NULL,
  `field_name` VARCHAR(64) NOT NULL,
  `field_label` VARCHAR(64) NOT NULL,
  `old_value_text` VARCHAR(1024) NULL,
  `new_value_text` VARCHAR(1024) NULL,
  `old_value_json` LONGTEXT NULL,
  `new_value_json` LONGTEXT NULL,
  `changed_time` DATETIME NOT NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  `creator` VARCHAR(64) NULL,
  `updater` VARCHAR(64) NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dcc_metadata_change_item_change` (`tenant_id`, `change_id`, `deleted`),
  KEY `idx_dcc_metadata_change_item_field_time` (`tenant_id`, `field_name`, `changed_time`, `deleted`),
  KEY `idx_dcc_metadata_change_item_project_field_time` (`tenant_id`, `project_code_id`, `field_name`, `changed_time`, `deleted`),
  KEY `idx_dcc_metadata_change_item_operator_field_time` (`tenant_id`, `operator_user_id`, `field_name`, `changed_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC controlled file metadata field change item';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990220, '我的DCC修正', 'dcc:project-code-assignment:execute', 2, 9, 6800, 'controlled-file/project-code-assignments/mine', 'ep:edit-pen', 'dcc/controlled-file/project-code-assignments/mine/index', 'DccProjectCodeAssignmentMinePage', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990220 OR `path` = 'controlled-file/project-code-assignments/mine' OR `permission` = 'dcc:project-code-assignment:execute');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990221, '项目代码修正追溯', 'dcc:project-code-assignment:audit:query', 2, 13, 6800, 'controlled-file/project-code-assignment-audit', 'ep:operation', 'dcc/controlled-file/project-code-assignment-audit/index', 'DccProjectCodeAssignmentAuditPage', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990221 OR `path` = 'controlled-file/project-code-assignment-audit' OR `permission` = 'dcc:project-code-assignment:audit:query');

SET @dcc_project_code_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `path` = 'project-code'
      AND `deleted` = b'0'
    LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990222, 'DCC项目代码分配', 'dcc:project-code-assignment:assign', 3, 6, @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990222 OR `permission` = 'dcc:project-code-assignment:assign');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990223, 'DCC项目代码分配查询', 'dcc:project-code-assignment:query', 3, 7, @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990223 OR `permission` = 'dcc:project-code-assignment:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990224, 'DCC项目代码分配撤回', 'dcc:project-code-assignment:revoke', 3, 8, @dcc_project_code_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @dcc_project_code_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 990224 OR `permission` = 'dcc:project-code-assignment:revoke');

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT src.`role_id`, target_menu.`id`, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_menu` source_menu
  ON source_menu.`path` = 'controlled-file/categories'
 AND source_menu.`deleted` = b'0'
JOIN `system_menu` target_menu
  ON (
      target_menu.`path` = 'controlled-file/project-code-assignments/mine'
      OR target_menu.`permission` = 'dcc:project-code-assignment:execute'
 )
 AND target_menu.`deleted` = b'0'
WHERE src.`menu_id` = source_menu.`id`
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = target_menu.`id`
        AND existing.`deleted` = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `updater`, `tenant_id`)
SELECT src.`role_id`, target_menu.`id`, src.`creator`, src.`updater`, src.`tenant_id`
FROM `system_role_menu` src
JOIN `system_role` role_admin
  ON role_admin.`id` = src.`role_id`
 AND role_admin.`tenant_id` = src.`tenant_id`
 AND role_admin.`deleted` = b'0'
 AND role_admin.`code` IN ('super_admin', 'doc_control', 'wenkong')
JOIN `system_menu` source_menu
  ON source_menu.`path` = 'controlled-file/categories'
 AND source_menu.`deleted` = b'0'
JOIN `system_menu` target_menu
  ON (
      target_menu.`path` = 'controlled-file/project-code-assignment-audit'
      OR target_menu.`permission` IN (
          'dcc:project-code-assignment:assign',
          'dcc:project-code-assignment:query',
          'dcc:project-code-assignment:revoke',
          'dcc:project-code-assignment:audit:query'
      )
 )
 AND target_menu.`deleted` = b'0'
WHERE src.`menu_id` = source_menu.`id`
  AND src.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` existing
      WHERE existing.`role_id` = src.`role_id`
        AND existing.`menu_id` = target_menu.`id`
        AND existing.`deleted` = b'0'
  );
