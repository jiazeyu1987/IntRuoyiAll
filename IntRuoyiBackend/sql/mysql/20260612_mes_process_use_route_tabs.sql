-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_process_use_route_tabs;

DELIMITER //
CREATE PROCEDURE ensure_mes_process_use_route_tabs()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge MES process use route tabs';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900120
      AND `name` = '智能排产'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling parent menu 900120; cannot append process schedule route tab';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `permission` = 'mes:pro-edhr-batch-processing:query'
      AND `path` = 'edhr-batch-processing'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch processing parent menu 900220; cannot append process batch record route tab';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900121
      AND `deleted` = b'0'
      AND (`name` <> '工艺排产路线' OR `parent_id` <> 900120)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Menu id 900121 is already used by another menu; cannot create process schedule route tab';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900221
      AND `deleted` = b'0'
      AND (`name` <> '工艺批记录路线' OR `parent_id` <> 900220)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Menu id 900221 is already used by another menu; cannot create process batch record route tab';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (5985, 5590, 5580, 5550, 5262, 5540, 900104)
      AND `deleted` = b'0'
  ) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES smart scheduling existing child menus; cannot reorder process schedule route tab';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (900002, 900024, 900025, 900026, 900033)
      AND `deleted` = b'0'
  ) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch processing existing child menus; cannot reorder process batch record route tab';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  ) VALUES
  (
    900121, '工艺排产路线', 'mes:pro-schedule-route:query', 2, 3, 900120,
    '/mes/pro/schedule-route', 'ep:connection', 'mes/pro/schedule-route/index',
    'MesProScheduleRoute', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  ),
  (
    900122, '工艺排产路线配置', 'mes:pro-schedule-route:update', 3, 1, 900121,
    '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  ),
  (
    900221, '工艺批记录路线', 'mes:pro-batch-record-route:query', 2, 1, 900220,
    '/mes/pro/feedback/edhr-batch-route', 'ep:document-checked',
    'mes/pro/edhr-batch-route/index', 'MesProEdhrBatchRoute', 0, b'1', b'1', b'1',
    '1', NOW(), '1', NOW(), b'0'
  ),
  (
    900222, '工艺批记录路线配置', 'mes:pro-batch-record-route:update', 3, 1, 900221,
    '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
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
  SET `sort` = 0,
      `parent_id` = 900120,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5985;

  UPDATE `system_menu`
  SET `sort` = 1,
      `parent_id` = 900120,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5590;

  UPDATE `system_menu`
  SET `sort` = 2,
      `parent_id` = 900120,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5580;

  UPDATE `system_menu`
  SET `name` = '工艺排产路线',
      `permission` = 'mes:pro-schedule-route:query',
      `type` = 2,
      `sort` = 3,
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
  SET `sort` = 4,
      `parent_id` = 900120,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5550;

  UPDATE `system_menu`
  SET `sort` = 5,
      `parent_id` = 900120,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5262;

  UPDATE `system_menu`
  SET `sort` = 6,
      `parent_id` = 900120,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 5540;

  UPDATE `system_menu`
  SET `sort` = 7,
      `parent_id` = 900120,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900104;

  UPDATE `system_menu`
  SET `sort` = 0,
      `parent_id` = 900220,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900002;

  UPDATE `system_menu`
  SET `name` = '工艺批记录路线',
      `permission` = 'mes:pro-batch-record-route:query',
      `type` = 2,
      `sort` = 1,
      `parent_id` = 900220,
      `path` = '/mes/pro/feedback/edhr-batch-route',
      `icon` = 'ep:document-checked',
      `component` = 'mes/pro/edhr-batch-route/index',
      `component_name` = 'MesProEdhrBatchRoute',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900221;

  UPDATE `system_menu`
  SET `sort` = 2,
      `parent_id` = 900220,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900024;

  UPDATE `system_menu`
  SET `sort` = 3,
      `parent_id` = 900220,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900025;

  UPDATE `system_menu`
  SET `sort` = 4,
      `parent_id` = 900220,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900026;

  UPDATE `system_menu`
  SET `sort` = 5,
      `parent_id` = 900220,
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900033;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` IN (900121, 900221)
      AND `deleted` = b'0'
      AND `permission` = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Process use route tabs must have query permissions before visibility can be controlled';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_schedule_route_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_process_schedule_route_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900120' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_batch_route_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_process_batch_route_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('900220' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_schedule_route_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_process_schedule_route_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_process_schedule_route_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_process_schedule_route_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_process_schedule_route_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_process_schedule_route_packages` AS `target_package`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900120, 900121, 900122)
   AND `menu`.`deleted` = b'0';

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_process_schedule_route_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = '1',
      `package`.`update_time` = NOW();

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_batch_route_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_process_batch_route_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_process_batch_route_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_process_batch_route_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_process_batch_route_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    `menu`.`id`
  FROM `tmp_mes_process_batch_route_packages` AS `target_package`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` IN (900220, 900221, 900222)
   AND `menu`.`deleted` = b'0';

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_process_batch_route_package_menu_ids`
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
  JOIN `tmp_mes_process_schedule_route_packages` AS `target_package`
    ON `target_package`.`package_id` = t.`package_id`
  JOIN `system_tenant_package` tp
    ON tp.`id` = t.`package_id`
   AND tp.`deleted` = b'0'
   AND JSON_VALID(tp.`menu_ids`)
  JOIN `system_menu` m ON m.`id` IN (900120, 900121, 900122)
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
  JOIN `tmp_mes_process_batch_route_packages` AS `target_package`
    ON `target_package`.`package_id` = t.`package_id`
  JOIN `system_tenant_package` tp
    ON tp.`id` = t.`package_id`
   AND tp.`deleted` = b'0'
   AND JSON_VALID(tp.`menu_ids`)
  JOIN `system_menu` m ON m.`id` IN (900220, 900221, 900222)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_batch_route_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_schedule_route_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_batch_route_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_process_schedule_route_packages`;
END//
DELIMITER ;

CALL ensure_mes_process_use_route_tabs();

DROP PROCEDURE IF EXISTS ensure_mes_process_use_route_tabs;
