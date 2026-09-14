-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260629_mes_smart_scheduling_role_scope,20260716_mes_route_version_permission_menu; type=data; riskLevel=low
-- Purpose: 补齐排产员在工艺流程列表的非删除型操作权限，恢复“产品/编辑/版本”操作入口。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_route_flow_list_permission;

DELIMITER //
CREATE PROCEDURE ensure_mes_scheduler_route_flow_list_permission()
BEGIN
  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `status` = 0
      AND (
        (`id` = 5723 AND `permission` = 'mes:pro-route:update')
        OR (`id` = 5730 AND `permission` = 'mes:pro-route:version-query')
      )
  ) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing route-flow list operation menus for MES scheduler role';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_role` AS `role`
    JOIN `system_tenant` AS `tenant`
      ON `tenant`.`id` = `role`.`tenant_id`
     AND `tenant`.`deleted` = b'0'
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `tenant`.`package_id`
     AND `package`.`deleted` = b'0'
    WHERE `role`.`deleted` = b'0'
      AND `role`.`tenant_id` <> 1
      AND `role`.`status` = 0
      AND (`role`.`code` = 'mes_scheduler'
        OR `role`.`name` COLLATE utf8mb4_unicode_ci = _utf8mb4'排产员' COLLATE utf8mb4_unicode_ci)
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids; cannot grant scheduler route-flow list permissions';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_route_flow_list_operation_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_route_flow_list_operation_menu` (
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`menu_id`)
  );

  INSERT INTO `tmp_mes_scheduler_route_flow_list_operation_menu` (`menu_id`)
  VALUES (5723), (5730);

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_route_flow_list_operation_package_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_route_flow_list_operation_package_menu` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_scheduler_route_flow_list_operation_package_menu` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$');

  INSERT IGNORE INTO `tmp_mes_scheduler_route_flow_list_operation_package_menu` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `operation_menu`.`menu_id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_mes_scheduler_route_flow_list_operation_menu` AS `operation_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_scheduler_route_flow_list_operation_package_menu`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'mes-scheduler-route-flow-list-permission',
      `package`.`update_time` = NOW();

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_route_flow_list_operation_target`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_route_flow_list_operation_target` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  );

  INSERT IGNORE INTO `tmp_mes_scheduler_route_flow_list_operation_target` (`role_id`, `tenant_id`)
  SELECT `role`.`id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`status` = 0
    AND (`role`.`code` = 'mes_scheduler'
      OR `role`.`name` COLLATE utf8mb4_unicode_ci = _utf8mb4'排产员' COLLATE utf8mb4_unicode_ci);

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_scheduler_route_flow_list_operation_target`
    WHERE `tenant_id` = 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing enabled MES scheduler role in tenant 1; cannot grant route-flow list permissions';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_route_flow_list_operation_effective_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_route_flow_list_operation_effective_menu` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_scheduler_route_flow_list_operation_effective_menu` (`role_id`, `tenant_id`, `menu_id`)
  SELECT
    `target_role`.`role_id`,
    `target_role`.`tenant_id`,
    `operation_menu`.`menu_id`
  FROM `tmp_mes_scheduler_route_flow_list_operation_target` AS `target_role`
  CROSS JOIN `tmp_mes_scheduler_route_flow_list_operation_menu` AS `operation_menu`
  WHERE `target_role`.`tenant_id` = 1;

  INSERT IGNORE INTO `tmp_mes_scheduler_route_flow_list_operation_effective_menu` (`role_id`, `tenant_id`, `menu_id`)
  SELECT
    `target_role`.`role_id`,
    `target_role`.`tenant_id`,
    `operation_menu`.`menu_id`
  FROM `tmp_mes_scheduler_route_flow_list_operation_target` AS `target_role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `target_role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  CROSS JOIN `tmp_mes_scheduler_route_flow_list_operation_menu` AS `operation_menu`
  WHERE `target_role`.`tenant_id` <> 1
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST(CONCAT('', `operation_menu`.`menu_id`) AS JSON), '$');

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_scheduler_route_flow_list_operation_effective_menu` AS `effective_menu`
    ON `effective_menu`.`role_id` = `role_menu`.`role_id`
   AND `effective_menu`.`tenant_id` = `role_menu`.`tenant_id`
   AND `effective_menu`.`menu_id` = `role_menu`.`menu_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'mes-scheduler-route-flow-list-permission',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `effective_menu`.`role_id`,
    `effective_menu`.`menu_id`,
    'mes-scheduler-route-flow-list-permission',
    NOW(),
    'mes-scheduler-route-flow-list-permission',
    NOW(),
    b'0',
    `effective_menu`.`tenant_id`
  FROM `tmp_mes_scheduler_route_flow_list_operation_effective_menu` AS `effective_menu`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `effective_menu`.`menu_id`
   AND `menu`.`deleted` = b'0'
   AND `menu`.`status` = 0
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `effective_menu`.`role_id`
      AND `existing`.`tenant_id` = `effective_menu`.`tenant_id`
      AND `existing`.`menu_id` = `effective_menu`.`menu_id`
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_route_flow_list_operation_effective_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_route_flow_list_operation_target`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_route_flow_list_operation_package_menu`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_route_flow_list_operation_menu`;
END//
DELIMITER ;

CALL ensure_mes_scheduler_route_flow_list_permission();

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_route_flow_list_permission;
