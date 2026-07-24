-- release-migration: allowedEnvironments=test,backup; dependsOn=20260618_post_release_role_e2e_gate_smoke_contract; type=permission; riskLevel=medium
-- Fix smart scheduling smoke login accounts: usernames must match AuthLoginReqVO pattern ^[a-zA-Z0-9]{4,30}$.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS apply_post_release_role_e2e_gate_smoke_username_fix;

DELIMITER $$

CREATE PROCEDURE apply_post_release_role_e2e_gate_smoke_username_fix()
BEGIN
  DECLARE admin_tenant_id BIGINT DEFAULT NULL;
  DECLARE erp_creator_role_id BIGINT DEFAULT NULL;
  DECLARE supervisor_role_id BIGINT DEFAULT NULL;
  DECLARE non_approver_role_id BIGINT DEFAULT NULL;
  DECLARE erp_creator_user_id BIGINT DEFAULT NULL;
  DECLARE supervisor_user_id BIGINT DEFAULT NULL;
  DECLARE non_approver_user_id BIGINT DEFAULT NULL;
  DECLARE active_user_count INT DEFAULT 0;
  DECLARE role_bind_count INT DEFAULT 0;
  DECLARE authorized_password_hash VARCHAR(100) DEFAULT '$2a$10$EzpuIftrlM8pmMAKMbPCqeGV/NOHGXMGwH8nKg3G0eNJr8Sg0hs0K';

  SELECT `id` INTO admin_tenant_id
  FROM `system_tenant`
  WHERE `name` = '芋道源码' AND `deleted` = b'0'
  LIMIT 1;

  IF admin_tenant_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release smoke username fix requires tenant 芋道源码';
  END IF;

  SELECT `id` INTO erp_creator_role_id
  FROM `system_role`
  WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_erp_creator' AND `deleted` = b'0'
  LIMIT 1;
  SELECT `id` INTO supervisor_role_id
  FROM `system_role`
  WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_supervisor' AND `deleted` = b'0'
  LIMIT 1;
  SELECT `id` INTO non_approver_role_id
  FROM `system_role`
  WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_non_approver' AND `deleted` = b'0'
  LIMIT 1;

  IF erp_creator_role_id IS NULL OR supervisor_role_id IS NULL OR non_approver_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release smoke username fix requires smoke roles';
  END IF;

  INSERT INTO `system_users` (
    `username`, `password`, `nickname`, `remark`, `dept_id`, `post_ids`, `email`, `mobile`, `sex`, `avatar`,
    `status`, `login_ip`, `login_date`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT src.`username`, authorized_password_hash, src.`nickname`, src.`remark`, NULL, '', '', '', 0, '',
         0, '', NULL, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  FROM (
    SELECT 'messmokeerp' AS `username`, 'ERP冒烟创建员' AS `nickname`, 'post-release smart scheduling ERP creator account' AS `remark`
    UNION ALL SELECT 'messmokesupervisor', 'eDHR矩阵-审批人', 'post-release smart scheduling supervisor account'
    UNION ALL SELECT 'messmokenonapprover', '报工冒烟非审批员', 'post-release smart scheduling non approver account'
  ) src
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_users` u
    WHERE u.`tenant_id` = admin_tenant_id AND u.`username` = src.`username` AND u.`deleted` = b'0'
  );

  UPDATE `system_users`
  SET `password` = authorized_password_hash,
      `status` = 0,
      `nickname` = CASE `username`
        WHEN 'messmokeerp' THEN 'ERP冒烟创建员'
        WHEN 'messmokesupervisor' THEN 'eDHR矩阵-审批人'
        WHEN 'messmokenonapprover' THEN '报工冒烟非审批员'
        ELSE `nickname`
      END,
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `deleted` = b'0'
    AND `username` IN ('messmokeerp', 'messmokesupervisor', 'messmokenonapprover');

  SELECT `id` INTO erp_creator_user_id FROM `system_users`
  WHERE `tenant_id` = admin_tenant_id AND `username` = 'messmokeerp' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO supervisor_user_id FROM `system_users`
  WHERE `tenant_id` = admin_tenant_id AND `username` = 'messmokesupervisor' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO non_approver_user_id FROM `system_users`
  WHERE `tenant_id` = admin_tenant_id AND `username` = 'messmokenonapprover' AND `deleted` = b'0' LIMIT 1;

  INSERT INTO `system_user_role` (`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT x.`user_id`, x.`role_id`, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  FROM (
    SELECT erp_creator_user_id AS `user_id`, erp_creator_role_id AS `role_id`
    UNION ALL SELECT supervisor_user_id, supervisor_role_id
    UNION ALL SELECT non_approver_user_id, non_approver_role_id
  ) x
  WHERE x.`user_id` IS NOT NULL
    AND x.`role_id` IS NOT NULL
    AND NOT EXISTS (
      SELECT 1 FROM `system_user_role` ur
      WHERE ur.`tenant_id` = admin_tenant_id AND ur.`user_id` = x.`user_id` AND ur.`role_id` = x.`role_id` AND ur.`deleted` = b'0'
    );

  SELECT COUNT(*) INTO active_user_count
  FROM `system_users`
  WHERE `tenant_id` = admin_tenant_id
    AND `deleted` = b'0'
    AND `status` = 0
    AND `username` IN ('messmokeerp', 'messmokesupervisor', 'messmokenonapprover');

  SELECT COUNT(*) INTO role_bind_count
  FROM `system_user_role`
  WHERE `tenant_id` = admin_tenant_id
    AND `deleted` = b'0'
    AND (`user_id`, `role_id`) IN (
      (erp_creator_user_id, erp_creator_role_id),
      (supervisor_user_id, supervisor_role_id),
      (non_approver_user_id, non_approver_role_id)
    );

  IF active_user_count <> 3 OR role_bind_count <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post release smoke username fix did not prepare all valid smoke users';
  END IF;
END$$

DELIMITER ;

CALL apply_post_release_role_e2e_gate_smoke_username_fix();

DROP PROCEDURE IF EXISTS apply_post_release_role_e2e_gate_smoke_username_fix;
