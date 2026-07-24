-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_mes_edhr_batch_execution_active_context; type=schema; riskLevel=medium
-- eDHR 质量终态重开与同生产批号新执行尝试追溯字段。
-- Rollback: ALTER TABLE mes_pro_edhr_batch_execution DROP COLUMN reexecuted_by_change_event_id;
-- Rollback: ALTER TABLE mes_pro_edhr_batch_execution DROP COLUMN superseded_by_batch_execution_id;
-- Rollback: ALTER TABLE mes_pro_edhr_batch_execution DROP COLUMN source_rejected_batch_execution_id;
-- Rollback: ALTER TABLE mes_pro_edhr_batch_execution DROP COLUMN attempt_no;

DROP PROCEDURE IF EXISTS intruoyi_upgrade_mes_edhr_quality_reexecute_trace;

DELIMITER //

CREATE PROCEDURE intruoyi_upgrade_mes_edhr_quality_reexecute_trace()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing mes_pro_edhr_batch_execution';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
      AND column_name = 'attempt_no'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD COLUMN `attempt_no` int NOT NULL DEFAULT 1
      COMMENT '同生产批号执行尝试序号' AFTER `active_context_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
      AND column_name = 'source_rejected_batch_execution_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD COLUMN `source_rejected_batch_execution_id` bigint DEFAULT NULL
      COMMENT '来源拒收批次执行ID' AFTER `attempt_no`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
      AND column_name = 'superseded_by_batch_execution_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD COLUMN `superseded_by_batch_execution_id` bigint DEFAULT NULL
      COMMENT '被重做的新批次执行ID' AFTER `source_rejected_batch_execution_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
      AND column_name = 'reexecuted_by_change_event_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD COLUMN `reexecuted_by_change_event_id` bigint DEFAULT NULL
      COMMENT '触发重做变更事件ID' AFTER `superseded_by_batch_execution_id`;
  END IF;

  UPDATE `mes_pro_edhr_batch_execution`
     SET `attempt_no` = 1
   WHERE `attempt_no` IS NULL;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
      AND index_name = 'idx_mes_pro_edhr_batch_execution_source_rejected'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD INDEX `idx_mes_pro_edhr_batch_execution_source_rejected`
      (`tenant_id`, `source_rejected_batch_execution_id`);
  END IF;
END//

DELIMITER ;

CALL intruoyi_upgrade_mes_edhr_quality_reexecute_trace();

DROP PROCEDURE IF EXISTS intruoyi_upgrade_mes_edhr_quality_reexecute_trace;
