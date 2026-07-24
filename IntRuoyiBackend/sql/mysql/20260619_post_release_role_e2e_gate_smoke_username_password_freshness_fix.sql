-- release-migration: allowedEnvironments=test,backup; dependsOn=20260619_post_release_role_e2e_gate_smoke_username_fix; type=permission; riskLevel=medium
-- Refresh login-compatible smart scheduling smoke accounts so password policy accepts the smoke flow.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS apply_post_release_role_e2e_gate_smoke_password_freshness_fix;

DELIMITER $$

CREATE PROCEDURE apply_post_release_role_e2e_gate_smoke_password_freshness_fix()
BEGIN
  DECLARE admin_tenant_id BIGINT DEFAULT NULL;
  DECLARE fresh_password_count INT DEFAULT 0;

  SELECT `id` INTO admin_tenant_id
  FROM `system_tenant`
  WHERE `name` = '芋道源码' AND `deleted` = b'0'
  LIMIT 1;

  IF admin_tenant_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release smoke password freshness fix requires tenant 芋道源码';
  END IF;

  UPDATE `system_users`
  SET `password_update_time` = NOW(),
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `deleted` = b'0'
    AND `status` = 0
    AND `username` IN ('messmokeerp', 'messmokesupervisor', 'messmokenonapprover');

  SELECT COUNT(*) INTO fresh_password_count
  FROM `system_users`
  WHERE `tenant_id` = admin_tenant_id
    AND `deleted` = b'0'
    AND `status` = 0
    AND `username` IN ('messmokeerp', 'messmokesupervisor', 'messmokenonapprover')
    AND `password_update_time` IS NOT NULL
    AND `password_update_time` >= DATE_SUB(NOW(), INTERVAL 1 MINUTE);

  IF fresh_password_count <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release smoke password freshness fix did not refresh all smoke users';
  END IF;
END$$

DELIMITER ;

CALL apply_post_release_role_e2e_gate_smoke_password_freshness_fix();

DROP PROCEDURE IF EXISTS apply_post_release_role_e2e_gate_smoke_password_freshness_fix;
