-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260711_mes_batch_record_cell_link_rule; type=schema; riskLevel=low

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_cell_link_work_order_source_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_record_cell_link_work_order_source_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_cell_link_rule'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_cell_link_rule is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_cell_link_rule'
      AND COLUMN_NAME = 'source_type'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_cell_link_rule`
      ADD COLUMN `source_type` varchar(32) NOT NULL DEFAULT 'BATCH_RECORD_CELL' COMMENT '来源类型' AFTER `batch_record_version_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_cell_link_rule'
      AND COLUMN_NAME = 'source_field_code'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_cell_link_rule`
      ADD COLUMN `source_field_code` varchar(1024) DEFAULT NULL COMMENT '来源字段编码' AFTER `source_cell_key`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_cell_link_rule'
      AND COLUMN_NAME = 'source_field_name'
  ) THEN
    ALTER TABLE `mes_pro_batch_record_cell_link_rule`
      ADD COLUMN `source_field_name` varchar(255) DEFAULT NULL COMMENT '来源字段名称快照' AFTER `source_field_code`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_batch_record_cell_link_work_order_source_columns();

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_cell_link_work_order_source_columns;
