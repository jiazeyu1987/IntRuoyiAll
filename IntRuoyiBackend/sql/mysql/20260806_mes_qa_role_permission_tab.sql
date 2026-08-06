-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260804_mes_edhr_qa_menu; type=menu; riskLevel=medium
-- Ensure QA owns the QA regulation tab permission; admin receives QA by user-role membership, not by broad admin-menu bypass.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_mes_qa_role_permission_tab_20260806;
DELIMITER //
CREATE PROCEDURE ensure_mes_qa_role_permission_tab_20260806()
BEGIN
  IF EXISTS (
    SELECT 1
      FROM `system_tenant_package`
     WHERE `deleted` = b'0'
       AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot assign QA role';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900434
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled QA regulation menu 900434';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900434
       AND `deleted` = b'0'
       AND NOT (
         `name` = 'QA'
         AND `path` = '/mes/pro/process-pool/qa-regulation'
         AND `component` = 'mes/pro/processpool/QaRegulationPage'
         AND `component_name` = 'MesProProcessPoolQaRegulation'
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QA menu 900434 route/component mismatch';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_qa_role_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_mes_qa_role_target_tenant` (
    `tenant_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_mes_qa_role_target_tenant` (`tenant_id`)
  SELECT DISTINCT `tenant`.`id`
    FROM `system_tenant` AS `tenant`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `tenant`.`package_id`
     AND `package`.`deleted` = b'0'
     AND JSON_VALID(`package`.`menu_ids`)
   WHERE `tenant`.`deleted` = b'0'
     AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900434' AS JSON), '$');

  INSERT IGNORE INTO `tmp_mes_qa_role_target_tenant` (`tenant_id`)
  SELECT 1 AS `tenant_id`
    FROM `system_tenant` AS `tenant`
   WHERE `tenant`.`id` = 1
     AND `tenant`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_qa_role_target_tenant`) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant package with QA menu 900434';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_qa_role_target_tenant` AS `target_tenant`
      LEFT JOIN `system_role_category` AS `category`
        ON `category`.`tenant_id` = `target_tenant`.`tenant_id`
       AND `category`.`code` = 'menu'
       AND `category`.`status` = 0
       AND `category`.`deleted` = b'0'
     WHERE `category`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing menu role category for QA role tenant';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
      JOIN `tmp_mes_qa_role_target_tenant` AS `target_tenant`
        ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
     WHERE `role`.`code` = 'qa'
     GROUP BY `role`.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate QA role code in target tenant';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_users` AS `user`
     WHERE `user`.`username` = 'admin'
       AND `user`.`tenant_id` = 1
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 admin user';
  END IF;

  UPDATE `system_menu`
     SET `permission` = 'mes:qa-inspection-regulation:query',
         `updater` = 'mes-qa-role-permission',
         `update_time` = NOW()
   WHERE `id` = 900434
     AND `deleted` = b'0';

  INSERT INTO `system_role` (
    `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`,
    `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`,
    `deleted`, `tenant_id`
  )
  SELECT
    'QA',
    'qa',
    900434,
    `category`.`id`,
    1,
    '',
    0,
    2,
    'QA 规程配置页签、发布和选线权限',
    'mes-qa-role-permission',
    NOW(),
    'mes-qa-role-permission',
    NOW(),
    b'0',
    `target_tenant`.`tenant_id`
    FROM `tmp_mes_qa_role_target_tenant` AS `target_tenant`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `target_tenant`.`tenant_id`
     AND `category`.`code` = 'menu'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role` AS `existing_role`
      WHERE `existing_role`.`tenant_id` = `target_tenant`.`tenant_id`
        AND `existing_role`.`code` = 'qa'
   );

  UPDATE `system_role` AS `role`
    JOIN `tmp_mes_qa_role_target_tenant` AS `target_tenant`
      ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `role`.`tenant_id`
     AND `category`.`code` = 'menu'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
     SET `role`.`name` = 'QA',
         `role`.`sort` = 900434,
         `role`.`category_id` = `category`.`id`,
         `role`.`data_scope` = 1,
         `role`.`data_scope_dept_ids` = '',
         `role`.`status` = 0,
         `role`.`type` = 2,
         `role`.`remark` = 'QA 规程配置页签、发布和选线权限',
         `role`.`updater` = 'mes-qa-role-permission',
         `role`.`update_time` = NOW(),
         `role`.`deleted` = b'0'
   WHERE `role`.`code` = 'qa';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_qa_role`;
  CREATE TEMPORARY TABLE `tmp_mes_qa_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_qa_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
    JOIN `tmp_mes_qa_role_target_tenant` AS `target_tenant`
      ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
   WHERE `role`.`code` = 'qa'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_qa_role`) <> (SELECT COUNT(*) FROM `tmp_mes_qa_role_target_tenant`) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QA role resolution failed for target tenants';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_qa_role_permission_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_qa_role_permission_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_qa_role_permission_menu` (`menu_id`)
  SELECT 900434 AS `menu_id`
  UNION ALL
  SELECT 5631 AS `menu_id`
  UNION ALL
  SELECT 5633 AS `menu_id`;

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_qa_role_permission_menu` AS `expected_menu`
      LEFT JOIN `system_menu` AS `menu`
        ON `menu`.`id` = `expected_menu`.`menu_id`
       AND `menu`.`status` = 0
       AND `menu`.`deleted` = b'0'
     WHERE `menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing QA role permission menu';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `system_role` AS `role`
      ON `role`.`id` = `role_menu`.`role_id`
     AND `role`.`tenant_id` = `role_menu`.`tenant_id`
     AND `role`.`deleted` = b'0'
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'mes-qa-role-permission',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`menu_id` = 900434
     AND `role_menu`.`deleted` = b'0'
     AND `role`.`code` <> 'qa';

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_mes_qa_role` AS `qa_role`
      ON `qa_role`.`role_id` = `role_menu`.`role_id`
     AND `qa_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_mes_qa_role_permission_menu` AS `expected_menu`
      ON `expected_menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'mes-qa-role-permission',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`role_id` = `qa_role`.`role_id`;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `qa_role`.`role_id`,
    `expected_menu`.`menu_id`,
    'mes-qa-role-permission',
    NOW(),
    'mes-qa-role-permission',
    NOW(),
    b'0',
    `qa_role`.`tenant_id`
    FROM `tmp_mes_qa_role` AS `qa_role`
    CROSS JOIN `tmp_mes_qa_role_permission_menu` AS `expected_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `qa_role`.`role_id`
        AND `existing`.`tenant_id` = `qa_role`.`tenant_id`
        AND `existing`.`menu_id` = `expected_menu`.`menu_id`
   );

  UPDATE `system_user_role` AS `user_role`
    JOIN `system_users` AS `user`
      ON `user`.`id` = `user_role`.`user_id`
     AND `user`.`username` = 'admin'
     AND `user`.`tenant_id` = 1
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0'
    JOIN `system_role` AS `role`
      ON `role`.`id` = `user_role`.`role_id`
     AND `role`.`tenant_id` = `user`.`tenant_id`
     AND `role`.`code` = 'qa'
     AND `role`.`deleted` = b'0'
     SET `user_role`.`deleted` = b'0',
         `user_role`.`tenant_id` = `user`.`tenant_id`,
         `user_role`.`updater` = 'mes-qa-role-permission',
         `user_role`.`update_time` = NOW();

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `user`.`id`,
    `role`.`id`,
    'mes-qa-role-permission',
    NOW(),
    'mes-qa-role-permission',
    NOW(),
    b'0',
    `user`.`tenant_id`
    FROM `system_users` AS `user`
    JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `user`.`tenant_id`
     AND `role`.`code` = 'qa'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0'
   WHERE `user`.`username` = 'admin'
     AND `user`.`tenant_id` = 1
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0'
     AND NOT EXISTS (
       SELECT 1
         FROM `system_user_role` AS `existing`
        WHERE `existing`.`user_id` = `user`.`id`
          AND `existing`.`role_id` = `role`.`id`
     );

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_qa_role` AS `qa_role`
      CROSS JOIN `tmp_mes_qa_role_permission_menu` AS `expected_menu`
     WHERE NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` AS `role_menu`
        WHERE `role_menu`.`role_id` = `qa_role`.`role_id`
          AND `role_menu`.`tenant_id` = `qa_role`.`tenant_id`
          AND `role_menu`.`menu_id` = `expected_menu`.`menu_id`
          AND `role_menu`.`deleted` = b'0'
     )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QA role menu permission grant incomplete';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_user_role` AS `user_role`
      JOIN `system_users` AS `user`
        ON `user`.`id` = `user_role`.`user_id`
       AND `user`.`username` = 'admin'
       AND `user`.`tenant_id` = 1
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
      JOIN `system_role` AS `role`
        ON `role`.`id` = `user_role`.`role_id`
       AND `role`.`tenant_id` = `user`.`tenant_id`
       AND `role`.`code` = 'qa'
       AND `role`.`deleted` = b'0'
     WHERE `user_role`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Admin user is not assigned QA role';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_qa_role_permission_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_qa_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_qa_role_target_tenant`;
END//
DELIMITER ;

CALL ensure_mes_qa_role_permission_tab_20260806();

DROP PROCEDURE IF EXISTS ensure_mes_qa_role_permission_tab_20260806;

COMMIT;
