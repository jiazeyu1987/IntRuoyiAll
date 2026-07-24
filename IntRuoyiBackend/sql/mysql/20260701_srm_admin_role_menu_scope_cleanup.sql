-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260629_srm_admin_role_visibility; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_srm_admin_role_menu_scope_cleanup;

DELIMITER //
CREATE PROCEDURE ensure_srm_admin_role_menu_scope_cleanup()
BEGIN
  DECLARE v_srm_admin_role_id BIGINT DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `id` = 991000
      AND `path` = '/srm'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing SRM root menu 991000; cannot cleanup SRM admin menu scope';
  END IF;

  SELECT `id`
  INTO v_srm_admin_role_id
  FROM `system_role`
  WHERE `deleted` = b'0'
    AND `tenant_id` = 1
    AND `status` = 0
    AND (`code` = 'srm_admin' OR `name` = 'SRM管理员')
  ORDER BY CASE WHEN `code` = 'srm_admin' THEN 0 ELSE 1 END, `id`
  LIMIT 1;

  IF v_srm_admin_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled srm_admin role in tenant 1 for menu scope cleanup';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_admin_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_srm_admin_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `id` = 991000
      OR `parent_id` = 991000
      OR `parent_id` IN (
        SELECT `id`
        FROM `system_menu`
        WHERE `deleted` = b'0'
          AND `parent_id` = 991000
      )
    );

  IF (SELECT COUNT(*) FROM `tmp_srm_admin_menu_ids`) < 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'SRM menu tree is incomplete; cannot cleanup SRM admin menu scope';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  LEFT JOIN `tmp_srm_admin_menu_ids` AS `srm_menu`
    ON `srm_menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = 'srm-admin-scope-cleanup',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`role_id` = v_srm_admin_role_id
    AND `role_menu`.`tenant_id` = 1
    AND `role_menu`.`deleted` = b'0'
    AND `srm_menu`.`id` IS NULL;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_srm_admin_menu_ids` AS `srm_menu`
    ON `srm_menu`.`id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'srm-admin-scope-cleanup',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`role_id` = v_srm_admin_role_id
    AND `role_menu`.`tenant_id` = 1
    AND `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    v_srm_admin_role_id,
    `srm_menu`.`id`,
    'srm-admin-scope-cleanup',
    NOW(),
    'srm-admin-scope-cleanup',
    NOW(),
    b'0',
    1
  FROM `tmp_srm_admin_menu_ids` AS `srm_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = v_srm_admin_role_id
      AND `existing`.`tenant_id` = 1
      AND `existing`.`menu_id` = `srm_menu`.`id`
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_srm_admin_menu_ids`;
END//
DELIMITER ;

CALL ensure_srm_admin_role_menu_scope_cleanup();

DROP PROCEDURE IF EXISTS ensure_srm_admin_role_menu_scope_cleanup;
