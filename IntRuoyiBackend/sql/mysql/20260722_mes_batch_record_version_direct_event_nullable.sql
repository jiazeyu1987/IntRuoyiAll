-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260708_mes_batch_record_version_phase_one; type=schema; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_batch_record_version_direct_event_nullable;

DELIMITER //
CREATE PROCEDURE ensure_batch_record_version_direct_event_nullable()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_batch_record_version_approval_event'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Batch record version direct event migration requires approval event table';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_batch_record_version_approval_event'
      AND column_name = 'approval_instance_id'
      AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_version_approval_event`
      MODIFY COLUMN `approval_instance_id` varchar(128) DEFAULT NULL COMMENT '审批实例ID';
  END IF;
END//
DELIMITER ;

CALL ensure_batch_record_version_direct_event_nullable();

DROP PROCEDURE IF EXISTS ensure_batch_record_version_direct_event_nullable;
