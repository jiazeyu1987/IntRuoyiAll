-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_mes_edhr_form_trace_menu,20260612_mes_edhr_record_change_menu; type=menu; riskLevel=medium
-- Merge old eDHR "变更与异常" visible entry into the "表单追溯" page as the "变更" tab.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_trace_change_menu;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_form_trace_change_menu()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `system_tenant_package`
    WHERE `deleted` = b'0'
      AND NOT JSON_VALID(`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR change menu into form trace';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900025
      AND `name` = '表单追溯'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR form trace menu 900025; cannot merge change menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900235
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing old eDHR 变更与异常 menu 900235; cannot hide it under form trace';
  END IF;

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT DISTINCT
    `source_role_menu`.`role_id`,
    900025,
    'edhr-form-trace-change-menu',
    NOW(),
    'edhr-form-trace-change-menu',
    NOW(),
    b'0',
    `source_role_menu`.`tenant_id`
  FROM `system_role_menu` AS `source_role_menu`
  WHERE `source_role_menu`.`menu_id` = 900235
    AND `source_role_menu`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing_role_menu`
      WHERE `existing_role_menu`.`role_id` = `source_role_menu`.`role_id`
        AND `existing_role_menu`.`tenant_id` = `source_role_menu`.`tenant_id`
        AND `existing_role_menu`.`menu_id` = 900025
        AND `existing_role_menu`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_form_trace_change_package_scope`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_form_trace_change_package_scope` AS
  SELECT DISTINCT `pkg`.`id` AS `package_id`
  FROM `system_tenant_package` AS `pkg`
  JOIN JSON_TABLE(
    CAST(`pkg`.`menu_ids` AS JSON),
    '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
  ) AS `change_menu`
    ON `change_menu`.`menu_id` = 900235
  WHERE `pkg`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM JSON_TABLE(
        CAST(`pkg`.`menu_ids` AS JSON),
        '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')
      ) AS `form_trace_menu`
      WHERE `form_trace_menu`.`menu_id` = 900025
    );

  UPDATE `system_tenant_package` AS `pkg`
  JOIN `tmp_mes_edhr_form_trace_change_package_scope` AS `scope`
    ON `scope`.`package_id` = `pkg`.`id`
  SET `pkg`.`menu_ids` = CAST(JSON_ARRAY_APPEND(CAST(`pkg`.`menu_ids` AS JSON), '$', 900025) AS CHAR),
      `pkg`.`updater` = 'edhr-form-trace-change-menu',
      `pkg`.`update_time` = NOW()
  WHERE `pkg`.`deleted` = b'0';

  UPDATE `system_menu`
  SET `type` = 3,
      `sort` = CASE
        WHEN `id` = 900235 THEN 20
        ELSE `sort`
      END,
      `parent_id` = 900025,
      `path` = '',
      `icon` = '',
      `component` = '',
      `component_name` = '',
      `visible` = CASE WHEN `id` = 900235 THEN b'0' ELSE b'1' END,
      `updater` = 'edhr-form-trace-change-menu',
      `update_time` = NOW()
  WHERE (`id` = 900235
      OR `permission` IN (
        'mes:pro-edhr-change:query',
        'mes:pro-edhr-change:void',
        'mes:pro-edhr-change:reopen',
        'mes:pro-edhr-change:supplement',
        'mes:pro-edhr-change:approve'
      ))
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 900235
      AND `type` = 2
      AND `visible` = b'1'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Old eDHR 变更与异常 visible menu is still exposed';
  END IF;

  IF (SELECT COUNT(*)
      FROM `system_menu`
      WHERE `id` = 900235
        AND `type` = 3
        AND `parent_id` = 900025
        AND `visible` = b'0'
        AND `deleted` = b'0') <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Old eDHR 变更与异常 menu was not hidden under form trace';
  END IF;

  IF (SELECT COUNT(DISTINCT `permission`)
      FROM `system_menu`
      WHERE `parent_id` = 900025
        AND `permission` IN (
          'mes:pro-edhr-change:query',
          'mes:pro-edhr-change:void',
          'mes:pro-edhr-change:reopen',
          'mes:pro-edhr-change:supplement',
          'mes:pro-edhr-change:approve'
        )
        AND `deleted` = b'0') <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR change permissions were not retained under eDHR form trace menu';
  END IF;
END//
DELIMITER ;

CALL ensure_mes_edhr_form_trace_change_menu();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_trace_change_menu;
