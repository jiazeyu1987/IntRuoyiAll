-- release-migration: allowedEnvironments=test,backup; dependsOn=20260617_mes_scheduler_role_smart_scheduling_tab,20260618_showroom_publicity_role_menu_scope,20260513_dcc_base_schema; type=menu; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_post_release_role_e2e_gate;

DELIMITER //
CREATE PROCEDURE ensure_post_release_role_e2e_gate()
BEGIN
  DECLARE admin_tenant_id bigint DEFAULT 1;
  DECLARE showroom_role_id bigint;
  DECLARE scheduler_role_id bigint;
  DECLARE dcc_role_id bigint;
  DECLARE gaomin_user_id bigint;
  DECLARE zhaojie_user_id bigint;
  DECLARE wangsiyu_user_id bigint;
  DECLARE test_password_hash varchar(100) DEFAULT '$2a$10$0acJOIk2D25/oC87nyclE..0lzeu9DtQ/n3geP4fkun/zIVRhHJIO';

  IF NOT EXISTS (
    SELECT 1
    FROM `system_tenant`
    WHERE `id` = admin_tenant_id
      AND `name` = '芋道源码'
      AND `status` = 0
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled tenant 芋道源码(1); cannot prepare post-release role E2E gate';
  END IF;

  SELECT `id`
    INTO showroom_role_id
  FROM `system_role`
  WHERE `tenant_id` = admin_tenant_id
    AND `code` = 'showroom_publicity'
    AND `status` = 0
    AND `deleted` = b'0'
  LIMIT 1;

  IF showroom_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled showroom_publicity role; cannot prepare gaomin showroom E2E account';
  END IF;

  SELECT `id`
    INTO scheduler_role_id
  FROM `system_role`
  WHERE `tenant_id` = admin_tenant_id
    AND (`name` = '排产员' OR `code` IN ('planner', 'scheduler', 'mes_planner', 'mes_scheduler', 'production_planner', 'production_scheduler', '排产员'))
  ORDER BY (`status` = 0 AND `deleted` = b'0') DESC, (`name` = '排产员') DESC, (`code` = '排产员') DESC, `id`
  LIMIT 1;

  IF scheduler_role_id IS NULL THEN
    INSERT INTO `system_role` (
      `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`,
      `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
    )
    VALUES (
      '排产员', '排产员', 12, 1, '', 0, 2, 'post-release scheduler E2E gate role',
      'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
    );
    SET scheduler_role_id = LAST_INSERT_ID();
  ELSE
    UPDATE `system_role`
    SET `name` = '排产员',
        `sort` = CASE WHEN `sort` <= 0 THEN 12 ELSE `sort` END,
        `data_scope` = 1,
        `data_scope_dept_ids` = '',
        `status` = 0,
        `type` = 2,
        `remark` = COALESCE(NULLIF(`remark`, ''), 'post-release scheduler E2E gate role'),
        `updater` = 'post-release-role-e2e-gate',
        `update_time` = NOW(),
        `deleted` = b'0'
    WHERE `id` = scheduler_role_id;
  END IF;

  SELECT `id`
    INTO dcc_role_id
  FROM `system_role`
  WHERE `tenant_id` = admin_tenant_id
    AND `code` IN ('doc_control', 'wenkong', 'wenkong_download')
    AND `status` = 0
    AND `deleted` = b'0'
  ORDER BY FIELD(`code`, 'doc_control', 'wenkong', 'wenkong_download'), `id`
  LIMIT 1;

  IF dcc_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled DCC role; cannot prepare wangsiyu DCC E2E account';
  END IF;

  IF (
    SELECT COUNT(DISTINCT `id`)
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `status` = 0
      AND `id` IN (980100, 980101, 980118, 980102, 980119, 980103, 980104, 900120, 5590, 5580, 5550, 5262, 5540, 900104, 6800, 6814, 1221)
  ) <> 17 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing required menu ids for post-release role E2E gate';
  END IF;

  INSERT INTO `system_users` (
    `username`, `password`, `password_update_time`, `nickname`, `remark`, `dept_id`, `post_ids`, `email`, `mobile`,
    `sex`, `avatar`, `status`, `login_ip`, `login_date`, `creator`, `create_time`, `updater`, `update_time`,
    `deleted`, `tenant_id`
  )
  SELECT
    'gaomin', test_password_hash, NOW(), '高敏', 'post-release showroom E2E gate account', NULL, NULL, '', '',
    0, '', 0, '', NULL, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(),
    b'0', admin_tenant_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_users`
    WHERE `tenant_id` = admin_tenant_id
      AND `username` = 'gaomin'
      AND `deleted` = b'0'
  );

  UPDATE `system_users`
  SET `password` = test_password_hash,
      `password_update_time` = NOW(),
      `nickname` = IFNULL(NULLIF(`nickname`, ''), '高敏'),
      `status` = 0,
      `deleted` = b'0',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `username` = 'gaomin';

  UPDATE `system_users`
  SET `password` = test_password_hash,
      `password_update_time` = NOW(),
      `status` = 0,
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `username` IN ('zhaojie', 'wangsiyu')
    AND `deleted` = b'0';

  SELECT `id` INTO gaomin_user_id FROM `system_users` WHERE `tenant_id` = admin_tenant_id AND `username` = 'gaomin' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO zhaojie_user_id FROM `system_users` WHERE `tenant_id` = admin_tenant_id AND `username` = 'zhaojie' AND `deleted` = b'0' LIMIT 1;
  SELECT `id` INTO wangsiyu_user_id FROM `system_users` WHERE `tenant_id` = admin_tenant_id AND `username` = 'wangsiyu' AND `deleted` = b'0' LIMIT 1;

  IF gaomin_user_id IS NULL OR zhaojie_user_id IS NULL OR wangsiyu_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing one or more post-release E2E users: gaomin, zhaojie, wangsiyu';
  END IF;

  UPDATE `system_user_role`
  SET `deleted` = b'1',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `user_id` = gaomin_user_id
    AND `role_id` <> showroom_role_id
    AND `deleted` = b'0';

  INSERT INTO `system_user_role` (`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT gaomin_user_id, showroom_role_id, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role`
    WHERE `tenant_id` = admin_tenant_id
      AND `user_id` = gaomin_user_id
      AND `role_id` = showroom_role_id
      AND `deleted` = b'0'
  );

  INSERT INTO `system_user_role` (`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT zhaojie_user_id, scheduler_role_id, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role`
    WHERE `tenant_id` = admin_tenant_id
      AND `user_id` = zhaojie_user_id
      AND `role_id` = scheduler_role_id
      AND `deleted` = b'0'
  );

  INSERT INTO `system_user_role` (`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT wangsiyu_user_id, dcc_role_id, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role`
    WHERE `tenant_id` = admin_tenant_id
      AND `user_id` = wangsiyu_user_id
      AND `role_id` = dcc_role_id
      AND `deleted` = b'0'
  );

  UPDATE `system_menu`
  SET `parent_id` = 0,
      `path` = 'smart-scheduling',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `id` = 900120
    AND (`parent_id` <> 0 OR `path` <> 'smart-scheduling');

  DROP TEMPORARY TABLE IF EXISTS `tmp_post_release_scheduler_menus`;
  CREATE TEMPORARY TABLE `tmp_post_release_scheduler_menus` (`menu_id` bigint NOT NULL PRIMARY KEY);
  INSERT INTO `tmp_post_release_scheduler_menus` (`menu_id`)
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `status` = 0
    AND `id` IN (900120, 5590, 5580, 5550, 5262, 5540, 900104);

  UPDATE `system_role_menu`
  SET `deleted` = b'1',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = admin_tenant_id
    AND `role_id` = scheduler_role_id
    AND `deleted` = b'0'
    AND `menu_id` NOT IN (SELECT `menu_id` FROM `tmp_post_release_scheduler_menus`);

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT scheduler_role_id, `menu_id`, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  FROM `tmp_post_release_scheduler_menus` AS `target_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu`
    WHERE `tenant_id` = admin_tenant_id
      AND `role_id` = scheduler_role_id
      AND `menu_id` = `target_menu`.`menu_id`
      AND `deleted` = b'0'
  );

  INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT dcc_role_id, 1221, 'post-release-role-e2e-gate', NOW(), 'post-release-role-e2e-gate', NOW(), b'0', admin_tenant_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu`
    WHERE `tenant_id` = admin_tenant_id
      AND `role_id` = dcc_role_id
      AND `menu_id` = 1221
      AND `deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_post_release_scheduler_menus`;
END//
DELIMITER ;

CALL ensure_post_release_role_e2e_gate();

DROP PROCEDURE IF EXISTS ensure_post_release_role_e2e_gate;
