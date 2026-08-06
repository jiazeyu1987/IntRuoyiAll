-- Rollback for local AC-M21 aggregate runtime-closure schema repair.
-- Scope: local Docker MySQL ruoyi-vue-pro only. Restores the pre-closure table shape when no aggregate rows exist.

DROP PROCEDURE IF EXISTS rollback_acm21_aggregate_runtime_closure;
DELIMITER $$
CREATE PROCEDURE rollback_acm21_aggregate_runtime_closure()
BEGIN
  DECLARE v_aggregate_rows int DEFAULT 0;
  SELECT COUNT(1)
    INTO v_aggregate_rows
    FROM `mes_pqc_process_inspection_aggregate_detail`;
  IF v_aggregate_rows > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'rollback_acm21_aggregate_runtime_closure refuses to drop columns while aggregate rows exist';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND index_name = 'idx_mes_pqc_process_inspection_submit_event'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      DROP INDEX `idx_mes_pqc_process_inspection_submit_event`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND index_name = 'idx_mes_pqc_process_inspection_task'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      DROP INDEX `idx_mes_pqc_process_inspection_task`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND index_name = 'idx_mes_pqc_process_inspection_review'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      DROP INDEX `idx_mes_pqc_process_inspection_review`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND index_name = 'uk_mes_pqc_process_inspection_aggregate'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      DROP INDEX `uk_mes_pqc_process_inspection_aggregate`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_process_inspection_aggregate_detail'
       AND column_name = 'actual_inspection_quantity'
  ) THEN
    ALTER TABLE `mes_pqc_process_inspection_aggregate_detail`
      DROP COLUMN `actual_inspection_quantity`;
  END IF;
END$$
DELIMITER ;

CALL rollback_acm21_aggregate_runtime_closure();

DROP PROCEDURE IF EXISTS rollback_acm21_aggregate_runtime_closure;
