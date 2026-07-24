-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260721_admin_full_scope_role_standardization; type=schema; riskLevel=medium
SET NAMES utf8mb4;

ALTER TABLE `system_tenant_package` MODIFY COLUMN `menu_ids` LONGTEXT NOT NULL;

CREATE TABLE IF NOT EXISTS `system_codex_test_case` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `method_text` text NOT NULL,
  `test_data_text` text NULL,
  `default_execution_mode` varchar(16) NOT NULL,
  `parallel_safe` bit NOT NULL DEFAULT b'0',
  `status` varchar(16) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_codex_test_case_tenant_name_deleted` (`tenant_id`, `name`, `deleted`),
  KEY `idx_system_codex_test_case_tenant_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Codex 自动测试项';

CREATE TABLE IF NOT EXISTS `system_codex_test_checkpoint` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `case_id` bigint NOT NULL,
  `sort` int NOT NULL,
  `name` varchar(128) NOT NULL,
  `expected_text` text NOT NULL,
  `severity` varchar(16) NOT NULL DEFAULT 'MAJOR',
  `remark` varchar(512) NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_codex_test_checkpoint_case_sort_deleted` (`case_id`, `sort`, `deleted`),
  KEY `idx_system_codex_test_checkpoint_tenant_case` (`tenant_id`, `case_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Codex 自动测试检查点';

CREATE TABLE IF NOT EXISTS `system_codex_test_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_tenant_id` bigint NOT NULL,
  `execution_mode` varchar(16) NOT NULL,
  `status` varchar(16) NOT NULL,
  `requested_by` bigint NOT NULL,
  `runner_session_id` bigint NULL,
  `started_at` datetime NULL,
  `finished_at` datetime NULL,
  `summary` text NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_system_codex_test_execution_tenant_status` (`tenant_id`, `status`, `deleted`),
  KEY `idx_system_codex_test_execution_target_tenant` (`target_tenant_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Codex 自动测试执行批次';

CREATE TABLE IF NOT EXISTS `system_codex_test_execution_case` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `execution_id` bigint NOT NULL,
  `case_id` bigint NOT NULL,
  `case_name_snapshot` varchar(128) NOT NULL,
  `method_text_snapshot` text NOT NULL,
  `test_data_text_snapshot` text NULL,
  `checkpoint_count` int NOT NULL,
  `status` varchar(16) NOT NULL,
  `runner_session_id` bigint NULL,
  `claim_time` datetime NULL,
  `started_at` datetime NULL,
  `finished_at` datetime NULL,
  `failure_reason` text NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_system_codex_test_execution_case_execution_status` (`execution_id`, `status`, `deleted`),
  KEY `idx_system_codex_test_execution_case_runner_status` (`runner_session_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Codex 自动测试项执行快照';

