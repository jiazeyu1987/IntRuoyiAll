-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Persist DCC submit idempotency evidence and reject duplicate keys per tenant/submitter.

DELIMITER //

DROP PROCEDURE IF EXISTS `apply_dcc_file_submit_idempotency`//
CREATE PROCEDURE `apply_dcc_file_submit_idempotency`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FILE_SUBMIT_IDEMPOTENCY_TABLE_MISSING';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
      AND COLUMN_NAME = 'creation_idempotency_key'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `creation_idempotency_key` VARCHAR(128) NULL AFTER `process_definition_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
      AND COLUMN_NAME = 'creation_payload_hash'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `creation_payload_hash` CHAR(64) NULL AFTER `creation_idempotency_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
      AND COLUMN_NAME = 'submit_idempotency_key'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `submit_idempotency_key` VARCHAR(128) NULL AFTER `process_definition_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
      AND COLUMN_NAME = 'submit_payload_hash'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `submit_payload_hash` CHAR(64) NULL AFTER `submit_idempotency_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
      AND INDEX_NAME = 'uk_dcc_file_creation_idempotency'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD UNIQUE KEY `uk_dcc_file_creation_idempotency`
        (`tenant_id`, `requester_id`, `creation_idempotency_key`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
      AND INDEX_NAME = 'uk_dcc_file_submit_idempotency'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD UNIQUE KEY `uk_dcc_file_submit_idempotency`
        (`tenant_id`, `submitter_id`, `submit_idempotency_key`, `deleted`);
  END IF;
END//

DELIMITER ;

CALL `apply_dcc_file_submit_idempotency`();
DROP PROCEDURE IF EXISTS `apply_dcc_file_submit_idempotency`;
