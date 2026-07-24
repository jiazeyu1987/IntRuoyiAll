-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260720_mes_schedule_replan_form_policy_seed; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_schedule_replan_approval_retired;

DELIMITER //
CREATE PROCEDURE ensure_mes_schedule_replan_approval_retired()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES schedule replan approval retirement requires bpm_form_action_policy';
  END IF;

  UPDATE `bpm_form_action_policy`
  SET
    `status` = 'RETIRED',
    `remark` = 'Manual replan is not approval-backed',
    `updater` = '1',
    `update_time` = NOW()
  WHERE `data_domain` = 'MES'
    AND `system_code` = 'MES'
    AND `object_type` = 'SCHEDULE_REPLAN_SCOPE'
    AND `action_code` = 'REPLAN'
    AND `object_state` = 'READY'
    AND `effect_executor_code` = 'MES_SCHEDULE_REPLAN'
    AND `status` = 'PUBLISHED'
    AND `deleted` = b'0';

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy`
    WHERE `system_code` = 'MES'
      AND `data_domain` = 'MES'
      AND `object_type` = 'SCHEDULE_REPLAN_SCOPE'
      AND `action_code` = 'REPLAN'
      AND `object_state` = 'READY'
      AND `effect_executor_code` = 'MES_SCHEDULE_REPLAN'
      AND `status` = 'PUBLISHED'
      AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Manual replan is not approval-backed';
  END IF;
END//
DELIMITER ;

CALL ensure_mes_schedule_replan_approval_retired();

DROP PROCEDURE IF EXISTS ensure_mes_schedule_replan_approval_retired;
