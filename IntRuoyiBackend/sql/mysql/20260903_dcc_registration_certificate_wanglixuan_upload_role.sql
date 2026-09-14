-- release-migration: allowedEnvironments=test; dependsOn=20260903_dcc_registration_certificate_upload_view_permission; type=permission; riskLevel=low
-- Assign the test-tenant wanglixuan account to the registration-certificate upload role.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_dcc_wanglixuan_upload_role_20260903;
DELIMITER //
CREATE PROCEDURE ensure_dcc_wanglixuan_upload_role_20260903()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_wanglixuan_upload_target_user`;
  CREATE TEMPORARY TABLE `tmp_dcc_wanglixuan_upload_target_user` (
    `user_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`user_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_wanglixuan_upload_target_user` (`user_id`, `tenant_id`)
  SELECT `target_user`.`id`, `target_user`.`tenant_id`
    FROM `system_users` AS `target_user`
   WHERE `target_user`.`username` = 'wanglixuan'
     AND `target_user`.`tenant_id` = 1
     AND `target_user`.`status` = 0
     AND `target_user`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_wanglixuan_upload_target_user`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous wanglixuan user in tenant 1';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_wanglixuan_upload_target_role`;
  CREATE TEMPORARY TABLE `tmp_dcc_wanglixuan_upload_target_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_dcc_wanglixuan_upload_target_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
   WHERE `role`.`code` = 'dcc_registration_certificate_upload'
     AND `role`.`tenant_id` = 1
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_dcc_wanglixuan_upload_target_role`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous registration certificate upload role for wanglixuan';
  END IF;

  UPDATE `system_user_role` AS `user_role`
    JOIN `tmp_dcc_wanglixuan_upload_target_user` AS `target_user`
      ON `target_user`.`user_id` = `user_role`.`user_id`
     AND `target_user`.`tenant_id` = `user_role`.`tenant_id`
    JOIN `tmp_dcc_wanglixuan_upload_target_role` AS `target_role`
      ON `target_role`.`role_id` = `user_role`.`role_id`
     AND `target_role`.`tenant_id` = `user_role`.`tenant_id`
     SET `user_role`.`deleted` = b'0',
         `user_role`.`updater` = 'dcc-wanglixuan-upload-role',
         `user_role`.`update_time` = NOW();

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target_user`.`user_id`,
    `target_role`.`role_id`,
    'dcc-wanglixuan-upload-role',
    NOW(),
    'dcc-wanglixuan-upload-role',
    NOW(),
    b'0',
    `target_user`.`tenant_id`
    FROM `tmp_dcc_wanglixuan_upload_target_user` AS `target_user`
    JOIN `tmp_dcc_wanglixuan_upload_target_role` AS `target_role`
      ON `target_role`.`tenant_id` = `target_user`.`tenant_id`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_user_role` AS `existing`
      WHERE `existing`.`user_id` = `target_user`.`user_id`
        AND `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`tenant_id` = `target_user`.`tenant_id`
   );

  IF NOT EXISTS (
    SELECT 1
      FROM `system_user_role` AS `user_role`
      JOIN `tmp_dcc_wanglixuan_upload_target_user` AS `target_user`
        ON `target_user`.`user_id` = `user_role`.`user_id`
       AND `target_user`.`tenant_id` = `user_role`.`tenant_id`
      JOIN `tmp_dcc_wanglixuan_upload_target_role` AS `target_role`
        ON `target_role`.`role_id` = `user_role`.`role_id`
       AND `target_role`.`tenant_id` = `user_role`.`tenant_id`
     WHERE `user_role`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Wanglixuan registration certificate upload role grant incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_wanglixuan_upload_target_role`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_wanglixuan_upload_target_user`;
END//
DELIMITER ;

CALL ensure_dcc_wanglixuan_upload_role_20260903();
DROP PROCEDURE IF EXISTS ensure_dcc_wanglixuan_upload_role_20260903;

COMMIT;
