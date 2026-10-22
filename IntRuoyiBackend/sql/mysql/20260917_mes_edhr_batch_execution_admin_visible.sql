-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260808_mes_edhr_batch_record_test_menu; type=menu; riskLevel=low
-- Restore admin visibility for the eDHR batch execution page without widening non-admin roles.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_execution_admin_visible;

DELIMITER $$

CREATE PROCEDURE ensure_mes_edhr_batch_execution_admin_visible()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `menu_ids` IS NOT NULL
      AND `menu_ids` <> ''
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot expose eDHR batch execution to admin';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR parent menu 900220; cannot expose eDHR batch execution to admin';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900033
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch execution page menu 900033';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/feedback/edhr-batch-execution'
      AND `id` <> 900033
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR batch execution route already exists on a different menu id';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_required_menus`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_execution_required_menus` (
    `menu_id` bigint NOT NULL PRIMARY KEY,
    `permission` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `type` tinyint NOT NULL
  );

  INSERT INTO `tmp_mes_edhr_batch_execution_required_menus` (`menu_id`, `permission`, `type`)
  SELECT 900220 AS `menu_id`, 'mes:pro-edhr-batch-processing:query' AS `permission`, 1 AS `type`
  UNION ALL
  SELECT 900033 AS `menu_id`, 'mes:pro-edhr-batch-execution:query' AS `permission`, 2 AS `type`
  UNION ALL
  SELECT 900034 AS `menu_id`, 'mes:pro-edhr-batch-execution:query' AS `permission`, 3 AS `type`
  UNION ALL
  SELECT 900036 AS `menu_id`, 'mes:pro-edhr-batch-execution:update' AS `permission`, 3 AS `type`;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_batch_execution_required_menus` AS `required_menu`
    LEFT JOIN `system_menu` AS `menu`
      ON `menu`.`id` = `required_menu`.`menu_id`
     AND `menu`.`permission` = `required_menu`.`permission`
     AND `menu`.`type` = `required_menu`.`type`
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0'
    WHERE `menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing required eDHR batch execution permission menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_users` AS `user`
    WHERE `user`.`username` = 'admin'
      AND `user`.`deleted` = b'0'
      AND NOT EXISTS (
        SELECT 1
        FROM `system_user_role` AS `user_role`
        JOIN `system_role` AS `role`
          ON `role`.`id` = `user_role`.`role_id`
         AND `role`.`tenant_id` = `user`.`tenant_id`
         AND `role`.`code` = 'super_admin'
         AND `role`.`status` = 0
         AND `role`.`deleted` = b'0'
        WHERE `user_role`.`user_id` = `user`.`id`
          AND `user_role`.`tenant_id` = `user`.`tenant_id`
          AND `user_role`.`deleted` = b'0'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Admin user is missing active super_admin role';
  END IF;

  UPDATE `system_menu`
  SET `name` = '批次执行',
      `permission` = 'mes:pro-edhr-batch-execution:query',
      `type` = 2,
      `sort` = 7,
      `parent_id` = 900220,
      `path` = '/mes/pro/feedback/edhr-batch-execution',
      `icon` = 'ep:document-checked',
      `component` = 'mes/pro/edhr-batch/BatchExecutionListPage',
      `component_name` = 'MesProEdhrBatchExecutionListPage',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'edhr-batch-execution-admin-visible',
      `update_time` = NOW()
  WHERE `id` = 900033;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900033
      AND `name` = '批次执行'
      AND `permission` = 'mes:pro-edhr-batch-execution:query'
      AND `type` = 2
      AND `sort` = 7
      AND `parent_id` = 900220
      AND `path` = '/mes/pro/feedback/edhr-batch-execution'
      AND `component` = 'mes/pro/edhr-batch/BatchExecutionListPage'
      AND `component_name` = 'MesProEdhrBatchExecutionListPage'
      AND `status` = 0
      AND `visible` = b'1'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid eDHR batch execution page menu 900033';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_admin_roles`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_execution_admin_roles` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_batch_execution_admin_roles` (`role_id`, `tenant_id`)
  SELECT DISTINCT `role`.`id`, `role`.`tenant_id`
  FROM `system_users` AS `user`
  JOIN `system_user_role` AS `user_role`
    ON `user_role`.`user_id` = `user`.`id`
   AND `user_role`.`tenant_id` = `user`.`tenant_id`
   AND `user_role`.`deleted` = b'0'
  JOIN `system_role` AS `role`
    ON `role`.`id` = `user_role`.`role_id`
   AND `role`.`tenant_id` = `user`.`tenant_id`
   AND `role`.`code` = 'super_admin'
   AND `role`.`status` = 0
   AND `role`.`deleted` = b'0'
  WHERE `user`.`username` = 'admin'
    AND `user`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_edhr_batch_execution_admin_roles`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Admin user is missing active super_admin role';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_role_menu_restore`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_execution_role_menu_restore` (
    `id` bigint NOT NULL PRIMARY KEY
  );

  INSERT INTO `tmp_mes_edhr_batch_execution_role_menu_restore` (`id`)
  SELECT MIN(`role_menu`.`id`) AS `id`
  FROM `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_edhr_batch_execution_admin_roles` AS `admin_role`
    ON `admin_role`.`role_id` = `role_menu`.`role_id`
   AND `admin_role`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `tmp_mes_edhr_batch_execution_required_menus` AS `required_menu`
    ON `required_menu`.`menu_id` = `role_menu`.`menu_id`
  WHERE `role_menu`.`deleted` = b'1'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `active_role_menu`
      WHERE `active_role_menu`.`role_id` = `role_menu`.`role_id`
        AND `active_role_menu`.`tenant_id` = `role_menu`.`tenant_id`
        AND `active_role_menu`.`menu_id` = `role_menu`.`menu_id`
        AND `active_role_menu`.`deleted` = b'0'
    )
  GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_edhr_batch_execution_role_menu_restore` AS `restore`
    ON `restore`.`id` = `role_menu`.`id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'edhr-batch-execution-admin-visible',
      `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `admin_role`.`role_id`,
    `required_menu`.`menu_id`,
    'edhr-batch-execution-admin-visible',
    NOW(),
    'edhr-batch-execution-admin-visible',
    NOW(),
    b'0',
    `admin_role`.`tenant_id`
  FROM `tmp_mes_edhr_batch_execution_admin_roles` AS `admin_role`
  JOIN `tmp_mes_edhr_batch_execution_required_menus` AS `required_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `admin_role`.`role_id`
      AND `existing`.`tenant_id` = `admin_role`.`tenant_id`
      AND `existing`.`menu_id` = `required_menu`.`menu_id`
      AND `existing`.`deleted` = b'0'
  );

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_batch_execution_admin_roles` AS `admin_role`
    JOIN `tmp_mes_edhr_batch_execution_required_menus` AS `required_menu`
    WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `role_menu`
      WHERE `role_menu`.`role_id` = `admin_role`.`role_id`
        AND `role_menu`.`tenant_id` = `admin_role`.`tenant_id`
        AND `role_menu`.`menu_id` = `required_menu`.`menu_id`
        AND `role_menu`.`deleted` = b'0'
    )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR batch execution menu is missing from admin super_admin role';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_admin_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_execution_admin_packages` (
    `package_id` bigint NOT NULL PRIMARY KEY
  );

  INSERT IGNORE INTO `tmp_mes_edhr_batch_execution_admin_packages` (`package_id`)
  SELECT DISTINCT `tenant`.`package_id`
  FROM `system_users` AS `user`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `user`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_batch_execution_admin_roles` AS `admin_role`
    ON `admin_role`.`tenant_id` = `user`.`tenant_id`
  WHERE `user`.`username` = 'admin'
    AND `user`.`deleted` = b'0'
    AND `tenant`.`package_id` IS NOT NULL
    AND `tenant`.`package_id` <> 0;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_batch_execution_admin_packages` AS `admin_package`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `admin_package`.`package_id`
     AND `package`.`deleted` = b'0'
    WHERE `package`.`menu_ids` IS NULL
       OR `package`.`menu_ids` = ''
       OR NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Admin tenant package menu_ids must be valid JSON';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_execution_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_batch_execution_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_batch_execution_admin_packages` AS `admin_package`
    ON `admin_package`.`package_id` = `package`.`id`
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    CAST(`package`.`menu_ids` AS JSON),
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_edhr_batch_execution_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `admin_package`.`package_id`, `required_menu`.`menu_id`
  FROM `tmp_mes_edhr_batch_execution_admin_packages` AS `admin_package`
  JOIN `tmp_mes_edhr_batch_execution_required_menus` AS `required_menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT `package_id`, `menu_ids`
    FROM (
      SELECT
        `package_id`,
        JSON_ARRAYAGG(`menu_id`) OVER (
          PARTITION BY `package_id`
          ORDER BY `menu_id`
          ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
        ) AS `menu_ids`,
        ROW_NUMBER() OVER (PARTITION BY `package_id` ORDER BY `menu_id` DESC) AS `rn`
      FROM `tmp_mes_edhr_batch_execution_package_menu_ids`
    ) AS `ordered_package_menu`
    WHERE `rn` = 1
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-batch-execution-admin-visible',
      `package`.`update_time` = NOW();

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_batch_execution_admin_packages` AS `admin_package`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `admin_package`.`package_id`
     AND `package`.`deleted` = b'0'
    JOIN `tmp_mes_edhr_batch_execution_required_menus` AS `required_menu`
    WHERE NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST(`required_menu`.`menu_id` AS JSON), '$')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR batch execution menu is missing from admin tenant package';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_admin_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_role_menu_restore`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_admin_roles`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_execution_required_menus`;
END$$

DELIMITER ;

CALL ensure_mes_edhr_batch_execution_admin_visible();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_execution_admin_visible;
