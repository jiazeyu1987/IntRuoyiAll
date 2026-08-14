-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260804_mes_edhr_qa_menu,20260707_system_role_category_management; type=menu; riskLevel=medium
-- Ensure only pqc_leader_permission owns the visible PQC leader tab; API permissions stay as hidden button grants.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_mes_pqc_leader_role_permission_tab_20260806;
DELIMITER //
CREATE PROCEDURE ensure_mes_pqc_leader_role_permission_tab_20260806()
BEGIN
  IF EXISTS (
    SELECT 1
      FROM `system_tenant_package`
     WHERE `deleted` = b'0'
       AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot assign PQC leader role';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900435
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled PQC leader menu 900435';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900435
       AND `deleted` = b'0'
       AND NOT (
         `name` = 'PQC组长'
         AND `path` = '/mes/pro/process-pool/pqc-leader'
         AND `component` = 'mes/pro/processpool/PqcLeaderWorkbenchPage'
         AND `component_name` = 'MesProProcessPoolPqcLeaderWorkbench'
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'PQC leader menu 900435 route/component mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900439
       AND `deleted` = b'0'
       AND NOT (
         `name` = 'PQC组长通用查询'
         AND `permission` = 'mes:pro-process-pool-team-leader:query'
         AND `type` = 3
         AND `parent_id` = 900435
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'system_menu id 900439 is already used by another active menu';
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
     SET `permission` = 'mes:pro-process-pool-pqc-leader:query',
         `updater` = 'mes-pqc-leader-role-permission',
         `update_time` = NOW()
   WHERE `id` = 900435
     AND `deleted` = b'0';

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    900439,
    'PQC组长通用查询',
    'mes:pro-process-pool-team-leader:query',
    3,
    1,
    900435,
    '',
    '',
    '',
    '',
    0,
    b'0',
    b'1',
    b'0',
    'mes-pqc-leader-role-permission',
    NOW(),
    'mes-pqc-leader-role-permission',
    NOW(),
    b'0'
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_menu`
      WHERE `id` = 900439
   );

  UPDATE `system_menu`
     SET `name` = 'PQC组长通用查询',
         `permission` = 'mes:pro-process-pool-team-leader:query',
         `type` = 3,
         `sort` = 1,
         `parent_id` = 900435,
         `path` = '',
         `icon` = '',
         `component` = '',
         `component_name` = '',
         `status` = 0,
         `visible` = b'0',
         `keep_alive` = b'1',
         `always_show` = b'0',
         `deleted` = b'0',
         `updater` = 'mes-pqc-leader-role-permission',
         `update_time` = NOW()
   WHERE `id` = 900439;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900439
       AND `name` = 'PQC组长通用查询'
       AND `permission` = 'mes:pro-process-pool-team-leader:query'
       AND `type` = 3
       AND `parent_id` = 900435
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing hidden team leader query menu 900439';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_role_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_mes_pqc_leader_role_target_tenant` (
    `tenant_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_mes_pqc_leader_role_target_tenant` (`tenant_id`)
  SELECT DISTINCT `tenant`.`id`
    FROM `system_tenant` AS `tenant`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `tenant`.`package_id`
     AND `package`.`deleted` = b'0'
     AND JSON_VALID(`package`.`menu_ids`)
   WHERE `tenant`.`deleted` = b'0'
     AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900435' AS JSON), '$');

  INSERT IGNORE INTO `tmp_mes_pqc_leader_role_target_tenant` (`tenant_id`)
  SELECT 1 AS `tenant_id`
    FROM `system_tenant` AS `tenant`
   WHERE `tenant`.`id` = 1
     AND `tenant`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_pqc_leader_role_target_tenant`) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant package with PQC leader menu 900435';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_pqc_leader_role_target_tenant` AS `target_tenant`
      LEFT JOIN `system_role_category` AS `category`
        ON `category`.`tenant_id` = `target_tenant`.`tenant_id`
       AND `category`.`code` = 'menu'
       AND `category`.`status` = 0
       AND `category`.`deleted` = b'0'
     WHERE `category`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing menu role category for PQC leader role tenant';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
      JOIN `tmp_mes_pqc_leader_role_target_tenant` AS `target_tenant`
        ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
     WHERE `role`.`code` = 'pqc_leader_permission'
     GROUP BY `role`.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate PQC leader role code in target tenant';
  END IF;

  INSERT INTO `system_role` (
    `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`,
    `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`,
    `deleted`, `tenant_id`
  )
  SELECT
    'PQC组长权限角色',
    'pqc_leader_permission',
    900435,
    `category`.`id`,
    1,
    '',
    0,
    2,
    'PQC组长页签可见性和运行权限',
    'mes-pqc-leader-role-permission',
    NOW(),
    'mes-pqc-leader-role-permission',
    NOW(),
    b'0',
    `target_tenant`.`tenant_id`
    FROM `tmp_mes_pqc_leader_role_target_tenant` AS `target_tenant`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `target_tenant`.`tenant_id`
     AND `category`.`code` = 'menu'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role` AS `existing_role`
      WHERE `existing_role`.`tenant_id` = `target_tenant`.`tenant_id`
        AND `existing_role`.`code` = 'pqc_leader_permission'
   );

  UPDATE `system_role` AS `role`
    JOIN `tmp_mes_pqc_leader_role_target_tenant` AS `target_tenant`
      ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `role`.`tenant_id`
     AND `category`.`code` = 'menu'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
     SET `role`.`name` = 'PQC组长权限角色',
         `role`.`sort` = 900435,
         `role`.`category_id` = `category`.`id`,
         `role`.`data_scope` = 1,
         `role`.`data_scope_dept_ids` = '',
         `role`.`status` = 0,
         `role`.`type` = 2,
         `role`.`remark` = 'PQC组长页签可见性和运行权限',
         `role`.`updater` = 'mes-pqc-leader-role-permission',
         `role`.`update_time` = NOW(),
         `role`.`deleted` = b'0'
   WHERE `role`.`code` = 'pqc_leader_permission';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_role`;
  CREATE TEMPORARY TABLE `tmp_mes_pqc_leader_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_pqc_leader_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
    JOIN `tmp_mes_pqc_leader_role_target_tenant` AS `target_tenant`
      ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
   WHERE `role`.`code` = 'pqc_leader_permission'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_pqc_leader_role`) <> (SELECT COUNT(*) FROM `tmp_mes_pqc_leader_role_target_tenant`) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'PQC leader role resolution failed for target tenants';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_role_permission_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_pqc_leader_role_permission_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_pqc_leader_role_permission_menu` (`menu_id`)
  SELECT 900435 AS `menu_id`
  UNION ALL
  SELECT 900439 AS `menu_id`
  UNION ALL
  SELECT 900312 AS `menu_id`
  UNION ALL
  SELECT 900313 AS `menu_id`
  UNION ALL
  SELECT 900314 AS `menu_id`;

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_pqc_leader_role_permission_menu` AS `expected_menu`
      LEFT JOIN `system_menu` AS `menu`
        ON `menu`.`id` = `expected_menu`.`menu_id`
       AND `menu`.`status` = 0
       AND `menu`.`deleted` = b'0'
     WHERE `menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing PQC leader role permission menu';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `system_role` AS `role`
      ON `role`.`id` = `role_menu`.`role_id`
     AND `role`.`tenant_id` = `role_menu`.`tenant_id`
     AND `role`.`deleted` = b'0'
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'mes-pqc-leader-role-permission',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`menu_id` = 900435
     AND `role_menu`.`deleted` = b'0'
     AND `role`.`code` <> 'pqc_leader_permission';

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_mes_pqc_leader_role` AS `pqc_leader_role`
      ON `pqc_leader_role`.`role_id` = `role_menu`.`role_id`
     AND `pqc_leader_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_mes_pqc_leader_role_permission_menu` AS `expected_menu`
      ON `expected_menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'mes-pqc-leader-role-permission',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`role_id` = `pqc_leader_role`.`role_id`;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `pqc_leader_role`.`role_id`,
    `expected_menu`.`menu_id`,
    'mes-pqc-leader-role-permission',
    NOW(),
    'mes-pqc-leader-role-permission',
    NOW(),
    b'0',
    `pqc_leader_role`.`tenant_id`
    FROM `tmp_mes_pqc_leader_role` AS `pqc_leader_role`
    CROSS JOIN `tmp_mes_pqc_leader_role_permission_menu` AS `expected_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `pqc_leader_role`.`role_id`
        AND `existing`.`tenant_id` = `pqc_leader_role`.`tenant_id`
        AND `existing`.`menu_id` = `expected_menu`.`menu_id`
   );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_role_menu_keep`;
  CREATE TEMPORARY TABLE `tmp_mes_pqc_leader_role_menu_keep` AS
  SELECT
    MIN(`role_menu`.`id`) AS `keep_id`,
    `role_menu`.`role_id`,
    `role_menu`.`tenant_id`,
    `role_menu`.`menu_id`
    FROM `system_role_menu` AS `role_menu`
    JOIN `tmp_mes_pqc_leader_role` AS `pqc_leader_role`
      ON `pqc_leader_role`.`role_id` = `role_menu`.`role_id`
     AND `pqc_leader_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_mes_pqc_leader_role_permission_menu` AS `expected_menu`
      ON `expected_menu`.`menu_id` = `role_menu`.`menu_id`
   WHERE `role_menu`.`deleted` = b'0'
   GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_mes_pqc_leader_role_menu_keep` AS `keep`
      ON `keep`.`role_id` = `role_menu`.`role_id`
     AND `keep`.`tenant_id` = `role_menu`.`tenant_id`
     AND `keep`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'mes-pqc-leader-role-permission',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`deleted` = b'0'
     AND `role_menu`.`id` <> `keep`.`keep_id`;

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
     AND `role`.`code` = 'pqc_leader_permission'
     AND `role`.`deleted` = b'0'
     SET `user_role`.`deleted` = b'0',
         `user_role`.`tenant_id` = `user`.`tenant_id`,
         `user_role`.`updater` = 'mes-pqc-leader-role-permission',
         `user_role`.`update_time` = NOW();

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `user`.`id`,
    `role`.`id`,
    'mes-pqc-leader-role-permission',
    NOW(),
    'mes-pqc-leader-role-permission',
    NOW(),
    b'0',
    `user`.`tenant_id`
    FROM `system_users` AS `user`
    JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `user`.`tenant_id`
     AND `role`.`code` = 'pqc_leader_permission'
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
      FROM `tmp_mes_pqc_leader_role` AS `pqc_leader_role`
      CROSS JOIN `tmp_mes_pqc_leader_role_permission_menu` AS `expected_menu`
     WHERE NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` AS `role_menu`
        WHERE `role_menu`.`role_id` = `pqc_leader_role`.`role_id`
          AND `role_menu`.`tenant_id` = `pqc_leader_role`.`tenant_id`
          AND `role_menu`.`menu_id` = `expected_menu`.`menu_id`
          AND `role_menu`.`deleted` = b'0'
     )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'PQC leader role menu permission grant incomplete';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `tmp_mes_pqc_leader_role` AS `pqc_leader_role`
        ON `pqc_leader_role`.`role_id` = `role_menu`.`role_id`
       AND `pqc_leader_role`.`tenant_id` = `role_menu`.`tenant_id`
      JOIN `tmp_mes_pqc_leader_role_permission_menu` AS `expected_menu`
        ON `expected_menu`.`menu_id` = `role_menu`.`menu_id`
     WHERE `role_menu`.`deleted` = b'0'
     GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'PQC leader role menu permission grant has duplicate active bindings';
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
       AND `role`.`code` = 'pqc_leader_permission'
       AND `role`.`deleted` = b'0'
     WHERE `user_role`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Admin user is not assigned PQC leader role';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `system_role` AS `role`
        ON `role`.`id` = `role_menu`.`role_id`
       AND `role`.`tenant_id` = `role_menu`.`tenant_id`
       AND `role`.`deleted` = b'0'
     WHERE `role_menu`.`menu_id` = 900435
       AND `role_menu`.`deleted` = b'0'
       AND `role`.`code` <> 'pqc_leader_permission'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active PQC leader tab menu is still granted to a non-PQC leader role';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_role_menu_keep`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_role_permission_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_pqc_leader_role_target_tenant`;
END//
DELIMITER ;

CALL ensure_mes_pqc_leader_role_permission_tab_20260806();

DROP PROCEDURE IF EXISTS ensure_mes_pqc_leader_role_permission_tab_20260806;

COMMIT;
