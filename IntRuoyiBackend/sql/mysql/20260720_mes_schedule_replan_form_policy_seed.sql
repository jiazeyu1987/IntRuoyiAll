-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center,20260720_mes_schedule_replan_approval_bpm_seed; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_schedule_replan_form_policy;

DELIMITER //
CREATE PROCEDURE ensure_mes_schedule_replan_form_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES schedule replan form policy requires bpm_form_action_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RE_PROCDEF'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES schedule replan form policy requires ACT_RE_PROCDEF';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'MES'
      AND `policy`.`system_code` = 'MES'
      AND `policy`.`object_type` = 'SCHEDULE_REPLAN_SCOPE'
      AND `policy`.`action_code` = 'REPLAN'
      AND `policy`.`object_state` = 'READY'
      AND `policy`.`status` = 'PUBLISHED'
      AND (
        COALESCE(`policy`.`bpm_process_key`, '') <> 'mes-schedule-replan-approval-v1'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'MES_SCHEDULE_REPLAN'
        OR COALESCE(`policy`.`policy_type`, '') <> 'NONE'
        OR COALESCE(`policy`.`slots_json`, '[]') <> '[]'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES schedule replan form policy conflict';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_schedule_replan_policy_source`;
  CREATE TEMPORARY TABLE `tmp_schedule_replan_policy_source` AS
  SELECT
    CAST(`proc`.`TENANT_ID_` AS UNSIGNED) AS `tenant_id`,
    MAX(`proc`.`KEY_`) AS `process_key`
  FROM `ACT_RE_PROCDEF` AS `proc`
  WHERE `proc`.`KEY_` = 'mes-schedule-replan-approval-v1'
    AND `proc`.`SUSPENSION_STATE_` = 1
    AND COALESCE(`proc`.`TENANT_ID_`, '') <> ''
    AND `proc`.`TENANT_ID_` REGEXP '^[0-9]+$'
  GROUP BY `proc`.`TENANT_ID_`;

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_schedule_replan_policy_source`
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES schedule replan form policy requires process definition';
  END IF;

  INSERT INTO `bpm_form_action_policy` (
    `tenant_id`,
    `data_domain`,
    `system_code`,
    `object_type`,
    `action_code`,
    `object_state`,
    `policy_type`,
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
    'SCHEDULE_REPLAN_SCOPE',
    'REPLAN',
    'READY',
    'NONE',
    `source`.`process_key`,
    'MES_SCHEDULE_REPLAN',
    'PUBLISHED',
    '[]',
    'MES manual schedule replan approval through form center',
    '1',
    NOW(),
    '1',
    NOW(),
    b'0'
  FROM `tmp_schedule_replan_policy_source` AS `source`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `source`.`tenant_id`
      AND `existing`.`data_domain` = 'MES'
      AND `existing`.`system_code` = 'MES'
      AND `existing`.`object_type` = 'SCHEDULE_REPLAN_SCOPE'
      AND `existing`.`action_code` = 'REPLAN'
      AND `existing`.`object_state` = 'READY'
      AND `existing`.`status` = 'PUBLISHED'
  );

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'MES'
      AND `system_code` = 'MES'
      AND `object_type` = 'SCHEDULE_REPLAN_SCOPE'
      AND `action_code` = 'REPLAN'
      AND `object_state` = 'READY'
      AND `status` = 'PUBLISHED'
    GROUP BY `tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES schedule replan form policy duplicate';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_schedule_replan_policy_source`;
END//
DELIMITER ;

CALL ensure_mes_schedule_replan_form_policy();

DROP PROCEDURE IF EXISTS ensure_mes_schedule_replan_form_policy;
