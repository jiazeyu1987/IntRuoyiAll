-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_smart_scheduling_tabs;

DELIMITER //
CREATE PROCEDURE ensure_mes_smart_scheduling_tabs()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge MES smart scheduling menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` = 5100
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES system parent menu 5100; cannot create smart scheduling tab';
  END IF;

  IF EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `id` = 900120
      AND `deleted` = b'0'
      AND (`name` <> '智能排产' OR `parent_id` <> 5100)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Menu id 900120 is already used by another menu; cannot create MES smart scheduling tab';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (5985, 5580, 5550, 5262)
      AND `deleted` = b'0'
  ) <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES scheduling page menus 5985/5580/5550/5262; cannot regroup smart scheduling tabs';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  ) VALUES (
    900120, '智能排产', 'mes:pro-smart-scheduling:query', 1, 0, 5100, 'smart-scheduling',
    'ep:calendar', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
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
  SET `name` = '排产看板',
      `permission` = 'mes:home:query',
      `type` = 2,
      `sort` = 0,
      `parent_id` = 900120,
      `path` = '/mes/home/index',
      `icon` = 'ep:home-filled',
      `component` = 'mes/home/index',
      `component_name` = 'MesHome',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5985;

  UPDATE `system_menu`
  SET `name` = '排产工单',
      `permission` = 'mes:pro-schedule-order:query',
      `type` = 2,
      `sort` = 1,
      `parent_id` = 900120,
      `path` = '/mes/pro/schedule-order',
      `icon` = 'ep:operation',
      `component` = 'mes/pro/scheduleorder/index',
      `component_name` = 'MesProScheduleOrder',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5580;

  UPDATE `system_menu`
  SET `name` = '报工',
      `permission` = 'mes:pro-feedback:query',
      `type` = 2,
      `sort` = 2,
      `parent_id` = 900120,
      `path` = '/mes/pro/feedback',
      `icon` = 'ep:finished',
      `component` = 'mes/pro/feedback/index',
      `component_name` = 'MesProFeedback',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5550;

  UPDATE `system_menu`
  SET `name` = '排程日历',
      `permission` = 'mes:pro-task:query',
      `type` = 2,
      `sort` = 3,
      `parent_id` = 900120,
      `path` = '/mes/pro/schedule-calendar',
      `icon` = 'ep:calendar',
      `component` = 'mes/pro/task/calendar/index',
      `component_name` = 'MesCalProScheduleCalendar',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5262;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` IN (5985, 5580, 5550, 5262)
      AND `deleted` = b'0'
      AND `permission` = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES smart scheduling page menus must have permissions before visibility can be controlled';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_smart_scheduling_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_smart_scheduling_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900120, 5985, 5580, 5581, 5550, 5551, 5262)
    AND `deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_smart_scheduling_menu_ids`) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling menu or query permission rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_smart_scheduling_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_smart_scheduling_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_smart_scheduling_package_menu_ids` (`package_id`, `menu_id`)
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
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5100' AS JSON), '$');

  INSERT IGNORE INTO `tmp_mes_smart_scheduling_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `smart_menu`.`id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_mes_smart_scheduling_menu_ids` AS `smart_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`menu_ids`)
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('5100' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_smart_scheduling_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = '1',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    r.`id`,
    m.`id`,
    '1',
    NOW(),
    '1',
    NOW(),
    b'0',
    r.`tenant_id`
  FROM `system_role` r
  JOIN `system_tenant` t
    ON t.`id` = r.`tenant_id`
   AND t.`deleted` = b'0'
  JOIN `system_tenant_package` tp
    ON tp.`id` = t.`package_id`
   AND tp.`deleted` = b'0'
   AND JSON_VALID(tp.`menu_ids`)
  JOIN `system_menu` m ON m.`id` IN (900120, 5985, 5580, 5581, 5550, 5551, 5262)
   AND m.`deleted` = b'0'
  WHERE r.`deleted` = b'0'
    AND r.`code` = 'tenant_admin'
    AND JSON_CONTAINS(CAST(tp.`menu_ids` AS JSON), CAST(CONCAT('', m.`id`) AS JSON), '$')
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` rm
      WHERE rm.`role_id` = r.`id`
        AND rm.`menu_id` = m.`id`
        AND rm.`tenant_id` = r.`tenant_id`
        AND rm.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_smart_scheduling_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_smart_scheduling_menu_ids`;
END//
DELIMITER ;

CALL ensure_mes_smart_scheduling_tabs();

DROP PROCEDURE IF EXISTS ensure_mes_smart_scheduling_tabs;
