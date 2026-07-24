-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_workbench_smart_scheduling_tab;

DELIMITER //
CREATE PROCEDURE ensure_mes_scheduler_workbench_smart_scheduling_tab()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge scheduler workbench into smart scheduling';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900120
      AND `name` = '智能排产'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling parent menu 900120; cannot move scheduler workbench';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5590
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES scheduler workbench menu 5590; cannot move scheduler workbench';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (5985, 5580, 5550, 5262, 5540, 900104)
      AND `deleted` = b'0'
  ) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling sibling menus; cannot preserve smart scheduling order';
  END IF;

  UPDATE `system_menu`
  SET `name` = '排产看板',
      `permission` = 'mes:home:query',
      `type` = 2,
      `sort` = 0,
      `parent_id` = 900120,
      `path` = '/mes/home/index',
      `icon` = 'ep:dashboard',
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
  SET `name` = '排产员工作台',
      `permission` = 'mes:pro-scheduler-workbench:query',
      `type` = 2,
      `sort` = 1,
      `parent_id` = 900120,
      `path` = '/mes/pro/scheduler-workbench',
      `icon` = 'ep:monitor',
      `component` = 'mes/pro/scheduler-workbench/index',
      `component_name` = 'MesProSchedulerWorkbench',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5590;

  UPDATE `system_menu`
  SET `name` = '排产工单',
      `permission` = 'mes:pro-schedule-order:query',
      `type` = 2,
      `sort` = 2,
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
      `sort` = 3,
      `parent_id` = 900120,
      `path` = '/mes/pro/feedback',
      `icon` = 'ep:edit',
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
      `sort` = 4,
      `parent_id` = 900120,
      `path` = '/mes/pro/schedule-calendar',
      `icon` = 'ep:calendar',
      `component` = 'mes/pro/task/calendar/index',
      `component_name` = 'MesProScheduleCalendar',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5262;

  UPDATE `system_menu`
  SET `name` = '生产排产',
      `permission` = 'mes:pro-task:query',
      `type` = 2,
      `sort` = 5,
      `parent_id` = 900120,
      `path` = '/mes/pro/task',
      `icon` = 'ep:calendar',
      `component` = 'mes/pro/task/index',
      `component_name` = 'MesProTask',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5540;

  UPDATE `system_menu`
  SET `name` = '璞慧排产',
      `permission` = 'mes:pro-puhui-schedule:query',
      `type` = 2,
      `sort` = 6,
      `parent_id` = 900120,
      `path` = '/mes/pro/puhui-schedule',
      `icon` = 'ep:calendar',
      `component` = 'mes/pro/puhui-schedule/index',
      `component_name` = 'MesProPuhuiSchedule',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900104;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5590
      AND `deleted` = b'0'
      AND `permission` = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES scheduler workbench must have permission before visibility can be controlled';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_workbench_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_scheduler_workbench_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_scheduler_workbench_package_menu_ids` (`package_id`, `menu_id`)
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
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900120' AS JSON), '$');

  INSERT IGNORE INTO `tmp_mes_scheduler_workbench_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `smart_menu`.`id`
  FROM `system_tenant_package` AS `package`
  JOIN `system_menu` AS `smart_menu`
    ON `smart_menu`.`id` IN (900120, 5590)
   AND `smart_menu`.`deleted` = b'0'
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`menu_ids`)
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900120' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_scheduler_workbench_package_menu_ids`
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
  JOIN `system_menu` m ON m.`id` IN (900120, 5590)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_scheduler_workbench_package_menu_ids`;
END//
DELIMITER ;

CALL ensure_mes_scheduler_workbench_smart_scheduling_tab();

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_workbench_smart_scheduling_tab;
