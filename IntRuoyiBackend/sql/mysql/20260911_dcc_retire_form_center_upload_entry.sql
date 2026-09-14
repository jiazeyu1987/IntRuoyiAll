-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy,20260719_dcc_upload_form_policy_seed; type=data; riskLevel=medium
-- DCC controlled-file upload is owned by /dcc/controlled-files/submit.
-- Retire the legacy form-center policy so it cannot bypass upload governance.

DELIMITER //

DROP PROCEDURE IF EXISTS `apply_dcc_retire_form_center_upload_entry`//
CREATE PROCEDURE `apply_dcc_retire_form_center_upload_entry`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_RETIRE_UPLOAD_POLICY_TABLE_MISSING';
  END IF;

  UPDATE `bpm_business_approval_policy`
  SET `deleted` = b'1',
      `update_time` = NOW(),
      `updater` = 'system:20260911-dcc-upload-remediation'
  WHERE `system_code` = 'DCC'
    AND `object_type` = 'CONTROLLED_FILE'
    AND `action_code` = 'UPLOAD'
    AND `effect_executor_code` = 'DCC_UPLOAD'
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1 FROM `bpm_business_approval_policy`
    WHERE `system_code` = 'DCC'
      AND `object_type` = 'CONTROLLED_FILE'
      AND `action_code` = 'UPLOAD'
      AND `effect_executor_code` = 'DCC_UPLOAD'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_RETIRE_UPLOAD_POLICY_INCOMPLETE';
  END IF;
END//

DELIMITER ;

CALL `apply_dcc_retire_form_center_upload_entry`();
DROP PROCEDURE IF EXISTS `apply_dcc_retire_form_center_upload_entry`;
