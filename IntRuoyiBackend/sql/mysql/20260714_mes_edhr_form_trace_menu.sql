-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_mes_edhr_release_trace_menu,20260618_mes_edhr_release_precheck_engine,20260618_mes_edhr_release_transaction_lifecycle; type=menu; riskLevel=medium
-- Merge old eDHR "审计与追溯" and "放行追溯/放行与归档" visible entries into one "表单追溯" entry.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_trace_menu;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_form_trace_menu()
BEGIN
  DECLARE v_merged_package_ids LONGTEXT DEFAULT '[]';

  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR form trace menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR batch record parent menu 900220; cannot create form trace menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900025
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing old eDHR audit trace menu 900025; cannot reuse it as form trace';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900260
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing old eDHR release trace menu 900260; cannot hide it under form trace';
  END IF;

  UPDATE `system_menu`
  SET `name` = '表单追溯',
      `permission` = 'mes:pro-batch-record-execution:track',
      `type` = 2,
      `sort` = 3,
      `parent_id` = 900220,
      `path` = '/mes/pro/feedback/edhr-form-trace',
      `icon` = 'ep:position',
      `component` = 'mes/pro/edhr/FormTracePage',
      `component_name` = 'MesProFeedbackEdhrFormTrace',
      `status` = 0,
      `visible` = b'1',
      `keep_alive` = b'1',
      `always_show` = b'1',
      `updater` = 'edhr-form-trace-menu',
      `update_time` = NOW(),
      `deleted` = b'0'
  WHERE `id` = 900025
    AND `deleted` = b'0';

  UPDATE `system_menu`
  SET `type` = 3,
      `sort` = 10,
      `parent_id` = 900025,
      `path` = '',
      `icon` = '',
      `component` = '',
      `component_name` = '',
      `visible` = b'0',
      `updater` = 'edhr-form-trace-menu',
      `update_time` = NOW()
  WHERE `id` = 900260
    AND `deleted` = b'0';

  UPDATE `system_menu`
  SET `type` = 3,
      `parent_id` = 900025,
      `path` = '',
      `icon` = '',
      `component` = '',
      `component_name` = '',
      `visible` = CASE WHEN `id` = 900260 THEN b'0' ELSE b'1' END,
      `updater` = 'edhr-form-trace-menu',
      `update_time` = NOW()
  WHERE `permission` IN (
      'mes:pro-edhr-release:query',
      'mes:pro-edhr-release:precheck',
      'mes:pro-edhr-release:submit',
      'mes:pro-edhr-release:approve',
      'mes:pro-edhr-release:reject',
      'mes:pro-edhr-release:withdraw',
      'mes:pro-edhr-release:event-query'
    )
    AND `deleted` = b'0';

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT DISTINCT
    `source_role_menu`.`role_id`,
    900025,
    'edhr-form-trace-menu',
    NOW(),
    'edhr-form-trace-menu',
    NOW(),
    b'0',
    `source_role_menu`.`tenant_id`
  FROM `system_role_menu` AS `source_role_menu`
  WHERE `source_role_menu`.`menu_id` = 900260
    AND `source_role_menu`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing_role_menu`
      WHERE `existing_role_menu`.`role_id` = `source_role_menu`.`role_id`
        AND `existing_role_menu`.`tenant_id` = `source_role_menu`.`tenant_id`
        AND `existing_role_menu`.`menu_id` = 900025
        AND `existing_role_menu`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_trace_package_scope`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_form_trace_package_scope` AS
  SELECT DISTINCT `pkg`.`id` AS `package_id`
  FROM `system_tenant_package` AS `pkg`
  JOIN JSON_TABLE(
    CAST(`pkg`.`menu_ids` AS JSON),
    '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `release_menu`
    ON `release_menu`.`menu_id` = 900260
  WHERE `pkg`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM JSON_TABLE(
        CAST(`pkg`.`menu_ids` AS JSON),
        '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
      ) AS `form_trace_menu`
      WHERE `form_trace_menu`.`menu_id` = 900025
    );

  SELECT CAST(COALESCE(JSON_ARRAYAGG(`package_id`), JSON_ARRAY()) AS CHAR)
  INTO v_merged_package_ids
  FROM `tmp_mes_edhr_form_trace_package_scope`;

  UPDATE `system_tenant_package` AS `pkg`
  JOIN `tmp_mes_edhr_form_trace_package_scope` AS `scope`
    ON `scope`.`package_id` = `pkg`.`id`
  SET `pkg`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`pkg`.`menu_ids` AS JSON), '$', 900025) AS CHAR),
      `pkg`.`updater` = 'edhr-form-trace-menu',
      `pkg`.`update_time` = NOW()
  WHERE `pkg`.`deleted` = b'0';

  IF (SELECT COUNT(*)
      FROM `system_menu`
      WHERE `id` = 900025
        AND `name` = '表单追溯'
        AND `path` = '/mes/pro/feedback/edhr-form-trace'
        AND `component` = 'mes/pro/edhr/FormTracePage'
        AND `component_name` = 'MesProFeedbackEdhrFormTrace'
        AND `visible` = b'1'
        AND `deleted` = b'0') <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing merged eDHR form trace visible menu';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900260
      AND `type` = 2
      AND `visible` = b'1'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Old eDHR 放行追溯 visible menu is still exposed';
  END IF;

  IF (SELECT COUNT(DISTINCT `permission`)
      FROM `system_menu`
      WHERE `parent_id` = 900025
        AND `permission` IN (
          'mes:pro-edhr-release:query',
          'mes:pro-edhr-release:precheck',
          'mes:pro-edhr-release:submit',
          'mes:pro-edhr-release:approve',
          'mes:pro-edhr-release:reject',
          'mes:pro-edhr-release:withdraw',
          'mes:pro-edhr-release:event-query'
        )
        AND `deleted` = b'0') <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Release trace permissions were not retained under eDHR form trace menu';
  END IF;
END//
DELIMITER ;

CALL ensure_mes_edhr_form_trace_menu();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_trace_menu;
