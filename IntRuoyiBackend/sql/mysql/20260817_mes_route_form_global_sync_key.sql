-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260722_mes_route_form_center_runtime_columns; type=schema; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_route_form_global_sync_key;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_form_global_sync_key()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_route_flow_process_batch_record is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'global_sync_key'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `global_sync_key` varchar(128) DEFAULT NULL COMMENT '路线附加表单全局联动组 Key'
      AFTER `form_binding_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND INDEX_NAME = 'idx_mes_route_flow_global_sync'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD KEY `idx_mes_route_flow_global_sync`
        (`tenant_id`, `route_id`, `use_type`, `global_sync_key`, `deleted`);
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_form_global_sync_key();
DROP PROCEDURE IF EXISTS ensure_mes_route_form_global_sync_key;
