-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_dcc_registration_certificate_upload_role_category,20261023_registration_certificate_management_menu_hierarchy_identity; type=permission; riskLevel=medium; requiresTargetPreflight=true; supersedes=20260903_dcc_registration_certificate_upload_view_permission; applyOrder=10
-- Grant the upload role the exact registration list and its existing management parent in one transaction.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_view_permission_20260903_identity;
DELIMITER //
CREATE PROCEDURE ensure_dcc_reg_cert_upload_view_permission_20260903_identity()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_upload_view_role`;
  CREATE TEMPORARY TABLE `tmp_dcc_registration_certificate_upload_view_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_registration_certificate_upload_view_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
   WHERE `role`.`code` = 'dcc_registration_certificate_upload'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_registration_certificate_upload_view_role`) = 0
     OR EXISTS (
       SELECT 1
         FROM `system_role` AS `role`
        WHERE `role`.`code` = 'dcc_registration_certificate_upload'
          AND `role`.`status` = 0
          AND `role`.`deleted` = b'0'
        GROUP BY `role`.`tenant_id`
       HAVING COUNT(*) <> 1
     ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous registration certificate upload role';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_query_permission_menu`;
  CREATE TEMPORARY TABLE `tmp_dcc_registration_certificate_query_permission_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_registration_certificate_query_permission_menu` (`menu_id`)
  SELECT `menu`.`id`
    FROM `system_menu` AS `menu`
   WHERE `menu`.`id` = 991601
     AND `menu`.`permission` = 'dcc:registration-certificate:query-current'
     AND `menu`.`type` = 2
     AND `menu`.`path` = '/mdm/registration-certificate'
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0';

  INSERT INTO `tmp_dcc_registration_certificate_query_permission_menu` (`menu_id`)
  SELECT `id` FROM `system_menu`
   WHERE `id`=990253 AND `type`=1 AND `parent_id`=990200
     AND `path`='registration-certificate-management' AND `permission`=''
     AND `component` IS NULL AND `component_name` IS NULL
     AND `status`=0 AND `deleted`=b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_registration_certificate_query_permission_menu`) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous registration certificate query permission menu';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_dcc_registration_certificate_upload_view_role` AS `upload_role`
      ON `upload_role`.`role_id` = `role_menu`.`role_id`
     AND `upload_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_dcc_registration_certificate_query_permission_menu` AS `query_menu`
      ON `query_menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'dcc-registration-certificate-upload-view-permission',
         `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `upload_role`.`role_id`,
    `query_menu`.`menu_id`,
    'dcc-registration-certificate-upload-view-permission',
    NOW(),
    'dcc-registration-certificate-upload-view-permission',
    NOW(),
    b'0',
    `upload_role`.`tenant_id`
    FROM `tmp_dcc_registration_certificate_upload_view_role` AS `upload_role`
    CROSS JOIN `tmp_dcc_registration_certificate_query_permission_menu` AS `query_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `upload_role`.`role_id`
        AND `existing`.`tenant_id` = `upload_role`.`tenant_id`
        AND `existing`.`menu_id` = `query_menu`.`menu_id`
   );

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_registration_certificate_upload_view_role` AS `upload_role`
      CROSS JOIN `tmp_dcc_registration_certificate_query_permission_menu` AS `query_menu`
      LEFT JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`role_id` = `upload_role`.`role_id`
       AND `role_menu`.`tenant_id` = `upload_role`.`tenant_id`
       AND `role_menu`.`menu_id` = `query_menu`.`menu_id`
       AND `role_menu`.`deleted` = b'0'
     WHERE `role_menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload role query permission grant incomplete';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `tmp_dcc_registration_certificate_upload_view_role` AS `upload_role`
        ON `upload_role`.`role_id` = `role_menu`.`role_id`
       AND `upload_role`.`tenant_id` = `role_menu`.`tenant_id`
      JOIN `tmp_dcc_registration_certificate_query_permission_menu` AS `query_menu`
        ON `query_menu`.`menu_id` = `role_menu`.`menu_id`
     WHERE `role_menu`.`deleted` = b'0'
     GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload role query permission has duplicate active bindings';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_query_permission_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_upload_view_role`;
  COMMIT;
END//
DELIMITER ;

CALL ensure_dcc_reg_cert_upload_view_permission_20260903_identity();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_view_permission_20260903_identity;
