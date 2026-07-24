-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_workbench_only_summary;

DELIMITER //
CREATE PROCEDURE ensure_mes_scheduler_workbench_only_summary()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900120
      AND `name` = '智能排产'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling parent menu 900120; cannot hide scheduler board tab';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (5985, 5590, 5580, 5550, 5262, 900121, 5540, 900104)
      AND `deleted` = b'0'
  ) <> 8 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling menus 5985/5590/5580/5550/5262/900121/5540/900104';
  END IF;

  UPDATE `system_menu`
  SET `name` = '排产看板',
      `permission` = 'mes:home:query',
      `type` = 2,
      `sort` = 99,
      `parent_id` = 900120,
      `path` = '/mes/home/index',
      `icon` = 'ep:dashboard',
      `component` = 'mes/home/index',
      `component_name` = 'MesHome',
      `status` = 0,
      `visible` = b'0',
      `keep_alive` = b'1',
      `always_show` = b'0',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5985;

  UPDATE `system_menu`
  SET `name` = '排产员工作台',
      `permission` = 'mes:pro-scheduler-workbench:query',
      `type` = 2,
      `sort` = 0,
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
      `sort` = 3,
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
  SET `name` = '工艺排产路线',
      `permission` = 'mes:pro-schedule-route:query',
      `type` = 2,
      `sort` = 4,
      `parent_id` = 900120,
      `path` = '/mes/pro/schedule-route',
      `icon` = 'ep:connection',
      `component` = 'mes/pro/schedule-route/index',
      `component_name` = 'MesProScheduleRoute',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900121;

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
END//
DELIMITER ;

CALL ensure_mes_scheduler_workbench_only_summary();

DROP PROCEDURE IF EXISTS ensure_mes_scheduler_workbench_only_summary;
