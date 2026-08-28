-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260822_mes_edhr_batch_traceability; type=schema; riskLevel=medium
-- Flow 7 supports both numeric Flow 4 receipts and string Flow 9 independent receipts.
DROP PROCEDURE IF EXISTS ensure_mes_edhr_trace_source_credential_type;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_trace_source_credential_type()
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'mes_pro_edhr_batch_execution_origin'
         AND COLUMN_NAME = 'source_credential_id'
  ) THEN
    ALTER TABLE mes_pro_edhr_batch_execution_origin
      MODIFY COLUMN source_credential_id varchar(128) DEFAULT NULL;
  END IF;
END$$
DELIMITER ;
CALL ensure_mes_edhr_trace_source_credential_type();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_trace_source_credential_type;
