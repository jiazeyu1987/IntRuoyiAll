-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_unified_signature_records_menu; type=menu; riskLevel=medium
-- 新增“我的签名”个人入口，并将“用户授权”收口到电子签名管理员角色。

SET NAMES utf8mb4;

SET @unified_signature_menu_id := 900218;
SET @unified_signature_records_menu_id := 900411;
SET @unified_signature_my_signature_menu_id := 900418;
SET @unified_signature_authorization_menu_id := 900413;

DROP PROCEDURE IF EXISTS ensure_electronic_signature_my_signature_menu;

DELIMITER //
CREATE PROCEDURE ensure_electronic_signature_my_signature_menu()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = @unified_signature_menu_id
      AND `path` = '/signature-governance'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing electronic signature root menu 900218; cannot add my signature menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = @unified_signature_records_menu_id
      AND `path` = 'signature-records'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing unified signature records menu 900411; cannot place my signature tab';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = @unified_signature_authorization_menu_id
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing signature authorization menu 900413; cannot enforce admin-only visibility';
  END IF;

  INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT @unified_signature_my_signature_menu_id, '我的签名', 'signature-governance:policy:query', 2, 3,
         @unified_signature_menu_id, 'my-signature', 'ep:edit-pen', 'signature-governance/index',
         'SignatureGovernanceMySignature', 0, b'1', b'1', b'0',
         'signature-my-signature', NOW(), 'signature-my-signature', NOW(), b'0'
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = @unified_signature_my_signature_menu_id
  )
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `permission` = VALUES(`permission`),
    `type` = VALUES(`type`),
    `sort` = VALUES(`sort`),
    `parent_id` = VALUES(`parent_id`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `component` = VALUES(`component`),
    `component_name` = VALUES(`component_name`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`),
    `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`),
    `deleted` = VALUES(`deleted`);

  UPDATE `system_menu`
  SET `name` = '我的签名',
      `permission` = 'signature-governance:policy:query',
      `type` = 2,
      `sort` = 3,
      `parent_id` = @unified_signature_menu_id,
      `path` = 'my-signature',
      `icon` = 'ep:edit-pen',
      `component` = 'signature-governance/index',
      `component_name` = 'SignatureGovernanceMySignature',
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'0',
      `deleted` = b'0',
      `updater` = 'signature-my-signature',
      `update_time` = NOW()
  WHERE `id` = @unified_signature_my_signature_menu_id;

  UPDATE `system_menu`
  SET `name` = '用户授权',
      `permission` = 'dcc:controlled-file:signature:manage',
      `type` = 2,
      `sort` = 4,
      `parent_id` = @unified_signature_menu_id,
      `path` = 'authorizations',
      `icon` = 'ep:user',
      `component` = 'signature-governance/index',
      `component_name` = 'SignatureGovernanceAuthorizations',
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'0',
      `deleted` = b'0',
      `updater` = 'signature-my-signature',
      `update_time` = NOW()
  WHERE `id` = @unified_signature_authorization_menu_id;

  DROP TEMPORARY TABLE IF EXISTS `tmp_signature_regular_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_signature_regular_menu_ids` (
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`menu_id`)
  );

  INSERT INTO `tmp_signature_regular_menu_ids` (`menu_id`)
  VALUES
    (900218),
    (900411),
    (900418);

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT DISTINCT
      `src`.`role_id`,
      `regular`.`menu_id`,
      'signature-my-signature',
      NOW(),
      'signature-my-signature',
      NOW(),
      b'0',
      `src`.`tenant_id`
  FROM `system_role_menu` AS `src`
  CROSS JOIN `tmp_signature_regular_menu_ids` AS `regular`
  WHERE `src`.`deleted` = b'0'
    AND `src`.`menu_id` = @unified_signature_menu_id
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `src`.`role_id`
        AND `existing`.`tenant_id` = `src`.`tenant_id`
        AND `existing`.`menu_id` = `regular`.`menu_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_signature_regular_menu_ids`;
END//
DELIMITER ;

CALL ensure_electronic_signature_my_signature_menu();

DROP PROCEDURE IF EXISTS ensure_electronic_signature_my_signature_menu;

DROP PROCEDURE IF EXISTS ensure_electronic_signature_admin_role;

DELIMITER //
CREATE PROCEDURE ensure_electronic_signature_admin_role()
BEGIN
  DECLARE v_signature_admin_role_id BIGINT DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_users`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND `status` = 0
      AND `username` = 'admin'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled admin user in tenant 1 for electronic signature admin role';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'dcc:controlled-file:signature:manage'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing DCC signature manage permission menu; cannot create electronic signature admin role';
  END IF;

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`,
    `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    (
      SELECT COALESCE(MAX(`existing_role`.`id`), 910417) + 1
      FROM `system_role` AS `existing_role`
    ),
    '电子签名管理员',
    'electronic_signature_admin',
    910418,
    1,
    '',
    0,
    1,
    '仅允许查看电子签名用户授权并管理个人签名权限',
    'signature-admin-role',
    NOW(),
    'signature-admin-role',
    NOW(),
    b'0',
    1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND (`code` = 'electronic_signature_admin' OR `name` = '电子签名管理员')
  );

  UPDATE `system_role`
  SET `name` = '电子签名管理员',
      `code` = 'electronic_signature_admin',
      `sort` = 910418,
      `data_scope` = 1,
      `data_scope_dept_ids` = '',
      `status` = 0,
      `type` = 1,
      `remark` = '仅允许查看电子签名用户授权并管理个人签名权限',
      `updater` = 'signature-admin-role',
      `update_time` = NOW(),
      `deleted` = b'0'
  WHERE `tenant_id` = 1
    AND (`code` = 'electronic_signature_admin' OR `name` = '电子签名管理员');

  SELECT `id`
  INTO v_signature_admin_role_id
  FROM `system_role`
  WHERE `deleted` = b'0'
    AND `tenant_id` = 1
    AND `status` = 0
    AND (`code` = 'electronic_signature_admin' OR `name` = '电子签名管理员')
  ORDER BY CASE WHEN `code` = 'electronic_signature_admin' THEN 0 ELSE 1 END, `id`
  LIMIT 1;

  IF v_signature_admin_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled electronic_signature_admin role in tenant 1 after migration';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_signature_admin_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_signature_admin_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `id` IN (900218, 900411, 900418, 900413)
      OR `permission` = 'dcc:controlled-file:signature:manage'
    );

  IF (SELECT COUNT(*) FROM `tmp_signature_admin_menu_ids`) < 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Electronic signature admin menu scope is incomplete';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `role_menu`.`menu_id`
  JOIN `system_role` AS `role`
    ON `role`.`id` = `role_menu`.`role_id`
   AND `role`.`tenant_id` = `role_menu`.`tenant_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'signature-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'0'
    AND `role`.`deleted` = b'0'
    AND `role`.`code` <> 'electronic_signature_admin'
    AND (
      `role_menu`.`menu_id` = @unified_signature_authorization_menu_id
      OR `menu`.`permission` = 'dcc:controlled-file:signature:manage'
    );

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_signature_admin_menu_ids` AS `admin_menu`
    ON `admin_menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'signature-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`role_id` = v_signature_admin_role_id
    AND `role_menu`.`tenant_id` = 1
    AND `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    v_signature_admin_role_id,
    `admin_menu`.`id`,
    'signature-admin-role',
    NOW(),
    'signature-admin-role',
    NOW(),
    b'0',
    1
  FROM `tmp_signature_admin_menu_ids` AS `admin_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = v_signature_admin_role_id
      AND `existing`.`tenant_id` = 1
      AND `existing`.`menu_id` = `admin_menu`.`id`
      AND `existing`.`deleted` = b'0'
  );

  UPDATE `system_user_role` AS `user_role`
  JOIN `system_users` AS `user`
    ON `user`.`id` = `user_role`.`user_id`
   AND `user`.`tenant_id` = `user_role`.`tenant_id`
  SET `user_role`.`deleted` = b'0',
      `user_role`.`updater` = 'signature-admin-role',
      `user_role`.`update_time` = NOW()
  WHERE `user_role`.`tenant_id` = 1
    AND `user_role`.`role_id` = v_signature_admin_role_id
    AND `user`.`username` = 'admin'
    AND `user_role`.`deleted` = b'1';

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `user`.`id`,
    v_signature_admin_role_id,
    'signature-admin-role',
    NOW(),
    'signature-admin-role',
    NOW(),
    b'0',
    `user`.`tenant_id`
  FROM `system_users` AS `user`
  WHERE `user`.`deleted` = b'0'
    AND `user`.`tenant_id` = 1
    AND `user`.`status` = 0
    AND `user`.`username` = 'admin'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_user_role` AS `existing`
      WHERE `existing`.`user_id` = `user`.`id`
        AND `existing`.`role_id` = v_signature_admin_role_id
        AND `existing`.`tenant_id` = `user`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_signature_admin_menu_ids`;
END//
DELIMITER ;

CALL ensure_electronic_signature_admin_role();

DROP PROCEDURE IF EXISTS ensure_electronic_signature_admin_role;
