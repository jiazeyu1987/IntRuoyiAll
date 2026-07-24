-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260723_unify_form_action_policy_into_business_approval_policy,20260714_mes_edhr_batch_execution_void_bpm_seed; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_edhr_batch_void_business_policy_all_state;

DELIMITER //
CREATE PROCEDURE ensure_edhr_batch_void_business_policy_all_state()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR batch void ALL-state business policy requires bpm_business_approval_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RE_PROCDEF'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR batch void ALL-state business policy requires ACT_RE_PROCDEF';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `ACT_RE_PROCDEF` AS `proc`
    WHERE `proc`.`KEY_` = 'mes-edhr-batch-execution-void-v1'
      AND `proc`.`SUSPENSION_STATE_` = 1
      AND COALESCE(`proc`.`TENANT_ID_`, '') <> ''
      AND `proc`.`TENANT_ID_` REGEXP '^[0-9]+$'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR batch void ALL-state business policy requires active void process definition';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `policy`.`action_code` = 'VOID'
      AND `policy`.`object_state` IN ('CLOSED', 'ALL')
      AND `policy`.`status` = 'PUBLISHED'
      AND (
        COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'
        OR COALESCE(`policy`.`process_definition_key`, '') <> 'mes-edhr-batch-execution-void-v1'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'EDHR_BATCH_VOID'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR batch void ALL-state business policy conflict';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `policy`.`action_code` = 'VOID'
      AND `policy`.`status` = 'PUBLISHED'
      AND `policy`.`object_state` NOT IN ('CLOSED', 'ALL')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR batch void ALL-state business policy has unsupported state-specific policy';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'EDHR_BATCH_EXECUTION'
      AND `policy`.`action_code` = 'VOID'
      AND `policy`.`object_state` IN ('CLOSED', 'ALL')
      AND `policy`.`status` = 'PUBLISHED'
    GROUP BY `policy`.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR batch void ALL-state business policy duplicate before update';
  END IF;

  UPDATE `bpm_business_approval_policy`
  SET `object_state` = 'ALL',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `deleted` = b'0'
    AND `data_domain` = 'MES'
    AND `system_code` = 'MES'
    AND `object_type` = 'EDHR_BATCH_EXECUTION'
    AND `action_code` = 'VOID'
    AND `object_state` = 'CLOSED';

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
    `policy_mode`, `process_definition_key`, `effect_executor_code`, `status`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT CAST(`proc`.`TENANT_ID_` AS UNSIGNED), 'MES', 'MES', 'EDHR_BATCH_EXECUTION', 'VOID', 'ALL',
         'BPM_REQUIRED', 'mes-edhr-batch-execution-void-v1', 'EDHR_BATCH_VOID', 'PUBLISHED',
         'eDHR batch void approval applies to all batch execution states',
         '1', NOW(), '1', NOW(), b'0'
  FROM `ACT_RE_PROCDEF` AS `proc`
  WHERE `proc`.`KEY_` = 'mes-edhr-batch-execution-void-v1'
    AND `proc`.`SUSPENSION_STATE_` = 1
    AND COALESCE(`proc`.`TENANT_ID_`, '') <> ''
    AND `proc`.`TENANT_ID_` REGEXP '^[0-9]+$'
    AND NOT EXISTS (
      SELECT 1
      FROM `bpm_business_approval_policy` AS `existing`
      WHERE `existing`.`deleted` = b'0'
        AND `existing`.`tenant_id` = CAST(`proc`.`TENANT_ID_` AS UNSIGNED)
        AND `existing`.`data_domain` = 'MES'
        AND `existing`.`system_code` = 'MES'
        AND `existing`.`object_type` = 'EDHR_BATCH_EXECUTION'
        AND `existing`.`action_code` = 'VOID'
        AND `existing`.`object_state` = 'ALL'
        AND `existing`.`status` = 'PUBLISHED'
    )
  GROUP BY `proc`.`TENANT_ID_`;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'MES'
      AND `system_code` = 'MES'
      AND `object_type` = 'EDHR_BATCH_EXECUTION'
      AND `action_code` = 'VOID'
      AND `status` = 'PUBLISHED'
    GROUP BY `tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'EDHR batch void ALL-state business policy duplicate';
  END IF;
END//
DELIMITER ;

CALL ensure_edhr_batch_void_business_policy_all_state();

DROP PROCEDURE IF EXISTS ensure_edhr_batch_void_business_policy_all_state;
