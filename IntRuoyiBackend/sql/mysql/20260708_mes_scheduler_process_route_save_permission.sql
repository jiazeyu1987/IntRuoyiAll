-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260629_mes_smart_scheduling_role_scope; type=data; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_process_route_save_permission;

DELIMITER //
CREATE PROCEDURE ensure_mes_scheduler_process_route_save_permission()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900122
      AND `permission` = 'mes:pro-schedule-route:update'
      AND `deleted` = b'0'
      AND `status` = 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled process schedule route save permission menu 900122';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `status` = 0
      AND (`name` = '排产员' OR `code` = 'mes_scheduler')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled scheduler role; cannot grant process schedule route save permission';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `system_role` AS `role`
    ON `role`.`id` = `role_menu`.`role_id`
   AND `role`.`tenant_id` = `role_menu`.`tenant_id`
   AND `role`.`deleted` = b'0'
   AND `role`.`status` = 0
   AND (`role`.`name` = '排产员' OR `role`.`code` = 'mes_scheduler')
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'scheduler-process-route-save-permission',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`menu_id` = 900122
    AND `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    900122,
    'scheduler-process-route-save-permission',
    NOW(),
    'scheduler-process-route-save-permission',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`status` = 0
    AND (`role`.`name` = '排产员' OR `role`.`code` = 'mes_scheduler')
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`menu_id` = 900122
        AND `existing`.`deleted` = b'0'
    );
END//
DELIMITER ;

CALL ensure_mes_scheduler_process_route_save_permission();

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_process_route_save_permission;
