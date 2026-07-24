-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260629_menu_title_srm_dcc_rename; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_srm_admin_role_visibility;

DELIMITER //
CREATE PROCEDURE ensure_srm_admin_role_visibility()
BEGIN
  DECLARE v_srm_admin_role_id BIGINT DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_users`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND `status` = 0
      AND `username` = 'admin'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled admin user in tenant 1 for SRM role assignment';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991000
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM root menu 991000; cannot create SRM admin role scope';
  END IF;

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`,
    `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    (
      SELECT COALESCE(MAX(`existing_role`.`id`), 910239) + 1
      FROM `system_role` AS `existing_role`
    ),
    'SRM管理员',
    'srm_admin',
    910240,
    1,
    '',
    0,
    1,
    '仅允许查看和处理 SRM 菜单及 SRM 审批模块',
    'srm-admin-role',
    NOW(),
    'srm-admin-role',
    NOW(),
    b'0',
    1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND (`code` = 'srm_admin' OR `name` = 'SRM管理员')
  );

  UPDATE `system_role`
  SET `name` = 'SRM管理员',
      `code` = 'srm_admin',
      `sort` = 910240,
      `data_scope` = 1,
      `data_scope_dept_ids` = '',
      `status` = 0,
      `type` = 1,
      `remark` = '仅允许查看和处理 SRM 菜单及 SRM 审批模块',
      `updater` = 'srm-admin-role',
      `update_time` = NOW(),
      `deleted` = b'0'
  WHERE `tenant_id` = 1
    AND (`code` = 'srm_admin' OR `name` = 'SRM管理员');

  SELECT `id`
  INTO v_srm_admin_role_id
  FROM `system_role`
  WHERE `deleted` = b'0'
    AND `tenant_id` = 1
    AND `status` = 0
    AND (`code` = 'srm_admin' OR `name` = 'SRM管理员')
  ORDER BY CASE WHEN `code` = 'srm_admin' THEN 0 ELSE 1 END, `id`
  LIMIT 1;

  IF v_srm_admin_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled srm_admin role in tenant 1 after SRM role migration';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_admin_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_admin_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `id` = 991000
      OR `parent_id` = 991000
      OR `parent_id` IN (
        SELECT `id`
        FROM `system_menu`
        WHERE `deleted` = b'0'
          AND `parent_id` = 991000
      )
    );

  IF (SELECT COUNT(*) FROM `tmp_srm_admin_menu_ids`) < 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'SRM menu tree is incomplete; refusing to assign SRM admin scope';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  LEFT JOIN `tmp_srm_admin_menu_ids` AS `srm_menu`
    ON `srm_menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'srm-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`role_id` = v_srm_admin_role_id
    AND `role_menu`.`tenant_id` = 1
    AND `role_menu`.`deleted` = b'0'
    AND `srm_menu`.`id` IS NULL;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_srm_admin_menu_ids` AS `srm_menu`
    ON `srm_menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'srm-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`role_id` = v_srm_admin_role_id
    AND `role_menu`.`tenant_id` = 1
    AND `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    v_srm_admin_role_id,
    `srm_menu`.`id`,
    'srm-admin-role',
    NOW(),
    'srm-admin-role',
    NOW(),
    b'0',
    1
  FROM `tmp_srm_admin_menu_ids` AS `srm_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = v_srm_admin_role_id
      AND `existing`.`tenant_id` = 1
      AND `existing`.`menu_id` = `srm_menu`.`id`
      AND `existing`.`deleted` = b'0'
  );

  UPDATE `system_user_role` AS `user_role`
  JOIN `system_users` AS `user`
    ON `user`.`id` = `user_role`.`user_id`
   AND `user`.`tenant_id` = `user_role`.`tenant_id`
  SET `user_role`.`deleted` = b'0',
      `user_role`.`updater` = 'srm-admin-role',
      `user_role`.`update_time` = NOW()
  WHERE `user_role`.`tenant_id` = 1
    AND `user_role`.`role_id` = v_srm_admin_role_id
    AND `user`.`username` = 'admin'
    AND `user_role`.`deleted` = b'1';

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `user`.`id`,
    v_srm_admin_role_id,
    'srm-admin-role',
    NOW(),
    'srm-admin-role',
    NOW(),
    b'0',
    `user`.`tenant_id`
  FROM `system_users` AS `user`
  WHERE `user`.`deleted` = b'0'
    AND `user`.`tenant_id` = 1
    AND `user`.`status` = 0
    AND `user`.`username` = 'admin'
    AND v_srm_admin_role_id IS NOT NULL
    AND NOT EXISTS (
      SELECT 1
      FROM `system_user_role` AS `existing`
      WHERE `existing`.`user_id` = `user`.`id`
        AND `existing`.`role_id` = v_srm_admin_role_id
        AND `existing`.`tenant_id` = `user`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_admin_menu_ids`;
END//
DELIMITER ;

CALL ensure_srm_admin_role_visibility();

DROP PROCEDURE IF EXISTS ensure_srm_admin_role_visibility;
