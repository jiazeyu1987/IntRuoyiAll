-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_erp_production_material_list_menu,20260613_erp_bom_list_menu,20260613_erp_inventory_list_menu; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_erp_production_admin_role;

DELIMITER //
CREATE PROCEDURE ensure_erp_production_admin_role()
BEGIN
  DECLARE v_erp_production_admin_role_id BIGINT DEFAULT NULL;
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;
  DECLARE v_role_name VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;
  DECLARE v_role_remark VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;

  SET v_role_name = CONVERT(0x455250E7949FE4BAA7E7AEA1E79086E59198 USING utf8mb4);
  SET v_role_remark = CONVERT(0x45525020E7949FE4BAA7E7AEA1E79086E88F9CE58D95E78BACE7AB8BE68E88E69D83E8A792E889B2 USING utf8mb4);

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (2563, 6020, 6021, 6022, 6023, 6024, 6025, 6026)
      AND `deleted` = b'0'
      AND `status` = 0
  ) <> 8 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing ERP production admin menu baseline in tenant 1';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `id` = 1
      AND `tenant_id` = 1
      AND `deleted` = b'0'
      AND `status` = 0
      AND `code` = 'super_admin'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled super_admin role baseline in tenant 1';
  END IF;

  UPDATE `system_role`
  SET `name` = v_role_name,
      `code` = 'erp_production_admin',
      `sort` = 6020,
      `data_scope` = 1,
      `data_scope_dept_ids` = '',
      `status` = 0,
      `type` = 2,
      `remark` = v_role_remark,
      `deleted` = b'0',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `tenant_id` = 1
    AND (`name` = v_role_name OR `code` = 'erp_production_admin');

  INSERT INTO `system_role` (
    `id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    (
      SELECT COALESCE(MAX(`existing_role`.`id`), 910294) + 1
      FROM `system_role` AS `existing_role`
    ),
    v_role_name, 'erp_production_admin', 6020, 1, '', 0, 2, v_role_remark,
    'codex', NOW(), 'codex', NOW(), b'0', 1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND (`name` = v_role_name OR `code` = 'erp_production_admin')
  );

  SELECT `id`
  INTO v_erp_production_admin_role_id
  FROM `system_role`
  WHERE `deleted` = b'0'
    AND `tenant_id` = 1
    AND `status` = 0
    AND (`name` = v_role_name OR `code` = 'erp_production_admin')
  ORDER BY CASE WHEN `code` = 'erp_production_admin' THEN 0 ELSE 1 END, `id`
  LIMIT 1;

  IF v_erp_production_admin_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled ERP production admin role in tenant 1';
  END IF;

  SELECT `id`
  INTO v_admin_user_id
  FROM `system_users` AS `user`
  WHERE `user`.`deleted` = b'0'
    AND `user`.`tenant_id` = 1
    AND `user`.`status` = 0
    AND `user`.`username` = 'admin'
  ORDER BY `user`.`id`
  LIMIT 1;

  IF v_admin_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing tenant 1 admin user for ERP production role binding';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_production_admin_target_roles`;
  CREATE TEMPORARY TABLE `tmp_erp_production_admin_target_roles` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  );

  INSERT INTO `tmp_erp_production_admin_target_roles` (`role_id`, `tenant_id`)
  SELECT v_erp_production_admin_role_id AS `role_id`, 1 AS `tenant_id`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_production_admin_allowed_menu`;
  CREATE TEMPORARY TABLE `tmp_erp_production_admin_allowed_menu` (
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`menu_id`)
  );

  INSERT INTO `tmp_erp_production_admin_allowed_menu` (`menu_id`)
  SELECT 2563 AS `menu_id`
  UNION ALL SELECT 6020
  UNION ALL SELECT 6021
  UNION ALL SELECT 6022
  UNION ALL SELECT 6023
  UNION ALL SELECT 6024
  UNION ALL SELECT 6025
  UNION ALL SELECT 6026;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_erp_production_admin_target_roles` AS `target_role`
    ON `target_role`.`role_id` = `role_menu`.`role_id`
   AND `target_role`.`tenant_id` = `role_menu`.`tenant_id`
  JOIN `tmp_erp_production_admin_allowed_menu` AS `allowed_menu`
    ON `allowed_menu`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'erp-production-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target_role`.`role_id`,
    `allowed_menu`.`menu_id`,
    'erp-production-admin-role',
    NOW(),
    'erp-production-admin-role',
    NOW(),
    b'0',
    `target_role`.`tenant_id`
  FROM `tmp_erp_production_admin_target_roles` AS `target_role`
  JOIN `tmp_erp_production_admin_allowed_menu` AS `allowed_menu`
    ON 1 = 1
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `allowed_menu`.`menu_id`
   AND `menu`.`deleted` = b'0'
   AND `menu`.`status` = 0
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `target_role`.`role_id`
      AND `existing`.`menu_id` = `allowed_menu`.`menu_id`
      AND `existing`.`tenant_id` = `target_role`.`tenant_id`
      AND `existing`.`deleted` = b'0'
  );

  UPDATE `system_role_menu` AS `role_menu`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'erp-production-admin-role',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`tenant_id` = 1
    AND `role_menu`.`deleted` = b'0'
    AND `role_menu`.`menu_id` IN (6020, 6021, 6022, 6023, 6024, 6025, 6026)
    AND `role_menu`.`role_id` <> v_erp_production_admin_role_id;

  UPDATE `system_user_role` AS `user_role`
  SET `user_role`.`deleted` = b'0',
      `user_role`.`updater` = 'erp-production-admin-role',
      `user_role`.`update_time` = NOW()
  WHERE `user_role`.`tenant_id` = 1
    AND `user_role`.`user_id` = v_admin_user_id
    AND `user_role`.`role_id` = v_erp_production_admin_role_id
    AND `user_role`.`deleted` = b'1';

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    v_admin_user_id,
    v_erp_production_admin_role_id,
    'erp-production-admin-role',
    NOW(),
    'erp-production-admin-role',
    NOW(),
    b'0',
    1
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role` AS `existing`
    WHERE `existing`.`tenant_id` = 1
      AND `existing`.`user_id` = v_admin_user_id
      AND `existing`.`role_id` = v_erp_production_admin_role_id
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_production_admin_target_roles`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_erp_production_admin_allowed_menu`;
END//
DELIMITER ;

CALL ensure_erp_production_admin_role();

DROP PROCEDURE IF EXISTS ensure_erp_production_admin_role;
