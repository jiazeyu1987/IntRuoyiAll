-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `ensure_approval_center_role_visibility`;
DELIMITER $$
CREATE PROCEDURE `ensure_approval_center_role_visibility`()
BEGIN
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;
  DECLARE v_zhaojie_user_id BIGINT DEFAULT NULL;
  DECLARE v_entry_role_id BIGINT DEFAULT 910295;
  DECLARE v_admin_role_id BIGINT DEFAULT 910296;
  DECLARE v_menu_1200 BIGINT DEFAULT NULL;
  DECLARE v_menu_1221 BIGINT DEFAULT NULL;

  SELECT `id`
  INTO v_admin_user_id
  FROM `system_users`
  WHERE `username` = 'admin'
    AND `tenant_id` = 1
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1;
  IF v_admin_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 admin user';
  END IF;

  SELECT `id`
  INTO v_zhaojie_user_id
  FROM `system_users`
  WHERE `username` = 'zhaojie'
    AND `tenant_id` = 1
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1;
  IF v_zhaojie_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 zhaojie user';
  END IF;

  SELECT `id`
  INTO v_menu_1200
  FROM `system_menu`
  WHERE `id` = 1200
    AND `deleted` = b'0'
  LIMIT 1;
  IF v_menu_1200 IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing approval center menu 1200';
  END IF;

  SELECT `id`
  INTO v_menu_1221
  FROM `system_menu`
  WHERE `id` = 1221
    AND `deleted` = b'0'
  LIMIT 1;
  IF v_menu_1221 IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing approval center permission menu 1221';
  END IF;

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`,
    `status`, `type`, `remark`, `creator`, `create_time`, `updater`,
    `update_time`, `deleted`, `tenant_id`
  )
  SELECT 910295, '审批中心入口', 'approval_center_entry', 910295, 1, '',
         0, 2, '审批中心最小入口角色', 'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `id` = 910295
  );

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`,
    `status`, `type`, `remark`, `creator`, `create_time`, `updater`,
    `update_time`, `deleted`, `tenant_id`
  )
  SELECT 910296, '审批管理员', 'approval_admin', 910296, 1, '',
         0, 2, '审批中心全量可见管理员角色', 'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `id` = 910296
  );

  UPDATE `system_role`
  SET `name` = '审批中心入口',
      `code` = 'approval_center_entry',
      `sort` = 910295,
      `data_scope` = 1,
      `data_scope_dept_ids` = '',
      `status` = 0,
      `type` = 2,
      `remark` = '审批中心最小入口角色',
      `updater` = 'codex',
      `update_time` = NOW(),
      `deleted` = b'0',
      `tenant_id` = 1
  WHERE `id` = 910295;

  UPDATE `system_role`
  SET `name` = '审批管理员',
      `code` = 'approval_admin',
      `sort` = 910296,
      `data_scope` = 1,
      `data_scope_dept_ids` = '',
      `status` = 0,
      `type` = 2,
      `remark` = '审批中心全量可见管理员角色',
      `updater` = 'codex',
      `update_time` = NOW(),
      `deleted` = b'0',
      `tenant_id` = 1
  WHERE `id` = 910296;

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_target_menu`;
  CREATE TEMPORARY TABLE `tmp_approval_center_target_menu` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_approval_center_target_menu` (`menu_id`)
  SELECT 1200 AS `menu_id`
  UNION ALL SELECT 1221;

  -- keep the legacy showroom approval menu 980104 untouched in this migration

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_enabled_users`;
  CREATE TEMPORARY TABLE `tmp_approval_center_enabled_users` (
    `user_id` BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_approval_center_enabled_users` (`user_id`)
  SELECT `id`
  FROM `system_users`
  WHERE `tenant_id` = 1
    AND `status` = 0
    AND `deleted` = b'0';

  UPDATE `system_role_menu`
  SET `deleted` = b'0',
      `updater` = 'codex',
      `update_time` = NOW(),
      `tenant_id` = 1
  WHERE `role_id` IN (v_entry_role_id, v_admin_role_id)
    AND `menu_id` IN (SELECT `menu_id` FROM `tmp_approval_center_target_menu`);

  UPDATE `system_role_menu`
  SET `deleted` = b'1',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `role_id` IN (v_entry_role_id, v_admin_role_id)
    AND `menu_id` NOT IN (SELECT `menu_id` FROM `tmp_approval_center_target_menu`)
    AND `menu_id` <> 980104
    AND `tenant_id` = 1;

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT v_entry_role_id, `menu_id`, 'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM `tmp_approval_center_target_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = v_entry_role_id
      AND `existing`.`menu_id` = `tmp_approval_center_target_menu`.`menu_id`
  );

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT v_admin_role_id, `menu_id`, 'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM `tmp_approval_center_target_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = v_admin_role_id
      AND `existing`.`menu_id` = `tmp_approval_center_target_menu`.`menu_id`
  );

  UPDATE `system_user_role`
  SET `deleted` = b'0',
      `updater` = 'codex',
      `update_time` = NOW(),
      `tenant_id` = 1
  WHERE `role_id` = v_entry_role_id
    AND `user_id` IN (SELECT `user_id` FROM `tmp_approval_center_enabled_users`);

  INSERT INTO `system_user_role`
    (`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `user_id`, v_entry_role_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM `tmp_approval_center_enabled_users`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role` AS `existing`
    WHERE `existing`.`user_id` = `tmp_approval_center_enabled_users`.`user_id`
      AND `existing`.`role_id` = v_entry_role_id
  );

  UPDATE `system_user_role`
  SET `deleted` = b'0',
      `updater` = 'codex',
      `update_time` = NOW(),
      `tenant_id` = 1
  WHERE `user_id` = v_admin_user_id
    AND `role_id` = v_admin_role_id;

  INSERT INTO `system_user_role`
    (`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT v_admin_user_id, v_admin_role_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role` AS `existing`
    WHERE `existing`.`user_id` = v_admin_user_id
      AND `existing`.`role_id` = v_admin_role_id
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_enabled_users`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_approval_center_target_menu`;
END$$
DELIMITER ;

CALL `ensure_approval_center_role_visibility`();
DROP PROCEDURE IF EXISTS `ensure_approval_center_role_visibility`;
