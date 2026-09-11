-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_mes_schedule_order_freeze_audit,20260629_mes_smart_scheduling_role_scope; type=permission; riskLevel=medium
-- Grant schedule-order delete permission to each active mes_scheduler role.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS grant_mes_scheduler_schedule_order_delete_permission_20260911;
DELIMITER //
CREATE PROCEDURE grant_mes_scheduler_schedule_order_delete_permission_20260911()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_delete_target_role`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_delete_target_role` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_scheduler_delete_target_role` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
    FROM `system_role` AS `role`
   WHERE `role`.`code` = 'mes_scheduler'
     AND `role`.`status` = 0
     AND `role`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_scheduler_delete_target_role`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing active mes_scheduler role';
  END IF;

  IF EXISTS (
    SELECT `role`.`tenant_id`
      FROM `system_role` AS `role`
     WHERE `role`.`code` = 'mes_scheduler'
       AND `role`.`status` = 0
       AND `role`.`deleted` = b'0'
     GROUP BY `role`.`tenant_id`
    HAVING COUNT(*) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate active mes_scheduler role in one tenant';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_delete_target_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_delete_target_menu` (
    `menu_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_scheduler_delete_target_menu` (`menu_id`)
  SELECT `menu`.`id`
    FROM `system_menu` AS `menu`
   WHERE `menu`.`permission` = 'mes:pro-schedule-order:delete'
     AND `menu`.`type` = 3
     AND `menu`.`status` = 0
     AND `menu`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_scheduler_delete_target_menu`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or ambiguous schedule order delete permission menu';
  END IF;

  IF EXISTS (
    SELECT `target_role`.`role_id`, `target_role`.`tenant_id`, `target_menu`.`menu_id`
      FROM `tmp_mes_scheduler_delete_target_role` AS `target_role`
      CROSS JOIN `tmp_mes_scheduler_delete_target_menu` AS `target_menu`
      JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`role_id` = `target_role`.`role_id`
       AND `role_menu`.`tenant_id` = `target_role`.`tenant_id`
       AND `role_menu`.`menu_id` = `target_menu`.`menu_id`
       AND `role_menu`.`deleted` = b'0'
     GROUP BY `target_role`.`role_id`, `target_role`.`tenant_id`, `target_menu`.`menu_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Scheduler delete permission has duplicate active bindings before grant';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_delete_existing_binding`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_delete_existing_binding` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    `keep_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`, `menu_id`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_mes_scheduler_delete_existing_binding` (
    `role_id`, `tenant_id`, `menu_id`, `keep_id`
  )
  SELECT
    `target_role`.`role_id`,
    `target_role`.`tenant_id`,
    `target_menu`.`menu_id`,
    MIN(`role_menu`.`id`) AS `keep_id`
    FROM `tmp_mes_scheduler_delete_target_role` AS `target_role`
    CROSS JOIN `tmp_mes_scheduler_delete_target_menu` AS `target_menu`
    JOIN `system_role_menu` AS `role_menu`
      ON `role_menu`.`role_id` = `target_role`.`role_id`
     AND `role_menu`.`tenant_id` = `target_role`.`tenant_id`
     AND `role_menu`.`menu_id` = `target_menu`.`menu_id`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `active_role_menu`
      WHERE `active_role_menu`.`role_id` = `target_role`.`role_id`
        AND `active_role_menu`.`tenant_id` = `target_role`.`tenant_id`
        AND `active_role_menu`.`menu_id` = `target_menu`.`menu_id`
        AND `active_role_menu`.`deleted` = b'0'
   )
   GROUP BY `target_role`.`role_id`, `target_role`.`tenant_id`, `target_menu`.`menu_id`;

  UPDATE `system_role_menu` AS `role_menu`
    JOIN `tmp_mes_scheduler_delete_existing_binding` AS `existing`
      ON `role_menu`.`id` = `existing`.`keep_id`
     SET `role_menu`.`deleted` = b'0',
         `role_menu`.`updater` = 'mes-scheduler-schedule-order-delete-20260911',
         `role_menu`.`update_time` = NOW()
   WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target_role`.`role_id`,
    `target_menu`.`menu_id`,
    'mes-scheduler-schedule-order-delete-20260911',
    NOW(),
    'mes-scheduler-schedule-order-delete-20260911',
    NOW(),
    b'0',
    `target_role`.`tenant_id`
    FROM `tmp_mes_scheduler_delete_target_role` AS `target_role`
    CROSS JOIN `tmp_mes_scheduler_delete_target_menu` AS `target_menu`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`menu_id` = `target_menu`.`menu_id`
        AND `existing`.`deleted` = b'0'
   );

  IF EXISTS (
    SELECT 1
      FROM `tmp_mes_scheduler_delete_target_role` AS `target_role`
      CROSS JOIN `tmp_mes_scheduler_delete_target_menu` AS `target_menu`
     WHERE NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` AS `role_menu`
        WHERE `role_menu`.`role_id` = `target_role`.`role_id`
          AND `role_menu`.`tenant_id` = `target_role`.`tenant_id`
          AND `role_menu`.`menu_id` = `target_menu`.`menu_id`
          AND `role_menu`.`deleted` = b'0'
     )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Scheduler delete permission grant incomplete';
  END IF;

  IF EXISTS (
    SELECT `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`
      FROM `tmp_mes_scheduler_delete_target_role` AS `target_role`
      CROSS JOIN `tmp_mes_scheduler_delete_target_menu` AS `target_menu`
      JOIN `system_role_menu` AS `role_menu`
        ON `role_menu`.`role_id` = `target_role`.`role_id`
       AND `role_menu`.`tenant_id` = `target_role`.`tenant_id`
       AND `role_menu`.`menu_id` = `target_menu`.`menu_id`
       AND `role_menu`.`deleted` = b'0'
     GROUP BY `role_menu`.`role_id`, `role_menu`.`tenant_id`, `role_menu`.`menu_id`
    HAVING COUNT(*) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Scheduler delete permission grant has duplicate active bindings';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_delete_existing_binding`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_delete_target_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_delete_target_role`;
END//
DELIMITER ;

CALL grant_mes_scheduler_schedule_order_delete_permission_20260911();
DROP PROCEDURE IF EXISTS grant_mes_scheduler_schedule_order_delete_permission_20260911;

COMMIT;
