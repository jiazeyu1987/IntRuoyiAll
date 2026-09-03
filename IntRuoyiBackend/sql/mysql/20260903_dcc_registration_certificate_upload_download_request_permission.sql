-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_dcc_registration_certificate_upload_hidden_routes; type=permission; riskLevel=low
-- Allow the registration-certificate upload role to submit formal download access requests from detail pages.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_download_request_20260903;
DELIMITER //
CREATE PROCEDURE ensure_dcc_reg_cert_upload_download_request_20260903()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_download_role`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_download_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_download_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
   WHERE `role`.`code` = 'dcc_registration_certificate_upload'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_reg_cert_upload_download_role`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate upload role for download request permission';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_download_menu`;
  CREATE TEMPORARY TABLE `tmp_dcc_reg_cert_upload_download_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_reg_cert_upload_download_menu` (`menu_id`)
  SELECT `menu`.`id`
    FROM `system_menu` AS `menu`
   WHERE `menu`.`id` = 990237
     AND `menu`.`permission` = 'dcc:registration-certificate:access-request:create'
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_reg_cert_upload_download_menu`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate download request menu contract mismatch';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_dcc_reg_cert_upload_download_role` AS `upload_role`
      ON `upload_role`.`role_id` = `role_menu`.`role_id`
     AND `upload_role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `tmp_dcc_reg_cert_upload_download_menu` AS `download_menu`
      ON `download_menu`.`menu_id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'dcc-registration-certificate-upload-download-request',
         `role_menu`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `upload_role`.`role_id`, `download_menu`.`menu_id`,
    'dcc-registration-certificate-upload-download-request', NOW(),
    'dcc-registration-certificate-upload-download-request', NOW(), b'0', `upload_role`.`tenant_id`
    FROM `tmp_dcc_reg_cert_upload_download_role` AS `upload_role`
    CROSS JOIN `tmp_dcc_reg_cert_upload_download_menu` AS `download_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `upload_role`.`role_id`
        AND `existing`.`tenant_id` = `upload_role`.`tenant_id`
        AND `existing`.`menu_id` = `download_menu`.`menu_id`
   );

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_reg_cert_upload_download_role` AS `upload_role`
      CROSS JOIN `tmp_dcc_reg_cert_upload_download_menu` AS `download_menu`
      LEFT JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`role_id` = `upload_role`.`role_id`
       AND `role_menu`.`tenant_id` = `upload_role`.`tenant_id`
       AND `role_menu`.`menu_id` = `download_menu`.`menu_id`
       AND `role_menu`.`deleted` = b'0'
     WHERE `role_menu`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload download request permission grant incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_download_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_reg_cert_upload_download_role`;
END//
DELIMITER ;

CALL ensure_dcc_reg_cert_upload_download_request_20260903();
DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_upload_download_request_20260903;

COMMIT;
