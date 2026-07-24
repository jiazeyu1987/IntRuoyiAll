-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260629_mes_smart_scheduling_role_scope; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_puhui_schedule_admin_role_visibility;

DELIMITER //
CREATE PROCEDURE ensure_mes_puhui_schedule_admin_role_visibility()
BEGIN
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot scope Puhui schedule admin role';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `status` = 0
      AND `id` = 5100
      AND `path` = '/mes'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES root menu 5100; cannot scope Puhui schedule admin role';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `status` = 0
      AND `id` = 900120
      AND `parent_id` = 5100
      AND `name` = '智能排产'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling menu 900120; cannot scope Puhui schedule admin role';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `status` = 0
      AND `id` = 900104
      AND `parent_id` = 900120
      AND `name` = '璞慧排产'
      AND `path` = '/mes/pro/puhui-schedule'
      AND `component_name` = 'MesProPuhuiSchedule'
      AND `permission` = 'mes:pro-puhui-schedule:query'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing Puhui schedule menu 900104; cannot scope Puhui schedule admin role';
  END IF;

  SELECT `id`
  INTO v_admin_user_id
  FROM `system_users`
  WHERE `deleted` = b'0'
    AND `tenant_id` = 1
    AND `status` = 0
    AND `username` = 'admin'
  LIMIT 1;

  IF v_admin_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled tenant 1 admin user for Puhui schedule admin role assignment';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_admin_target_tenants`;
  CREATE TEMPORARY TABLE `tmp_mes_puhui_schedule_admin_target_tenants` AS
  SELECT DISTINCT `tenant_id`
  FROM (
    SELECT 1 AS `tenant_id`
    UNION
    SELECT `tenant`.`id` AS `tenant_id`
    FROM `system_tenant` AS `tenant`
    JOIN `system_tenant_package` AS `tenant_package`
      ON `tenant_package`.`id` = `tenant`.`package_id`
     AND `tenant_package`.`deleted` = b'0'
     AND JSON_VALID(`tenant_package`.`menu_ids`)
    WHERE `tenant`.`deleted` = b'0'
      AND JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST('900104' AS JSON), '$')
  ) AS `target`;

  UPDATE `system_role` AS `role`
  JOIN `tmp_mes_puhui_schedule_admin_target_tenants` AS `target_tenant`
    ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
  SET `role`.`name` = '璞慧排产管理员',
      `role`.`code` = 'mes_puhui_schedule_admin',
      `role`.`sort` = 910300,
      `role`.`data_scope` = 1,
      `role`.`data_scope_dept_ids` = '',
      `role`.`status` = 0,
      `role`.`type` = 1,
      `role`.`remark` = '仅允许查看和操作璞慧排产',
      `role`.`deleted` = b'0',
      `role`.`updater` = 'mes-puhui-schedule-admin-role',
      `role`.`update_time` = NOW()
  WHERE `role`.`code` = 'mes_puhui_schedule_admin'
     OR `role`.`name` = '璞慧排产管理员';

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`,
    `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    (
      SELECT COALESCE(MAX(`existing_role`.`id`), 910299)
      FROM `system_role` AS `existing_role`
    ) + ROW_NUMBER() OVER (ORDER BY `target_tenant`.`tenant_id`),
    '璞慧排产管理员',
    'mes_puhui_schedule_admin',
    910300,
    1,
    '',
    0,
    1,
    '仅允许查看和操作璞慧排产',
    'mes-puhui-schedule-admin-role',
    NOW(),
    'mes-puhui-schedule-admin-role',
    NOW(),
    b'0',
    `target_tenant`.`tenant_id`
  FROM `tmp_mes_puhui_schedule_admin_target_tenants` AS `target_tenant`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `target_tenant`.`tenant_id`
      AND (`existing`.`code` = 'mes_puhui_schedule_admin' OR `existing`.`name` = '璞慧排产管理员')
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_admin_roles`;
  CREATE TEMPORARY TABLE `tmp_mes_puhui_schedule_admin_roles` AS
  SELECT `role`.`id` AS `role_id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `tmp_mes_puhui_schedule_admin_target_tenants` AS `target_tenant`
    ON `target_tenant`.`tenant_id` = `role`.`tenant_id`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`status` = 0
    AND (`role`.`code` = 'mes_puhui_schedule_admin' OR `role`.`name` = '璞慧排产管理员');

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT `tenant_id`, COUNT(*) AS `role_count`
      FROM `tmp_mes_puhui_schedule_admin_roles`
      GROUP BY `tenant_id`
    ) AS `role_group`
    WHERE `role_count` <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate or missing Puhui schedule admin role in target tenant';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_admin_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_puhui_schedule_admin_menu_ids` AS
  SELECT 5100 AS `menu_id`
  UNION ALL SELECT 900120
  UNION ALL SELECT 900104;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_puhui_schedule_admin_roles` AS `puhui_role`
    ON `puhui_role`.`role_id` = `role_menu`.`role_id`
   AND `puhui_role`.`tenant_id` = `role_menu`.`tenant_id`
  LEFT JOIN `tmp_mes_puhui_schedule_admin_menu_ids` AS `puhui_menu`
    ON `puhui_menu`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'mes-puhui-schedule-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'0'
    AND `puhui_menu`.`menu_id` IS NULL;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_puhui_schedule_admin_roles` AS `puhui_role`
    ON `puhui_role`.`role_id` = `role_menu`.`role_id`
   AND `puhui_role`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `tmp_mes_puhui_schedule_admin_menu_ids` AS `puhui_menu`
    ON `puhui_menu`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'mes-puhui-schedule-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `puhui_role`.`role_id`,
    `puhui_menu`.`menu_id`,
    'mes-puhui-schedule-admin-role',
    NOW(),
    'mes-puhui-schedule-admin-role',
    NOW(),
    b'0',
    `puhui_role`.`tenant_id`
  FROM `tmp_mes_puhui_schedule_admin_roles` AS `puhui_role`
  JOIN `tmp_mes_puhui_schedule_admin_menu_ids` AS `puhui_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `puhui_role`.`role_id`
      AND `existing`.`tenant_id` = `puhui_role`.`tenant_id`
      AND `existing`.`menu_id` = `puhui_menu`.`menu_id`
      AND `existing`.`deleted` = b'0'
  );

  UPDATE `system_role_menu` AS `role_menu`
  LEFT JOIN `tmp_mes_puhui_schedule_admin_roles` AS `puhui_role`
    ON `puhui_role`.`role_id` = `role_menu`.`role_id`
   AND `puhui_role`.`tenant_id` = `role_menu`.`tenant_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'mes-puhui-schedule-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`menu_id` = 900104
    AND `role_menu`.`deleted` = b'0'
    AND `puhui_role`.`role_id` IS NULL;

  UPDATE `system_user_role` AS `user_role`
  JOIN `tmp_mes_puhui_schedule_admin_roles` AS `puhui_role`
    ON `puhui_role`.`tenant_id` = `user_role`.`tenant_id`
   AND `puhui_role`.`role_id` = `user_role`.`role_id`
  SET `user_role`.`deleted` = b'0',
      `user_role`.`updater` = 'mes-puhui-schedule-admin-role',
      `user_role`.`update_time` = NOW()
  WHERE `user_role`.`tenant_id` = 1
    AND `user_role`.`user_id` = v_admin_user_id
    AND `user_role`.`deleted` = b'1';

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    v_admin_user_id,
    `puhui_role`.`role_id`,
    'mes-puhui-schedule-admin-role',
    NOW(),
    'mes-puhui-schedule-admin-role',
    NOW(),
    b'0',
    1
  FROM `tmp_mes_puhui_schedule_admin_roles` AS `puhui_role`
  WHERE `puhui_role`.`tenant_id` = 1
    AND NOT EXISTS (
      SELECT 1
      FROM `system_user_role` AS `existing`
      WHERE `existing`.`user_id` = v_admin_user_id
        AND `existing`.`role_id` = `puhui_role`.`role_id`
        AND `existing`.`tenant_id` = 1
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_admin_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_admin_roles`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_puhui_schedule_admin_target_tenants`;
END//
DELIMITER ;

CALL ensure_mes_puhui_schedule_admin_role_visibility();

DROP PROCEDURE IF EXISTS ensure_mes_puhui_schedule_admin_role_visibility;
