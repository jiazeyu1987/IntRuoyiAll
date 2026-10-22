-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260822_mes_edhr_batch_traceability; type=schema; riskLevel=medium
-- Keep formal provisioning states and Tx-C failure reasons lossless in the traceability schema.
DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_traceability_capacity;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_batch_traceability_capacity()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_execution_origin'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_edhr_batch_execution_origin; apply Flow 7 traceability schema first';
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_trace_outbox_event'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_edhr_batch_trace_outbox_event; apply Flow 7 traceability schema first';
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_execution_origin'
         AND COLUMN_NAME = 'batch_provision_status'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing batch_provision_status; apply Flow 7 traceability schema first';
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_trace_outbox_event'
         AND COLUMN_NAME = 'reason'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing reason; apply Flow 7 traceability schema first';
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_execution_origin'
         AND COLUMN_NAME = 'batch_provision_status'
         AND (DATA_TYPE <> 'varchar' OR CHARACTER_MAXIMUM_LENGTH < 32)
  ) THEN
    ALTER TABLE mes_pro_edhr_batch_execution_origin
      MODIFY COLUMN batch_provision_status varchar(32) NOT NULL;
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_trace_outbox_event'
         AND COLUMN_NAME = 'reason'
         AND DATA_TYPE <> 'longtext'
  ) THEN
    ALTER TABLE mes_pro_edhr_batch_trace_outbox_event
      MODIFY COLUMN reason longtext DEFAULT NULL;
  END IF;
END$$
DELIMITER ;
CALL ensure_mes_edhr_batch_traceability_capacity();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_batch_traceability_capacity;
