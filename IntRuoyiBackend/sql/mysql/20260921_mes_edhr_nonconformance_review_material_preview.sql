-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260920_mes_edhr_nonconformance_review_active_order_fact; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS upgrade_mes_edhr_ncr_review_material_preview;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_edhr_ncr_review_material_preview()
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
        AND COLUMN_NAME = 'review_material_file_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD COLUMN `review_material_file_id` bigint DEFAULT NULL COMMENT '评审材料正式文件ID' AFTER `review_material_url`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_edhr_nonconformance_review'
        AND INDEX_NAME = 'idx_mes_edhr_ncr_review_material_file'
  ) THEN
    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD KEY `idx_mes_edhr_ncr_review_material_file` (`tenant_id`, `review_material_file_id`, `deleted`);
  END IF;
END$$
DELIMITER ;

CALL upgrade_mes_edhr_ncr_review_material_preview();
DROP PROCEDURE IF EXISTS upgrade_mes_edhr_ncr_review_material_preview;
