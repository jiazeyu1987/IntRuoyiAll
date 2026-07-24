-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260721_form_center_menu_under_basic_data; type=menu; riskLevel=low
-- Purpose: hide the Form Center policy menu tab while keeping runtime routes and permissions intact.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS hide_form_center_policy_menu_tab;
DELIMITER //
CREATE PROCEDURE hide_form_center_policy_menu_tab()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071200
      AND `path` = 'form-center'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center parent menu 605071200';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071214
      AND `name` = '表单策略'
      AND `permission` = 'form:policy:query'
      AND `type` = 2
      AND `parent_id` = 605071200
      AND `path` = 'policy'
      AND `component` = 'form-center/policy/index'
      AND `component_name` = 'FormCenterPolicy'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center policy menu 605071214';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM `system_menu`
    WHERE `id` IN (605071215, 605071216, 605071217)
      AND `type` = 3
      AND `parent_id` = 605071214
      AND `deleted` = b'0'
  ) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center policy permission rows';
  END IF;

  UPDATE `system_menu`
  SET `visible` = b'0',
      `always_show` = b'0',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 605071214
    AND `deleted` = b'0';
END//
DELIMITER ;

CALL hide_form_center_policy_menu_tab();

DROP PROCEDURE IF EXISTS hide_form_center_policy_menu_tab;
