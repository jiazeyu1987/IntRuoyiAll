-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_production_release_roles; type=permission; riskLevel=medium
-- Bind only the exact admin account in its own tenant to the existing management representative role.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_management_representative_admin_permission;

DELIMITER $$

CREATE PROCEDURE ensure_mes_management_representative_admin_permission()
BEGIN
  DECLARE v_tenant_id BIGINT DEFAULT NULL;
  DECLARE v_admin_id BIGINT DEFAULT NULL;
  DECLARE v_role_id BIGINT DEFAULT NULL;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_management_representative_admin`;
  CREATE TEMPORARY TABLE `tmp_mes_management_representative_admin` (
    `user_id` BIGINT NOT NULL PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_management_representative_admin` (`user_id`, `tenant_id`)
  SELECT `user`.`id`, `user`.`tenant_id`
  FROM `system_users` AS `user`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `user`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  WHERE `user`.`username` = 'admin'
    AND `user`.`status` = 0
    AND `user`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_management_representative_admin`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected exactly one active admin user in one enabled tenant';
  END IF;

  SELECT `user_id`, `tenant_id`
    INTO v_admin_id, v_tenant_id
  FROM `tmp_mes_management_representative_admin`;

  IF (
    SELECT COUNT(*)
    FROM `system_role` AS `role`
    WHERE `role`.`tenant_id` = v_tenant_id
      AND `role`.`code` = 'MES_MANAGEMENT_REPRESENTATIVE'
      AND `role`.`status` = 0
      AND `role`.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous active management representative role in admin tenant';
  END IF;

  SELECT `role`.`id`
    INTO v_role_id
  FROM `system_role` AS `role`
  WHERE `role`.`tenant_id` = v_tenant_id
    AND `role`.`code` = 'MES_MANAGEMENT_REPRESENTATIVE'
    AND `role`.`status` = 0
    AND `role`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_management_representative_required_menus`;
  CREATE TEMPORARY TABLE `tmp_mes_management_representative_required_menus` (
    `permission` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_management_representative_required_menus` (`permission`)
  VALUES
    ('mes:pro-edhr-release:query'),
    ('mes:pro-edhr-release:approve');

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_management_representative_required_menus` AS `required_menu`
    WHERE (
      SELECT COUNT(*)
      FROM `system_menu` AS `menu`
      WHERE `menu`.`permission` = `required_menu`.`permission`
        AND `menu`.`status` = 0
        AND `menu`.`deleted` = b'0'
    ) < 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Management representative release permission menu is missing';
  END IF;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    v_role_id,
    `menu`.`id`,
    'mes-management-representative-admin',
    NOW(),
    'mes-management-representative-admin',
    NOW(),
    b'0',
    v_tenant_id
  FROM `system_menu` AS `menu`
  JOIN `tmp_mes_management_representative_required_menus` AS `required_menu`
    ON `required_menu`.`permission` = `menu`.`permission`
  WHERE `menu`.`status` = 0
    AND `menu`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = v_role_id
        AND `existing`.`menu_id` = `menu`.`id`
        AND `existing`.`tenant_id` = v_tenant_id
        AND `existing`.`deleted` = b'0'
    );

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `role_menu`.`menu_id`
   AND `menu`.`status` = 0
   AND `menu`.`deleted` = b'0'
  JOIN `tmp_mes_management_representative_required_menus` AS `required_menu`
    ON `required_menu`.`permission` = `menu`.`permission`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'mes-management-representative-admin',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`role_id` = v_role_id
    AND `role_menu`.`tenant_id` = v_tenant_id;

  UPDATE `system_user_role`
  SET `deleted` = b'0',
      `updater` = 'mes-management-representative-admin',
      `update_time` = NOW()
  WHERE `user_id` = v_admin_id
    AND `role_id` = v_role_id
    AND `tenant_id` = v_tenant_id
    AND `deleted` = b'1';

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    v_admin_id,
    v_role_id,
    'mes-management-representative-admin',
    NOW(),
    'mes-management-representative-admin',
    NOW(),
    b'0',
    v_tenant_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role` AS `existing`
    WHERE `existing`.`user_id` = v_admin_id
      AND `existing`.`role_id` = v_role_id
      AND `existing`.`tenant_id` = v_tenant_id
      AND `existing`.`deleted` = b'0'
  );

  IF NOT EXISTS (
    SELECT 1
    FROM `system_user_role` AS `user_role`
    WHERE `user_role`.`user_id` = v_admin_id
      AND `user_role`.`role_id` = v_role_id
      AND `user_role`.`tenant_id` = v_tenant_id
      AND `user_role`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Admin management representative role binding was not persisted';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_management_representative_required_menus`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_management_representative_admin`;
END$$

DELIMITER ;

CALL ensure_mes_management_representative_admin_permission();
DROP PROCEDURE IF EXISTS ensure_mes_management_representative_admin_permission;
