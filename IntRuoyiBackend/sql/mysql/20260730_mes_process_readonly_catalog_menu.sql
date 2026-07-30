-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260709_mes_route_flow_config_unification; type=menu; riskLevel=low
-- Purpose: 在“工序设置”和“工艺流程”之间新增只读标准模板列表菜单，复用现有工艺路线/工序/设备配置读模型。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_process_readonly_catalog_menu;

DELIMITER //
CREATE PROCEDURE ensure_mes_process_readonly_catalog_menu()
BEGIN
  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (5700, 5710, 5720)
      AND `deleted` = b'0'
  ) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES production parent/process/route menus; cannot add MES process catalog';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5718
      AND (`parent_id` <> 5700 OR `path` <> 'mes-process' OR `component` <> 'mes/pro/mes-process/index')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 5718 is occupied by another menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5719
      AND (`parent_id` <> 5718 OR `permission` <> 'mes:pro-mes-process:query')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 5719 is occupied by another menu';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
    `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
  )
  VALUES (
    5718,
    CONVERT(UNHEX('E6A087E58786E6A8A1E69DBFE58897E8A1A8') USING utf8mb4),
    '',
    2,
    25,
    5700,
    'mes-process',
    'ep:connection',
    'mes/pro/mes-process/index',
    'MesProMesProcess',
    0,
    b'1',
    b'1',
    b'1',
    'mes-process-readonly-catalog-menu',
    NOW(),
    'mes-process-readonly-catalog-menu',
    NOW(),
    b'0'
  ),
  (
    5719,
    CONVERT(UNHEX('E6A087E58786E6A8A1E69DBFE58897E8A1A8E69FA5E8AFA2') USING utf8mb4),
    'mes:pro-mes-process:query',
    3,
    1,
    5718,
    '',
    '',
    '',
    '',
    0,
    b'1',
    b'1',
    b'1',
    'mes-process-readonly-catalog-menu',
    NOW(),
    'mes-process-readonly-catalog-menu',
    NOW(),
    b'0'
  )
  ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `permission` = VALUES(`permission`),
    `type` = VALUES(`type`),
    `sort` = VALUES(`sort`),
    `parent_id` = VALUES(`parent_id`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `component` = VALUES(`component`),
    `component_name` = VALUES(`component_name`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`),
    `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`),
    `deleted` = VALUES(`deleted`);

  UPDATE `system_menu`
  SET `sort` = CASE `id`
        WHEN 5710 THEN 20
        WHEN 5718 THEN 25
        WHEN 5720 THEN 30
        ELSE `sort`
      END,
      `updater` = 'mes-process-readonly-catalog-menu',
      `update_time` = NOW()
  WHERE `id` IN (5710, 5718, 5720);

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` = 5718
      AND `deleted` = b'0'
      AND `parent_id` = 5700
      AND `path` = 'mes-process'
      AND `component` = 'mes/pro/mes-process/index'
      AND `component_name` = 'MesProMesProcess'
      AND EXISTS (
        SELECT 1
        FROM `system_menu` AS `query_menu`
        WHERE `query_menu`.`id` = 5719
          AND `query_menu`.`deleted` = b'0'
          AND `query_menu`.`parent_id` = 5718
          AND `query_menu`.`permission` = 'mes:pro-mes-process:query'
      )
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES process catalog menu was not created correctly';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_catalog_package_menu`;
  CREATE TEMPORARY TABLE `tmp_mes_process_catalog_package_menu` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_process_catalog_package_menu` (`package_id`, `menu_id`)
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
    AND (
      JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5710' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$')
    );

  INSERT IGNORE INTO `tmp_mes_process_catalog_package_menu` (`package_id`, `menu_id`)
  SELECT `package`.`id`, `new_menu`.`menu_id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN (
    SELECT 5718 AS `menu_id`
    UNION ALL SELECT 5719
  ) AS `new_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND (
      JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5710' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('5720' AS JSON), '$')
    );

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_process_catalog_package_menu`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'mes-process-readonly-catalog-menu',
      `package`.`update_time` = NOW();

  UPDATE `system_role_menu` AS `role_menu`
  JOIN (
    SELECT DISTINCT `role_id`, `tenant_id`
    FROM `system_role_menu`
    WHERE `menu_id` IN (5710, 5720)
      AND `deleted` = b'0'
  ) AS `target`
    ON `target`.`role_id` = `role_menu`.`role_id`
   AND `target`.`tenant_id` = `role_menu`.`tenant_id`
   AND `role_menu`.`menu_id` IN (5718, 5719)
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'mes-process-readonly-catalog-menu',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`deleted` = b'1';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target`.`role_id`,
    `new_menu`.`menu_id`,
    'mes-process-readonly-catalog-menu',
    NOW(),
    'mes-process-readonly-catalog-menu',
    NOW(),
    b'0',
    `target`.`tenant_id`
  FROM (
    SELECT DISTINCT `role_id`, `tenant_id`
    FROM `system_role_menu`
    WHERE `menu_id` IN (5710, 5720)
      AND `deleted` = b'0'
  ) AS `target`
  CROSS JOIN (
    SELECT 5718 AS `menu_id`
    UNION ALL SELECT 5719
  ) AS `new_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `target`.`role_id`
      AND `existing`.`tenant_id` = `target`.`tenant_id`
      AND `existing`.`menu_id` = `new_menu`.`menu_id`
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_catalog_package_menu`;
END//
DELIMITER ;

CALL ensure_mes_process_readonly_catalog_menu();

DROP PROCEDURE IF EXISTS ensure_mes_process_readonly_catalog_menu;
