-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260829_dcc_registration_certificate_upload_approver_role,20260707_system_role_category_management,20260816_dcc_registration_certificate_menu; type=permission; riskLevel=low
-- Create the registration-certificate upload role, grant its upload permission, and classify it with the registration manager.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_role_category_20260903;
DELIMITER //
CREATE PROCEDURE ensure_dcc_reg_cert_upload_role_category_20260903()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_department_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_dcc_registration_department_target_tenant` (
    `tenant_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_registration_department_target_tenant` (`tenant_id`)
  SELECT `role`.`tenant_id`
    FROM `system_role` AS `role`
   WHERE `role`.`code` = 'dcc_registration_certificate_approver'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0'
   GROUP BY `role`.`tenant_id`;

  IF (SELECT COUNT(*) FROM `tmp_dcc_registration_department_target_tenant`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous target registration certificate role';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
      JOIN `tmp_dcc_registration_department_target_tenant` AS `target`
        ON `target`.`tenant_id` = `role`.`tenant_id`
     WHERE `role`.`code` = 'dcc_registration_certificate_approver'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
     GROUP BY `role`.`tenant_id`
    HAVING COUNT(*) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous target registration certificate role';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role_category` AS `category`
      JOIN `tmp_dcc_registration_department_target_tenant` AS `target`
        ON `target`.`tenant_id` = `category`.`tenant_id`
     WHERE `category`.`code` = 'registration'
     GROUP BY `category`.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous registration department role category';
  END IF;

  INSERT INTO `system_role_category` (
    `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    '注册部',
    'registration',
    45,
    0,
    '注册部权限角色',
    'dcc-registration-certificate-upload-role-category',
    NOW(),
    'dcc-registration-certificate-upload-role-category',
    NOW(),
    b'0',
    `target`.`tenant_id`
    FROM `tmp_dcc_registration_department_target_tenant` AS `target`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_category` AS `existing`
      WHERE `existing`.`tenant_id` = `target`.`tenant_id`
        AND `existing`.`code` = 'registration'
   );

  UPDATE `system_role_category` AS `category`
    JOIN `tmp_dcc_registration_department_target_tenant` AS `target`
      ON `target`.`tenant_id` = `category`.`tenant_id`
     SET `category`.`name` = '注册部',
         `category`.`sort` = 45,
         `category`.`status` = 0,
         `category`.`remark` = '注册部权限角色',
         `category`.`deleted` = b'0',
         `category`.`updater` = 'dcc-registration-certificate-upload-role-category',
         `category`.`update_time` = NOW()
   WHERE `category`.`code` = 'registration';

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_registration_department_target_tenant` AS `target`
      LEFT JOIN `system_role_category` AS `category`
        ON `category`.`tenant_id` = `target`.`tenant_id`
       AND `category`.`code` = 'registration'
       AND `category`.`status` = 0
       AND `category`.`deleted` = b'0'
     GROUP BY `target`.`tenant_id`
    HAVING COUNT(`category`.`id`) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous registration department role category';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role` AS `role`
      JOIN `tmp_dcc_registration_department_target_tenant` AS `target`
        ON `target`.`tenant_id` = `role`.`tenant_id`
     WHERE `role`.`code` = 'dcc_registration_certificate_upload'
     GROUP BY `role`.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate registration certificate upload role code in target tenant';
  END IF;

  INSERT INTO `system_role` (
    `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`,
    `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    '注册证上传',
    'dcc_registration_certificate_upload',
    845,
    `category`.`id`,
    1,
    '',
    0,
    2,
    '注册证上传权限角色，权限由注册证上传迁移精确维护',
    'dcc-registration-certificate-upload-role-category',
    NOW(),
    'dcc-registration-certificate-upload-role-category',
    NOW(),
    b'0',
    `target`.`tenant_id`
    FROM `tmp_dcc_registration_department_target_tenant` AS `target`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `target`.`tenant_id`
     AND `category`.`code` = 'registration'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role` AS `existing`
      WHERE `existing`.`tenant_id` = `target`.`tenant_id`
        AND `existing`.`code` = 'dcc_registration_certificate_upload'
   );

  UPDATE `system_role` AS `role`
    JOIN `tmp_dcc_registration_department_target_tenant` AS `target`
      ON `target`.`tenant_id` = `role`.`tenant_id`
    JOIN `system_role_category` AS `category`
      ON `category`.`tenant_id` = `target`.`tenant_id`
     AND `category`.`code` = 'registration'
     AND `category`.`status` = 0
     AND `category`.`deleted` = b'0'
     SET `role`.`name` = CASE `role`.`code`
                            WHEN 'dcc_registration_certificate_upload' THEN '注册证上传'
                            WHEN 'dcc_registration_certificate_approver' THEN '注册部经理'
                          END,
         `role`.`sort` = CASE `role`.`code`
                            WHEN 'dcc_registration_certificate_upload' THEN 845
                            WHEN 'dcc_registration_certificate_approver' THEN 846
                          END,
         `role`.`category_id` = `category`.`id`,
         `role`.`status` = 0,
         `role`.`type` = 2,
         `role`.`deleted` = b'0',
         `role`.`updater` = 'dcc-registration-certificate-upload-role-category',
         `role`.`update_time` = NOW()
   WHERE `role`.`code` IN ('dcc_registration_certificate_upload', 'dcc_registration_certificate_approver');

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_upload_role`;
  CREATE TEMPORARY TABLE `tmp_dcc_registration_certificate_upload_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_registration_certificate_upload_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
    JOIN `tmp_dcc_registration_department_target_tenant` AS `target`
      ON `target`.`tenant_id` = `role`.`tenant_id`
   WHERE `role`.`code` = 'dcc_registration_certificate_upload'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_registration_certificate_upload_role`)
     <> (SELECT COUNT(*) FROM `tmp_dcc_registration_department_target_tenant`) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload role creation incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_upload_permission_menu`;
  CREATE TEMPORARY TABLE `tmp_dcc_registration_certificate_upload_permission_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_registration_certificate_upload_permission_menu` (`menu_id`)
  SELECT `menu`.`id`
    FROM `system_menu` AS `menu`
   WHERE `menu`.`permission` = 'dcc:registration-certificate:upload:create'
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_registration_certificate_upload_permission_menu`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous registration certificate upload permission menu';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_dcc_registration_certificate_upload_role` AS `upload_role`
      ON `upload_role`.`role_id` = `role_menu`.`role_id`
     AND `upload_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_dcc_registration_certificate_upload_permission_menu` AS `permission_menu`
      ON `permission_menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'dcc-registration-certificate-upload-role-category',
         `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `upload_role`.`role_id`,
    `permission_menu`.`menu_id`,
    'dcc-registration-certificate-upload-role-category',
    NOW(),
    'dcc-registration-certificate-upload-role-category',
    NOW(),
    b'0',
    `upload_role`.`tenant_id`
    FROM `tmp_dcc_registration_certificate_upload_role` AS `upload_role`
    CROSS JOIN `tmp_dcc_registration_certificate_upload_permission_menu` AS `permission_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `upload_role`.`role_id`
        AND `existing`.`tenant_id` = `upload_role`.`tenant_id`
        AND `existing`.`menu_id` = `permission_menu`.`menu_id`
   );

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_registration_certificate_upload_role` AS `upload_role`
      CROSS JOIN `tmp_dcc_registration_certificate_upload_permission_menu` AS `permission_menu`
      LEFT JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`role_id` = `upload_role`.`role_id`
       AND `role_menu`.`tenant_id` = `upload_role`.`tenant_id`
       AND `role_menu`.`menu_id` = `permission_menu`.`menu_id`
       AND `role_menu`.`deleted` = b'0'
     WHERE `role_menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload role permission grant incomplete';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `tmp_dcc_registration_certificate_upload_role` AS `upload_role`
        ON `upload_role`.`role_id` = `role_menu`.`role_id`
       AND `upload_role`.`tenant_id` = `role_menu`.`tenant_id`
      JOIN `tmp_dcc_registration_certificate_upload_permission_menu` AS `permission_menu`
        ON `permission_menu`.`menu_id` = `role_menu`.`menu_id`
     WHERE `role_menu`.`deleted` = b'0'
     GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload role permission has duplicate active bindings';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_upload_permission_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_upload_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_department_target_tenant`;
END//
DELIMITER ;

CALL ensure_dcc_reg_cert_upload_role_category_20260903();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_role_category_20260903;

COMMIT;
