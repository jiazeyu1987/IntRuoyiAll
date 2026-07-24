-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260526_edhr_field_audit_schema,20260720_mes_batch_shared_form_binding; type=schema; riskLevel=medium
-- eDHR recordbook-to-batch-record controlled sync. Missing prerequisite tables fail fast.

DROP PROCEDURE IF EXISTS ensure_mes_recordbook_batch_controlled_sync;
DELIMITER $$
CREATE PROCEDURE ensure_mes_recordbook_batch_controlled_sync()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_flow_process_batch_record is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_batch_execution_task is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_execution is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution_field_audit_item'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_execution_field_audit_item is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'recordbook_enabled'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `recordbook_enabled` bit(1) NOT NULL DEFAULT b'1'
        COMMENT 'Recordbook entry enabled for controlled batch record'
        AFTER `validation_profile`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'recordbook_enabled'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `recordbook_enabled` bit(1) NOT NULL DEFAULT b'1'
        COMMENT 'Frozen recordbook entry enabled state'
        AFTER `validation_profile`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'recordbook_enabled'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `recordbook_enabled` bit(1) NOT NULL DEFAULT b'1'
        COMMENT 'Frozen recordbook entry enabled state'
        AFTER `validation_profile`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution_field_audit_item'
      AND COLUMN_NAME = 'recordbook_value_json'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution_field_audit_item`
      ADD COLUMN `recordbook_value_json` longtext DEFAULT NULL
        COMMENT 'Original value filled in recordbook'
        AFTER `new_value_hash`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution_field_audit_item'
      AND COLUMN_NAME = 'recordbook_value_display'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution_field_audit_item`
      ADD COLUMN `recordbook_value_display` varchar(1000) DEFAULT NULL
        COMMENT 'Original display value filled in recordbook'
        AFTER `recordbook_value_json`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution_field_audit_item'
      AND COLUMN_NAME = 'batch_record_value_json'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution_field_audit_item`
      ADD COLUMN `batch_record_value_json` longtext DEFAULT NULL
        COMMENT 'Final value stored in controlled batch record'
        AFTER `recordbook_value_display`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution_field_audit_item'
      AND COLUMN_NAME = 'batch_record_value_display'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution_field_audit_item`
      ADD COLUMN `batch_record_value_display` varchar(1000) DEFAULT NULL
        COMMENT 'Final display value stored in controlled batch record'
        AFTER `batch_record_value_json`;
  END IF;

  UPDATE `mes_pro_route_flow_process_batch_record`
     SET `recordbook_enabled` = b'0'
   WHERE `record_category` = 'INTERNAL_RECORD';

  UPDATE `mes_pro_edhr_batch_execution_task`
     SET `recordbook_enabled` = b'0'
   WHERE `record_category` = 'INTERNAL_RECORD';

  UPDATE `mes_pro_batch_record_execution`
     SET `recordbook_enabled` = b'0'
   WHERE `record_category` = 'INTERNAL_RECORD';
END$$
DELIMITER ;

CALL ensure_mes_recordbook_batch_controlled_sync();
DROP PROCEDURE IF EXISTS ensure_mes_recordbook_batch_controlled_sync;
