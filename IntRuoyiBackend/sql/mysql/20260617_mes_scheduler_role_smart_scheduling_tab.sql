-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260615_mes_scheduler_workbench_only_summary; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_role_smart_scheduling_tab;

DELIMITER //
CREATE PROCEDURE ensure_mes_scheduler_role_smart_scheduling_tab()
BEGIN
  ensure_mes_scheduler_role_smart_scheduling_tab: BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot grant MES scheduler role smart scheduling tab';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (900120, 5590, 5580, 5262, 5540)
      AND `deleted` = b'0'
      AND `status` = 0
      AND `visible` = b'1'
  ) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling core menu ids 900120/5590/5580/5262/5540; cannot grant scheduler role tab';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_role_allowed_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_role_allowed_menu_ids` AS
  WITH RECURSIVE `smart_menu_tree` AS (
    SELECT
      `menu`.`id`
    FROM `system_menu` AS `menu`
    WHERE `menu`.`id` = 900120
      AND `menu`.`deleted` = b'0'
      AND `menu`.`status` = 0
    UNION ALL
    SELECT
      `child`.`id`
    FROM `system_menu` AS `child`
    JOIN `smart_menu_tree` AS `parent`
      ON `parent`.`id` = `child`.`parent_id`
    WHERE `child`.`deleted` = b'0'
      AND `child`.`status` = 0
  )
  SELECT DISTINCT
    `smart_menu_tree`.`id` AS `menu_id`
  FROM `smart_menu_tree`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_role_targets`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_role_targets` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    `role_name` varchar(64) NOT NULL,
    `role_code` varchar(64) NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  );

  INSERT IGNORE INTO `tmp_mes_scheduler_role_targets` (
    `role_id`, `tenant_id`, `role_name`, `role_code`
  )
  SELECT
    `role`.`id`,
    `role`.`tenant_id`,
    `role`.`name`,
    `role`.`code`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `system_tenant_package` AS `tenant_package`
    ON `tenant_package`.`id` = `tenant`.`package_id`
   AND `tenant_package`.`deleted` = b'0'
   AND JSON_VALID(`tenant_package`.`menu_ids`)
  WHERE `role`.`deleted` = b'0'
    AND `role`.`status` = 0
    AND (
      `role`.`name` IN ('排产员', '计划员', '生产计划员', '排产员/计划员')
      OR `role`.`code` IN ('planner', 'scheduler', 'mes_planner', 'mes_scheduler', 'production_planner', 'production_scheduler')
    )
    AND JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST('900120' AS JSON), '$');

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_scheduler_role_targets`
  ) THEN
    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_role_targets`;
    DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_role_allowed_menu_ids`;
    LEAVE ensure_mes_scheduler_role_smart_scheduling_tab;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_scheduler_role_targets` AS `target_role`
    JOIN `system_tenant` AS `tenant`
      ON `tenant`.`id` = `target_role`.`tenant_id`
     AND `tenant`.`deleted` = b'0'
    JOIN `system_tenant_package` AS `tenant_package`
      ON `tenant_package`.`id` = `tenant`.`package_id`
     AND `tenant_package`.`deleted` = b'0'
     AND JSON_VALID(`tenant_package`.`menu_ids`)
    JOIN `tmp_mes_scheduler_role_allowed_menu_ids` AS `allowed_menu`
    WHERE NOT JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST(CONCAT('', `allowed_menu`.`menu_id`) AS JSON), '$')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling menu tree in tenant package; cannot grant scheduler role tab';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_scheduler_role_targets` AS `target_role`
    ON `target_role`.`role_id` = `role_menu`.`role_id`
   AND `target_role`.`tenant_id` = `role_menu`.`tenant_id`
  LEFT JOIN `tmp_mes_scheduler_role_allowed_menu_ids` AS `allowed_menu`
    ON `allowed_menu`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'1',
      `role_menu`.`updater` = '1',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'0'
    AND `allowed_menu`.`menu_id` IS NULL;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target_role`.`role_id`,
    `smart_menu`.`id`,
    '1',
    NOW(),
    '1',
    NOW(),
    b'0',
    `target_role`.`tenant_id`
  FROM `tmp_mes_scheduler_role_targets` AS `target_role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `target_role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `system_tenant_package` AS `tenant_package`
    ON `tenant_package`.`id` = `tenant`.`package_id`
   AND `tenant_package`.`deleted` = b'0'
   AND JSON_VALID(`tenant_package`.`menu_ids`)
  JOIN `tmp_mes_scheduler_role_allowed_menu_ids` AS `allowed_menu`
  JOIN `system_menu` AS `smart_menu`
    ON `smart_menu`.`id` = `allowed_menu`.`menu_id`
   AND `smart_menu`.`deleted` = b'0'
   AND `smart_menu`.`status` = 0
  WHERE JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST(CONCAT('', `allowed_menu`.`menu_id`) AS JSON), '$')
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `target_role`.`role_id`
        AND `existing`.`menu_id` = `smart_menu`.`id`
        AND `existing`.`tenant_id` = `target_role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_role_targets`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_role_allowed_menu_ids`;
  END;
END//
DELIMITER ;

CALL ensure_mes_scheduler_role_smart_scheduling_tab();

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_role_smart_scheduling_tab;
