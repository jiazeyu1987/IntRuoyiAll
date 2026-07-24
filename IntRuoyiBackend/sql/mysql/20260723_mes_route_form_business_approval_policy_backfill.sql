-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy,20260722_mes_route_form_center_runtime_columns; type=data; riskLevel=medium
-- Purpose: backfill unified business approval policies for published MES route dynamic eDHR form policies.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_route_form_business_approval_policy;
DELIMITER //
CREATE PROCEDURE ensure_mes_route_form_business_approval_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form policy backfill requires bpm_form_action_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form policy backfill requires bpm_business_approval_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
      AND column_name = 'form_policy_type'
  ) THEN
    ALTER TABLE `bpm_business_approval_policy`
      ADD COLUMN `form_policy_type` varchar(32) DEFAULT NULL COMMENT 'form policy type when the action needs form slots'
      AFTER `effect_executor_code`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
      AND column_name = 'form_slots_json'
  ) THEN
    ALTER TABLE `bpm_business_approval_policy`
      ADD COLUMN `form_slots_json` longtext DEFAULT NULL COMMENT 'form policy slots json'
      AFTER `form_policy_type`;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    WHERE `form_policy`.`deleted` = b'0'
      AND `form_policy`.`data_domain` = 'MES'
      AND `form_policy`.`system_code` = 'MES'
      AND `form_policy`.`object_type` = 'EDHR_ROUTE_FORM'
      AND `form_policy`.`status` = 'PUBLISHED'
      AND COALESCE(`form_policy`.`approval_mode`, '') NOT IN ('DIRECT', 'BPM_REQUIRED')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form policy backfill found unsupported approval_mode';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    WHERE `form_policy`.`deleted` = b'0'
      AND `form_policy`.`data_domain` = 'MES'
      AND `form_policy`.`system_code` = 'MES'
      AND `form_policy`.`object_type` = 'EDHR_ROUTE_FORM'
      AND `form_policy`.`status` = 'PUBLISHED'
      AND COALESCE(`form_policy`.`effect_executor_code`, '') = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form policy backfill requires effect_executor_code';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    WHERE `form_policy`.`deleted` = b'0'
      AND `form_policy`.`data_domain` = 'MES'
      AND `form_policy`.`system_code` = 'MES'
      AND `form_policy`.`object_type` = 'EDHR_ROUTE_FORM'
      AND `form_policy`.`status` = 'PUBLISHED'
      AND `form_policy`.`effect_executor_code` <> 'MES_EDHR_ROUTE_FORM_FILL'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form policy backfill found unexpected effect_executor_code';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    JOIN `bpm_business_approval_policy` AS `business_policy`
      ON `business_policy`.`tenant_id` = `form_policy`.`tenant_id`
     AND `business_policy`.`data_domain` = `form_policy`.`data_domain`
     AND `business_policy`.`system_code` = `form_policy`.`system_code`
     AND `business_policy`.`object_type` = `form_policy`.`object_type`
     AND `business_policy`.`action_code` = `form_policy`.`action_code`
     AND `business_policy`.`object_state` = `form_policy`.`object_state`
     AND `business_policy`.`status` = 'PUBLISHED'
     AND `business_policy`.`deleted` = b'0'
    WHERE `form_policy`.`deleted` = b'0'
      AND `form_policy`.`data_domain` = 'MES'
      AND `form_policy`.`system_code` = 'MES'
      AND `form_policy`.`object_type` = 'EDHR_ROUTE_FORM'
      AND `form_policy`.`status` = 'PUBLISHED'
      AND (
        `business_policy`.`policy_mode` <> `form_policy`.`approval_mode`
        OR COALESCE(`business_policy`.`process_definition_key`, '') <> COALESCE(`form_policy`.`bpm_process_key`, '')
        OR `business_policy`.`effect_executor_code` <> `form_policy`.`effect_executor_code`
        OR COALESCE(NULLIF(TRIM(`business_policy`.`form_policy_type`), ''), COALESCE(`form_policy`.`policy_type`, ''))
            <> COALESCE(`form_policy`.`policy_type`, '')
        OR COALESCE(NULLIF(TRIM(`business_policy`.`form_slots_json`), ''),
             COALESCE(NULLIF(TRIM(`form_policy`.`slots_json`), ''), '[]'))
            <> COALESCE(NULLIF(TRIM(`form_policy`.`slots_json`), ''), '[]')
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form policy backfill found conflicting published business policy';
  END IF;

  UPDATE `bpm_business_approval_policy` AS `business_policy`
  JOIN `bpm_form_action_policy` AS `form_policy`
    ON `business_policy`.`tenant_id` = `form_policy`.`tenant_id`
   AND `business_policy`.`data_domain` = `form_policy`.`data_domain`
   AND `business_policy`.`system_code` = `form_policy`.`system_code`
   AND `business_policy`.`object_type` = `form_policy`.`object_type`
   AND `business_policy`.`action_code` = `form_policy`.`action_code`
   AND `business_policy`.`object_state` = `form_policy`.`object_state`
   AND `business_policy`.`policy_mode` = `form_policy`.`approval_mode`
   AND COALESCE(`business_policy`.`process_definition_key`, '') = COALESCE(`form_policy`.`bpm_process_key`, '')
   AND `business_policy`.`effect_executor_code` = `form_policy`.`effect_executor_code`
   AND `business_policy`.`status` = `form_policy`.`status`
   AND `business_policy`.`deleted` = b'0'
  SET `business_policy`.`form_policy_type` = NULLIF(`form_policy`.`policy_type`, ''),
      `business_policy`.`form_slots_json` = NULLIF(`form_policy`.`slots_json`, ''),
      `business_policy`.`updater` = 'codex',
      `business_policy`.`update_time` = NOW()
  WHERE `form_policy`.`deleted` = b'0'
    AND `form_policy`.`data_domain` = 'MES'
    AND `form_policy`.`system_code` = 'MES'
    AND `form_policy`.`object_type` = 'EDHR_ROUTE_FORM'
    AND `form_policy`.`status` = 'PUBLISHED';

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`,
    `data_domain`,
    `system_code`,
    `object_type`,
    `action_code`,
    `object_state`,
    `policy_mode`,
    `process_definition_key`,
    `effect_executor_code`,
    `form_policy_type`,
    `form_slots_json`,
    `status`,
    `remark`,
    `creator`,
    `create_time`,
    `updater`,
    `update_time`,
    `deleted`
  )
  SELECT
    `form_policy`.`tenant_id`,
    `form_policy`.`data_domain`,
    `form_policy`.`system_code`,
    `form_policy`.`object_type`,
    `form_policy`.`action_code`,
    `form_policy`.`object_state`,
    `form_policy`.`approval_mode`,
    NULLIF(`form_policy`.`bpm_process_key`, ''),
    `form_policy`.`effect_executor_code`,
    NULLIF(`form_policy`.`policy_type`, ''),
    NULLIF(`form_policy`.`slots_json`, ''),
    `form_policy`.`status`,
    COALESCE(NULLIF(`form_policy`.`remark`, ''), 'MES route dynamic eDHR form business approval backfill'),
    COALESCE(NULLIF(`form_policy`.`creator`, ''), 'codex'),
    `form_policy`.`create_time`,
    'codex',
    NOW(),
    b'0'
  FROM `bpm_form_action_policy` AS `form_policy`
  WHERE `form_policy`.`deleted` = b'0'
    AND `form_policy`.`data_domain` = 'MES'
    AND `form_policy`.`system_code` = 'MES'
    AND `form_policy`.`object_type` = 'EDHR_ROUTE_FORM'
    AND `form_policy`.`status` = 'PUBLISHED'
    AND NOT EXISTS (
      SELECT 1
      FROM `bpm_business_approval_policy` AS `business_policy`
      WHERE `business_policy`.`tenant_id` = `form_policy`.`tenant_id`
        AND `business_policy`.`data_domain` = `form_policy`.`data_domain`
        AND `business_policy`.`system_code` = `form_policy`.`system_code`
        AND `business_policy`.`object_type` = `form_policy`.`object_type`
        AND `business_policy`.`action_code` = `form_policy`.`action_code`
        AND `business_policy`.`object_state` = `form_policy`.`object_state`
        AND `business_policy`.`policy_mode` = `form_policy`.`approval_mode`
        AND COALESCE(`business_policy`.`process_definition_key`, '') = COALESCE(`form_policy`.`bpm_process_key`, '')
        AND `business_policy`.`effect_executor_code` = `form_policy`.`effect_executor_code`
        AND `business_policy`.`status` = `form_policy`.`status`
        AND `business_policy`.`deleted` = b'0'
    );

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    LEFT JOIN `bpm_business_approval_policy` AS `business_policy`
      ON `business_policy`.`tenant_id` = `form_policy`.`tenant_id`
     AND `business_policy`.`data_domain` = `form_policy`.`data_domain`
     AND `business_policy`.`system_code` = `form_policy`.`system_code`
     AND `business_policy`.`object_type` = `form_policy`.`object_type`
     AND `business_policy`.`action_code` = `form_policy`.`action_code`
     AND `business_policy`.`object_state` = `form_policy`.`object_state`
     AND `business_policy`.`policy_mode` = `form_policy`.`approval_mode`
     AND COALESCE(`business_policy`.`process_definition_key`, '') = COALESCE(`form_policy`.`bpm_process_key`, '')
     AND `business_policy`.`effect_executor_code` = `form_policy`.`effect_executor_code`
     AND `business_policy`.`status` = 'PUBLISHED'
     AND `business_policy`.`deleted` = b'0'
    WHERE `form_policy`.`deleted` = b'0'
      AND `form_policy`.`data_domain` = 'MES'
      AND `form_policy`.`system_code` = 'MES'
      AND `form_policy`.`object_type` = 'EDHR_ROUTE_FORM'
      AND `form_policy`.`status` = 'PUBLISHED'
      AND `business_policy`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form policy backfill did not publish every route form business policy';
  END IF;
END//
DELIMITER ;

CALL ensure_mes_route_form_business_approval_policy();

DROP PROCEDURE IF EXISTS ensure_mes_route_form_business_approval_policy;
