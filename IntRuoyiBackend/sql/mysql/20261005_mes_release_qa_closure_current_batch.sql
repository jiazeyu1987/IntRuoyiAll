-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260922_mes_release_rework_current_batch; type=schema; riskLevel=medium
-- QA lifecycle closure is separate from immutable PQC decisions. No historical row is rewritten.
DROP PROCEDURE IF EXISTS upgrade_mes_release_qa_closure_current_batch;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_release_qa_closure_current_batch()
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
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process_pool_active_order_release_application' AND COLUMN_NAME='qa_closure_review_id') THEN
    ALTER TABLE mes_pro_process_pool_active_order_release_application ADD COLUMN qa_closure_review_id BIGINT NULL COMMENT 'Formal QA closure review; immutable PQC decision';
  END IF;
  ALTER TABLE mes_pro_process_pool_active_order_release_application
    MODIFY COLUMN current_batch_execution_id BIGINT GENERATED ALWAYS AS
      (CASE WHEN application_status='NONCONFORMANCE_REWORK'
       OR (application_status='PQC_RELEASE_REJECTED' AND pqc_decision='NONCONFORMANCE_REWORK')
       THEN NULL ELSE batch_execution_id END) STORED;
END$$
DELIMITER ;
CALL upgrade_mes_release_qa_closure_current_batch();
DROP PROCEDURE upgrade_mes_release_qa_closure_current_batch;
