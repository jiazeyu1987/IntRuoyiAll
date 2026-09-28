-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260922_mes_release_rework_current_batch; type=schema; riskLevel=medium
-- Keep terminal rework transactions as history while reserving one current transaction per batch.
DROP PROCEDURE IF EXISTS upgrade_mes_edhr_release_transaction_rework_history;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_edhr_release_transaction_rework_history()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_edhr_release_transaction') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing release transaction prerequisite table';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_edhr_release_transaction'
      AND COLUMN_NAME='current_batch_execution_id') THEN
    ALTER TABLE mes_pro_edhr_release_transaction
      ADD COLUMN current_batch_execution_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN release_status='REJECTED' AND reject_reason='NONCONFORMANCE_REWORK'
         THEN NULL ELSE batch_execution_id END) STORED;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_edhr_release_transaction'
      AND INDEX_NAME='uk_mes_pro_edhr_release_transaction_batch') THEN
    ALTER TABLE mes_pro_edhr_release_transaction
      DROP INDEX uk_mes_pro_edhr_release_transaction_batch,
      ADD UNIQUE KEY uk_mes_edhr_release_transaction_current_batch
          (tenant_id,current_batch_execution_id,deleted),
      ADD KEY idx_mes_edhr_release_transaction_batch_history
          (tenant_id,batch_execution_id,deleted);
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_edhr_release_transaction'
      AND INDEX_NAME='uk_mes_edhr_release_transaction_current_batch'
      AND NON_UNIQUE=0 AND COLUMN_NAME='current_batch_execution_id') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Current release transaction unique index is missing or incompatible';
  END IF;
END$$
DELIMITER ;
CALL upgrade_mes_edhr_release_transaction_rework_history();
DROP PROCEDURE upgrade_mes_edhr_release_transaction_rework_history;
