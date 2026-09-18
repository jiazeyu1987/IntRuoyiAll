-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_production_release_roles; type=data; riskLevel=medium

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS ensure_mes_pqc_release_owner_admin_assignment_20260918;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pqc_release_owner_admin_assignment_20260918()
BEGIN
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;
  DECLARE v_release_owner_role_id BIGINT DEFAULT NULL;

  IF (
    SELECT COUNT(*)
      FROM `system_users` AS `user`
      JOIN `system_tenant` AS `tenant`
        ON `tenant`.`id` = `user`.`tenant_id`
       AND `tenant`.`deleted` = b'0'
     WHERE `user`.`tenant_id` = 1
       AND `user`.`username` = 'admin'
       AND `user`.`status` = 0
       AND `user`.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or duplicate enabled admin user in tenant 1';
  END IF;

  IF (
    SELECT COUNT(*)
      FROM `system_role` AS `role`
     WHERE `role`.`tenant_id` = 1
       AND `role`.`code` = 'MES_PQC_RELEASE_OWNER'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Admin user is missing active MES_PQC_RELEASE_OWNER role in tenant 1';
  END IF;

  SELECT `user`.`id`
    INTO v_admin_user_id
    FROM `system_users` AS `user`
   WHERE `user`.`tenant_id` = 1
     AND `user`.`username` = 'admin'
     AND `user`.`status` = 0
     AND `user`.`deleted` = b'0';

  SELECT `role`.`id`
    INTO v_release_owner_role_id
    FROM `system_role` AS `role`
   WHERE `role`.`tenant_id` = 1
     AND `role`.`code` = 'MES_PQC_RELEASE_OWNER'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  UPDATE `system_user_role` AS `user_role`
     SET `user_role`.`deleted` = b'0',
         `user_role`.`updater` = 'mes-pqc-release-owner-admin-assignment',
         `user_role`.`update_time` = NOW(),
         `user_role`.`tenant_id` = 1
   WHERE `user_role`.`user_id` = v_admin_user_id
     AND `user_role`.`role_id` = v_release_owner_role_id
     AND `user_role`.`tenant_id` = 1
     AND `user_role`.`deleted` = b'1';

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    v_admin_user_id, v_release_owner_role_id,
    'mes-pqc-release-owner-admin-assignment', NOW(),
    'mes-pqc-release-owner-admin-assignment', NOW(), b'0', 1
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_user_role` AS `existing`
      WHERE `existing`.`user_id` = v_admin_user_id
        AND `existing`.`role_id` = v_release_owner_role_id
        AND `existing`.`tenant_id` = 1
        AND `existing`.`deleted` = b'0'
   );

  IF (
    SELECT COUNT(*)
      FROM `system_user_role` AS `user_role`
     WHERE `user_role`.`user_id` = v_admin_user_id
       AND `user_role`.`role_id` = v_release_owner_role_id
       AND `user_role`.`tenant_id` = 1
       AND `user_role`.`deleted` = b'0'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'admin role binding is incomplete';
  END IF;
END$$
DELIMITER ;

START TRANSACTION;
CALL ensure_mes_pqc_release_owner_admin_assignment_20260918();
COMMIT;

DROP PROCEDURE IF EXISTS ensure_mes_pqc_release_owner_admin_assignment_20260918;
