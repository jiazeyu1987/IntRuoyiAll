-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Persist DCC submit idempotency evidence and reject duplicate keys per tenant/submitter.
-- The guarded procedure is intentionally repeatable because release rehearsal runs twice
-- and a previously applied migration may be revalidated against an updated manifest digest.

DROP PROCEDURE IF EXISTS ensure_dcc_controlled_file_submit_idempotency;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_controlled_file_submit_idempotency()
BEGIN
  DECLARE v_table_count INT DEFAULT 0;
  DECLARE v_column_count INT DEFAULT 0;
  DECLARE v_existing_index_count INT DEFAULT 0;
  DECLARE v_valid_index_count INT DEFAULT 0;

  SELECT COUNT(*)
    INTO v_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_controlled_file';

  IF v_table_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC controlled file table missing for submit idempotency migration';
  END IF;

  SELECT COUNT(*)
    INTO v_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_controlled_file'
     AND COLUMN_NAME = 'submit_idempotency_key';

  IF v_column_count = 0 THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `submit_idempotency_key` VARCHAR(128) NULL AFTER `process_definition_key`;
  END IF;

  SELECT COUNT(*)
    INTO v_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_controlled_file'
     AND COLUMN_NAME = 'submit_payload_hash';

  IF v_column_count = 0 THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `submit_payload_hash` CHAR(64) NULL AFTER `submit_idempotency_key`;
  END IF;

  SELECT COUNT(*)
    INTO v_existing_index_count
    FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_controlled_file'
     AND INDEX_NAME = 'uk_dcc_file_submit_idempotency';

  SELECT COUNT(*)
    INTO v_valid_index_count
    FROM (
      SELECT INDEX_NAME
        FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_controlled_file'
         AND INDEX_NAME = 'uk_dcc_file_submit_idempotency'
         AND NON_UNIQUE = 0
       GROUP BY INDEX_NAME
      HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) =
             'tenant_id,submitter_id,submit_idempotency_key,deleted'
    ) AS valid_index;

  IF v_existing_index_count = 0 THEN
    ALTER TABLE `dcc_controlled_file`
      ADD UNIQUE KEY `uk_dcc_file_submit_idempotency`
        (`tenant_id`, `submitter_id`, `submit_idempotency_key`, `deleted`);
  ELSEIF v_valid_index_count = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Existing DCC submit idempotency index has an incompatible definition';
  END IF;
END$$
DELIMITER ;

CALL ensure_dcc_controlled_file_submit_idempotency();
DROP PROCEDURE IF EXISTS ensure_dcc_controlled_file_submit_idempotency;
