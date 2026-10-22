-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_production_release_flow; type=schema; riskLevel=medium
-- Preserve every historical batch association. Only a formally rework-closed
-- application releases the current-batch slot; rejected/void/released rows retain it.
DROP PROCEDURE IF EXISTS upgrade_mes_release_rework_current_batch;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_release_rework_current_batch()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_active_order_release_application') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing production release application prerequisite table';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_active_order_release_application'
      AND COLUMN_NAME='current_batch_execution_id') THEN
    ALTER TABLE mes_pro_process_pool_active_order_release_application
      ADD COLUMN current_batch_execution_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN application_status='PQC_RELEASE_REJECTED' AND pqc_decision='NONCONFORMANCE_REWORK'
         THEN NULL ELSE batch_execution_id END) STORED;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_active_order_release_application'
      AND INDEX_NAME='uk_mes_pp_release_batch_execution' AND COLUMN_NAME='batch_execution_id') THEN
    -- Atomic replacement: never leave the table without current-application uniqueness.
    ALTER TABLE mes_pro_process_pool_active_order_release_application
      DROP INDEX uk_mes_pp_release_batch_execution,
      ADD UNIQUE KEY uk_mes_pp_release_batch_execution (tenant_id,current_batch_execution_id,deleted),
      ADD KEY idx_mes_pp_release_batch_history (tenant_id,batch_execution_id,deleted);
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_active_order_release_application'
      AND INDEX_NAME='uk_mes_pp_release_batch_execution' AND NON_UNIQUE=0
      AND COLUMN_NAME='current_batch_execution_id') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Current release batch unique index is missing or incompatible';
  END IF;
END$$
DELIMITER ;
CALL upgrade_mes_release_rework_current_batch();
DROP PROCEDURE upgrade_mes_release_rework_current_batch;
