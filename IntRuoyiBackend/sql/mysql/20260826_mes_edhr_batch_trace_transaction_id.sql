-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260822_mes_edhr_batch_traceability; type=schema; riskLevel=medium
-- Flow 4 completion transactions are opaque idempotency strings, not numeric IDs.
DROP PROCEDURE IF EXISTS ensure_mes_edhr_trace_completion_transaction_type;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_trace_completion_transaction_type()
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_execution_origin'
         AND COLUMN_NAME = 'completion_transaction_id'
  ) THEN
    ALTER TABLE mes_pro_edhr_batch_execution_origin
      MODIFY COLUMN completion_transaction_id varchar(180) DEFAULT NULL;
  END IF;
END$$
DELIMITER ;
CALL ensure_mes_edhr_trace_completion_transaction_type();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_trace_completion_transaction_type;
