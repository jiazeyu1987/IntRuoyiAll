-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_mes_edhr_work_task_ownership; type=menu; riskLevel=medium
-- Purpose: Retire the obsolete eDHR execution list page and migrate access to eDHR batch execution.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_execution_list_retired;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_execution_list_retired()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900033
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch execution replacement menu 900033';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900023
      AND COALESCE(`permission`, '') NOT IN (
        'mes:pro-batch-record-execution:query',
        'RETIRED_EDHR_EXECUTION_LIST'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Unexpected system_menu 900023 permission; refusing to retire unrelated menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    WHERE `package`.`deleted` = b'0'
      AND NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON';
  END IF;

  UPDATE `system_menu`
  SET `path` = '/mes/pro/feedback/edhr-batch-execution',
      `component` = 'mes/pro/edhr-batch/BatchExecutionListPage',
      `component_name` = 'MesProEdhrBatchExecutionListPage',
      `visible` = b'1',
      `status` = 0,
      `deleted` = b'0',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 900033;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900033
      AND `path` = '/mes/pro/feedback/edhr-batch-execution'
      AND `component` = 'mes/pro/edhr-batch/BatchExecutionListPage'
      AND `component_name` = 'MesProEdhrBatchExecutionListPage'
      AND `visible` = b'1'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid eDHR batch execution replacement menu 900033';
  END IF;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT DISTINCT
    `old_role_menu`.`role_id`,
    900033,
    'codex',
    NOW(),
    'codex',
    NOW(),
    b'0',
    `old_role_menu`.`tenant_id`
  FROM `system_role_menu` AS `old_role_menu`
  WHERE `old_role_menu`.`menu_id` = 900023
    AND `old_role_menu`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `old_role_menu`.`role_id`
        AND `existing`.`tenant_id` = `old_role_menu`.`tenant_id`
        AND `existing`.`menu_id` = 900033
        AND `existing`.`deleted` = b'0'
    );

  DELETE FROM `system_role_menu`
  WHERE `menu_id` = 900023;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_execution_list_retired_packages`;
  CREATE TEMPORARY TABLE `tmp_edhr_execution_list_retired_packages` (
    `package_id` bigint NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_edhr_execution_list_retired_packages` (`package_id`)
  SELECT `package`.`id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(`package`.`menu_ids`, CAST('900023' AS JSON), '$');

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_execution_list_retired_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_edhr_execution_list_retired_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  ) ENGINE=Memory;

  INSERT IGNORE INTO `tmp_edhr_execution_list_retired_package_menu_ids` (`package_id`, `menu_id`)
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
    AND `existing_menu`.`menu_id` <> 900023;

  INSERT IGNORE INTO `tmp_edhr_execution_list_retired_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package`.`package_id`, 900033
  FROM `tmp_edhr_execution_list_retired_packages` AS `package`;

  UPDATE `system_tenant_package` AS `package`
  LEFT JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_edhr_execution_list_retired_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = COALESCE(`merged`.`menu_ids`, JSON_ARRAY()),
      `package`.`updater` = 'codex',
      `package`.`update_time` = NOW()
  WHERE `package`.`id` IN (
    SELECT `package_id`
    FROM `tmp_edhr_execution_list_retired_packages`
  );

  UPDATE `system_menu`
  SET `name` = '已废弃-eDHR执行列表',
      `permission` = 'RETIRED_EDHR_EXECUTION_LIST',
      `type` = 2,
      `sort` = 99,
      `path` = '/retired/edhr-execution-list',
      `icon` = '',
      `component` = '',
      `component_name` = 'RETIRED_EDHR_EXECUTION_LIST',
      `status` = 1,
      `visible` = b'0',
      `keep_alive` = b'0',
      `always_show` = b'0',
      `deleted` = b'1',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 900023;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_execution_list_retired_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_execution_list_retired_packages`;
END//
DELIMITER ;

CALL ensure_mes_edhr_execution_list_retired();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_execution_list_retired;
