-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260805_mes_edhr_frontline_pqc_menu; type=menu; riskLevel=low
-- Expose the eDHR batch record test page in the admin-visible eDHR menu.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_record_test_menu;

DELIMITER $$

CREATE PROCEDURE ensure_mes_edhr_batch_record_test_menu()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `menu_ids` IS NOT NULL
      AND `menu_ids` <> ''
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot insert batch record test menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR parent menu 900220; cannot insert batch record test menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900033
      AND `deleted` = b'0'
      AND `path` = '/mes/pro/feedback/edhr-batch-execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch execution menu 900033; cannot insert batch record test menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900440
      AND `deleted` = b'0'
      AND NOT (
        `name` = '批记录测试'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/feedback/edhr-batch-test'
        AND `component` = 'mes/pro/edhr-batch/BatchRecordTestPage'
        AND `component_name` = 'MesProEdhrBatchRecordTest'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 900440 is already used by another active menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/feedback/edhr-batch-test'
      AND `id` <> 900440
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record test menu route already exists on a different menu id';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    900440 AS `id`,
    '批记录测试' AS `name`,
    'mes:pro-edhr-batch-execution:query' AS `permission`,
    2 AS `type`,
    7 AS `sort`,
    900220 AS `parent_id`,
    '/mes/pro/feedback/edhr-batch-test' AS `path`,
    'ep:operation' AS `icon`,
    'mes/pro/edhr-batch/BatchRecordTestPage' AS `component`,
    'MesProEdhrBatchRecordTest' AS `component_name`,
    0 AS `status`,
    b'1' AS `visible`,
    b'1' AS `keep_alive`,
    b'1' AS `always_show`,
    'edhr-batch-record-test-menu' AS `creator`,
    NOW() AS `create_time`,
    'edhr-batch-record-test-menu' AS `updater`,
    NOW() AS `update_time`,
    b'0' AS `deleted`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900440
  );

  UPDATE `system_menu`
  SET `name` = '批记录测试',
      `permission` = 'mes:pro-edhr-batch-execution:query',
      `type` = 2,
      `sort` = 7,
      `parent_id` = 900220,
      `path` = '/mes/pro/feedback/edhr-batch-test',
      `icon` = 'ep:operation',
      `component` = 'mes/pro/edhr-batch/BatchRecordTestPage',
      `component_name` = 'MesProEdhrBatchRecordTest',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = 'edhr-batch-record-test-menu',
      `update_time` = NOW()
  WHERE `id` = 900440;

  UPDATE `system_menu` AS `menu`
  JOIN (
    SELECT 900033 AS `menu_id`, 6 AS `sort`
    UNION ALL
    SELECT 900440 AS `menu_id`, 7 AS `sort`
    UNION ALL
    SELECT 900025 AS `menu_id`, 8 AS `sort`
    UNION ALL
    SELECT 900432 AS `menu_id`, 9 AS `sort`
  ) AS `ordered_menu`
    ON `ordered_menu`.`menu_id` = `menu`.`id`
  SET `menu`.`sort` = `ordered_menu`.`sort`,
      `menu`.`updater` = 'edhr-batch-record-test-menu',
      `menu`.`update_time` = NOW()
  WHERE `menu`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_record_test_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_record_test_target_packages` (
    `package_id` bigint NOT NULL PRIMARY KEY
  );

  INSERT IGNORE INTO `tmp_mes_edhr_batch_record_test_target_packages` (`package_id`)
  SELECT `package`.`id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$')
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900033' AS JSON), '$');

  IF (SELECT COUNT(*) FROM `tmp_mes_edhr_batch_record_test_target_packages`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing target tenant package with eDHR batch execution menu; cannot expose batch record test menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_record_test_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_record_test_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_batch_record_test_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `package`.`id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_batch_record_test_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `package`.`id`
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    CAST(`package`.`menu_ids` AS JSON),
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_edhr_batch_record_test_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, 900440
  FROM `tmp_mes_edhr_batch_record_test_target_packages`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT `package_id`, `menu_ids`
    FROM (
      SELECT
        `package_id`,
        JSON_ARRAYAGG(`menu_id`) OVER (
          PARTITION BY `package_id`
          ORDER BY `menu_id`
          ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
        ) AS `menu_ids`,
        ROW_NUMBER() OVER (PARTITION BY `package_id` ORDER BY `menu_id` DESC) AS `rn`
      FROM `tmp_mes_edhr_batch_record_test_package_menu_ids`
    ) AS `ordered_package_menu`
    WHERE `rn` = 1
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-batch-record-test-menu',
      `package`.`update_time` = NOW();

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package` AS `package`
    JOIN `tmp_mes_edhr_batch_record_test_target_packages` AS `target_package`
      ON `target_package`.`package_id` = `package`.`id`
    WHERE NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900440' AS JSON), '$')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record test menu is missing from target tenant packages';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_record_test_target_roles`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_record_test_target_roles` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_batch_record_test_target_roles` (`role_id`, `tenant_id`)
  SELECT DISTINCT `role_menu`.`role_id`, `role_menu`.`tenant_id`
  FROM `system_role_menu` AS `role_menu`
  JOIN `system_role` AS `role`
    ON `role`.`id` = `role_menu`.`role_id`
   AND `role`.`tenant_id` = `role_menu`.`tenant_id`
   AND `role`.`deleted` = b'0'
  WHERE `role_menu`.`deleted` = b'0'
    AND `role_menu`.`menu_id` IN (900220, 900033);

  INSERT IGNORE INTO `tmp_mes_edhr_batch_record_test_target_roles` (`role_id`, `tenant_id`)
  SELECT DISTINCT `role`.`id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_batch_record_test_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` IN ('tenant_admin', 'super_admin');

  INSERT IGNORE INTO `tmp_mes_edhr_batch_record_test_target_roles` (`role_id`, `tenant_id`)
  SELECT DISTINCT `role`.`id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` = 'super_admin';

  IF (SELECT COUNT(*) FROM `tmp_mes_edhr_batch_record_test_target_roles`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record test menu is not bound to any admin role';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_edhr_batch_record_test_target_roles` AS `target_role`
    ON `target_role`.`role_id` = `role_menu`.`role_id`
   AND `target_role`.`tenant_id` = `role_menu`.`tenant_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'edhr-batch-record-test-menu',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`menu_id` = 900440;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target_role`.`role_id`,
    900440 AS `menu_id`,
    'edhr-batch-record-test-menu',
    NOW(),
    'edhr-batch-record-test-menu',
    NOW(),
    b'0',
    `target_role`.`tenant_id`
  FROM `tmp_mes_edhr_batch_record_test_target_roles` AS `target_role`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `target_role`.`role_id`
      AND `existing`.`tenant_id` = `target_role`.`tenant_id`
      AND `existing`.`menu_id` = 900440
      AND `existing`.`deleted` = b'0'
  );

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_batch_record_test_target_roles` AS `target_role`
    WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `role_menu`
      WHERE `role_menu`.`role_id` = `target_role`.`role_id`
        AND `role_menu`.`tenant_id` = `target_role`.`tenant_id`
        AND `role_menu`.`menu_id` = 900440
        AND `role_menu`.`deleted` = b'0'
    )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record test menu is not bound to any admin role';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900440
      AND `name` = '批记录测试'
      AND `permission` = 'mes:pro-edhr-batch-execution:query'
      AND `type` = 2
      AND `sort` = 7
      AND `parent_id` = 900220
      AND `path` = '/mes/pro/feedback/edhr-batch-test'
      AND `component` = 'mes/pro/edhr-batch/BatchRecordTestPage'
      AND `component_name` = 'MesProEdhrBatchRecordTest'
      AND `status` = 0
      AND `visible` = b'1'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record test visible menu is incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_record_test_target_roles`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_record_test_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_record_test_target_packages`;
END$$

DELIMITER ;

CALL ensure_mes_edhr_batch_record_test_menu();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_record_test_menu;
