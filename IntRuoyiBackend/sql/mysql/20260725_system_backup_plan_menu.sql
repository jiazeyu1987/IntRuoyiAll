-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_system_backup_plan_menu;

DELIMITER //
CREATE PROCEDURE ensure_system_backup_plan_menu()
BEGIN
  DECLARE v_menu_id BIGINT DEFAULT NULL;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 1
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing system management root menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE (
        `id` = 901100
        AND (
          `deleted` <> b'0'
          OR `permission` <> 'system:backup-plan:query'
          OR `component` <> 'system/backup-plan/index'
        )
      )
      OR (
        `id` = 901101
        AND (`deleted` <> b'0' OR `permission` <> 'system:backup-plan:update')
      )
      OR (
        `id` = 901102
        AND (`deleted` <> b'0' OR `permission` <> 'system:backup-plan:execute')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting system_menu id exists for system backup plan menus';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    901100, '备份计划', 'system:backup-plan:query', 2, 101, 1, 'backup-plan',
    'ep:calendar', 'system/backup-plan/index', 'SystemBackupPlan',
    0, b'1', b'1', b'1', 'codex', NOW(), 'codex', NOW(), b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (
        `id` = 901100
        OR `permission` = 'system:backup-plan:query'
        OR `path` = 'backup-plan'
        OR `component` = 'system/backup-plan/index'
      )
  );

  SELECT `id` INTO v_menu_id
  FROM `system_menu`
  WHERE `permission` = 'system:backup-plan:query'
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1;

  IF v_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing system backup plan menu';
  END IF;

  UPDATE `system_menu`
  SET `name` = '备份计划',
      `type` = 2,
      `sort` = 101,
      `parent_id` = 1,
      `path` = 'backup-plan',
      `icon` = 'ep:calendar',
      `component` = 'system/backup-plan/index',
      `component_name` = 'SystemBackupPlan',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = v_menu_id;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    `seed`.`id`, `seed`.`name`, `seed`.`permission`, 3, `seed`.`sort`, v_menu_id,
    '', '', '', '', 0, b'1', b'1', b'1', 'codex', NOW(), 'codex', NOW(), b'0'
  FROM (
    SELECT 901101 AS `id`, '保存备份计划' AS `name`, 'system:backup-plan:update' AS `permission`, 1 AS `sort`
    UNION ALL SELECT 901102, '立即备份一次', 'system:backup-plan:execute', 2
  ) AS `seed`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND (`existing`.`id` = `seed`.`id` OR `existing`.`permission` = `seed`.`permission`)
  );

  UPDATE `system_menu`
  SET `parent_id` = v_menu_id,
      `type` = 3,
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `permission` IN ('system:backup-plan:update', 'system:backup-plan:execute')
    AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_backup_plan_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_system_backup_plan_menu_ids` (
    `menu_id` BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO `tmp_system_backup_plan_menu_ids` (`menu_id`)
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` IN (
      'system:backup-plan:query',
      'system:backup-plan:update',
      'system:backup-plan:execute'
    )
    AND `status` = 0
    AND `deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_system_backup_plan_menu_ids`) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or duplicated system backup plan permissions';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge system backup plan menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_backup_plan_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_system_backup_plan_package_menu_ids` (
    `package_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_system_backup_plan_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `existing_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`);

  INSERT IGNORE INTO `tmp_system_backup_plan_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    `backup_menu`.`menu_id`
  FROM `system_tenant_package` AS `package`
  CROSS JOIN `tmp_system_backup_plan_menu_ids` AS `backup_menu`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('1' AS JSON), '$');

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_system_backup_plan_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'codex',
      `package`.`update_time` = NOW();

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `role`.`id`,
    `backup_menu`.`menu_id`,
    'codex',
    NOW(),
    'codex',
    NOW(),
    b'0',
    `role`.`tenant_id`
  FROM `system_role` AS `role`
  CROSS JOIN `tmp_system_backup_plan_menu_ids` AS `backup_menu`
  LEFT JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  LEFT JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `tenant`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  WHERE `role`.`deleted` = b'0'
    AND (
      `role`.`code` = 'super_admin'
      OR (
        `role`.`code` = 'tenant_admin'
        AND `package`.`id` IS NOT NULL
        AND JSON_CONTAINS(`package`.`menu_ids`, CAST(`backup_menu`.`menu_id` AS JSON), '$')
      )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `backup_menu`.`menu_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_system_backup_plan_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_system_backup_plan_menu_ids`;
END//
DELIMITER ;

CALL ensure_system_backup_plan_menu();

DROP PROCEDURE IF EXISTS ensure_system_backup_plan_menu;
