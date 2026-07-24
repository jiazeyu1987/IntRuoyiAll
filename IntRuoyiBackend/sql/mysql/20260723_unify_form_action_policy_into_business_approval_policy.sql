-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260722_form_center_policy_menu_hide; type=data; riskLevel=medium
-- Purpose: migrate Form Center action approval strategies into the unified business approval policy table.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS unify_form_action_policy_into_business_approval_policy;
DELIMITER //
CREATE PROCEDURE unify_form_action_policy_into_business_approval_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Unified policy migration requires bpm_form_action_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Unified policy migration requires bpm_business_approval_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_instance'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Unified policy migration requires bpm_form_action_instance';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    WHERE `form_policy`.`deleted` = b'0'
      AND (
        COALESCE(`form_policy`.`policy_type`, '') <> 'NONE'
        OR COALESCE(NULLIF(TRIM(`form_policy`.`slots_json`), ''), '[]') <> '[]'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot migrate form action policy with form slots into business approval policy';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    WHERE `form_policy`.`deleted` = b'0'
      AND COALESCE(`form_policy`.`approval_mode`, '') NOT IN ('BPM_REQUIRED', 'DIRECT')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Unsupported form action approval mode for unified business approval policy';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    WHERE `form_policy`.`deleted` = b'0'
      AND `form_policy`.`approval_mode` = 'BPM_REQUIRED'
      AND COALESCE(`form_policy`.`bpm_process_key`, '') = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'BPM_REQUIRED form action policy requires bpm_process_key before unified migration';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `form_policy`
    WHERE `form_policy`.`deleted` = b'0'
      AND COALESCE(`form_policy`.`effect_executor_code`, '') = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form action policy requires effect_executor_code before unified migration';
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
      AND `form_policy`.`status` = 'PUBLISHED'
      AND (
        `business_policy`.`policy_mode` <> `form_policy`.`approval_mode`
        OR COALESCE(`business_policy`.`process_definition_key`, '') <> COALESCE(`form_policy`.`bpm_process_key`, '')
        OR `business_policy`.`effect_executor_code` <> `form_policy`.`effect_executor_code`
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting published business approval policy exists for form action policy';
  END IF;

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
    `form_policy`.`status`,
    `form_policy`.`remark`,
    COALESCE(NULLIF(`form_policy`.`creator`, ''), 'codex'),
    `form_policy`.`create_time`,
    'codex',
    NOW(),
    b'0'
  FROM `bpm_form_action_policy` AS `form_policy`
  WHERE `form_policy`.`deleted` = b'0'
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
    FROM `bpm_form_action_instance` AS `instance`
    JOIN `bpm_form_action_policy` AS `form_policy`
      ON `form_policy`.`id` = `instance`.`policy_id`
     AND `form_policy`.`tenant_id` = `instance`.`tenant_id`
     AND `form_policy`.`deleted` = b'0'
    LEFT JOIN (
      SELECT
        `tenant_id`,
        `data_domain`,
        `system_code`,
        `object_type`,
        `action_code`,
        `object_state`,
        `policy_mode`,
        COALESCE(`process_definition_key`, '') AS `process_definition_key`,
        `effect_executor_code`,
        `status`,
        COUNT(*) AS `policy_count`
      FROM `bpm_business_approval_policy`
      WHERE `deleted` = b'0'
      GROUP BY `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
               `policy_mode`, COALESCE(`process_definition_key`, ''), `effect_executor_code`, `status`
    ) AS `resolved_policy`
      ON `resolved_policy`.`tenant_id` = `form_policy`.`tenant_id`
     AND `resolved_policy`.`data_domain` = `form_policy`.`data_domain`
     AND `resolved_policy`.`system_code` = `form_policy`.`system_code`
     AND `resolved_policy`.`object_type` = `form_policy`.`object_type`
     AND `resolved_policy`.`action_code` = `form_policy`.`action_code`
     AND `resolved_policy`.`object_state` = `form_policy`.`object_state`
     AND `resolved_policy`.`policy_mode` = `form_policy`.`approval_mode`
     AND `resolved_policy`.`process_definition_key` = COALESCE(`form_policy`.`bpm_process_key`, '')
     AND `resolved_policy`.`effect_executor_code` = `form_policy`.`effect_executor_code`
     AND `resolved_policy`.`status` = `form_policy`.`status`
    WHERE `instance`.`deleted` = b'0'
      AND (`resolved_policy`.`policy_count` IS NULL OR `resolved_policy`.`policy_count` <> 1)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form action instance policy migration requires exactly one business approval policy';
  END IF;

  UPDATE `bpm_form_action_instance` AS `instance`
  JOIN `bpm_form_action_policy` AS `form_policy`
    ON `form_policy`.`id` = `instance`.`policy_id`
   AND `form_policy`.`tenant_id` = `instance`.`tenant_id`
   AND `form_policy`.`deleted` = b'0'
  JOIN `bpm_business_approval_policy` AS `business_policy`
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
  SET `instance`.`policy_id` = `business_policy`.`id`,
      `instance`.`updater` = 'codex',
      `instance`.`update_time` = NOW()
  WHERE `instance`.`deleted` = b'0'
    AND `instance`.`policy_id` = `form_policy`.`id`;
END//
DELIMITER ;

CALL unify_form_action_policy_into_business_approval_policy();

DROP PROCEDURE IF EXISTS unify_form_action_policy_into_business_approval_policy;