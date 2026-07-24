-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=permission; riskLevel=low
-- MES 排产工单：同步真实报工进度按钮权限。
-- 设计边界：仅补齐当前排产工单页已有 update 接口对应的菜单权限，不扩大到非排产工单套餐。

DROP PROCEDURE IF EXISTS ensure_mes_schedule_order_sync_progress_permission;
DELIMITER $$
CREATE PROCEDURE ensure_mes_schedule_order_sync_progress_permission()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 5580
        AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing schedule order menu 5580; cannot add sync progress permission';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 5583
        AND `deleted` = b'0'
        AND NOT (
          `parent_id` = 5580
          AND `permission` = 'mes:pro-schedule-order:update'
        )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 5583 is already used by another menu';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `parent_id` = 5580
        AND `permission` = 'mes:pro-schedule-order:update'
        AND `deleted` = b'0'
        AND `id` <> 5583
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schedule order update permission already exists with a different id';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 5583) THEN
    INSERT INTO `system_menu`
      (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
    VALUES
      (5583, '排产工单同步进度', 'mes:pro-schedule-order:update', 3, 3, 5580, '', '', '', '', 0, b'1', b'1', b'1', 'mes-schedule-order-sync-progress-permission', NOW(), 'mes-schedule-order-sync-progress-permission', NOW(), b'0');
  ELSE
    UPDATE `system_menu`
    SET `name` = '排产工单同步进度',
        `permission` = 'mes:pro-schedule-order:update',
        `type` = 3,
        `sort` = 3,
        `parent_id` = 5580,
        `path` = '',
        `icon` = '',
        `component` = '',
        `component_name` = '',
        `status` = 0,
        `visible` = b'1',
        `keep_alive` = b'1',
        `always_show` = b'1',
        `updater` = 'mes-schedule-order-sync-progress-permission',
        `update_time` = NOW(),
        `deleted` = b'0'
    WHERE `id` = 5583;
  END IF;

  IF EXISTS (
      SELECT 1
      FROM `system_tenant_package`
      WHERE `deleted` = b'0'
        AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge schedule order sync progress permission';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_sync_permission_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_schedule_order_sync_permission_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND `existing_menu`.`menu_id` = 5580;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_sync_permission_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_schedule_order_sync_permission_package_menu_ids` (
      `package_id` BIGINT NOT NULL,
      `menu_id` BIGINT NOT NULL,
      PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_schedule_order_sync_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `target_package`.`package_id`, `existing_menu`.`menu_id`
  FROM `tmp_mes_schedule_order_sync_permission_packages` AS `target_package`
  INNER JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target_package`.`package_id`
  INNER JOIN JSON_TABLE(
      `package`.`menu_ids`,
      '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_schedule_order_sync_permission_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, 5583
  FROM `tmp_mes_schedule_order_sync_permission_packages`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_sync_permission_package_menu_json`;
  CREATE TEMPORARY TABLE `tmp_mes_schedule_order_sync_permission_package_menu_json` AS
  SELECT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
  FROM (
      SELECT `package_id`, `menu_id`
      FROM `tmp_mes_schedule_order_sync_permission_package_menu_ids`
      ORDER BY `package_id`, `menu_id`
  ) AS `ordered_menu`
  GROUP BY `package_id`;

  UPDATE `system_tenant_package` AS `package`
  INNER JOIN `tmp_mes_schedule_order_sync_permission_package_menu_json` AS `merged`
      ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'mes-schedule-order-sync-progress-permission',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_schedule_order_sync_permission_target_roles`;
  CREATE TEMPORARY TABLE `tmp_mes_schedule_order_sync_permission_target_roles` AS
  SELECT DISTINCT
      `tenant`.`id` AS `tenant_id`,
      `role`.`id` AS `role_id`
  FROM `system_tenant` AS `tenant`
  INNER JOIN `tmp_mes_schedule_order_sync_permission_packages` AS `target_package`
      ON `target_package`.`package_id` = `tenant`.`package_id`
  INNER JOIN `system_role` AS `role`
      ON `role`.`tenant_id` = `tenant`.`id`
     AND `role`.`code` = 'tenant_admin'
     AND `role`.`deleted` = b'0'
  WHERE `tenant`.`deleted` = b'0';

  UPDATE `system_role_menu` AS `role_menu`
  INNER JOIN `tmp_mes_schedule_order_sync_permission_target_roles` AS `target_role`
      ON `target_role`.`tenant_id` = `role_menu`.`tenant_id`
     AND `target_role`.`role_id` = `role_menu`.`role_id`
     AND `role_menu`.`menu_id` = 5583
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'mes-schedule-order-sync-progress-permission',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT
      `target_role`.`role_id`,
      5583,
      'mes-schedule-order-sync-progress-permission',
      NOW(),
      'mes-schedule-order-sync-progress-permission',
      NOW(),
      b'0',
      `target_role`.`tenant_id`
  FROM `tmp_mes_schedule_order_sync_permission_target_roles` AS `target_role`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`menu_id` = 5583
        AND `existing`.`deleted` = b'0'
  );
END$$
DELIMITER ;

CALL ensure_mes_schedule_order_sync_progress_permission();
DROP PROCEDURE IF EXISTS ensure_mes_schedule_order_sync_progress_permission;
