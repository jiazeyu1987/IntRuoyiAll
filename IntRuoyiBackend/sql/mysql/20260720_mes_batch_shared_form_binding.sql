-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260707_mes_batch_record_extra_form_slots,20260708_mes_batch_record_version_phase_one; type=schema; riskLevel=medium
-- Batch shared form binding: route config captures shared form identity and fillable scope; runtime execution freezes one shared instance per batch.

DROP PROCEDURE IF EXISTS ensure_mes_batch_shared_form_binding;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_shared_form_binding()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_flow_process_batch_record is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_execution is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_batch_execution_task is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'instance_scope'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `instance_scope` varchar(32) NOT NULL DEFAULT 'PROCESS'
        COMMENT 'Execution instance scope: PROCESS/BATCH_SHARED'
        AFTER `form_slot_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'shared_form_key'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `shared_form_key` varchar(64) DEFAULT NULL
        COMMENT 'Stable shared form key within one batch'
        AFTER `instance_scope`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'fillable_scope_json'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `fillable_scope_json` json DEFAULT NULL
        COMMENT 'Allowed table and row ranges for this process'
        AFTER `shared_form_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'batch_execution_id'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `batch_execution_id` bigint DEFAULT NULL
        COMMENT 'Owning eDHR batch execution id for BATCH_SHARED forms'
        AFTER `batch_record_version_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'instance_scope'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `instance_scope` varchar(32) NOT NULL DEFAULT 'PROCESS'
        COMMENT 'Execution instance scope: PROCESS/BATCH_SHARED'
        AFTER `route_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'shared_form_key'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD COLUMN `shared_form_key` varchar(64) DEFAULT NULL
        COMMENT 'Stable shared form key within one batch'
        AFTER `instance_scope`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND COLUMN_NAME = 'active_context_key'
      AND CHARACTER_MAXIMUM_LENGTH < 512
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      MODIFY COLUMN `active_context_key` varchar(512) DEFAULT NULL COMMENT 'Active execution context key';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'instance_scope'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `instance_scope` varchar(32) NOT NULL DEFAULT 'PROCESS'
        COMMENT 'Execution instance scope: PROCESS/BATCH_SHARED'
        AFTER `form_slot_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'shared_form_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `shared_form_key` varchar(64) DEFAULT NULL
        COMMENT 'Stable shared form key within one batch'
        AFTER `instance_scope`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND COLUMN_NAME = 'fillable_scope_json'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `fillable_scope_json` json DEFAULT NULL
        COMMENT 'Allowed table and row ranges for this process'
        AFTER `shared_form_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
      AND INDEX_NAME = 'idx_mes_batch_record_execution_shared'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_execution`
      ADD INDEX `idx_mes_batch_record_execution_shared`
        (`tenant_id`, `batch_execution_id`, `instance_scope`, `shared_form_key`, `batch_code`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task'
      AND INDEX_NAME = 'idx_mes_edhr_batch_task_shared'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD INDEX `idx_mes_edhr_batch_task_shared`
        (`tenant_id`, `batch_execution_id`, `instance_scope`, `shared_form_key`, `deleted`);
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_batch_shared_form_binding();
DROP PROCEDURE IF EXISTS ensure_mes_batch_shared_form_binding;
