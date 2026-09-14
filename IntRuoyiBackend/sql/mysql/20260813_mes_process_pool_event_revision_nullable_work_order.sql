-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260808_mes_process_pool_frontline_no_work_order; type=schema; riskLevel=low
-- 一线生产允许先报工后分配：报工修订审计保留正式空工单上下文。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_pp_event_revision_nullable_work_order_20260813;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_event_revision_nullable_work_order_20260813()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event_revision'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_event_revision table';
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event_revision'
        AND column_name = 'work_order_id'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_event_revision.work_order_id column';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event_revision'
        AND column_name = 'work_order_id'
        AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event_revision`
      MODIFY COLUMN `work_order_id` bigint DEFAULT NULL COMMENT '生产工单ID；一线生产可为空';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event_revision'
        AND column_name = 'work_order_id'
        AND is_nullable <> 'YES'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Event revision work_order_id nullable contract mismatch';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_event_revision_nullable_work_order_20260813();

DROP PROCEDURE IF EXISTS ensure_mes_pp_event_revision_nullable_work_order_20260813;
