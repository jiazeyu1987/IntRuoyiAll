-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261011_dcc_approval_matrix_baseline,20260930_dcc_a_lifecycle; type=schema; riskLevel=medium
-- Prepared definition identity only; old rows remain null and never imply a legacy contract.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_prepared_execution;
DELIMITER //
CREATE PROCEDURE ensure_dcc_prepared_execution(IN field_name VARCHAR(64),IN expected_length INT)
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC16 requires exact InnoDB controlled file table';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME=field_name) THEN
    SET @dcc_prepared_execution_ddl=CONCAT('ALTER TABLE dcc_controlled_file ADD COLUMN ',field_name,' VARCHAR(',expected_length,') CHARACTER SET ascii COLLATE ascii_bin NULL');
    PREPARE dcc_prepared_execution_statement FROM @dcc_prepared_execution_ddl;
    EXECUTE dcc_prepared_execution_statement;
    DEALLOCATE PREPARE dcc_prepared_execution_statement;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME=field_name
      AND COLUMN_TYPE=CONCAT('varchar(',expected_length,')') AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA=''
      AND COALESCE(GENERATION_EXPRESSION,'')='' AND CHARACTER_SET_NAME='ascii' AND COLLATION_NAME='ascii_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC16 prepared execution definition drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_prepared_execution('prepared_workflow_contract',64);
CALL ensure_dcc_prepared_execution('prepared_process_definition_id',128);
CALL ensure_dcc_prepared_execution('prepared_definition_digest',64);
DROP PROCEDURE IF EXISTS ensure_dcc_prepared_execution;
