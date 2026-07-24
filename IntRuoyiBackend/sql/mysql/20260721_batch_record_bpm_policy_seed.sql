-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_batch_record_bpm_policy;

DELIMITER //
CREATE PROCEDURE ensure_batch_record_bpm_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record BPM policy seed requires bpm_business_approval_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RE_PROCDEF'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record BPM policy seed requires ACT_RE_PROCDEF';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'BATCH_RECORD_VERSION'
      AND `policy`.`action_code` = 'PUBLISH'
      AND `policy`.`object_state` = 'PRECHECK_PASSED'
      AND `policy`.`status` = 'PUBLISHED'
      AND (
        COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'
        OR COALESCE(`policy`.`process_definition_key`, '') <> 'mes-batch-record-version-approval-v1'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'MES_BATCH_RECORD_VERSION_PUBLISH'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record version BPM policy conflict';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `policy`.`action_code` = 'SUBMIT_REVIEW'
      AND `policy`.`object_state` = 'DRAFT'
      AND `policy`.`status` = 'PUBLISHED'
      AND (
        COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'
        OR COALESCE(`policy`.`process_definition_key`, '') <> 'mes-edhr-approval-v1'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'EDHR_BATCH_EXECUTION_SUBMIT_REVIEW'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record execution BPM policy conflict';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_batch_record_policy_source`;
  CREATE TEMPORARY TABLE `tmp_batch_record_policy_source` AS
  SELECT
    CAST(`proc`.`TENANT_ID_` AS UNSIGNED) AS `tenant_id`,
    MAX(CASE WHEN `proc`.`KEY_` = 'mes-batch-record-version-approval-v1'
      THEN `proc`.`KEY_` END) AS `version_process_key`,
    MAX(CASE WHEN `proc`.`KEY_` = 'mes-edhr-approval-v1'
      THEN `proc`.`KEY_` END) AS `execution_process_key`
  FROM `ACT_RE_PROCDEF` AS `proc`
  WHERE `proc`.`KEY_` IN ('mes-batch-record-version-approval-v1', 'mes-edhr-approval-v1')
    AND `proc`.`SUSPENSION_STATE_` = 1
    AND COALESCE(`proc`.`TENANT_ID_`, '') <> ''
    AND `proc`.`TENANT_ID_` REGEXP '^[0-9]+$'
  GROUP BY `proc`.`TENANT_ID_`;

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_batch_record_policy_source`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record BPM policy requires version and execution process definitions';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_batch_record_policy_source`
    WHERE `version_process_key` IS NULL
      OR `execution_process_key` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record BPM policy requires version and execution process definitions';
  END IF;

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
    `policy_mode`, `process_definition_key`, `effect_executor_code`, `status`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT `source`.`tenant_id`, 'MES', 'MES', 'BATCH_RECORD_VERSION', 'PUBLISH', 'PRECHECK_PASSED',
         'BPM_REQUIRED', `source`.`version_process_key`, 'MES_BATCH_RECORD_VERSION_PUBLISH', 'PUBLISHED',
         'Batch record version publish approval through platform business approval policy',
         '1', NOW(), '1', NOW(), b'0'
  FROM `tmp_batch_record_policy_source` AS `source`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `source`.`tenant_id`
      AND `existing`.`data_domain` = 'MES'
      AND `existing`.`system_code` = 'MES'
      AND `existing`.`object_type` = 'BATCH_RECORD_VERSION'
      AND `existing`.`action_code` = 'PUBLISH'
      AND `existing`.`object_state` = 'PRECHECK_PASSED'
      AND `existing`.`status` = 'PUBLISHED'
  );

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
    `policy_mode`, `process_definition_key`, `effect_executor_code`, `status`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT `source`.`tenant_id`, 'MES', 'MES', 'EDHR_BATCH_EXECUTION', 'SUBMIT_REVIEW', 'DRAFT',
         'BPM_REQUIRED', `source`.`execution_process_key`, 'EDHR_BATCH_EXECUTION_SUBMIT_REVIEW', 'PUBLISHED',
         'Batch record execution submit review through platform business approval policy',
         '1', NOW(), '1', NOW(), b'0'
  FROM `tmp_batch_record_policy_source` AS `source`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `source`.`tenant_id`
      AND `existing`.`data_domain` = 'MES'
      AND `existing`.`system_code` = 'MES'
      AND `existing`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `existing`.`action_code` = 'SUBMIT_REVIEW'
      AND `existing`.`object_state` = 'DRAFT'
      AND `existing`.`status` = 'PUBLISHED'
  );

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'MES'
      AND `system_code` = 'MES'
      AND (
        (`object_type` = 'BATCH_RECORD_VERSION' AND `action_code` = 'PUBLISH' AND `object_state` = 'PRECHECK_PASSED')
        OR (`object_type` = 'EDHR_BATCH_EXECUTION' AND `action_code` = 'SUBMIT_REVIEW' AND `object_state` = 'DRAFT')
      )
      AND `status` = 'PUBLISHED'
    GROUP BY `tenant_id`, `object_type`, `action_code`, `object_state`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record BPM policy duplicate';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_batch_record_policy_source`;
END//
DELIMITER ;

CALL ensure_batch_record_bpm_policy();

DROP PROCEDURE IF EXISTS ensure_batch_record_bpm_policy;
