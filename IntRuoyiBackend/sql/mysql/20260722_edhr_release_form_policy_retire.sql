-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260720_edhr_release_void_form_policy_seed; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_edhr_release_form_policy_retired;

DELIMITER //
CREATE PROCEDURE ensure_edhr_release_form_policy_retired()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR release policy retirement requires bpm_business_approval_policy';
  END IF;

  UPDATE `bpm_business_approval_policy`
  SET
    `status` = 'DISABLED',
    `remark` = 'eDHR release uses owner electronic signature submit; no form-center release approval',
    `updater` = '1',
    `update_time` = NOW()
  WHERE `data_domain` = 'MES'
    AND `system_code` = 'MES'
    AND `object_type` = 'EDHR_BATCH_EXECUTION'
    AND `action_code` = 'RELEASE'
    AND `object_state` = 'PRECHECK_PASSED'
    AND `effect_executor_code` = 'EDHR_RELEASE'
    AND `status` = 'PUBLISHED'
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `data_domain` = 'MES'
      AND `system_code` = 'MES'
      AND `object_type` = 'EDHR_BATCH_EXECUTION'
      AND `action_code` = 'RELEASE'
      AND `object_state` = 'PRECHECK_PASSED'
      AND `effect_executor_code` = 'EDHR_RELEASE'
      AND `status` = 'PUBLISHED'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR release must not keep a published business approval policy';
  END IF;
END//
DELIMITER ;

CALL ensure_edhr_release_form_policy_retired();

DROP PROCEDURE IF EXISTS ensure_edhr_release_form_policy_retired;
