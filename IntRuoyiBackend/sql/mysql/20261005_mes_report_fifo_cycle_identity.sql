-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260809_mes_process_pool_report_shared_allocation; type=schema; riskLevel=medium
-- SA30: new REPORT lines carry the exact formal allocation and execution cycle.
-- Deploy this structure before the new writer. Historical rows are never associated or backfilled.
-- Missing identities on retained historical REPORT evidence must fail; no work-order inference is valid.
DROP PROCEDURE IF EXISTS upgrade_mes_report_fifo_cycle_identity;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_report_fifo_cycle_identity()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line'
      AND COLUMN_NAME IN ('tenant_id','report_allocation_version','lifecycle_status')) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing report FIFO lifecycle prerequisite';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line' AND COLUMN_NAME='report_allocation_id') THEN
    ALTER TABLE mes_pro_process_pool_fifo_allocation_line ADD COLUMN report_allocation_id BIGINT NULL;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line' AND COLUMN_NAME='report_allocation_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected FIFO report_allocation_id definition';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line' AND COLUMN_NAME='target_active_order_id') THEN
    ALTER TABLE mes_pro_process_pool_fifo_allocation_line ADD COLUMN target_active_order_id BIGINT NULL;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line' AND COLUMN_NAME='target_active_order_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected FIFO target_active_order_id definition';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line' AND INDEX_NAME='idx_fifo_report_allocation') THEN
    ALTER TABLE mes_pro_process_pool_fifo_allocation_line ADD INDEX idx_fifo_report_allocation (tenant_id, report_allocation_id, lifecycle_status);
  ELSEIF COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line'
      AND INDEX_NAME='idx_fifo_report_allocation' AND NON_UNIQUE=1 AND SUB_PART IS NULL), '') <> 'tenant_id,report_allocation_id,lifecycle_status' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected FIFO idx_fifo_report_allocation definition';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line' AND INDEX_NAME='idx_fifo_target_cycle') THEN
    ALTER TABLE mes_pro_process_pool_fifo_allocation_line ADD INDEX idx_fifo_target_cycle (tenant_id, target_active_order_id, lifecycle_status);
  ELSEIF COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_process_pool_fifo_allocation_line'
      AND INDEX_NAME='idx_fifo_target_cycle' AND NON_UNIQUE=1 AND SUB_PART IS NULL), '') <> 'tenant_id,target_active_order_id,lifecycle_status' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected FIFO idx_fifo_target_cycle definition';
  END IF;
END$$
DELIMITER ;
CALL upgrade_mes_report_fifo_cycle_identity();
DROP PROCEDURE upgrade_mes_report_fifo_cycle_identity;
