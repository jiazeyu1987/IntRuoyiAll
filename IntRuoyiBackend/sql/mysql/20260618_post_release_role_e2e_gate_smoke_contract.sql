-- release-migration: allowedEnvironments=test,backup; dependsOn=20260618_post_release_role_e2e_gate_password_fix; type=permission; riskLevel=medium
-- Prepare real tenant-1 accounts and permissions required by the smart scheduling smoke flow.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_post_release_role_e2e_smoke_contract;

DELIMITER //
CREATE PROCEDURE ensure_post_release_role_e2e_smoke_contract()
BEGIN
  DECLARE admin_tenant_id bigint DEFAULT 1;
  DECLARE scheduler_role_id bigint;
  DECLARE erp_creator_role_id bigint;
  DECLARE supervisor_role_id bigint;
  DECLARE non_approver_role_id bigint;
  DECLARE erp_creator_user_id bigint;
  DECLARE supervisor_user_id bigint;
  DECLARE non_approver_user_id bigint;
  DECLARE feedback_worker_user_id bigint;
  DECLARE password_hash varchar(100) DEFAULT '$2a$10$EzpuIftrlM8pmMAKMbPCqeGV/NOHGXMGwH8nKg3G0eNJr8Sg0hs0K';

  IF NOT EXISTS (
    SELECT 1
    FROM `system_tenant`
    WHERE `id` = admin_tenant_id
      AND `name` = '芋道源码'
      AND `status` = 0
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled tenant 芋道源码(1); cannot prepare scheduler smoke contract';
  END IF;

  IF (
    SELECT COUNT(DISTINCT `id`)
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `status` = 0
      AND `id` IN (2563, 6013, 6014, 1075, 900120, 5262, 5531, 5532, 5540, 5542, 5550, 5551, 5552, 5553, 5580, 5581, 5582, 5584, 5585, 5969)
  ) <> 20 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing required menu ids for scheduler smoke contract';
  END IF;

  SELECT `id`
    INTO scheduler_role_id
  FROM `system_role`
  WHERE `tenant_id` = admin_tenant_id
    AND `status` = 0
    AND `deleted` = b'0'
    AND (`name` = '排产员' OR `code` IN ('planner', 'scheduler', 'mes_planner', 'mes_scheduler', 'production_planner', 'production_scheduler', '排产员'))
  ORDER BY (`name` = '排产员') DESC, `id`
  LIMIT 1;

  IF scheduler_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled scheduler role; cannot prepare scheduler smoke contract';
  END IF;

  INSERT INTO `system_role` (`name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT 'ERP冒烟创建员', 'post_release_mes_smoke_erp_creator', 910, 1, '', 0, 2, 'post-release smart scheduling smoke ERP creator', 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role`
    WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_erp_creator' AND `deleted` = b'0'
  );

  INSERT INTO `system_role` (`name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT '报工冒烟审批员', 'post_release_mes_smoke_supervisor', 911, 1, '', 0, 2, 'post-release smart scheduling smoke supervisor', 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role`
    WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_supervisor' AND `deleted` = b'0'
  );

  INSERT INTO `system_role` (`name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT '报工冒烟非审批员', 'post_release_mes_smoke_non_approver', 912, 1, '', 0, 2, 'post-release smart scheduling smoke non approver', 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  WHERE NOT EXISTS (
    SELECT 1 FROM `system_role`
    WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_non_approver' AND `deleted` = b'0'
  );

  UPDATE `system_role`
  SET `status` = 0,
      `type` = 2,
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `code` IN ('post_release_mes_smoke_erp_creator', 'post_release_mes_smoke_supervisor', 'post_release_mes_smoke_non_approver')
    AND `deleted` = b'0';

  SELECT `id` INTO erp_creator_role_id FROM `system_role` WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_erp_creator' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO supervisor_role_id FROM `system_role` WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_supervisor' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO non_approver_role_id FROM `system_role` WHERE `tenant_id` = admin_tenant_id AND `code` = 'post_release_mes_smoke_non_approver' AND `deleted` = b'0' LIMIT 1;

  INSERT INTO `system_users` (
    `username`, `password`, `password_update_time`, `nickname`, `remark`, `dept_id`, `post_ids`, `email`, `mobile`,
    `sex`, `avatar`, `status`, `login_ip`, `login_date`, `creator`, `create_time`, `updater`, `update_time`,
    `deleted`, `tenant_id`
  )
  SELECT `username`, password_hash, NOW(), `nickname`, `remark`, NULL, NULL, '', '', 0, '', 0, '', NULL,
         'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  FROM (
    SELECT 'mes_smoke_erp_creator' AS `username`, 'ERP冒烟创建员' AS `nickname`, 'post-release smart scheduling ERP creator account' AS `remark`
    UNION ALL SELECT 'mes_smoke_supervisor', 'eDHR矩阵-审批人', 'post-release smart scheduling supervisor account'
    UNION ALL SELECT 'mes_smoke_non_approver', '报工冒烟非审批员', 'post-release smart scheduling non approver account'
    UNION ALL SELECT 'aoteman', '芋道1', 'post-release smart scheduling feedback worker account'
  ) AS `seed`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_users`
    WHERE `tenant_id` = admin_tenant_id
      AND `username` = `seed`.`username`
      AND `deleted` = b'0'
  );

  UPDATE `system_users`
  SET `password` = password_hash,
      `password_update_time` = NOW(),
      `nickname` = CASE `username`
        WHEN 'mes_smoke_erp_creator' THEN 'ERP冒烟创建员'
        WHEN 'mes_smoke_supervisor' THEN 'eDHR矩阵-审批人'
        WHEN 'mes_smoke_non_approver' THEN '报工冒烟非审批员'
        WHEN 'aoteman' THEN '芋道1'
        ELSE `nickname`
      END,
      `status` = 0,
      `deleted` = b'0',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `username` IN ('mes_smoke_erp_creator', 'mes_smoke_supervisor', 'mes_smoke_non_approver', 'aoteman');

  SELECT `id` INTO erp_creator_user_id FROM `system_users` WHERE `tenant_id` = admin_tenant_id AND `username` = 'mes_smoke_erp_creator' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO supervisor_user_id FROM `system_users` WHERE `tenant_id` = admin_tenant_id AND `username` = 'mes_smoke_supervisor' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO non_approver_user_id FROM `system_users` WHERE `tenant_id` = admin_tenant_id AND `username` = 'mes_smoke_non_approver' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO feedback_worker_user_id FROM `system_users` WHERE `tenant_id` = admin_tenant_id AND `username` = 'aoteman' AND `deleted` = b'0' LIMIT 1;

  IF erp_creator_user_id IS NULL OR supervisor_user_id IS NULL OR non_approver_user_id IS NULL OR feedback_worker_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing one or more scheduler smoke users in tenant 芋道源码';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_post_release_smoke_user_roles`;
  CREATE TEMPORARY TABLE `tmp_post_release_smoke_user_roles` (`user_id` bigint NOT NULL, `role_id` bigint NOT NULL, PRIMARY KEY (`user_id`, `role_id`));
  INSERT INTO `tmp_post_release_smoke_user_roles` (`user_id`, `role_id`) VALUES
    (erp_creator_user_id, erp_creator_role_id),
    (supervisor_user_id, supervisor_role_id),
    (non_approver_user_id, non_approver_role_id);

  UPDATE `system_user_role`
  SET `deleted` = b'1',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `user_id` IN (erp_creator_user_id, supervisor_user_id, non_approver_user_id)
    AND `deleted` = b'0'
    AND (`user_id`, `role_id`) NOT IN (SELECT `user_id`, `role_id` FROM `tmp_post_release_smoke_user_roles`);

  INSERT INTO `system_user_role` (`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `user_id`, `role_id`, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  FROM `tmp_post_release_smoke_user_roles` AS `target_role`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role`
    WHERE `tenant_id` = admin_tenant_id
      AND `user_id` = `target_role`.`user_id`
      AND `role_id` = `target_role`.`role_id`
      AND `deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_post_release_smoke_role_menus`;
  CREATE TEMPORARY TABLE `tmp_post_release_smoke_role_menus` (`role_id` bigint NOT NULL, `menu_id` bigint NOT NULL, PRIMARY KEY (`role_id`, `menu_id`));
  INSERT INTO `tmp_post_release_smoke_role_menus` (`role_id`, `menu_id`) VALUES
    (scheduler_role_id, 5531), (scheduler_role_id, 5532), (scheduler_role_id, 5542),
    (scheduler_role_id, 5581), (scheduler_role_id, 5582), (scheduler_role_id, 5584), (scheduler_role_id, 5585),
    (erp_creator_role_id, 2563), (erp_creator_role_id, 6013), (erp_creator_role_id, 6014), (erp_creator_role_id, 1075),
    (supervisor_role_id, 900120), (supervisor_role_id, 5262), (supervisor_role_id, 5550), (supervisor_role_id, 5551),
    (supervisor_role_id, 5552), (supervisor_role_id, 5553), (supervisor_role_id, 5969),
    (non_approver_role_id, 900120), (non_approver_role_id, 5550), (non_approver_role_id, 5551);

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `role_id`, `menu_id`, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  FROM `tmp_post_release_smoke_role_menus` AS `target_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu`
    WHERE `tenant_id` = admin_tenant_id
      AND `role_id` = `target_menu`.`role_id`
      AND `menu_id` = `target_menu`.`menu_id`
      AND `deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_post_release_smoke_role_menus`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_post_release_smoke_user_roles`;
END//
DELIMITER ;

CALL ensure_post_release_role_e2e_smoke_contract();

DROP PROCEDURE IF EXISTS ensure_post_release_role_e2e_smoke_contract;
