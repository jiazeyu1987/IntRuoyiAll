-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260906_dcc_new_file_lifecycle_p4; type=data; riskLevel=medium
-- Content approval is complete before READY_TO_PUBLISH.
-- Publishing is an explicit document-control command, not a second four-stage approval.

DELIMITER //

DROP PROCEDURE IF EXISTS `apply_dcc_publish_direct_policy`//
CREATE PROCEDURE `apply_dcc_publish_direct_policy`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_PUBLISH_POLICY_TABLE_MISSING';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM `bpm_business_approval_policy`
    WHERE `system_code` = 'DCC'
      AND `object_type` = 'CONTROLLED_FILE'
      AND `action_code` = 'PUBLISH'
      AND `object_state` = 'READY_TO_PUBLISH'
      AND `effect_executor_code` = 'DCC_PUBLISH'
      AND `status` = 'PUBLISHED'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_PUBLISH_POLICY_TARGET_MISSING';
  END IF;

  UPDATE `bpm_business_approval_policy`
  SET `policy_mode` = 'DIRECT',
      `process_definition_key` = NULL,
      `update_time` = NOW(),
      `updater` = 'system:20260911-dcc-upload-remediation'
  WHERE `system_code` = 'DCC'
    AND `object_type` = 'CONTROLLED_FILE'
    AND `action_code` = 'PUBLISH'
    AND `object_state` = 'READY_TO_PUBLISH'
    AND `effect_executor_code` = 'DCC_PUBLISH'
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1 FROM `bpm_business_approval_policy`
    WHERE `system_code` = 'DCC'
      AND `object_type` = 'CONTROLLED_FILE'
      AND `action_code` = 'PUBLISH'
      AND `object_state` = 'READY_TO_PUBLISH'
      AND `effect_executor_code` = 'DCC_PUBLISH'
      AND `deleted` = b'0'
      AND (`policy_mode` <> 'DIRECT' OR `process_definition_key` IS NOT NULL)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_PUBLISH_DIRECT_POLICY_INCOMPLETE';
  END IF;
END//

DELIMITER ;

CALL `apply_dcc_publish_direct_policy`();
DROP PROCEDURE IF EXISTS `apply_dcc_publish_direct_policy`;
