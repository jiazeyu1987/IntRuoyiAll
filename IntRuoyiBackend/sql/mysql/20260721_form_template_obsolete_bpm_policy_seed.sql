-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy,20260722_form_template_obsolete_bpm_process_seed; type=data; riskLevel=medium
-- Purpose: bind Form Center template obsolete requests to the platform Business Approval Bridge.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_form_template_obsolete_bpm_policy;

DELIMITER //
CREATE PROCEDURE ensure_form_template_obsolete_bpm_policy()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template obsolete policy seed requires bpm_business_approval_policy';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ACT_RE_PROCDEF'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template obsolete policy seed requires ACT_RE_PROCDEF';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_template_obsolete_policy_source`;
  CREATE TEMPORARY TABLE `tmp_form_template_obsolete_policy_source` AS
  SELECT CAST(`proc`.`TENANT_ID_` AS UNSIGNED) AS `tenant_id`,
         `proc`.`KEY_` AS `process_key`
  FROM `ACT_RE_PROCDEF` AS `proc`
  WHERE `proc`.`KEY_` = 'form-template-obsolete-v1'
    AND `proc`.`SUSPENSION_STATE_` = 1
    AND `proc`.`TENANT_ID_` REGEXP '^[0-9]+$'
  GROUP BY `proc`.`TENANT_ID_`, `proc`.`KEY_`;

  IF (SELECT COUNT(*) FROM `tmp_form_template_obsolete_policy_source`) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template obsolete policy requires active process definition form-template-obsolete-v1';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_template_obsolete_states`;
  CREATE TEMPORARY TABLE `tmp_form_template_obsolete_states` (`object_state` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY);
  INSERT INTO `tmp_form_template_obsolete_states` (`object_state`)
  VALUES ('DRAFT'), ('READY'), ('REJECTED'), ('PUBLISHED'), ('DISABLED');

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    JOIN `tmp_form_template_obsolete_states` AS `state_scope`
      ON `state_scope`.`object_state` = `policy`.`object_state`
    WHERE `policy`.`tenant_id` IN (SELECT `tenant_id` FROM `tmp_form_template_obsolete_policy_source`)
      AND `policy`.`data_domain` = 'FORM_CENTER'
      AND `policy`.`system_code` = 'FORM_CENTER'
      AND `policy`.`object_type` = 'FORM_TEMPLATE'
      AND `policy`.`action_code` = 'OBSOLETE'
      AND `policy`.`status` = 'PUBLISHED'
      AND `policy`.`deleted` = b'0'
      AND `policy`.`policy_mode` = 'BPM_REQUIRED'
      AND (
        COALESCE(`policy`.`process_definition_key`, '') <> 'form-template-obsolete-v1'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'FORM_TEMPLATE_OBSOLETE'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template obsolete BPM policy conflict';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    JOIN `tmp_form_template_obsolete_states` AS `state_scope`
      ON `state_scope`.`object_state` = `policy`.`object_state`
    WHERE `policy`.`tenant_id` IN (SELECT `tenant_id` FROM `tmp_form_template_obsolete_policy_source`)
      AND `policy`.`data_domain` = 'FORM_CENTER'
      AND `policy`.`system_code` = 'FORM_CENTER'
      AND `policy`.`object_type` = 'FORM_TEMPLATE'
      AND `policy`.`action_code` = 'OBSOLETE'
      AND `policy`.`status` = 'PUBLISHED'
      AND `policy`.`deleted` = b'0'
    GROUP BY `policy`.`tenant_id`, `policy`.`object_state`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template obsolete BPM policy duplicate';
  END IF;

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
    `policy_mode`, `process_definition_key`, `effect_executor_code`, `status`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT `source`.`tenant_id`, 'FORM_CENTER', 'FORM_CENTER', 'FORM_TEMPLATE', 'OBSOLETE', `state_scope`.`object_state`,
         'BPM_REQUIRED', 'form-template-obsolete-v1', 'FORM_TEMPLATE_OBSOLETE', 'PUBLISHED',
         'Form template obsolete approval through platform business approval policy',
         'codex', NOW(), 'codex', NOW(), b'0'
  FROM `tmp_form_template_obsolete_policy_source` AS `source`
  CROSS JOIN `tmp_form_template_obsolete_states` AS `state_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `existing`
    WHERE `existing`.`tenant_id` = `source`.`tenant_id`
      AND `existing`.`data_domain` = 'FORM_CENTER'
      AND `existing`.`system_code` = 'FORM_CENTER'
      AND `existing`.`object_type` = 'FORM_TEMPLATE'
      AND `existing`.`action_code` = 'OBSOLETE'
      AND `existing`.`object_state` = `state_scope`.`object_state`
      AND `existing`.`status` = 'PUBLISHED'
      AND `existing`.`deleted` = b'0'
  );

  DROP TEMPORARY TABLE IF EXISTS `tmp_form_template_obsolete_states`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_form_template_obsolete_policy_source`;
END//
DELIMITER ;

START TRANSACTION;
CALL ensure_form_template_obsolete_bpm_policy();
COMMIT;

DROP PROCEDURE IF EXISTS ensure_form_template_obsolete_bpm_policy;
