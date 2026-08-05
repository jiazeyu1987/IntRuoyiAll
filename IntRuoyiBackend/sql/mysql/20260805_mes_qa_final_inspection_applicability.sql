-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_qa_inspection_regulation; type=schema; riskLevel=medium
-- MES AC-M15: persist explicit final-inspection applicability and not-applicable evidence on QA regulation versions.

DROP PROCEDURE IF EXISTS ensure_mes_qa_final_inspection_column;

DELIMITER //
CREATE PROCEDURE ensure_mes_qa_final_inspection_column(IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @mes_qa_final_inspection_column_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_qa_inspection_regulation_version'
    AND COLUMN_NAME = target_column;

  SET @mes_qa_final_inspection_column_sql = IF(
    @mes_qa_final_inspection_column_count = 0,
    ddl_statement,
    CONCAT('SELECT ''mes_qa_inspection_regulation_version.', target_column, ' already exists'' AS migration_status')
  );

  PREPARE mes_qa_final_inspection_column_stmt FROM @mes_qa_final_inspection_column_sql;
  EXECUTE mes_qa_final_inspection_column_stmt;
  DEALLOCATE PREPARE mes_qa_final_inspection_column_stmt;
END//
DELIMITER ;

CALL ensure_mes_qa_final_inspection_column('final_inspection_applicable',
  'ALTER TABLE `mes_qa_inspection_regulation_version` ADD COLUMN `final_inspection_applicable` bit(1) DEFAULT NULL COMMENT ''末检是否适用'' AFTER `retired_at`');
CALL ensure_mes_qa_final_inspection_column('final_inspection_not_applicable_reason',
  'ALTER TABLE `mes_qa_inspection_regulation_version` ADD COLUMN `final_inspection_not_applicable_reason` varchar(512) DEFAULT NULL COMMENT ''末检不适用依据'' AFTER `final_inspection_applicable`');

DROP PROCEDURE IF EXISTS ensure_mes_qa_final_inspection_column;
