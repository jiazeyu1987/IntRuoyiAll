-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260629_mes_smart_scheduling_role_scope; type=data; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_smart_scheduling_role_assignment;

DELIMITER //
CREATE PROCEDURE ensure_mes_smart_scheduling_role_assignment()
BEGIN
  IF (
    SELECT COUNT(*)
    FROM `system_users`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND `status` = 0
      AND `username` IN ('zhaojie', 'guliya', 'wuxiaolei', 'zhangjiayi')
  ) <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing target MES smart scheduling users in tenant 1';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND `status` = 0
      AND (`name` = '排产员' OR `code` = 'mes_scheduler')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled MES scheduler role in tenant 1 for assignment';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `tenant_id` = 1
      AND `status` = 0
      AND (`name` = '车间主任' OR `code` = 'mes_workshop_director')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled MES workshop director role in tenant 1 for assignment';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_assignment_targets`;
  CREATE TEMPORARY TABLE `tmp_mes_role_assignment_targets` (
    `user_id` bigint NOT NULL,
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`, `tenant_id`)
  );

  INSERT INTO `tmp_mes_role_assignment_targets` (`user_id`, `role_id`, `tenant_id`)
  SELECT
    `user`.`id`,
    `role`.`id`,
    `user`.`tenant_id`
  FROM `system_users` AS `user`
  JOIN `system_role` AS `role`
    ON `role`.`tenant_id` = `user`.`tenant_id`
   AND `role`.`deleted` = b'0'
   AND `role`.`status` = 0
   AND (`role`.`name` = '排产员' OR `role`.`code` = 'mes_scheduler')
  WHERE `user`.`deleted` = b'0'
    AND `user`.`tenant_id` = 1
    AND `user`.`status` = 0
    AND `user`.`username` = 'zhaojie';

  INSERT INTO `tmp_mes_role_assignment_targets` (`user_id`, `role_id`, `tenant_id`)
  SELECT
    `user`.`id`,
    `role`.`id`,
    `user`.`tenant_id`
  FROM `system_users` AS `user`
  JOIN `system_role` AS `role`
    ON `role`.`tenant_id` = `user`.`tenant_id`
   AND `role`.`deleted` = b'0'
   AND `role`.`status` = 0
   AND (`role`.`name` = '车间主任' OR `role`.`code` = 'mes_workshop_director')
  WHERE `user`.`deleted` = b'0'
    AND `user`.`tenant_id` = 1
    AND `user`.`status` = 0
    AND `user`.`username` IN ('guliya', 'wuxiaolei', 'zhangjiayi');

  UPDATE `system_user_role` AS `user_role`
  JOIN `tmp_mes_role_assignment_targets` AS `target`
    ON `target`.`user_id` = `user_role`.`user_id`
   AND `target`.`role_id` = `user_role`.`role_id`
   AND `target`.`tenant_id` = `user_role`.`tenant_id`
  SET `user_role`.`deleted` = b'0',
      `user_role`.`updater` = 'mes-smart-role-assignment',
      `user_role`.`update_time` = NOW()
  WHERE `user_role`.`deleted` = b'1';

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target`.`user_id`,
    `target`.`role_id`,
    'mes-smart-role-assignment',
    NOW(),
    'mes-smart-role-assignment',
    NOW(),
    b'0',
    `target`.`tenant_id`
  FROM `tmp_mes_role_assignment_targets` AS `target`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_user_role` AS `existing`
    WHERE `existing`.`user_id` = `target`.`user_id`
      AND `existing`.`role_id` = `target`.`role_id`
      AND `existing`.`tenant_id` = `target`.`tenant_id`
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_role_assignment_targets`;
END//
DELIMITER ;

CALL ensure_mes_smart_scheduling_role_assignment();

DROP PROCEDURE IF EXISTS ensure_mes_smart_scheduling_role_assignment;
