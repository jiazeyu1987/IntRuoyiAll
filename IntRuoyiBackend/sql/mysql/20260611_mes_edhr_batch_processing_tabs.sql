-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_processing_tabs;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_batch_processing_tabs()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR batch processing menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 5100
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES system parent menu 5100; cannot create eDHR batch processing menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `deleted` = b'0'
      AND (
        `name` NOT IN ('eDHR批处理', 'eDHR批记录')
        OR `permission` <> 'mes:pro-edhr-batch-processing:query'
        OR `parent_id` <> 5100
        OR `path` <> 'edhr-batch-processing'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Menu id 900220 is already used by another menu; cannot create eDHR batch processing menu';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (900002, 900024, 900025, 900026, 900033)
      AND `deleted` = b'0'
  ) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch processing target menus; cannot regroup eDHR batch processing tabs';
  END IF;

  INSERT INTO `system_menu` (
    `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  ) VALUES (
    900220, 'eDHR批记录', 'mes:pro-edhr-batch-processing:query', 1, 1, 5100,
    'edhr-batch-processing', 'ep:document-copy', '', '', 0, b'1', b'1', b'1',
    '1', NOW(), '1', NOW(), b'0'
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
  SET `name` = '电子批记录',
      `permission` = 'mes:pro-batch-record-template:query',
      `type` = 2,
      `sort` = 0,
      `parent_id` = 900220,
      `path` = '/mes/pro/batch-record-template',
      `icon` = 'ep:document-copy',
      `component` = 'mes/pro/batchrecordtemplate/index',
      `component_name` = 'MesProBatchRecordTemplate',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'0',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900002;

  UPDATE `system_menu`
  SET `name` = 'eDHR审批',
      `permission` = 'mes:pro-batch-record-execution:approve',
      `type` = 2,
      `sort` = 1,
      `parent_id` = 900220,
      `path` = '/mes/pro/feedback/edhr-approval',
      `icon` = 'ep:stamp',
      `component` = 'mes/pro/edhr/ApprovalPage',
      `component_name` = 'MesProFeedbackEdhrApproval',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900024;

  UPDATE `system_menu`
  SET `name` = 'eDHR追踪',
      `permission` = 'mes:pro-batch-record-execution:track',
      `type` = 2,
      `sort` = 2,
      `parent_id` = 900220,
      `path` = '/mes/pro/feedback/edhr-tracking',
      `icon` = 'ep:position',
      `component` = 'mes/pro/edhr/TrackingPage',
      `component_name` = 'MesProFeedbackEdhrTracking',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900025;

  UPDATE `system_menu`
  SET `name` = 'eDHR签名记录',
      `permission` = 'mes:pro-batch-record-execution:signature-query',
      `type` = 2,
      `sort` = 3,
      `parent_id` = 900220,
      `path` = '/mes/pro/feedback/edhr-signatures',
      `icon` = 'ep:edit-pen',
      `component` = 'mes/pro/edhr/SignaturePage',
      `component_name` = 'MesProFeedbackEdhrSignatures',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900026;

  UPDATE `system_menu`
  SET `name` = 'eDHR批次执行',
      `permission` = 'mes:pro-edhr-batch-execution:query',
      `type` = 2,
      `sort` = 4,
      `parent_id` = 900220,
      `path` = '/mes/pro/feedback/edhr-batch-execution',
      `icon` = 'ep:document-checked',
      `component` = 'mes/pro/edhr-batch/BatchExecutionListPage',
      `component_name` = 'MesProEdhrBatchExecutionListPage',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `deleted` = b'0',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = 900033;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` IN (900002, 900024, 900025, 900026, 900033)
      AND `deleted` = b'0'
      AND `permission` = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR batch processing child menus must have permissions before visibility can be controlled';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_processing_target_packages`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_processing_target_packages` AS
  SELECT DISTINCT `package`.`id` AS `package_id`
  FROM `system_tenant_package` AS `package`
  WHERE `package`.`deleted` = b'0'
    AND JSON_VALID(`package`.`menu_ids`)
    AND (
      JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900002' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900024' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900025' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900026' AS JSON), '$')
      OR JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900033' AS JSON), '$')
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_processing_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_processing_menu_ids` AS
  SELECT `id`
  FROM `system_menu`
  WHERE `id` IN (900220, 900002, 900024, 900025, 900026, 900033)
    AND `deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_mes_edhr_batch_processing_menu_ids`) <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch processing menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_processing_package_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_processing_package_menu_ids` (
    `package_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`package_id`, `menu_id`)
  );

  INSERT IGNORE INTO `tmp_mes_edhr_batch_processing_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    CAST(`existing_menu`.`menu_id` AS UNSIGNED)
  FROM `tmp_mes_edhr_batch_processing_target_packages` AS `target_package`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target_package`.`package_id`
   AND `package`.`deleted` = b'0'
   AND JSON_VALID(`package`.`menu_ids`)
  JOIN JSON_TABLE(
    `package`.`menu_ids`,
    '$[*]' COLUMNS (`menu_id` bigint PATH '$')
  ) AS `existing_menu`;

  INSERT IGNORE INTO `tmp_mes_edhr_batch_processing_package_menu_ids` (`package_id`, `menu_id`)
  SELECT
    `target_package`.`package_id`,
    `edhr_menu`.`id`
  FROM `tmp_mes_edhr_batch_processing_target_packages` AS `target_package`
  CROSS JOIN `tmp_mes_edhr_batch_processing_menu_ids` AS `edhr_menu`;

  UPDATE `system_tenant_package` AS `package`
  JOIN (
    SELECT DISTINCT
      `package_id`,
      JSON_ARRAYAGG(`menu_id`) OVER (
        PARTITION BY `package_id`
        ORDER BY `menu_id`
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
      ) AS `menu_ids`
    FROM `tmp_mes_edhr_batch_processing_package_menu_ids`
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
  JOIN `tmp_mes_edhr_batch_processing_target_packages` AS `target_package`
    ON `target_package`.`package_id` = t.`package_id`
  JOIN `system_tenant_package` tp
    ON tp.`id` = t.`package_id`
   AND tp.`deleted` = b'0'
   AND JSON_VALID(tp.`menu_ids`)
  JOIN `system_menu` m ON m.`id` IN (900220, 900002, 900024, 900025, 900026, 900033)
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

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_processing_package_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_processing_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_batch_processing_target_packages`;
END//
DELIMITER ;

CALL ensure_mes_edhr_batch_processing_tabs();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_processing_tabs;
