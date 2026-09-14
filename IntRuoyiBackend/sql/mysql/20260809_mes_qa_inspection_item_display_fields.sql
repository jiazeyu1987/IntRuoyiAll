-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_pqc_item_equipment_standard_snapshot; type=schema; riskLevel=low
-- Persist the exact QA inspection-item display text used by the frontline PQC detail dialog.
-- Existing published rows are intentionally not guessed because the original text cannot be reconstructed losslessly.

DROP PROCEDURE IF EXISTS ensure_mes_qa_item_display_column;

DELIMITER //
CREATE PROCEDURE ensure_mes_qa_item_display_column(IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @mes_qa_item_display_column_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_qa_inspection_regulation_item'
    AND COLUMN_NAME = target_column;

  SET @mes_qa_item_display_column_sql = IF(
    @mes_qa_item_display_column_count = 0,
    ddl_statement,
    'SELECT ''mes_qa_inspection_regulation_item display column already exists'' AS migration_status'
  );

  PREPARE mes_qa_item_display_column_stmt FROM @mes_qa_item_display_column_sql;
  EXECUTE mes_qa_item_display_column_stmt;
  DEALLOCATE PREPARE mes_qa_item_display_column_stmt;
END//
DELIMITER ;

CALL ensure_mes_qa_item_display_column('inspection_tool',
  'ALTER TABLE `mes_qa_inspection_regulation_item` ADD COLUMN `inspection_tool` varchar(512) DEFAULT NULL COMMENT ''检验器具及设备原文'' AFTER `inspection_method`');
CALL ensure_mes_qa_item_display_column('sampling_plan_text',
  'ALTER TABLE `mes_qa_inspection_regulation_item` ADD COLUMN `sampling_plan_text` varchar(512) DEFAULT NULL COMMENT ''抽样方案原文'' AFTER `standard_text`');

DROP PROCEDURE IF EXISTS ensure_mes_qa_item_display_column;
