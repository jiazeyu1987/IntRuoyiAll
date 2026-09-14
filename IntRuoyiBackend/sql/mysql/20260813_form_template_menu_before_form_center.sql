-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260722_form_center_business_action_page_retire,20260722_form_center_policy_menu_hide; type=menu; riskLevel=low
-- Purpose: show Form Template directly under Basic Data, immediately before Form Center.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS move_form_template_before_form_center;
DELIMITER //
CREATE PROCEDURE move_form_template_before_form_center()
BEGIN
  SET @form_template_basic_data_menu_id := (
    SELECT `id`
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`id` = 990200 OR `path` = '/mdm')
    ORDER BY CASE WHEN `id` = 990200 THEN 0 ELSE 1 END
    LIMIT 1
  );

  IF @form_template_basic_data_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing global basic data menu 990200 for form template move';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071200
      AND `name` = '表单中心'
      AND `type` = 1
      AND `parent_id` = @form_template_basic_data_menu_id
      AND `path` = 'form-center'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center parent menu 605071200 under global basic data';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `parent_id` = @form_template_basic_data_menu_id
      AND `path` = 'form-center/template'
      AND `id` <> 605071201
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting basic data form-center/template menu path';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071201
      AND `name` = '表单模板'
      AND `permission` = 'form:template:query'
      AND `type` = 2
      AND `component` = 'form-center/template/index'
      AND `component_name` = 'FormCenterTemplate'
      AND `deleted` = b'0'
      AND (
        (`parent_id` = 605071200 AND `path` = 'template')
        OR
        (`parent_id` = @form_template_basic_data_menu_id AND `path` = 'form-center/template')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form template menu 605071201 under form center';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (
      605071202, 605071203, 605071204, 605071205,
      605071206, 605071207, 605071208
    )
      AND `type` = 3
      AND `parent_id` = 605071201
      AND `deleted` = b'0'
  ) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form template permission rows';
  END IF;

  UPDATE `system_menu`
  SET `parent_id` = @form_template_basic_data_menu_id,
      `sort` = 29,
      `path` = 'form-center/template',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 605071201
    AND `deleted` = b'0';

  UPDATE `system_menu`
  SET `sort` = 30,
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 605071200
    AND `deleted` = b'0';

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071201
      AND `parent_id` = @form_template_basic_data_menu_id
      AND `path` = 'form-center/template'
      AND `sort` = 29
      AND `permission` = 'form:template:query'
      AND `component` = 'form-center/template/index'
      AND `component_name` = 'FormCenterTemplate'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template menu move did not reach the required final state';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071200
      AND `parent_id` = @form_template_basic_data_menu_id
      AND `path` = 'form-center'
      AND `sort` = 30
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center menu order changed unexpectedly';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (
      605071202, 605071203, 605071204, 605071205,
      605071206, 605071207, 605071208
    )
      AND `type` = 3
      AND `parent_id` = 605071201
      AND `deleted` = b'0'
  ) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template permission rows changed unexpectedly';
  END IF;
END//
DELIMITER ;

CALL move_form_template_before_form_center();

DROP PROCEDURE IF EXISTS move_form_template_before_form_center;
