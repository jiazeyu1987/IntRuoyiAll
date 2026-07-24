-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_form_action_policy_approval_mode;

DELIMITER //
CREATE PROCEDURE ensure_form_action_policy_approval_mode()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form action approval mode requires bpm_form_action_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
      AND column_name = 'approval_mode'
  ) THEN
    ALTER TABLE `bpm_form_action_policy`
      ADD COLUMN `approval_mode` varchar(32) NOT NULL DEFAULT 'BPM_REQUIRED' COMMENT 'approval mode: BPM_REQUIRED or DIRECT' AFTER `policy_type`;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy`
    WHERE `deleted` = b'0'
      AND `approval_mode` NOT IN ('BPM_REQUIRED', 'DIRECT')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'FORM_APPROVAL_MODE_INVALID';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy`
    WHERE `deleted` = b'0'
      AND `approval_mode` = 'BPM_REQUIRED'
      AND COALESCE(`bpm_process_key`, '') = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'BPM_REQUIRED form action policy requires bpm_process_key';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy`
    WHERE `deleted` = b'0'
      AND `approval_mode` = 'DIRECT'
      AND COALESCE(`effect_executor_code`, '') = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DIRECT form action policy requires effect_executor_code';
  END IF;
END//
DELIMITER ;

CALL ensure_form_action_policy_approval_mode();

DROP PROCEDURE IF EXISTS ensure_form_action_policy_approval_mode;
