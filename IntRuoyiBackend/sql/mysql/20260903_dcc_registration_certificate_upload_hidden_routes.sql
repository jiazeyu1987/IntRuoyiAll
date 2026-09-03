-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_dcc_registration_certificate_upload_view_permission; type=permission; riskLevel=low
-- Grant the registration-certificate upload role the hidden detail and history routes required by list navigation.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_hidden_routes_20260903;
DELIMITER //
CREATE PROCEDURE ensure_dcc_reg_cert_upload_hidden_routes_20260903()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_hidden_role`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_hidden_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_hidden_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
   WHERE `role`.`code` = 'dcc_registration_certificate_upload'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_reg_cert_upload_hidden_role`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate upload role for hidden routes';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_hidden_menu`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_hidden_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_hidden_menu` (`menu_id`)
  SELECT `menu`.`id`
    FROM `system_menu` AS `menu`
   WHERE (
     (`menu`.`id` = 990231
      AND `menu`.`path` = '/mdm/registration-certificate/detail/:id'
      AND `menu`.`component` = 'dcc/registration-certificate/detail/index'
      AND `menu`.`component_name` = 'DccRegistrationCertificateDetail')
     OR
     (`menu`.`id` = 990232
      AND `menu`.`path` = '/mdm/registration-certificate/history/:id'
      AND `menu`.`component` = 'dcc/registration-certificate/history/index'
      AND `menu`.`component_name` = 'DccRegistrationCertificateHistory')
   )
     AND `menu`.`permission` = 'dcc:registration-certificate:query-current'
     AND `menu`.`type` = 2
     AND `menu`.`visible` = b'0'
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_reg_cert_upload_hidden_menu`) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate detail or history hidden route contract mismatch';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_dcc_reg_cert_upload_hidden_role` AS `upload_role`
      ON `upload_role`.`role_id` = `role_menu`.`role_id`
     AND `upload_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_dcc_reg_cert_upload_hidden_menu` AS `hidden_menu`
      ON `hidden_menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'dcc-registration-certificate-upload-hidden-routes',
         `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `upload_role`.`role_id`, `hidden_menu`.`menu_id`,
    'dcc-registration-certificate-upload-hidden-routes', NOW(),
    'dcc-registration-certificate-upload-hidden-routes', NOW(), b'0', `upload_role`.`tenant_id`
    FROM `tmp_dcc_reg_cert_upload_hidden_role` AS `upload_role`
    CROSS JOIN `tmp_dcc_reg_cert_upload_hidden_menu` AS `hidden_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `upload_role`.`role_id`
        AND `existing`.`tenant_id` = `upload_role`.`tenant_id`
        AND `existing`.`menu_id` = `hidden_menu`.`menu_id`
   );

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_reg_cert_upload_hidden_role` AS `upload_role`
      CROSS JOIN `tmp_dcc_reg_cert_upload_hidden_menu` AS `hidden_menu`
      LEFT JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`role_id` = `upload_role`.`role_id`
       AND `role_menu`.`tenant_id` = `upload_role`.`tenant_id`
       AND `role_menu`.`menu_id` = `hidden_menu`.`menu_id`
       AND `role_menu`.`deleted` = b'0'
     WHERE `role_menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload hidden route grant incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_hidden_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_hidden_role`;
END//
DELIMITER ;

CALL ensure_dcc_reg_cert_upload_hidden_routes_20260903();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_hidden_routes_20260903;

COMMIT;
