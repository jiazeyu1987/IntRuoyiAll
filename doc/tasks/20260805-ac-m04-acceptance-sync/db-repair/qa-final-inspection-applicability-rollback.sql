-- Rollback for local QA final-inspection applicability schema repair.
-- Scope: local Docker MySQL int-ruoyi-mysql / ruoyi-vue-pro only.

DROP PROCEDURE IF EXISTS rollback_mes_qa_final_inspection_column;

DELIMITER //
CREATE PROCEDURE rollback_mes_qa_final_inspection_column(IN target_column VARCHAR(64), IN ddl_statement TEXT)
BEGIN
  SELECT COUNT(*)
  INTO @mes_qa_final_inspection_rollback_column_count
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_qa_inspection_regulation_version'
    AND COLUMN_NAME = target_column;

  SET @mes_qa_final_inspection_rollback_sql = IF(
    @mes_qa_final_inspection_rollback_column_count = 1,
    ddl_statement,
    CONCAT('SELECT ''mes_qa_inspection_regulation_version.', target_column, ' is absent'' AS rollback_status')
  );

  PREPARE mes_qa_final_inspection_rollback_stmt FROM @mes_qa_final_inspection_rollback_sql;
  EXECUTE mes_qa_final_inspection_rollback_stmt;
  DEALLOCATE PREPARE mes_qa_final_inspection_rollback_stmt;
END//
DELIMITER ;

CALL rollback_mes_qa_final_inspection_column('final_inspection_not_applicable_reason',
  'ALTER TABLE `mes_qa_inspection_regulation_version` DROP COLUMN `final_inspection_not_applicable_reason`');
CALL rollback_mes_qa_final_inspection_column('final_inspection_applicable',
  'ALTER TABLE `mes_qa_inspection_regulation_version` DROP COLUMN `final_inspection_applicable`');

DROP PROCEDURE IF EXISTS rollback_mes_qa_final_inspection_column;
