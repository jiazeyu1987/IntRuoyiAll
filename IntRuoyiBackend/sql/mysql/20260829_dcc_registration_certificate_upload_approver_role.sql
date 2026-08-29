-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260816_dcc_registration_certificate_menu,20260707_system_role_category_management; type=permission; riskLevel=medium
-- Purpose: Create the registration-manager upload approver role and bind it to approval-center-capable registration users.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_role_20260829;
DELIMITER //
CREATE PROCEDURE ensure_dcc_reg_cert_upload_role_20260829()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_required_permission`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_approver_required_permission` (
    `permission` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_approver_required_permission` (`permission`)
  VALUES
    ('dcc:registration-certificate:upload:approve'),
    ('bpm:task:query'),
    ('bpm:process-instance:query');

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_reg_cert_upload_approver_required_permission` AS `required`
      LEFT JOIN `system_menu` AS `menu`
        ON `menu`.`permission` = `required`.`permission`
       AND `menu`.`status` = 0
       AND `menu`.`deleted` = b'0'
     GROUP BY `required`.`permission`
    HAVING COUNT(`menu`.`id`) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled upload approval permission menu for registration certificate upload approver role';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_initial_user`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_approver_initial_user` (
    `tenant_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`, `user_id`)
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_dcc_reg_cert_upload_approver_initial_user` (`tenant_id`, `user_id`)
  SELECT DISTINCT
         `user`.`tenant_id`,
         `user`.`id`
    FROM `system_users` AS `user`
    JOIN `system_dept` AS `dept`
      ON `dept`.`id` = `user`.`dept_id`
     AND `dept`.`tenant_id` = `user`.`tenant_id`
     AND `dept`.`status` = 0
     AND `dept`.`deleted` = b'0'
    JOIN `system_user_role` AS `approval_user_role`
      ON `approval_user_role`.`user_id` = `user`.`id`
     AND `approval_user_role`.`tenant_id` = `user`.`tenant_id`
     AND `approval_user_role`.`deleted` = b'0'
    JOIN `system_role` AS `approval_role`
      ON `approval_role`.`id` = `approval_user_role`.`role_id`
     AND `approval_role`.`tenant_id` = `approval_user_role`.`tenant_id`
     AND `approval_role`.`code` = 'approval_center_entry'
     AND `approval_role`.`status` = 0
     AND `approval_role`.`deleted` = b'0'
   WHERE `user`.`status` = 0
     AND `user`.`deleted` = b'0'
     AND `user`.`username` <> 'admin'
     AND `dept`.`name` COLLATE utf8mb4_unicode_ci = '注册部' COLLATE utf8mb4_unicode_ci;

  IF (SELECT COUNT(*) FROM `tmp_dcc_reg_cert_upload_approver_initial_user`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration department approval-center user for registration certificate upload approver role';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_approver_target_tenant` (
    `tenant_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_approver_target_tenant` (`tenant_id`)
  SELECT DISTINCT `tenant_id`
    FROM `tmp_dcc_reg_cert_upload_approver_initial_user`;

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_reg_cert_upload_approver_target_tenant` AS `target`
      LEFT JOIN `system_role_category` AS `category`
        ON `category`.`tenant_id` = `target`.`tenant_id`
       AND `category`.`code` = 'dcc'
       AND `category`.`status` = 0
       AND `category`.`deleted` = b'0'
     GROUP BY `target`.`tenant_id`
    HAVING COUNT(`category`.`id`) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous DCC role category for registration certificate upload approver role';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
      JOIN `tmp_dcc_reg_cert_upload_approver_target_tenant` AS `target`
        ON `target`.`tenant_id` = `role`.`tenant_id`
     WHERE `role`.`code` = 'dcc_registration_certificate_approver'
     GROUP BY `role`.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate registration certificate upload approver role code in target tenant';
  END IF;

  UPDATE `system_role` AS `role`
    JOIN `tmp_dcc_reg_cert_upload_approver_target_tenant` AS `target`
      ON `target`.`tenant_id` = `role`.`tenant_id`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `target`.`tenant_id`
     AND `category`.`code` = 'dcc'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
     SET `role`.`name` = '注册部经理',
         `role`.`sort` = 846,
         `role`.`category_id` = `category`.`id`,
         `role`.`data_scope` = 1,
         `role`.`data_scope_dept_ids` = '',
         `role`.`status` = 0,
         `role`.`type` = 2,
         `role`.`remark` = '注册证上传审批角色，权限由注册证上传审批迁移精确维护',
         `role`.`deleted` = b'0',
         `role`.`updater` = 'dcc-reg-cert-upload-approver-role',
         `role`.`update_time` = NOW()
   WHERE `role`.`code` = 'dcc_registration_certificate_approver';

  INSERT INTO `system_role` (
    `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`,
    `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    '注册部经理',
    'dcc_registration_certificate_approver',
    846,
    `category`.`id`,
    1,
    '',
    0,
    2,
    '注册证上传审批角色，权限由注册证上传审批迁移精确维护',
    'dcc-reg-cert-upload-approver-role',
    NOW(),
    'dcc-reg-cert-upload-approver-role',
    NOW(),
    b'0',
    `target`.`tenant_id`
    FROM `tmp_dcc_reg_cert_upload_approver_target_tenant` AS `target`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `target`.`tenant_id`
     AND `category`.`code` = 'dcc'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role` AS `existing_role`
      WHERE `existing_role`.`tenant_id` = `target`.`tenant_id`
        AND `existing_role`.`code` = 'dcc_registration_certificate_approver'
   );

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_target_role`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_approver_target_role` (
    `tenant_id` bigint NOT NULL,
    `role_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`, `role_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_approver_target_role` (`tenant_id`, `role_id`)
  SELECT `role`.`tenant_id`, `role`.`id`
    FROM `system_role` AS `role`
    JOIN `tmp_dcc_reg_cert_upload_approver_target_tenant` AS `target`
      ON `target`.`tenant_id` = `role`.`tenant_id`
   WHERE `role`.`code` = 'dcc_registration_certificate_approver'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_reg_cert_upload_approver_target_role`)
     <> (SELECT COUNT(*) FROM `tmp_dcc_reg_cert_upload_approver_target_tenant`) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload approver role creation incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_required_role_menu`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_approver_required_role_menu` (
    `tenant_id` bigint NOT NULL,
    `role_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`, `role_id`, `menu_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_approver_required_role_menu` (`tenant_id`, `role_id`, `menu_id`)
  SELECT `target_role`.`tenant_id`, `target_role`.`role_id`, `menu`.`id`
    FROM `tmp_dcc_reg_cert_upload_approver_target_role` AS `target_role`
    JOIN `tmp_dcc_reg_cert_upload_approver_required_permission` AS `required`
    JOIN `system_menu` AS `menu`
      ON `menu`.`permission` = `required`.`permission`
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0';

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_dcc_reg_cert_upload_approver_required_role_menu` AS `required_role_menu`
      ON `required_role_menu`.`role_id` = `role_menu`.`role_id`
     AND `required_role_menu`.`menu_id` = `role_menu`.`menu_id`
     AND `required_role_menu`.`tenant_id` = `role_menu`.`tenant_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'dcc-reg-cert-upload-approver-role',
         `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `required_role_menu`.`role_id`,
    `required_role_menu`.`menu_id`,
    'dcc-reg-cert-upload-approver-role',
    NOW(),
    'dcc-reg-cert-upload-approver-role',
    NOW(),
    b'0',
    `required_role_menu`.`tenant_id`
    FROM `tmp_dcc_reg_cert_upload_approver_required_role_menu` AS `required_role_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing_role_menu`
      WHERE `existing_role_menu`.`tenant_id` = `required_role_menu`.`tenant_id`
        AND `existing_role_menu`.`role_id` = `required_role_menu`.`role_id`
        AND `existing_role_menu`.`menu_id` = `required_role_menu`.`menu_id`
   );

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_reg_cert_upload_approver_required_role_menu` AS `required_role_menu`
      LEFT JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`tenant_id` = `required_role_menu`.`tenant_id`
       AND `role_menu`.`role_id` = `required_role_menu`.`role_id`
       AND `role_menu`.`menu_id` = `required_role_menu`.`menu_id`
       AND `role_menu`.`deleted` = b'0'
     WHERE `role_menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload approver role permission grant incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_required_user_role`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_approver_required_user_role` (
    `tenant_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `role_id` bigint NOT NULL,
    PRIMARY KEY (`tenant_id`, `user_id`, `role_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_approver_required_user_role` (`tenant_id`, `user_id`, `role_id`)
  SELECT `initial_user`.`tenant_id`, `initial_user`.`user_id`, `target_role`.`role_id`
    FROM `tmp_dcc_reg_cert_upload_approver_initial_user` AS `initial_user`
    JOIN `tmp_dcc_reg_cert_upload_approver_target_role` AS `target_role`
      ON `target_role`.`tenant_id` = `initial_user`.`tenant_id`;

  UPDATE `system_user_role` AS `user_role`
    JOIN `tmp_dcc_reg_cert_upload_approver_required_user_role` AS `required_user_role`
      ON `required_user_role`.`tenant_id` = `user_role`.`tenant_id`
     AND `required_user_role`.`user_id` = `user_role`.`user_id`
     AND `required_user_role`.`role_id` = `user_role`.`role_id`
     SET `user_role`.`deleted` = b'0',
         `user_role`.`updater` = 'dcc-reg-cert-upload-approver-role',
         `user_role`.`update_time` = NOW();

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `required_user_role`.`user_id`,
    `required_user_role`.`role_id`,
    'dcc-reg-cert-upload-approver-role',
    NOW(),
    'dcc-reg-cert-upload-approver-role',
    NOW(),
    b'0',
    `required_user_role`.`tenant_id`
    FROM `tmp_dcc_reg_cert_upload_approver_required_user_role` AS `required_user_role`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_user_role` AS `existing_user_role`
      WHERE `existing_user_role`.`tenant_id` = `required_user_role`.`tenant_id`
        AND `existing_user_role`.`user_id` = `required_user_role`.`user_id`
        AND `existing_user_role`.`role_id` = `required_user_role`.`role_id`
   );

  UPDATE `system_user_role` AS `user_role`
    JOIN `tmp_dcc_reg_cert_upload_approver_target_role` AS `target_role`
      ON `target_role`.`tenant_id` = `user_role`.`tenant_id`
     AND `target_role`.`role_id` = `user_role`.`role_id`
    LEFT JOIN `tmp_dcc_reg_cert_upload_approver_required_user_role` AS `required_user_role`
      ON `required_user_role`.`tenant_id` = `user_role`.`tenant_id`
     AND `required_user_role`.`user_id` = `user_role`.`user_id`
     AND `required_user_role`.`role_id` = `user_role`.`role_id`
     SET `user_role`.`deleted` = b'1',
         `user_role`.`updater` = 'dcc-reg-cert-upload-approver-role',
         `user_role`.`update_time` = NOW()
   WHERE `required_user_role`.`user_id` IS NULL
     AND `user_role`.`creator` = 'dcc-reg-cert-upload-approver-role'
     AND `user_role`.`deleted` = b'0';

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_reg_cert_upload_approver_required_user_role` AS `required_user_role`
      LEFT JOIN `system_user_role` AS `user_role`
        ON `user_role`.`tenant_id` = `required_user_role`.`tenant_id`
       AND `user_role`.`user_id` = `required_user_role`.`user_id`
       AND `user_role`.`role_id` = `required_user_role`.`role_id`
       AND `user_role`.`deleted` = b'0'
     WHERE `user_role`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload approver user binding incomplete';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_user_role` AS `user_role`
      JOIN `tmp_dcc_reg_cert_upload_approver_target_role` AS `target_role`
        ON `target_role`.`tenant_id` = `user_role`.`tenant_id`
       AND `target_role`.`role_id` = `user_role`.`role_id`
      LEFT JOIN `tmp_dcc_reg_cert_upload_approver_required_user_role` AS `required_user_role`
        ON `required_user_role`.`tenant_id` = `user_role`.`tenant_id`
       AND `required_user_role`.`user_id` = `user_role`.`user_id`
       AND `required_user_role`.`role_id` = `user_role`.`role_id`
     WHERE `required_user_role`.`user_id` IS NULL
       AND `user_role`.`creator` = 'dcc-reg-cert-upload-approver-role'
       AND `user_role`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload approver stale migration user binding cleanup failed';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_required_user_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_required_role_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_target_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_target_tenant`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_initial_user`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_approver_required_permission`;
END//
DELIMITER ;

CALL ensure_dcc_reg_cert_upload_role_20260829();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_role_20260829;

COMMIT;
