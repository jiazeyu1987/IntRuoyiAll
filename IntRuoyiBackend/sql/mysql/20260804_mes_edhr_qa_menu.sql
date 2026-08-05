-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_edhr_template_config_menu_removal; type=menu; riskLevel=low
-- Insert the standalone QA regulation, production leader, and PQC leader menus under eDHR before batch execution.
-- Leader pages open formal process-pool workbench wrappers without using eDHR internal tabs.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_qa_menu;

DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_qa_menu()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot insert eDHR QA menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR parent menu 900220; cannot insert QA menu';
  END IF;

  IF (SELECT COUNT(*)
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `id` IN (900365, 900033, 900025, 900432)) <> 4 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing retained eDHR visible menu rows; cannot insert QA menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900434
      AND `deleted` = b'0'
      AND NOT (
        `name` = 'QA'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/process-pool/qa-regulation'
        AND `component` = 'mes/pro/processpool/QaRegulationPage'
        AND `component_name` = 'MesProProcessPoolQaRegulation'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 900434 is already used by another active menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900436
      AND `deleted` = b'0'
      AND NOT (
        `name` = '生产组长'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/process-pool/production-leader'
        AND `component` = 'mes/pro/processpool/ProductionLeaderWorkbenchPage'
        AND `component_name` = 'MesProProcessPoolProductionLeaderWorkbench'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 900436 is already used by another active menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900435
      AND `deleted` = b'0'
      AND NOT (
        `name` = 'PQC组长'
        AND `parent_id` = 900220
        AND `path` = '/mes/pro/process-pool/pqc-leader'
        AND `component` = 'mes/pro/processpool/PqcLeaderWorkbenchPage'
        AND `component_name` = 'MesProProcessPoolPqcLeaderWorkbench'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'system_menu id 900435 is already used by another active menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/process-pool/qa-regulation'
      AND `id` <> 900434
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA menu route already exists on a different menu id';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/process-pool/production-leader'
      AND `id` <> 900436
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Production leader menu route already exists on a different menu id';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `path` = '/mes/pro/process-pool/pqc-leader'
      AND `id` <> 900435
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'PQC leader menu route already exists on a different menu id';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_qa_visible_order`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_qa_visible_order` (
    `id` bigint NOT NULL PRIMARY KEY,
    `name` varchar(64) NOT NULL,
    `permission` varchar(128) NOT NULL,
    `sort` int NOT NULL,
    `path` varchar(255) NOT NULL,
    `icon` varchar(64) NOT NULL,
    `component` varchar(255) NOT NULL,
    `component_name` varchar(128) NOT NULL
  );

  INSERT INTO `tmp_mes_edhr_qa_visible_order`
  SELECT 900365 AS `id`, '批记录表单' AS `name`, 'mes:pro-batch-record-template:query' AS `permission`, 0 AS `sort`,
         '/mes/pro/batch-record-form-list' AS `path`, 'ep:tickets' AS `icon`,
         'mes/pro/batchrecordformlist/index' AS `component`, 'MesProBatchRecordFormList' AS `component_name`
  UNION ALL
  SELECT 900434 AS `id`, 'QA' AS `name`, 'mes:pro-process-pool-team-leader:query' AS `permission`, 1 AS `sort`,
         '/mes/pro/process-pool/qa-regulation' AS `path`, 'ep:document-checked' AS `icon`,
         'mes/pro/processpool/QaRegulationPage' AS `component`, 'MesProProcessPoolQaRegulation' AS `component_name`
  UNION ALL
  SELECT 900436 AS `id`, '生产组长' AS `name`, 'mes:pro-process-pool-team-leader:query' AS `permission`, 2 AS `sort`,
         '/mes/pro/process-pool/production-leader' AS `path`, 'ep:user' AS `icon`,
         'mes/pro/processpool/ProductionLeaderWorkbenchPage' AS `component`, 'MesProProcessPoolProductionLeaderWorkbench' AS `component_name`
  UNION ALL
  SELECT 900435 AS `id`, 'PQC组长' AS `name`, 'mes:pro-process-pool-team-leader:query' AS `permission`, 3 AS `sort`,
         '/mes/pro/process-pool/pqc-leader' AS `path`, 'ep:user-filled' AS `icon`,
         'mes/pro/processpool/PqcLeaderWorkbenchPage' AS `component`, 'MesProProcessPoolPqcLeaderWorkbench' AS `component_name`
  UNION ALL
  SELECT 900033 AS `id`, '批次执行' AS `name`, 'mes:pro-edhr-batch-execution:query' AS `permission`, 4 AS `sort`,
         '/mes/pro/feedback/edhr-batch-execution' AS `path`, 'ep:document-checked' AS `icon`,
         'mes/pro/edhr-batch/BatchExecutionListPage' AS `component`, 'MesProEdhrBatchExecutionListPage' AS `component_name`
  UNION ALL
  SELECT 900025 AS `id`, '表单追溯' AS `name`, 'mes:pro-batch-record-execution:track' AS `permission`, 5 AS `sort`,
         '/mes/pro/feedback/edhr-form-trace' AS `path`, 'ep:position' AS `icon`,
         'mes/pro/edhr/FormTracePage' AS `component`, 'MesProFeedbackEdhrFormTrace' AS `component_name`
  UNION ALL
  SELECT 900432 AS `id`, '表单日志' AS `name`, 'mes:pro-edhr-form-fill-log:query' AS `permission`, 6 AS `sort`,
         '/mes/pro/feedback/edhr-form-fill-log' AS `path`, 'ep:document-copy' AS `icon`,
         'mes/pro/edhr/FormFillLogPage' AS `component`, 'MesProEdhrFormFillLogPage' AS `component_name`;

  IF (SELECT COUNT(*) FROM `tmp_mes_edhr_qa_visible_order`) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR QA menu visible order contract must declare exactly seven entries';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    `visible_order`.`id`,
    `visible_order`.`name`,
    `visible_order`.`permission`,
    2,
    `visible_order`.`sort`,
    900220,
    `visible_order`.`path`,
    `visible_order`.`icon`,
    `visible_order`.`component`,
    `visible_order`.`component_name`,
    0,
    b'1',
    b'1',
    b'1',
    'edhr-qa-menu',
    NOW(),
    'edhr-qa-menu',
    NOW(),
    b'0'
  FROM `tmp_mes_edhr_qa_visible_order` AS `visible_order`
  WHERE `visible_order`.`id` IN (900434, 900435, 900436)
    AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = `visible_order`.`id`
    );

  UPDATE `system_menu` AS `menu`
  JOIN `tmp_mes_edhr_qa_visible_order` AS `visible_order`
    ON `visible_order`.`id` = `menu`.`id`
  SET `menu`.`name` = `visible_order`.`name`,
      `menu`.`permission` = `visible_order`.`permission`,
      `menu`.`type` = 2,
      `menu`.`sort` = `visible_order`.`sort`,
      `menu`.`parent_id` = 900220,
      `menu`.`path` = `visible_order`.`path`,
      `menu`.`icon` = `visible_order`.`icon`,
      `menu`.`component` = `visible_order`.`component`,
      `menu`.`component_name` = `visible_order`.`component_name`,
      `menu`.`status` = 0,
      `menu`.`visible` = b'1',
      `menu`.`keep_alive` = b'1',
      `menu`.`always_show` = b'1',
      `menu`.`deleted` = b'0',
      `menu`.`updater` = 'edhr-qa-menu',
      `menu`.`update_time` = NOW();

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_qa_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_qa_target_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$')
    AND (
      JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900365' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900033' AS JSON), '$')
    );

  IF (SELECT COUNT(*) FROM `tmp_mes_edhr_qa_target_packages`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing target tenant package with eDHR parent menu; cannot expose QA menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_qa_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_qa_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_qa_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_qa_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    CAST(`package`.`menu_ids` AS JSON),
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_edhr_qa_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, 900434
  FROM `tmp_mes_edhr_qa_target_packages`;

  INSERT IGNORE INTO `tmp_mes_edhr_qa_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, 900436
  FROM `tmp_mes_edhr_qa_target_packages`;

  INSERT IGNORE INTO `tmp_mes_edhr_qa_package_menu_ids` (`package_id`, `menu_id`)
  SELECT `package_id`, 900435
  FROM `tmp_mes_edhr_qa_target_packages`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_edhr_qa_package_menu_ids`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'edhr-qa-menu',
      `package`.`update_time` = NOW();

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_qa_target_packages` AS `target_package`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target_package`.`package_id`
     AND `package`.`deleted` = b'0'
    WHERE NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900434' AS JSON), '$')
       OR NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900436' AS JSON), '$')
       OR NOT JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900435' AS JSON), '$')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA or leader menu is missing from target tenant packages';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_qa_target_roles`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_qa_target_roles` (
    `role_id` bigint NOT NULL,
    `tenant_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `tenant_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_qa_target_roles` (`role_id`, `tenant_id`)
  SELECT DISTINCT `role_menu`.`role_id`, `role_menu`.`tenant_id`
  FROM `system_role_menu` AS `role_menu`
  JOIN `system_role` AS `role`
    ON `role`.`id` = `role_menu`.`role_id`
   AND `role`.`tenant_id` = `role_menu`.`tenant_id`
   AND `role`.`deleted` = b'0'
  WHERE `role_menu`.`deleted` = b'0'
    AND `role_menu`.`menu_id` IN (900220, 900365, 900033);

  INSERT IGNORE INTO `tmp_mes_edhr_qa_target_roles` (`role_id`, `tenant_id`)
  SELECT DISTINCT `role`.`id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN `system_tenant` AS `tenant`
    ON `tenant`.`id` = `role`.`tenant_id`
   AND `tenant`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_qa_target_packages` AS `target_package`
    ON `target_package`.`package_id` = `tenant`.`package_id`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` IN ('tenant_admin', 'super_admin');

  INSERT IGNORE INTO `tmp_mes_edhr_qa_target_roles` (`role_id`, `tenant_id`)
  SELECT DISTINCT `role`.`id`, `role`.`tenant_id`
  FROM `system_role` AS `role`
  WHERE `role`.`deleted` = b'0'
    AND `role`.`code` = 'super_admin';

  IF (SELECT COUNT(*) FROM `tmp_mes_edhr_qa_target_roles`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA menu is not bound to any admin role';
  END IF;

  UPDATE `system_role_menu` AS `role_menu`
  JOIN `tmp_mes_edhr_qa_target_roles` AS `target_role`
    ON `target_role`.`role_id` = `role_menu`.`role_id`
   AND `target_role`.`tenant_id` = `role_menu`.`tenant_id`
  SET `role_menu`.`deleted` = b'0',
      `role_menu`.`updater` = 'edhr-qa-menu',
      `role_menu`.`update_time` = NOW()
  WHERE `role_menu`.`menu_id` IN (900434, 900435, 900436);

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `target_role`.`role_id`,
    `target_menu`.`menu_id`,
    'edhr-qa-menu',
    NOW(),
    'edhr-qa-menu',
    NOW(),
    b'0',
    `target_role`.`tenant_id`
  FROM `tmp_mes_edhr_qa_target_roles` AS `target_role`
  JOIN (
    SELECT 900434 AS `menu_id`
    UNION ALL
    SELECT 900436 AS `menu_id`
    UNION ALL
    SELECT 900435 AS `menu_id`
  ) AS `target_menu`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`role_id` = `target_role`.`role_id`
      AND `existing`.`tenant_id` = `target_role`.`tenant_id`
      AND `existing`.`menu_id` = `target_menu`.`menu_id`
      AND `existing`.`deleted` = b'0'
  );

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_qa_target_roles` AS `target_role`
    JOIN (
      SELECT 900434 AS `menu_id`
      UNION ALL
      SELECT 900436 AS `menu_id`
      UNION ALL
      SELECT 900435 AS `menu_id`
    ) AS `target_menu`
    WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `role_menu`
      WHERE `role_menu`.`role_id` = `target_role`.`role_id`
        AND `role_menu`.`tenant_id` = `target_role`.`tenant_id`
        AND `role_menu`.`menu_id` = `target_menu`.`menu_id`
        AND `role_menu`.`deleted` = b'0'
    )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA or leader menu is not bound to any admin role';
  END IF;

  IF (SELECT COUNT(*)
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `parent_id` = 900220
        AND `type` = 2
        AND `visible` = b'1'
        AND (
          (`id` = 900365 AND `name` = '批记录表单' AND `sort` = 0 AND `path` = '/mes/pro/batch-record-form-list' AND `component` = 'mes/pro/batchrecordformlist/index')
          OR (`id` = 900434 AND `name` = 'QA' AND `sort` = 1 AND `path` = '/mes/pro/process-pool/qa-regulation' AND `component` = 'mes/pro/processpool/QaRegulationPage' AND `component_name` = 'MesProProcessPoolQaRegulation')
          OR (`id` = 900436 AND `name` = '生产组长' AND `sort` = 2 AND `path` = '/mes/pro/process-pool/production-leader' AND `component` = 'mes/pro/processpool/ProductionLeaderWorkbenchPage' AND `component_name` = 'MesProProcessPoolProductionLeaderWorkbench')
          OR (`id` = 900435 AND `name` = 'PQC组长' AND `sort` = 3 AND `path` = '/mes/pro/process-pool/pqc-leader' AND `component` = 'mes/pro/processpool/PqcLeaderWorkbenchPage' AND `component_name` = 'MesProProcessPoolPqcLeaderWorkbench')
          OR (`id` = 900033 AND `name` = '批次执行' AND `sort` = 4 AND `path` = '/mes/pro/feedback/edhr-batch-execution' AND `component` = 'mes/pro/edhr-batch/BatchExecutionListPage')
          OR (`id` = 900025 AND `name` = '表单追溯' AND `sort` = 5 AND `path` = '/mes/pro/feedback/edhr-form-trace' AND `component` = 'mes/pro/edhr/FormTracePage')
          OR (`id` = 900432 AND `name` = '表单日志' AND `sort` = 6 AND `path` = '/mes/pro/feedback/edhr-form-fill-log' AND `component` = 'mes/pro/edhr/FormFillLogPage')
        )) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR QA visible menu order is incomplete';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_qa_target_roles`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_qa_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_qa_target_packages`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_qa_visible_order`;
END $$
DELIMITER ;

CALL ensure_mes_edhr_qa_menu();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_qa_menu;
