-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260708_mes_scheduler_process_route_save_permission; type=data; riskLevel=low
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_edhr_route_900025_route_edit_permission;

DELIMITER //
CREATE PROCEDURE ensure_mes_scheduler_edhr_route_900025_route_edit_permission()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `mes_pro_route`
    WHERE `id` = 900025
      AND `deleted` = b'0'
      AND `tenant_id` IN (
        SELECT `tenant_id`
        FROM `system_role`
        WHERE `deleted` = b'0'
          AND `status` = 0
          AND (`name` COLLATE utf8mb4_unicode_ci = _utf8mb4'排产员' COLLATE utf8mb4_unicode_ci OR `code` = 'mes_scheduler')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing route 900025 for scheduler tenant';
  END IF;

  INSERT INTO `mes_pro_edhr_permission_scope` (
    `scope_name`, `object_type`, `object_id`, `parent_scope_id`, `status`, `version`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    CONCAT('工艺路线:', `route`.`code`),
    'ROUTE',
    CAST(`route`.`id` AS CHAR),
    NULL,
    'ENABLED',
    1,
    'scheduler-edhr-route-900025-route-edit',
    NOW(),
    'scheduler-edhr-route-900025-route-edit',
    NOW(),
    b'0',
    `route`.`tenant_id`
  FROM `mes_pro_route` AS `route`
  WHERE `route`.`id` = 900025
    AND `route`.`deleted` = b'0'
    AND EXISTS (
      SELECT 1
      FROM `system_role` AS `role`
      WHERE `role`.`tenant_id` = `route`.`tenant_id`
        AND `role`.`deleted` = b'0'
        AND `role`.`status` = 0
        AND (`role`.`name` COLLATE utf8mb4_unicode_ci = _utf8mb4'排产员' COLLATE utf8mb4_unicode_ci OR `role`.`code` = 'mes_scheduler')
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_edhr_permission_scope` AS `scope`
      WHERE `scope`.`tenant_id` = `route`.`tenant_id`
        AND `scope`.`object_type` = 'ROUTE'
        AND `scope`.`object_id` = CAST(`route`.`id` AS CHAR)
        AND `scope`.`deleted` = b'0'
    );

  UPDATE `mes_pro_edhr_permission_scope` AS `scope`
  JOIN `mes_pro_route` AS `route`
    ON `route`.`tenant_id` = `scope`.`tenant_id`
   AND `route`.`id` = 900025
   AND `route`.`deleted` = b'0'
  SET `scope`.`scope_name` = IF(
        `scope`.`scope_name` = '' OR `scope`.`scope_name` IS NULL,
        CONCAT('工艺路线:', `route`.`code`),
        `scope`.`scope_name`
      ),
      `scope`.`status` = 'ENABLED',
      `scope`.`version` = GREATEST(`scope`.`version`, 1),
      `scope`.`updater` = 'scheduler-edhr-route-900025-route-edit',
      `scope`.`update_time` = NOW()
  WHERE `scope`.`object_type` = 'ROUTE'
    AND `scope`.`object_id` = '900025'
    AND `scope`.`deleted` = b'0'
    AND `scope`.`status` <> 'ENABLED';

  IF NOT EXISTS (
    SELECT 1
    FROM `mes_pro_edhr_permission_scope`
    WHERE `object_type` = 'ROUTE'
      AND `object_id` = '900025'
      AND `status` = 'ENABLED'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled eDHR permission scope ROUTE:900025';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `deleted` = b'0'
      AND `status` = 0
      AND (`name` COLLATE utf8mb4_unicode_ci = _utf8mb4'排产员' COLLATE utf8mb4_unicode_ci OR `code` = 'mes_scheduler')
      AND `tenant_id` IN (
        SELECT `tenant_id`
        FROM `mes_pro_edhr_permission_scope`
        WHERE `object_type` = 'ROUTE'
          AND `object_id` = '900025'
          AND `status` = 'ENABLED'
          AND `deleted` = b'0'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled scheduler role for eDHR scope ROUTE:900025';
  END IF;

  UPDATE `mes_pro_edhr_permission_rule` AS `rule`
  JOIN `mes_pro_edhr_permission_scope` AS `scope`
    ON `scope`.`id` = `rule`.`scope_id`
   AND `scope`.`tenant_id` = `rule`.`tenant_id`
   AND `scope`.`object_type` = 'ROUTE'
   AND `scope`.`object_id` = '900025'
   AND `scope`.`status` = 'ENABLED'
   AND `scope`.`deleted` = b'0'
  JOIN `system_role` AS `role`
    ON `role`.`id` = `rule`.`subject_id`
   AND `role`.`tenant_id` = `rule`.`tenant_id`
   AND `role`.`deleted` = b'0'
   AND `role`.`status` = 0
   AND (`role`.`name` COLLATE utf8mb4_unicode_ci = _utf8mb4'排产员' COLLATE utf8mb4_unicode_ci OR `role`.`code` = 'mes_scheduler')
  SET `rule`.`decision` = 'ALLOW',
      `rule`.`priority` = 10,
      `rule`.`status` = 'ENABLED',
      `rule`.`deleted` = b'0',
      `rule`.`updater` = 'scheduler-edhr-route-900025-route-edit',
      `rule`.`update_time` = NOW()
  WHERE `rule`.`subject_type` = 'ROLE'
    AND `rule`.`ability` = 'ROUTE_EDIT'
    AND (
      `rule`.`decision` <> 'ALLOW'
      OR `rule`.`status` <> 'ENABLED'
      OR `rule`.`deleted` = b'1'
    );

  INSERT INTO `mes_pro_edhr_permission_rule` (
    `scope_id`, `subject_type`, `subject_id`, `ability`, `decision`, `priority`,
    `effective_from`, `effective_to`, `status`, `version`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `scope`.`id`,
    'ROLE',
    `role`.`id`,
    'ROUTE_EDIT',
    'ALLOW',
    10,
    NULL,
    NULL,
    'ENABLED',
    1,
    'scheduler-edhr-route-900025-route-edit',
    NOW(),
    'scheduler-edhr-route-900025-route-edit',
    NOW(),
    b'0',
    `scope`.`tenant_id`
  FROM `mes_pro_edhr_permission_scope` AS `scope`
  JOIN `system_role` AS `role`
    ON `role`.`tenant_id` = `scope`.`tenant_id`
   AND `role`.`deleted` = b'0'
   AND `role`.`status` = 0
   AND (`role`.`name` COLLATE utf8mb4_unicode_ci = _utf8mb4'排产员' COLLATE utf8mb4_unicode_ci OR `role`.`code` = 'mes_scheduler')
  WHERE `scope`.`object_type` = 'ROUTE'
    AND `scope`.`object_id` = '900025'
    AND `scope`.`status` = 'ENABLED'
    AND `scope`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_edhr_permission_rule` AS `existing`
      WHERE `existing`.`scope_id` = `scope`.`id`
        AND `existing`.`tenant_id` = `scope`.`tenant_id`
        AND `existing`.`subject_type` = 'ROLE'
        AND `existing`.`subject_id` = `role`.`id`
        AND `existing`.`ability` = 'ROUTE_EDIT'
        AND `existing`.`decision` = 'ALLOW'
        AND `existing`.`status` = 'ENABLED'
        AND `existing`.`deleted` = b'0'
    );
END//
DELIMITER ;

CALL ensure_mes_scheduler_edhr_route_900025_route_edit_permission();

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_edhr_route_900025_route_edit_permission;
