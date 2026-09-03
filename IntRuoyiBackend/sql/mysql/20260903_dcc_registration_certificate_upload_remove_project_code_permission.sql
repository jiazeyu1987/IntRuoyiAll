-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_dcc_registration_certificate_upload_action_permissions; type=permission; riskLevel=low
-- Keep the registration-certificate upload role isolated from the DCC project-code basic-data page.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS remove_dcc_reg_cert_upload_project_code_20260903;
DELIMITER //
CREATE PROCEDURE remove_dcc_reg_cert_upload_project_code_20260903()
BEGIN
  UPDATE `system_role_menu` AS `role_menu`
    JOIN `system_role` AS `role`
      ON `role`.`id` = `role_menu`.`role_id`
     AND `role`.`tenant_id` = `role_menu`.`tenant_id`
    JOIN `system_menu` AS `menu`
      ON `menu`.`id` = `role_menu`.`menu_id`
     SET `role_menu`.`deleted` = b'1',
         `role_menu`.`updater` = 'dcc-registration-certificate-upload-project-code-isolation',
         `role_menu`.`update_time` = NOW()
   WHERE `role`.`code` = 'dcc_registration_certificate_upload'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0'
     AND `menu`.`permission` = 'dcc:project-code:query'
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0'
     AND `role_menu`.`deleted` = b'0';

  IF EXISTS (
    SELECT 1
      FROM `system_role_menu` AS `role_menu`
      JOIN `system_role` AS `role`
        ON `role`.`id` = `role_menu`.`role_id`
       AND `role`.`tenant_id` = `role_menu`.`tenant_id`
      JOIN `system_menu` AS `menu`
        ON `menu`.`id` = `role_menu`.`menu_id`
     WHERE `role`.`code` = 'dcc_registration_certificate_upload'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
       AND `menu`.`permission` = 'dcc:project-code:query'
       AND `menu`.`status` = 0
       AND `menu`.`deleted` = b'0'
       AND `role_menu`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate upload role still has DCC project code permission';
  END IF;
END//
DELIMITER ;

CALL remove_dcc_reg_cert_upload_project_code_20260903();
DROP PROCEDURE IF EXISTS remove_dcc_reg_cert_upload_project_code_20260903;

COMMIT;
