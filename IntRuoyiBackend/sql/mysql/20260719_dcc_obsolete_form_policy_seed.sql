-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_bpm_form_center; type=data; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_obsolete_form_policy;

DELIMITER //
CREATE PROCEDURE ensure_dcc_obsolete_form_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bpm_form_action_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC obsolete form policy requires bpm_form_action_policy';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `policy`
    WHERE `policy`.`deleted` = b'0'
      AND `policy`.`data_domain` = 'DCC'
      AND `policy`.`system_code` = 'DCC'
      AND `policy`.`object_type` = 'CONTROLLED_FILE'
      AND `policy`.`action_code` = 'OBSOLETE'
      AND `policy`.`object_state` = 'ACTIVE'
      AND `policy`.`status` = 'PUBLISHED'
      AND (
        COALESCE(`policy`.`bpm_process_key`, '') = ''
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'DCC_OBSOLETE'
        OR COALESCE(`policy`.`policy_type`, '') <> 'NONE'
        OR COALESCE(`policy`.`slots_json`, '[]') <> '[]'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC obsolete form policy conflict';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_obsolete_policy_source`;
  CREATE TEMPORARY TABLE `tmp_dcc_obsolete_policy_source` AS
  SELECT
    `upload`.`tenant_id`,
    `upload`.`bpm_process_key`
  FROM `bpm_form_action_policy` AS `upload`
  JOIN (
    SELECT
      `tenant_id`,
      MAX(`id`) AS `id`
    FROM `bpm_form_action_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'DCC'
      AND `system_code` = 'DCC'
      AND `object_type` = 'CONTROLLED_FILE'
      AND `action_code` = 'UPLOAD'
      AND `object_state` = 'DRAFT'
      AND `status` = 'PUBLISHED'
      AND COALESCE(`bpm_process_key`, '') <> ''
    GROUP BY `tenant_id`
  ) AS `latest_upload`
    ON `latest_upload`.`id` = `upload`.`id`;

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
    'DCC',
    'DCC',
    'CONTROLLED_FILE',
    'OBSOLETE',
    'ACTIVE',
    'NONE',
    `source`.`bpm_process_key`,
    'DCC_OBSOLETE',
    'PUBLISHED',
    '[]',
    'DCC obsolete approval through form center',
    '1',
    NOW(),
    '1',
    NOW(),
    b'0'
  FROM `tmp_dcc_obsolete_policy_source` AS `source`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy` AS `existing`
    WHERE `existing`.`deleted` = b'0'
      AND `existing`.`tenant_id` = `source`.`tenant_id`
      AND `existing`.`data_domain` = 'DCC'
      AND `existing`.`system_code` = 'DCC'
      AND `existing`.`object_type` = 'CONTROLLED_FILE'
      AND `existing`.`action_code` = 'OBSOLETE'
      AND `existing`.`object_state` = 'ACTIVE'
      AND `existing`.`status` = 'PUBLISHED'
  );

  IF EXISTS (
    SELECT 1
    FROM `bpm_form_action_policy`
    WHERE `deleted` = b'0'
      AND `data_domain` = 'DCC'
      AND `system_code` = 'DCC'
      AND `object_type` = 'CONTROLLED_FILE'
      AND `action_code` = 'OBSOLETE'
      AND `object_state` = 'ACTIVE'
      AND `status` = 'PUBLISHED'
    GROUP BY `tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC obsolete form policy duplicate';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_obsolete_policy_source`;
END//
DELIMITER ;

CALL ensure_dcc_obsolete_form_policy();

DROP PROCEDURE IF EXISTS ensure_dcc_obsolete_form_policy;
