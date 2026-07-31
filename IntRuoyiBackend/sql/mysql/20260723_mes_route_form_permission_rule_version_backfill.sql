-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260723_mes_edhr_process_form_permission_rule_version_index_repair,20260715_mes_route_version_lifecycle,20260720_mes_batch_shared_form_binding,20260706_mes_edhr_process_form_permission_rule; type=data; riskLevel=medium
-- Purpose: backfill route-versioned eDHR process form filler rules for existing published MES route dynamic form bindings.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_route_form_permission_rule_version_backfill;
DELIMITER //
CREATE PROCEDURE ensure_mes_route_form_permission_rule_version_backfill()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_version'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form permission backfill requires mes_pro_route_version';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_flow_process_batch_record'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form permission backfill requires mes_pro_route_flow_process_batch_record';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_process_form_permission_rule'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form permission backfill requires mes_pro_edhr_process_form_permission_rule';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_flow_process_batch_record` AS `binding`
    JOIN `mes_pro_route_version` AS `route_version`
      ON `route_version`.`tenant_id` = `binding`.`tenant_id`
     AND `route_version`.`route_id` = `binding`.`route_id`
     AND `route_version`.`deleted` = b'0'
     AND `route_version`.`active` = b'1'
     AND `route_version`.`lifecycle_status` = 'ACTIVE'
    WHERE `binding`.`deleted` = b'0'
      AND COALESCE(NULLIF(`binding`.`form_binding_key`, ''), '') <> ''
      AND COALESCE(NULLIF(`binding`.`candidate_source_type`, ''), '') <> ''
      AND COALESCE(NULLIF(`binding`.`candidate_source_ids`, ''), '') = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form permission backfill found dynamic form binding without candidate ids';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_flow_process_batch_record` AS `binding`
    LEFT JOIN `mes_pro_route_version` AS `route_version`
      ON `route_version`.`tenant_id` = `binding`.`tenant_id`
     AND `route_version`.`route_id` = `binding`.`route_id`
     AND `route_version`.`deleted` = b'0'
     AND `route_version`.`active` = b'1'
     AND `route_version`.`lifecycle_status` = 'ACTIVE'
    WHERE `binding`.`deleted` = b'0'
      AND COALESCE(NULLIF(`binding`.`form_binding_key`, ''), '') <> ''
      AND COALESCE(NULLIF(`binding`.`candidate_source_type`, ''), '') <> ''
      AND COALESCE(NULLIF(`binding`.`candidate_source_ids`, ''), '') <> ''
      AND `route_version`.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form permission backfill found dynamic form binding without active route version';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `mes_pro_route_flow_process_batch_record` AS `binding`
    JOIN `mes_pro_route_version` AS `route_version`
      ON `route_version`.`tenant_id` = `binding`.`tenant_id`
     AND `route_version`.`route_id` = `binding`.`route_id`
     AND `route_version`.`deleted` = b'0'
     AND `route_version`.`active` = b'1'
     AND `route_version`.`lifecycle_status` = 'ACTIVE'
    JOIN `mes_pro_edhr_process_form_permission_rule` AS `rule`
      ON `rule`.`tenant_id` = `binding`.`tenant_id`
     AND `rule`.`route_process_id` = `binding`.`route_process_id`
     AND `rule`.`batch_record_report_id` = `binding`.`form_binding_key`
     AND `rule`.`batch_record_version_id` = `route_version`.`id`
     AND `rule`.`rule_type` = 'FILL'
     AND `rule`.`signature_cell_key` = ''
     AND `rule`.`deleted` = b'0'
    WHERE `binding`.`deleted` = b'0'
      AND COALESCE(NULLIF(`binding`.`form_binding_key`, ''), '') <> ''
      AND COALESCE(NULLIF(`binding`.`candidate_source_type`, ''), '') <> ''
      AND COALESCE(NULLIF(`binding`.`candidate_source_ids`, ''), '') <> ''
      AND `rule`.`enabled` <> b'1'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES route form permission backfill found disabled versioned filler rule';
  END IF;

  INSERT INTO `mes_pro_edhr_process_form_permission_rule` (
    `tenant_id`,
    `route_process_id`,
    `batch_record_report_id`,
    `batch_record_definition_id`,
    `batch_record_version_id`,
    `rule_type`,
    `signature_cell_key`,
    `signature_role`,
    `candidate_source_type`,
    `candidate_source_ids`,
    `completion_policy`,
    `due_minutes`,
    `enabled`,
    `remark`,
    `creator`,
    `create_time`,
    `updater`,
    `update_time`,
    `deleted`
  )
  SELECT
    `binding`.`tenant_id`,
    `binding`.`route_process_id`,
    `binding`.`form_binding_key`,
    NULL,
    `route_version`.`id`,
    'FILL',
    '',
    NULL,
    `binding`.`candidate_source_type`,
    `binding`.`candidate_source_ids`,
    'ANY_ONE',
    2147483647,
    b'1',
    'MES route dynamic form filler version backfill',
    COALESCE(NULLIF(`binding`.`creator`, ''), 'codex'),
    `binding`.`create_time`,
    'codex',
    NOW(),
    b'0'
  FROM `mes_pro_route_flow_process_batch_record` AS `binding`
  JOIN `mes_pro_route_version` AS `route_version`
    ON `route_version`.`tenant_id` = `binding`.`tenant_id`
   AND `route_version`.`route_id` = `binding`.`route_id`
   AND `route_version`.`deleted` = b'0'
   AND `route_version`.`active` = b'1'
   AND `route_version`.`lifecycle_status` = 'ACTIVE'
  WHERE `binding`.`deleted` = b'0'
    AND COALESCE(NULLIF(`binding`.`form_binding_key`, ''), '') <> ''
    AND COALESCE(NULLIF(`binding`.`candidate_source_type`, ''), '') <> ''
    AND COALESCE(NULLIF(`binding`.`candidate_source_ids`, ''), '') <> ''
    AND NOT EXISTS (
      SELECT 1
      FROM `mes_pro_edhr_process_form_permission_rule` AS `existing_rule`
      WHERE `existing_rule`.`tenant_id` = `binding`.`tenant_id`
        AND `existing_rule`.`route_process_id` = `binding`.`route_process_id`
        AND `existing_rule`.`batch_record_report_id` = `binding`.`form_binding_key`
        AND `existing_rule`.`batch_record_version_id` = `route_version`.`id`
        AND `existing_rule`.`rule_type` = 'FILL'
        AND `existing_rule`.`signature_cell_key` = ''
        AND `existing_rule`.`deleted` = b'0'
    );
END//
DELIMITER ;

CALL ensure_mes_route_form_permission_rule_version_backfill();

DROP PROCEDURE IF EXISTS ensure_mes_route_form_permission_rule_version_backfill;
