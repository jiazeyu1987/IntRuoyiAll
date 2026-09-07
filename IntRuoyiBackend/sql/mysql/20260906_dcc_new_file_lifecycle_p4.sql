-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260906_dcc_new_file_lifecycle_p3,20260720_dcc_publish_form_policy_seed; type=data; riskLevel=medium
-- Publishing is a separate document-control activation step after revision approval, not a second copy of the four-stage approval.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_new_file_lifecycle_p4;

DELIMITER //
CREATE PROCEDURE ensure_dcc_new_file_lifecycle_p4()
BEGIN
  DECLARE source_tenant_count INT DEFAULT 0;
  DECLARE direct_tenant_count INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC direct publish policy requires bpm_business_approval_policy';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `bpm_business_approval_policy`
     WHERE `deleted` = b'0'
       AND `data_domain` = 'DCC'
       AND `system_code` = 'DCC'
       AND `object_type` = 'CONTROLLED_FILE'
       AND `action_code` = 'PUBLISH'
       AND `object_state` = 'READY_TO_PUBLISH'
       AND `status` = 'PUBLISHED'
     GROUP BY `tenant_id`
    HAVING COUNT(*) <> 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC direct publish policy source is missing or duplicated';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_publish_direct_source`;
  CREATE TEMPORARY TABLE `tmp_dcc_publish_direct_source` AS
  SELECT *
    FROM `bpm_business_approval_policy`
   WHERE `deleted` = b'0'
     AND `data_domain` = 'DCC'
     AND `system_code` = 'DCC'
     AND `object_type` = 'CONTROLLED_FILE'
     AND `action_code` = 'PUBLISH'
     AND `object_state` = 'READY_TO_PUBLISH'
     AND `status` = 'PUBLISHED';

  SELECT COUNT(DISTINCT `tenant_id`)
    INTO source_tenant_count
    FROM `tmp_dcc_publish_direct_source`;

  IF source_tenant_count = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC direct publish policy source is missing or duplicated';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_publish_direct_source`
     WHERE COALESCE(`effect_executor_code`, '') <> 'DCC_PUBLISH'
        OR COALESCE(`form_policy_type`, '') <> 'NONE'
        OR COALESCE(`form_slots_json`, '[]') <> '[]'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC direct publish policy source contract is invalid';
  END IF;

  UPDATE `bpm_business_approval_policy` AS `policy`
  JOIN `tmp_dcc_publish_direct_source` AS `source` ON `source`.`id` = `policy`.`id`
     SET `policy`.`status` = 'DISABLED',
         `policy`.`updater` = '1',
         `policy`.`update_time` = NOW()
   WHERE `source`.`policy_mode` <> 'DIRECT';

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
    `policy_mode`, `process_definition_key`, `effect_executor_code`, `form_policy_type`,
    `form_slots_json`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    `source`.`tenant_id`, `source`.`data_domain`, `source`.`system_code`, `source`.`object_type`,
    `source`.`action_code`, `source`.`object_state`, 'DIRECT', `source`.`process_definition_key`,
    `source`.`effect_executor_code`, `source`.`form_policy_type`, `source`.`form_slots_json`,
    'PUBLISHED', 'DCC approved revision is activated by a separate document-control publish action',
    '1', NOW(), '1', NOW(), b'0'
  FROM `tmp_dcc_publish_direct_source` AS `source`
  WHERE `source`.`policy_mode` <> 'DIRECT';

  SELECT COUNT(DISTINCT `policy`.`tenant_id`)
    INTO direct_tenant_count
    FROM `bpm_business_approval_policy` AS `policy`
    JOIN `tmp_dcc_publish_direct_source` AS `source`
      ON `source`.`tenant_id` = `policy`.`tenant_id`
   WHERE `policy`.`deleted` = b'0'
     AND `policy`.`data_domain` = 'DCC'
     AND `policy`.`system_code` = 'DCC'
     AND `policy`.`object_type` = 'CONTROLLED_FILE'
     AND `policy`.`action_code` = 'PUBLISH'
     AND `policy`.`object_state` = 'READY_TO_PUBLISH'
     AND `policy`.`policy_mode` = 'DIRECT'
     AND `policy`.`effect_executor_code` = 'DCC_PUBLISH'
     AND `policy`.`status` = 'PUBLISHED';

  IF direct_tenant_count <> source_tenant_count THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC direct publish policy conversion failed';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_publish_direct_source`;
  COMMIT;
END//
DELIMITER ;

CALL ensure_dcc_new_file_lifecycle_p4();

DROP PROCEDURE IF EXISTS ensure_dcc_new_file_lifecycle_p4;
