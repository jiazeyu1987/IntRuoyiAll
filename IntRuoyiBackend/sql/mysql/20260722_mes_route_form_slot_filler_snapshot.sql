-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260722_mes_route_form_center_runtime_columns; type=schema; riskLevel=medium
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_route_form_slot_filler_snapshot;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_form_slot_filler_snapshot()
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
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'candidate_source_type'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `candidate_source_type` varchar(32) DEFAULT NULL COMMENT '动态表单填写人来源：USERS/ROLE'
      AFTER `slot_config_snapshot_hash`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'candidate_source_ids'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `candidate_source_ids` varchar(1000) DEFAULT NULL COMMENT '动态表单填写人来源 ID 快照，逗号分隔'
      AFTER `candidate_source_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_route_flow_process_batch_record'
      AND COLUMN_NAME = 'candidate_source_names'
  ) THEN
    ALTER TABLE `mes_pro_route_flow_process_batch_record`
      ADD COLUMN `candidate_source_names` varchar(1000) DEFAULT NULL COMMENT '动态表单填写人名称快照 JSON'
      AFTER `candidate_source_ids`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_form_slot_filler_snapshot();
DROP PROCEDURE IF EXISTS ensure_mes_route_form_slot_filler_snapshot;
