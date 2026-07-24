-- release-migration: allowedEnvironments=test,backup; dependsOn=20260618_post_release_role_e2e_gate; type=config; riskLevel=medium
-- Fix post-release role E2E accounts to the explicitly authorized password `111111`.

DELIMITER $$

DROP PROCEDURE IF EXISTS apply_post_release_role_e2e_gate_password_fix $$
CREATE PROCEDURE apply_post_release_role_e2e_gate_password_fix()
BEGIN
  DECLARE admin_tenant_id bigint DEFAULT 1;
  DECLARE active_user_count int DEFAULT 0;
  DECLARE password_hash varchar(100) DEFAULT '$2a$10$EzpuIftrlM8pmMAKMbPCqeGV/NOHGXMGwH8nKg3G0eNJr8Sg0hs0K';

  IF NOT EXISTS (
    SELECT 1
      FROM `system_tenant`
     WHERE `id` = admin_tenant_id
       AND `name` = '芋道源码'
       AND `deleted` = b'0'
       AND `status` = 0
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release role e2e password fix requires active tenant 芋道源码(id=1)';
  END IF;

  SELECT COUNT(*)
    INTO active_user_count
    FROM `system_users`
   WHERE `tenant_id` = admin_tenant_id
     AND `username` IN ('gaomin', 'zhaojie', 'wangsiyu')
     AND `deleted` = b'0';

  IF active_user_count <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release role e2e password fix requires gaomin, zhaojie and wangsiyu in tenant 芋道源码';
  END IF;

  UPDATE `system_users`
     SET `password` = password_hash,
         `password_update_time` = NOW(),
         `status` = 0,
         `updater` = 'post-release-role-e2e-gate',
         `update_time` = NOW()
   WHERE `tenant_id` = admin_tenant_id
     AND `username` IN ('gaomin', 'zhaojie', 'wangsiyu')
     AND `deleted` = b'0';
END $$

CALL apply_post_release_role_e2e_gate_password_fix() $$
DROP PROCEDURE IF EXISTS apply_post_release_role_e2e_gate_password_fix $$

DELIMITER ;
