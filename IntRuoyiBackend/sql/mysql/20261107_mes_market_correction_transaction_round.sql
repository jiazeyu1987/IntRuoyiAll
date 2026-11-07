-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260927_mes_edhr_release_transaction_rework_history,20261028_mes_market_withdrawal_correction_round; type=schema; riskLevel=medium
-- Match selectCurrentByBatchExecutionId: REJECTED/WITHDRAWN are immutable history.
-- No business rows or decisions are updated. One current transaction per tenant/batch remains mandatory.
-- Local repair: inspect prerequisites, apply 20260927_mes_edhr_release_transaction_rework_history
-- if missing, then apply this migration. Do not replay unrelated historical migrations.
-- Recovery: before creating new rounds the old index may be restored only after checking
-- tenant_id,batch_execution_id,deleted uniqueness. After new rounds exist, use a forward
-- schema correction; never delete historical decisions to force the legacy index back.
DROP PROCEDURE IF EXISTS upgrade_mes_market_correction_transaction_round;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_market_correction_transaction_round()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_edhr_release_transaction') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing release transaction table';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_pro_edhr_release_transaction
      WHERE release_status NOT IN ('REJECTED','WITHDRAWN')
      GROUP BY tenant_id,batch_execution_id,deleted HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate current release transactions; migration aborted';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_edhr_release_transaction' AND COLUMN_NAME='current_batch_execution_id'
      AND EXTRA LIKE '%STORED GENERATED%') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Apply release transaction rework-history prerequisite first';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_edhr_release_transaction'
      AND INDEX_NAME='uk_mes_pro_edhr_release_transaction_batch') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Legacy batch unique index still exists';
  END IF;
  IF (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_edhr_release_transaction'
      AND INDEX_NAME='uk_mes_edhr_release_transaction_current_batch' AND NON_UNIQUE=0)
      <=> 'tenant_id,current_batch_execution_id,deleted' THEN
    ALTER TABLE mes_pro_edhr_release_transaction
      MODIFY COLUMN current_batch_execution_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN release_status IN ('REJECTED','WITHDRAWN')
         THEN NULL ELSE batch_execution_id END) STORED;
  ELSE
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or incompatible current transaction unique index';
  END IF;
END$$
DELIMITER ;
CALL upgrade_mes_market_correction_transaction_round();
DROP PROCEDURE upgrade_mes_market_correction_transaction_round;