CREATE TABLE IF NOT EXISTS `system_codex_test_checkpoint_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `execution_case_id` bigint NOT NULL,
  `checkpoint_sort` int NOT NULL,
  `checkpoint_name_snapshot` varchar(128) NOT NULL,
  `expected_text_snapshot` text NOT NULL,
  `actual_text` text NULL,
  `status` varchar(16) NOT NULL,
  `mismatch_description` text NULL,
  `screenshot_artifact_id` bigint NULL,
  `completed_at` datetime NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_codex_test_checkpoint_result_case_sort_deleted` (`execution_case_id`, `checkpoint_sort`, `deleted`),
  KEY `idx_system_codex_test_checkpoint_result_case_status` (`execution_case_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Codex 自动测试检查点结果';

CREATE TABLE IF NOT EXISTS `system_codex_test_artifact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `execution_id` bigint NOT NULL,
  `execution_case_id` bigint NOT NULL,
  `checkpoint_result_id` bigint NULL,
  `artifact_type` varchar(32) NOT NULL,
  `relative_temp_path` varchar(512) NOT NULL,
  `content_type` varchar(128) NOT NULL,
  `size_bytes` bigint NOT NULL,
  `sha256` varchar(64) NOT NULL,
  `expires_at` datetime NOT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_system_codex_test_artifact_case_expire` (`execution_case_id`, `expires_at`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Codex 自动测试临时证据文件';

CREATE TABLE IF NOT EXISTS `system_codex_test_runner_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `runner_name` varchar(128) NOT NULL,
  `status` varchar(16) NOT NULL,
  `capabilities_json` text NOT NULL,
  `max_parallelism` int NOT NULL,
  `playwright_version` varchar(64) NULL,
  `codex_version` varchar(64) NULL,
  `last_heartbeat_time` datetime NOT NULL,
  `current_running_count` int NOT NULL DEFAULT 0,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_system_codex_test_runner_session_online` (`status`, `last_heartbeat_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Codex 自动测试 Runner 会话';

DROP PROCEDURE IF EXISTS ensure_system_codex_test_management;
DELIMITER //
CREATE PROCEDURE ensure_system_codex_test_management()
BEGIN
  DECLARE v_menu_id BIGINT DEFAULT NULL;
  DECLARE v_codex_test_admin_role_id BIGINT DEFAULT NULL;
  DECLARE v_menu_category_id BIGINT DEFAULT NULL;
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
     WHERE `id` = 1 AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing system management root menu';
  END IF;

  INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    '测试管理', 'system:codex-test:query', 2, 100, 1, 'codex-test-management', 'ep:aim',
    'system/codex-test-management/index', 'SystemCodexTestManagement', 0, b'1', b'1', b'1',
    'codex', NOW(), 'codex', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
     WHERE `permission` = 'system:codex-test:query' AND `deleted` = b'0'
  );

  SELECT `id` INTO v_menu_id
    FROM `system_menu`
   WHERE `permission` = 'system:codex-test:query'
     AND `deleted` = b'0'
   ORDER BY `id`
   LIMIT 1;

  IF v_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing codex test management menu';
  END IF;

  UPDATE `system_menu`
     SET `name` = '测试管理',
         `type` = 2,
         `sort` = 100,
         `parent_id` = 1,
         `path` = 'codex-test-management',
         `icon` = 'ep:aim',
         `component` = 'system/codex-test-management/index',
         `component_name` = 'SystemCodexTestManagement',
         `status` = 0,
         `visible` = b'1',
         `keep_alive` = b'1',
         `always_show` = b'1',
         `updater` = 'codex',
         `update_time` = NOW()
   WHERE `id` = v_menu_id;

  INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT `seed`.`name`, `seed`.`permission`, 3, `seed`.`sort`, v_menu_id, '', '',
         '', '', 0, b'1', b'1', b'1', 'codex', NOW(), 'codex', NOW(), b'0'
    FROM (
      SELECT '新增测试项' AS `name`, 'system:codex-test:create' AS `permission`, 1 AS `sort`
      UNION ALL SELECT '修改测试项', 'system:codex-test:update', 2
      UNION ALL SELECT '删除测试项', 'system:codex-test:delete', 3
      UNION ALL SELECT '执行测试项', 'system:codex-test:execute', 4
      UNION ALL SELECT '取消执行', 'system:codex-test:cancel', 5
      UNION ALL SELECT '查看失败证据', 'system:codex-test:artifact', 6
    ) AS `seed`
   WHERE NOT EXISTS (
     SELECT 1 FROM `system_menu` AS `existing`
      WHERE `existing`.`permission` = `seed`.`permission`
        AND `existing`.`deleted` = b'0'
   );

  UPDATE `system_menu`
     SET `parent_id` = v_menu_id,
         `type` = 3,
         `status` = 0,
         `updater` = 'codex',
         `update_time` = NOW()
   WHERE `permission` IN (
     'system:codex-test:create',
     'system:codex-test:update',
     'system:codex-test:delete',
     'system:codex-test:execute',
     'system:codex-test:cancel',
     'system:codex-test:artifact'
   )
     AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_test_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_codex_test_menu_ids` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_codex_test_menu_ids` (`menu_id`)
  SELECT `id`
    FROM `system_menu`
   WHERE `permission` IN (
     'system:codex-test:query',
     'system:codex-test:create',
     'system:codex-test:update',
     'system:codex-test:delete',
     'system:codex-test:execute',
     'system:codex-test:cancel',
     'system:codex-test:artifact'
   )
     AND `status` = 0
     AND `deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_codex_test_menu_ids`) <> 7 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or duplicated codex test management permissions';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_tenant_package` AS `package`
     WHERE `package`.`deleted` = b'0'
       AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge codex test menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_test_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_codex_test_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_codex_test_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package`.`id`, CAST(`existing_menu`.`menu_id` AS UNSIGNED)
    FROM `system_tenant_package` AS `package`
    JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` bigint PATH '$')
    ) AS `existing_menu`
   WHERE `package`.`deleted` = b'0'
     AND JSON_VALID(`package`.`menu_ids`);

  INSERT IGNORE INTO `tmp_codex_test_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package`.`id`, `menu`.`menu_id`
    FROM `system_tenant_package` AS `package`
    CROSS JOIN `tmp_codex_test_menu_ids` AS `menu`
   WHERE `package`.`deleted` = b'0'
     AND JSON_VALID(`package`.`menu_ids`)
     AND JSON_CONTAINS(`package`.`menu_ids`, CAST('1' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_codex_test_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'codex',
      `package`.`update_time` = NOW();

  SELECT `id` INTO v_menu_category_id
    FROM `system_role_category`
   WHERE `code` = 'menu'
     AND `tenant_id` = 1
     AND `deleted` = b'0'
   ORDER BY `id`
   LIMIT 1;

  IF v_menu_category_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 menu role category';
  END IF;

  SELECT `id` INTO v_codex_test_admin_role_id
    FROM `system_role`
   WHERE `code` = 'codex_test_admin'
     AND `tenant_id` = 1
   ORDER BY `deleted`, `id`
   LIMIT 1;

  IF v_codex_test_admin_role_id IS NULL THEN
    INSERT INTO `system_role` (
      `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`,
      `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
    )
    VALUES (
      '测试管理员', 'codex_test_admin', 910320, v_menu_category_id, 1, '',
      0, 2, '测试管理角色；可维护自然语言测试项并发起、查看 Codex Playwright 自动测试。', 'codex', NOW(), 'codex', NOW(), b'0', 1
    );
    SET v_codex_test_admin_role_id = LAST_INSERT_ID();
  END IF;

  UPDATE `system_role`
     SET `name` = '测试管理员',
         `sort` = 910320,
         `category_id` = v_menu_category_id,
         `data_scope` = 1,
         `data_scope_dept_ids` = '',
         `status` = 0,
         `type` = 2,
         `remark` = '测试管理角色；可维护自然语言测试项并发起、查看 Codex Playwright 自动测试。',
         `updater` = 'codex',
         `update_time` = NOW(),
         `deleted` = b'0',
         `tenant_id` = 1
   WHERE `id` = v_codex_test_admin_role_id;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_codex_test_menu_ids` AS `menu`
    ON `menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`tenant_id` = 1,
         `role_menu`.`updater` = 'codex',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`role_id` = v_codex_test_admin_role_id;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT v_codex_test_admin_role_id, `menu_id`, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM `tmp_codex_test_menu_ids` AS `menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = v_codex_test_admin_role_id
        AND `existing`.`menu_id` = `menu`.`menu_id`
        AND `existing`.`deleted` = b'0'
   );

  SELECT `id` INTO v_admin_user_id
    FROM `system_users`
   WHERE `username` = 'admin'
     AND `tenant_id` = 1
     AND `status` = 0
     AND `deleted` = b'0'
   ORDER BY `id`
   LIMIT 1;

  IF v_admin_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled tenant 1 admin user';
  END IF;

  UPDATE `system_user_role`
     SET `deleted` = b'0',
         `tenant_id` = 1,
         `updater` = 'codex',
         `update_time` = NOW()
   WHERE `user_id` = v_admin_user_id
     AND `role_id` = v_codex_test_admin_role_id;

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT v_admin_user_id, v_codex_test_admin_role_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM DUAL
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_user_role` AS `existing`
      WHERE `existing`.`user_id` = v_admin_user_id
        AND `existing`.`role_id` = v_codex_test_admin_role_id
        AND `existing`.`deleted` = b'0'
   );

  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_test_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_test_menu_ids`;
END//
DELIMITER ;

CALL ensure_system_codex_test_management();

DROP PROCEDURE IF EXISTS ensure_system_codex_test_management;
