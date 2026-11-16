-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261028_mes_pqc_rejection_correction_round; type=schema; riskLevel=medium
-- Preserve the prior PQC approval and market withdrawal while the new correction round claims the same formal batch.
DROP PROCEDURE IF EXISTS upgrade_mes_market_withdrawal_correction_round;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_market_withdrawal_correction_round()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_active_order_release_application'
      AND COLUMN_NAME='current_batch_execution_id' AND EXTRA LIKE '%STORED GENERATED%') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing generated current release batch prerequisite';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_active_order_release_application'
      AND INDEX_NAME='uk_mes_pp_release_batch_execution' AND NON_UNIQUE=0
      AND COLUMN_NAME='current_batch_execution_id') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing current release batch unique index';
  END IF;
  ALTER TABLE mes_pro_process_pool_active_order_release_application
    MODIFY COLUMN current_batch_execution_id BIGINT GENERATED ALWAYS AS
      (CASE WHEN application_status='NONCONFORMANCE_REWORK'
       OR (application_status IN ('MARKET_RELEASE_WITHDRAWN','MARKET_RELEASE_REJECTED') AND pqc_decision='APPROVE')
       OR (application_status='PQC_RELEASE_REJECTED' AND pqc_decision IN ('NONCONFORMANCE_REWORK','REJECT'))
       THEN NULL ELSE batch_execution_id END) STORED;
END$$
DELIMITER ;
CALL upgrade_mes_market_withdrawal_correction_round();
DROP PROCEDURE upgrade_mes_market_withdrawal_correction_round;
