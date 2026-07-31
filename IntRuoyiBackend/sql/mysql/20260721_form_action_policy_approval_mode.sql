-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_business_approval_policy_form_slots;

DELIMITER //
CREATE PROCEDURE ensure_business_approval_policy_form_slots()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Business approval policy form slots require bpm_business_approval_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
      AND column_name = 'form_policy_type'
  ) THEN
    ALTER TABLE `bpm_business_approval_policy`
      ADD COLUMN `form_policy_type` varchar(32) DEFAULT NULL COMMENT 'form policy type when the action needs form slots' AFTER `effect_executor_code`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
      AND column_name = 'form_slots_json'
  ) THEN
    ALTER TABLE `bpm_business_approval_policy`
      ADD COLUMN `form_slots_json` longtext DEFAULT NULL COMMENT 'form policy slots json' AFTER `form_policy_type`;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `deleted` = b'0'
      AND `policy_mode` NOT IN ('BPM_REQUIRED', 'SIGNATURE_REQUIRED', 'DIRECT')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'BUSINESS_APPROVAL_POLICY_MODE_INVALID';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `deleted` = b'0'
      AND `policy_mode` = 'BPM_REQUIRED'
      AND COALESCE(`process_definition_key`, '') = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'BPM_REQUIRED business approval policy requires process_definition_key';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `deleted` = b'0'
      AND `policy_mode` = 'DIRECT'
      AND COALESCE(`effect_executor_code`, '') = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DIRECT business approval policy requires effect_executor_code';
  END IF;
END//
DELIMITER ;

CALL ensure_business_approval_policy_form_slots();

DROP PROCEDURE IF EXISTS ensure_business_approval_policy_form_slots;
