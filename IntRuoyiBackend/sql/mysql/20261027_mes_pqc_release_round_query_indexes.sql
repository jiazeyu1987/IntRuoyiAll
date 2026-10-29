-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260808_mes_active_order_release_application,20260920_mes_edhr_nonconformance_review_active_order_fact; type=schema; riskLevel=medium
-- Required for the intended access paths of PQC history pagination/count and page review decoration.
-- Application equality prefix plus applied_at supports the next strictly later round lookup.
-- Review equality prefix plus id supports reverse latest-review scans for one active order.
-- No data rewrite. Existing business/uniqueness indexes are retained.
DROP PROCEDURE IF EXISTS add_mes_pqc_round_query_indexes;
DELIMITER $$
CREATE PROCEDURE add_mes_pqc_round_query_indexes()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_process_pool_active_order_release_application')
     OR NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
                      AND COLUMN_NAME = 'active_order_id') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing PQC release application or active-order review schema';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_process_pool_active_order_release_application'
                   AND INDEX_NAME = 'idx_mes_pqc_application_round') THEN
    ALTER TABLE mes_pro_process_pool_active_order_release_application
      ADD KEY idx_mes_pqc_application_round (tenant_id, deleted, active_order_id, applied_at, id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
                   AND INDEX_NAME = 'idx_mes_pqc_review_round_latest') THEN
    ALTER TABLE mes_pro_edhr_nonconformance_review
      ADD KEY idx_mes_pqc_review_round_latest (tenant_id, deleted, active_order_id, source_type, id);
  END IF;
END$$
DELIMITER ;
CALL add_mes_pqc_round_query_indexes();
DROP PROCEDURE IF EXISTS add_mes_pqc_round_query_indexes;
