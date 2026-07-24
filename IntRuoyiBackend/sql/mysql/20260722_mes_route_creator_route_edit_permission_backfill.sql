-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260615_mes_edhr_tail_four_goals; type=data; riskLevel=low
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS ensure_mes_route_creator_route_edit_permission_backfill;

DELIMITER //
CREATE PROCEDURE ensure_mes_route_creator_route_edit_permission_backfill()
BEGIN
  INSERT INTO `mes_pro_edhr_permission_scope` (
    `scope_name`, `object_type`, `object_id`, `parent_scope_id`, `status`, `version`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    CONCAT('工艺路线:', COALESCE(`route`.`code`, CAST(`route`.`id` AS CHAR))),
    'ROUTE',
    CAST(`route`.`id` AS CHAR),
    NULL,
    'ENABLED',
    1,
    'route-creator-route-edit-backfill',
    NOW(),
    'route-creator-route-edit-backfill',
    NOW(),
    b'0',
    `route`.`tenant_id`
  FROM `mes_pro_route` AS `route`
  JOIN `system_users` AS `creator_user`
    ON `creator_user`.`id` = CAST(`route`.`creator` AS UNSIGNED)
   AND `creator_user`.`tenant_id` = `route`.`tenant_id`
   AND `creator_user`.`deleted` = b'0'
   AND `creator_user`.`status` = 0
  WHERE `route`.`deleted` = b'0'
    AND `route`.`creator` REGEXP '^[0-9]+$'
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_edhr_permission_scope` AS `scope`
      WHERE `scope`.`tenant_id` = `route`.`tenant_id`
        AND `scope`.`object_type` = 'ROUTE'
        AND `scope`.`object_id` = CAST(`route`.`id` AS CHAR)
        AND `scope`.`status` = 'ENABLED'
        AND `scope`.`deleted` = b'0'
    );

  INSERT INTO `mes_pro_edhr_permission_rule` (
    `scope_id`, `subject_type`, `subject_id`, `ability`, `decision`, `priority`,
    `effective_from`, `effective_to`, `status`, `version`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `scope`.`id`,
    'USER',
    `creator_user`.`id`,
    `ability_set`.`ability`,
    'ALLOW',
    10,
    NULL,
    NULL,
    'ENABLED',
    1,
    'route-creator-route-edit-backfill',
    NOW(),
    'route-creator-route-edit-backfill',
    NOW(),
    b'0',
    `scope`.`tenant_id`
  FROM `mes_pro_route` AS `route`
  JOIN `system_users` AS `creator_user`
    ON `creator_user`.`id` = CAST(`route`.`creator` AS UNSIGNED)
   AND `creator_user`.`tenant_id` = `route`.`tenant_id`
   AND `creator_user`.`deleted` = b'0'
   AND `creator_user`.`status` = 0
  JOIN `mes_pro_edhr_permission_scope` AS `scope`
    ON `scope`.`tenant_id` = `route`.`tenant_id`
   AND `scope`.`object_type` = 'ROUTE'
   AND `scope`.`object_id` = CAST(`route`.`id` AS CHAR)
   AND `scope`.`status` = 'ENABLED'
   AND `scope`.`deleted` = b'0'
  JOIN (
    SELECT 'VIEW' AS `ability`
    UNION ALL SELECT 'ROUTE_EDIT'
    UNION ALL SELECT 'PERMISSION_ADMIN'
  ) AS `ability_set`
  WHERE `route`.`deleted` = b'0'
    AND `route`.`creator` REGEXP '^[0-9]+$'
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_edhr_permission_rule` AS `existing`
      WHERE `existing`.`tenant_id` = `scope`.`tenant_id`
        AND `existing`.`scope_id` = `scope`.`id`
        AND `existing`.`subject_type` = 'USER'
        AND `existing`.`subject_id` = `creator_user`.`id`
        AND `existing`.`ability` = `ability_set`.`ability`
        AND `existing`.`decision` = 'ALLOW'
        AND `existing`.`status` = 'ENABLED'
        AND `existing`.`deleted` = b'0'
    );

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route` AS `route`
    WHERE `route`.`id` = 922119
      AND `route`.`deleted` = b'0'
  ) THEN
    IF NOT EXISTS (
      SELECT 1
      FROM `mes_pro_route` AS `route`
      JOIN `system_users` AS `creator_user`
        ON `creator_user`.`id` = CAST(`route`.`creator` AS UNSIGNED)
       AND `creator_user`.`tenant_id` = `route`.`tenant_id`
       AND `creator_user`.`deleted` = b'0'
       AND `creator_user`.`status` = 0
      JOIN `mes_pro_edhr_permission_scope` AS `scope`
        ON `scope`.`tenant_id` = `route`.`tenant_id`
       AND `scope`.`object_type` = 'ROUTE'
       AND `scope`.`object_id` = CAST(`route`.`id` AS CHAR)
       AND `scope`.`status` = 'ENABLED'
       AND `scope`.`deleted` = b'0'
      WHERE `route`.`id` = 922119
        AND `route`.`deleted` = b'0'
        AND `route`.`creator` REGEXP '^[0-9]+$'
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Route 922119 creator permission scope backfill failed';
    END IF;

    IF (
      SELECT COUNT(DISTINCT `rule`.`ability`)
      FROM `mes_pro_route` AS `route`
      JOIN `system_users` AS `creator_user`
        ON `creator_user`.`id` = CAST(`route`.`creator` AS UNSIGNED)
       AND `creator_user`.`tenant_id` = `route`.`tenant_id`
       AND `creator_user`.`deleted` = b'0'
       AND `creator_user`.`status` = 0
      JOIN `mes_pro_edhr_permission_scope` AS `scope`
        ON `scope`.`tenant_id` = `route`.`tenant_id`
       AND `scope`.`object_type` = 'ROUTE'
       AND `scope`.`object_id` = CAST(`route`.`id` AS CHAR)
       AND `scope`.`status` = 'ENABLED'
       AND `scope`.`deleted` = b'0'
      JOIN `mes_pro_edhr_permission_rule` AS `rule`
        ON `rule`.`tenant_id` = `scope`.`tenant_id`
       AND `rule`.`scope_id` = `scope`.`id`
       AND `rule`.`subject_type` = 'USER'
       AND `rule`.`subject_id` = `creator_user`.`id`
       AND `rule`.`ability` IN ('VIEW', 'ROUTE_EDIT', 'PERMISSION_ADMIN')
       AND `rule`.`decision` = 'ALLOW'
       AND `rule`.`status` = 'ENABLED'
       AND `rule`.`deleted` = b'0'
      WHERE `route`.`id` = 922119
        AND `route`.`deleted` = b'0'
        AND `route`.`creator` REGEXP '^[0-9]+$'
    ) < 3 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Route 922119 creator ROUTE_EDIT rule backfill failed';
    END IF;
  END IF;
END//
DELIMITER ;

CALL ensure_mes_route_creator_route_edit_permission_backfill();

DROP PROCEDURE IF EXISTS ensure_mes_route_creator_route_edit_permission_backfill;
