-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260805_mes_process_pool_production_personnel; type=data; riskLevel=medium
-- Backfill the authoritative production-leader employee scope for historical personnel profiles.
-- Recovery: rows created by this migration use a dedicated creator/updater marker for scoped recovery.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_pp_employee_scope_backfill_20260809;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_employee_scope_backfill_20260809()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_team_employee_profile'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_team_employee_profile';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_team_leader_scope'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_team_leader_scope';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_process_pool_team_employee_profile` AS `profile`
     WHERE `profile`.`deleted` = b'0'
     GROUP BY
       `profile`.`tenant_id`,
       `profile`.`leader_user_id`,
       COALESCE(`profile`.`system_user_id`, `profile`.`id`)
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate production employee profile leader identity';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_process_pool_team_leader_scope` AS `existing`
     WHERE `existing`.`leader_type` = 'PRODUCTION'
       AND `existing`.`scope_type` = 'EMPLOYEE'
       AND `existing`.`deleted` = b'0'
     GROUP BY
       `existing`.`tenant_id`,
       `existing`.`leader_user_id`,
       `existing`.`employee_user_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate active production employee leader scope';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_process_pool_team_employee_profile` AS `profile`
      JOIN `mes_pro_process_pool_team_leader_scope` AS `existing`
        ON `existing`.`tenant_id` = `profile`.`tenant_id`
       AND `existing`.`leader_user_id` = `profile`.`leader_user_id`
       AND `existing`.`leader_type` = 'PRODUCTION'
       AND `existing`.`scope_type` = 'EMPLOYEE'
       AND `existing`.`employee_user_id` = COALESCE(`profile`.`system_user_id`, `profile`.`id`)
       AND `existing`.`deleted` = b'0'
     WHERE `profile`.`deleted` = b'0'
       AND NOT (`existing`.`enabled` <=> `profile`.`enabled`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production employee profile and leader scope enabled status mismatch';
  END IF;

  INSERT INTO `mes_pro_process_pool_team_leader_scope` (
    `leader_user_id`,
    `leader_type`,
    `scope_type`,
    `employee_user_id`,
    `enabled`,
    `remark`,
    `creator`,
    `create_time`,
    `updater`,
    `update_time`,
    `deleted`,
    `tenant_id`
  )
  SELECT
    `profile`.`leader_user_id`,
    'PRODUCTION',
    'EMPLOYEE',
    COALESCE(`profile`.`system_user_id`, `profile`.`id`),
    `profile`.`enabled`,
    'Historical production personnel leader-scope backfill',
    '20260809-team-leader-scope-backfill',
    NOW(),
    '20260809-team-leader-scope-backfill',
    NOW(),
    b'0',
    `profile`.`tenant_id`
  FROM `mes_pro_process_pool_team_employee_profile` AS `profile`
  WHERE `profile`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
        FROM `mes_pro_process_pool_team_leader_scope` AS `existing`
       WHERE `existing`.`tenant_id` = `profile`.`tenant_id`
         AND `existing`.`leader_user_id` = `profile`.`leader_user_id`
         AND `existing`.`leader_type` = 'PRODUCTION'
         AND `existing`.`scope_type` = 'EMPLOYEE'
         AND `existing`.`employee_user_id` = COALESCE(`profile`.`system_user_id`, `profile`.`id`)
         AND `existing`.`deleted` = b'0'
    );

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_process_pool_team_employee_profile` AS `profile`
      LEFT JOIN `mes_pro_process_pool_team_leader_scope` AS `existing`
        ON `existing`.`tenant_id` = `profile`.`tenant_id`
       AND `existing`.`leader_user_id` = `profile`.`leader_user_id`
       AND `existing`.`leader_type` = 'PRODUCTION'
       AND `existing`.`scope_type` = 'EMPLOYEE'
       AND `existing`.`employee_user_id` = COALESCE(`profile`.`system_user_id`, `profile`.`id`)
       AND `existing`.`deleted` = b'0'
     WHERE `profile`.`deleted` = b'0'
     GROUP BY `profile`.`id`, `profile`.`enabled`
    HAVING COUNT(`existing`.`id`) <> 1
        OR SUM(
          CASE
            WHEN `existing`.`id` IS NOT NULL
             AND NOT (`existing`.`enabled` <=> `profile`.`enabled`)
            THEN 1
            ELSE 0
          END
        ) <> 0
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production employee leader scope backfill verification failed';
  END IF;

  COMMIT;
END$$
DELIMITER ;

CALL ensure_mes_pp_employee_scope_backfill_20260809();

DROP PROCEDURE IF EXISTS ensure_mes_pp_employee_scope_backfill_20260809;
