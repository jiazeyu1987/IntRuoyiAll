-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260831_mes_pqc_release_nonconformance_scope; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS upgrade_mes_edhr_ncr_active_order_fact;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_edhr_ncr_active_order_fact()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_edhr_nonconformance_review prerequisite table';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
        AND COLUMN_NAME = 'active_order_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD COLUMN `active_order_id` bigint DEFAULT NULL COMMENT '统一活跃订单ID' AFTER `source_id`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
        AND INDEX_NAME = 'idx_mes_edhr_ncr_active_order'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD KEY `idx_mes_edhr_ncr_active_order` (`tenant_id`, `active_order_id`, `review_status`, `deleted`);
  END IF;
END$$
DELIMITER ;

CALL upgrade_mes_edhr_ncr_active_order_fact();
DROP PROCEDURE IF EXISTS upgrade_mes_edhr_ncr_active_order_fact;
