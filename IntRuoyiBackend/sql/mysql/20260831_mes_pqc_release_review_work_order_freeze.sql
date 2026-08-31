-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260831_mes_pqc_release_nonconformance_scope; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS upgrade_mes_pqc_release_review_work_order_freeze;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_pqc_release_review_work_order_freeze()
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
        AND COLUMN_NAME = 'previous_work_order_temporary_frozen'
  ) THEN
    IF EXISTS (
        SELECT 1
        FROM `mes_pro_edhr_nonconformance_review`
        WHERE `deleted` = b'0'
          AND `source_type` = 'PQC_RELEASE'
          AND `batch_execution_id` IS NULL
          AND `review_status` = 'pending_review'
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Pending PQC release review lacks auditable previous work-order freeze state';
    END IF;

    ALTER TABLE `mes_pro_edhr_nonconformance_review`
      ADD COLUMN `previous_work_order_temporary_frozen` bit(1) DEFAULT NULL
      COMMENT '不合格评审冻结前工单临时冻结状态'
      AFTER `previous_batch_status`;
  END IF;
END$$
DELIMITER ;

CALL upgrade_mes_pqc_release_review_work_order_freeze();
DROP PROCEDURE IF EXISTS upgrade_mes_pqc_release_review_work_order_freeze;
