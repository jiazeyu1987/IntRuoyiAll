-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260830_mes_edhr_nonconformance_review_mvp; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS upgrade_mes_pqc_release_nonconformance_scope;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_pqc_release_nonconformance_scope()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_edhr_nonconformance_review prerequisite table';
  END IF;

  ALTER TABLE `mes_pro_edhr_nonconformance_review`
    MODIFY COLUMN `batch_execution_id` bigint NULL COMMENT 'eDHR批次执行ID；PQC生产放行申请建批前可为空';

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
        AND INDEX_NAME = 'idx_mes_edhr_ncr_source'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD KEY `idx_mes_edhr_ncr_source` (`tenant_id`, `source_type`, `source_id`, `review_status`);
  END IF;
END$$
DELIMITER ;

CALL upgrade_mes_pqc_release_nonconformance_scope();
DROP PROCEDURE IF EXISTS upgrade_mes_pqc_release_nonconformance_scope;
