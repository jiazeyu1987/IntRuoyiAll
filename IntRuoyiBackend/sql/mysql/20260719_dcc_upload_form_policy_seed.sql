-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_upload_form_policy;

DELIMITER //
CREATE PROCEDURE ensure_dcc_upload_form_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC upload form policy requires bpm_form_action_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_process_definition_info'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC upload form policy requires bpm_process_definition_info';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'act_re_procdef'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC upload form policy requires act_re_procdef';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT 1 AS tenant_id
      UNION ALL
      SELECT 122 AS tenant_id
    ) AS tenant_scope
    WHERE NOT EXISTS (
      SELECT 1
      FROM `act_re_procdef` AS `procdef`
      WHERE `procdef`.`KEY_` = 'dcc-controlled-file-approval'
        AND `procdef`.`TENANT_ID_` = CAST(`tenant_scope`.`tenant_id` AS CHAR)
    )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC upload form policy requires dcc-controlled-file-approval process';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT 1 AS tenant_id
      UNION ALL
      SELECT 122 AS tenant_id
    ) AS tenant_scope
    WHERE NOT EXISTS (
      SELECT 1
      FROM `bpm_process_definition_info` AS `info`
      JOIN `act_re_procdef` AS `procdef`
        ON `procdef`.`ID_` = `info`.`process_definition_id`
      WHERE `procdef`.`KEY_` = 'dcc-controlled-file-approval'
        AND `procdef`.`TENANT_ID_` = CAST(`tenant_scope`.`tenant_id` AS CHAR)
        AND `info`.`tenant_id` = `tenant_scope`.`tenant_id`
        AND `info`.`deleted` = b'0'
    )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC upload form policy requires dcc-controlled-file-approval process info';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'DCC'
      AND `policy`.`system_code` = 'DCC'
      AND `policy`.`object_type` = 'CONTROLLED_FILE'
      AND `policy`.`action_code` = 'UPLOAD'
      AND `policy`.`object_state` = 'DRAFT'
      AND `policy`.`status` = 'PUBLISHED'
      AND (
        COALESCE(`policy`.`bpm_process_key`, '') <> 'dcc-controlled-file-approval'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'DCC_UPLOAD'
        OR COALESCE(`policy`.`policy_type`, '') <> 'NONE'
        OR COALESCE(`policy`.`slots_json`, '[]') <> '[]'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC upload form policy conflict';
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
    `tenant_scope`.`tenant_id`,
    'DCC',
    'DCC',
    'CONTROLLED_FILE',
    'UPLOAD',
    'DRAFT',
    'NONE',
    'dcc-controlled-file-approval',
    'DCC_UPLOAD',
    'PUBLISHED',
    '[]',
    'DCC upload approval through form center',
    '1',
    NOW(),
    '1',
    NOW(),
    b'0'
  FROM (
    SELECT 1 AS tenant_id
    UNION ALL
    SELECT 122 AS tenant_id
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `tenant_scope`.`tenant_id`
      AND `existing`.`data_domain` = 'DCC'
      AND `existing`.`system_code` = 'DCC'
      AND `existing`.`object_type` = 'CONTROLLED_FILE'
      AND `existing`.`action_code` = 'UPLOAD'
      AND `existing`.`object_state` = 'DRAFT'
      AND `existing`.`status` = 'PUBLISHED'
  );

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'DCC'
      AND `system_code` = 'DCC'
      AND `object_type` = 'CONTROLLED_FILE'
      AND `action_code` = 'UPLOAD'
      AND `object_state` = 'DRAFT'
      AND `status` = 'PUBLISHED'
    GROUP BY `tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC upload form policy duplicate';
  END IF;
END//
DELIMITER ;

CALL ensure_dcc_upload_form_policy();

DROP PROCEDURE IF EXISTS ensure_dcc_upload_form_policy;
