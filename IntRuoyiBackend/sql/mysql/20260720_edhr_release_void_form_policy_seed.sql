-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_edhr_release_void_form_policy;

DELIMITER //
CREATE PROCEDURE ensure_edhr_release_void_form_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR form policy requires bpm_form_action_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RE_PROCDEF'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR form policy requires ACT_RE_PROCDEF';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `policy`.`action_code` = 'RELEASE'
      AND `policy`.`object_state` = 'PRECHECK_PASSED'
      AND `policy`.`status` = 'PUBLISHED'
      AND (
        COALESCE(`policy`.`bpm_process_key`, '') <> 'mes-edhr-approval-v1'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'EDHR_RELEASE'
        OR COALESCE(`policy`.`policy_type`, '') <> 'NONE'
        OR COALESCE(`policy`.`approval_mode`, '') <> 'BPM_REQUIRED'
        OR COALESCE(`policy`.`slots_json`, '[]') <> '[]'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR release form policy conflict';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `policy`.`action_code` = 'VOID'
      AND `policy`.`object_state` = 'CLOSED'
      AND `policy`.`status` = 'PUBLISHED'
      AND (
        COALESCE(`policy`.`bpm_process_key`, '') <> 'mes-edhr-batch-execution-void-v1'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'EDHR_BATCH_VOID'
        OR COALESCE(`policy`.`policy_type`, '') <> 'NONE'
        OR COALESCE(`policy`.`approval_mode`, '') <> 'BPM_REQUIRED'
        OR COALESCE(`policy`.`slots_json`, '[]') <> '[]'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR batch void form policy conflict';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_policy_source`;
  CREATE TEMPORARY TABLE `tmp_edhr_policy_source` AS
  SELECT
    CAST(`proc`.`TENANT_ID_` AS UNSIGNED) AS `tenant_id`,
    MAX(CASE WHEN `proc`.`KEY_` = 'mes-edhr-approval-v1'
      THEN `proc`.`KEY_` END) AS `release_process_key`,
    MAX(CASE WHEN `proc`.`KEY_` = 'mes-edhr-batch-execution-void-v1'
      THEN `proc`.`KEY_` END) AS `void_process_key`
  FROM `ACT_RE_PROCDEF` AS `proc`
  WHERE `proc`.`KEY_` IN ('mes-edhr-approval-v1', 'mes-edhr-batch-execution-void-v1')
    AND `proc`.`SUSPENSION_STATE_` = 1
    AND COALESCE(`proc`.`TENANT_ID_`, '') <> ''
    AND `proc`.`TENANT_ID_` REGEXP '^[0-9]+$'
  GROUP BY `proc`.`TENANT_ID_`;

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_edhr_policy_source`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR form policy requires release and void process definitions';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_edhr_policy_source`
    WHERE `release_process_key` IS NULL
      OR `void_process_key` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR form policy requires release and void process definitions';
  END IF;

  INSERT INTO `bpm_form_action_policy` (
    `tenant_id`,
    `data_domain`,
    `system_code`,
    `object_type`,
    `action_code`,
    `object_state`,
    `policy_type`,
    `approval_mode`,
    `bpm_process_key`,
    `effect_executor_code`,
    `status`,
    `slots_json`,
    `remark`,
    `creator`,
    `create_time`,
    `updater`,
    `update_time`,
    `deleted`
  )
  SELECT
    `source`.`tenant_id`,
    'MES',
    'MES',
    'EDHR_BATCH_EXECUTION',
    'RELEASE',
    'PRECHECK_PASSED',
    'NONE',
    'BPM_REQUIRED',
    `source`.`release_process_key`,
    'EDHR_RELEASE',
    'PUBLISHED',
    '[]',
    'eDHR release approval through form center',
    '1',
    NOW(),
    '1',
    NOW(),
    b'0'
  FROM `tmp_edhr_policy_source` AS `source`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `source`.`tenant_id`
      AND `existing`.`data_domain` = 'MES'
      AND `existing`.`system_code` = 'MES'
      AND `existing`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `existing`.`action_code` = 'RELEASE'
      AND `existing`.`object_state` = 'PRECHECK_PASSED'
      AND `existing`.`status` = 'PUBLISHED'
  );

  INSERT INTO `bpm_form_action_policy` (
    `tenant_id`,
    `data_domain`,
    `system_code`,
    `object_type`,
    `action_code`,
    `object_state`,
    `policy_type`,
    `approval_mode`,
    `bpm_process_key`,
    `effect_executor_code`,
    `status`,
    `slots_json`,
    `remark`,
    `creator`,
    `create_time`,
    `updater`,
    `update_time`,
    `deleted`
  )
  SELECT
    `source`.`tenant_id`,
    'MES',
    'MES',
    'EDHR_BATCH_EXECUTION',
    'VOID',
    'CLOSED',
    'NONE',
    'BPM_REQUIRED',
    `source`.`void_process_key`,
    'EDHR_BATCH_VOID',
    'PUBLISHED',
    '[]',
    'eDHR batch void approval through form center',
    '1',
    NOW(),
    '1',
    NOW(),
    b'0'
  FROM `tmp_edhr_policy_source` AS `source`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `source`.`tenant_id`
      AND `existing`.`data_domain` = 'MES'
      AND `existing`.`system_code` = 'MES'
      AND `existing`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `existing`.`action_code` = 'VOID'
      AND `existing`.`object_state` = 'CLOSED'
      AND `existing`.`status` = 'PUBLISHED'
  );

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'MES'
      AND `system_code` = 'MES'
      AND `object_type` = 'EDHR_BATCH_EXECUTION'
      AND (
        (`action_code` = 'RELEASE' AND `object_state` = 'PRECHECK_PASSED')
        OR (`action_code` = 'VOID' AND `object_state` = 'CLOSED')
      )
      AND `status` = 'PUBLISHED'
    GROUP BY `tenant_id`, `action_code`, `object_state`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR form policy duplicate';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_policy_source`;
END//
DELIMITER ;

CALL ensure_edhr_release_void_form_policy();

DROP PROCEDURE IF EXISTS ensure_edhr_release_void_form_policy;
