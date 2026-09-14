-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260813_form_template_menu_before_form_center; type=menu; riskLevel=low
-- Purpose: hide the Form Center sidebar parent and its visible effect child while preserving runtime permissions and the Form Template sibling.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS hide_form_center_sidebar_entry;
DELIMITER //
CREATE PROCEDURE hide_form_center_sidebar_entry()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071200
      AND `parent_id` = 990200
      AND `path` = 'form-center'
      AND `type` = 1
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center parent menu 605071200';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071220
      AND `name` = '生效待处理'
      AND `permission` = 'form:effect:query'
      AND `type` = 2
      AND `parent_id` = 605071200
      AND `path` = 'effect'
      AND `component` = 'form-center/effect/index'
      AND `component_name` = 'FormCenterEffect'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center effect menu 605071220';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071221
      AND `permission` = 'form:effect:retry'
      AND `type` = 3
      AND `parent_id` = 605071220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing form center effect retry permission 605071221';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071201
      AND `parent_id` = 990200
      AND `sort` = 29
      AND `path` = 'form-center/template'
      AND `permission` = 'form:template:query'
      AND `component` = 'form-center/template/index'
      AND `component_name` = 'FormCenterTemplate'
      AND `visible` = b'1'
      AND `status` = 0
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing active form template sibling menu 605071201';
  END IF;

  UPDATE `system_menu`
  SET `visible` = b'0',
      `always_show` = b'0',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 605071220
    AND `deleted` = b'0';

  UPDATE `system_menu`
  SET `visible` = b'0',
      `always_show` = b'0',
      `updater` = 'codex',
      `update_time` = NOW()
  WHERE `id` = 605071200
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071200
      AND (`visible` <> b'0' OR `always_show` <> b'0')
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center parent menu is still visible';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071220
      AND (`visible` <> b'0' OR `always_show` <> b'0')
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center effect menu is still visible';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071201
      AND `parent_id` = 990200
      AND `sort` = 29
      AND `path` = 'form-center/template'
      AND `permission` = 'form:template:query'
      AND `component` = 'form-center/template/index'
      AND `component_name` = 'FormCenterTemplate'
      AND `visible` = b'1'
      AND `status` = 0
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template sibling menu was changed';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 605071221
      AND `permission` = 'form:effect:retry'
      AND `type` = 3
      AND `parent_id` = 605071220
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form center effect retry permission was changed';
  END IF;
END//
DELIMITER ;

CALL hide_form_center_sidebar_entry();

DROP PROCEDURE IF EXISTS hide_form_center_sidebar_entry;
