-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260707_system_role_category_management; type=data; riskLevel=medium
-- Purpose: create the tenant 1 user-list access permission role and bind only system:user:query.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_user_list_access_role_20260728;
DELIMITER //
CREATE PROCEDURE ensure_user_list_access_role_20260728()
BEGIN
  DECLARE v_menu_category_id BIGINT DEFAULT NULL;
  DECLARE v_user_query_menu_id BIGINT DEFAULT NULL;
  DECLARE v_role_id BIGINT DEFAULT NULL;

  IF (
    SELECT COUNT(*)
      FROM `system_role`
     WHERE `tenant_id` = 1
       AND `code` = _utf8mb4'user_list_access' COLLATE utf8mb4_unicode_ci
       AND `deleted` = b'0'
  ) > 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate active tenant 1 user_list_access roles';
  END IF;

  SELECT `id`
    INTO v_menu_category_id
    FROM `system_role_category`
   WHERE `tenant_id` = 1
     AND `code` = _utf8mb4'menu' COLLATE utf8mb4_unicode_ci
     AND `status` = 0
     AND `deleted` = b'0'
   ORDER BY `id`
   LIMIT 1;

  IF v_menu_category_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing tenant 1 menu role category';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_user_list_access_permission`;
  CREATE TEMPORARY TABLE `tmp_user_list_access_permission` (
    `permission` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_user_list_access_permission` (`permission`)
  SELECT _utf8mb4'system:user:query' COLLATE utf8mb4_unicode_ci;

  SELECT `menu`.`id`
    INTO v_user_query_menu_id
    FROM `system_menu` AS `menu`
    JOIN `tmp_user_list_access_permission` AS `expected_permission`
      ON `menu`.`permission` = `expected_permission`.`permission`
   WHERE `menu`.`status` = 0
     AND `menu`.`deleted` = b'0'
   ORDER BY `menu`.`id`
   LIMIT 1;

  IF v_user_query_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled system:user:query menu permission';
  END IF;

  SELECT `id`
    INTO v_role_id
    FROM `system_role`
   WHERE `tenant_id` = 1
     AND `code` = _utf8mb4'user_list_access' COLLATE utf8mb4_unicode_ci
   ORDER BY `deleted` ASC, `id`
   LIMIT 1;

  IF v_role_id IS NULL THEN
    INSERT INTO `system_role` (
      `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`,
      `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`,
      `deleted`, `tenant_id`
    )
    VALUES (
      _utf8mb4'用户列表访问' COLLATE utf8mb4_unicode_ci,
      _utf8mb4'user_list_access' COLLATE utf8mb4_unicode_ci,
      910320,
      v_menu_category_id,
      1,
      '',
      0,
      2,
      _utf8mb4'人员选择弹窗用户列表访问角色；仅授予 system:user:query 用户查询权限。' COLLATE utf8mb4_unicode_ci,
      'codex',
      NOW(),
      'codex',
      NOW(),
      b'0',
      1
    );
    SET v_role_id = LAST_INSERT_ID();
  END IF;

  UPDATE `system_role`
     SET `name` = _utf8mb4'用户列表访问' COLLATE utf8mb4_unicode_ci,
         `code` = _utf8mb4'user_list_access' COLLATE utf8mb4_unicode_ci,
         `sort` = 910320,
         `category_id` = v_menu_category_id,
         `data_scope` = 1,
         `data_scope_dept_ids` = '',
         `status` = 0,
         `type` = 2,
         `remark` = _utf8mb4'人员选择弹窗用户列表访问角色；仅授予 system:user:query 用户查询权限。' COLLATE utf8mb4_unicode_ci,
         `updater` = 'codex',
         `update_time` = NOW(),
         `deleted` = b'0',
         `tenant_id` = 1
   WHERE `id` = v_role_id;

  UPDATE `system_role_menu`
     SET `deleted` = b'0',
         `updater` = 'codex',
         `update_time` = NOW(),
         `tenant_id` = 1
   WHERE `role_id` = v_role_id
     AND `menu_id` = v_user_query_menu_id;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT v_role_id, v_user_query_menu_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM DUAL
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = v_role_id
        AND `existing`.`menu_id` = v_user_query_menu_id
   );

  DROP TEMPORARY TABLE IF EXISTS `tmp_user_list_access_permission`;
END//
DELIMITER ;

CALL ensure_user_list_access_role_20260728();

DROP PROCEDURE IF EXISTS ensure_user_list_access_role_20260728;

COMMIT;
