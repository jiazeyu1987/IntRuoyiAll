-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_lifecycle,20261003_dcc_approval_file_owner_snapshot; type=schema; riskLevel=medium
-- IC05 explicit new application selections and real effectiveness facts. No historical backfill.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_application_selection_column;
DELIMITER //
CREATE PROCEDURE ensure_dcc_application_selection_column(IN field_name VARCHAR(64),IN field_definition TEXT,IN expected_type VARCHAR(64))
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file' AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC05 requires actual InnoDB controlled file schema';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME=field_name) THEN
    SET @dcc_application_selection_ddl=CONCAT('ALTER TABLE dcc_controlled_file ADD COLUMN ',field_name,' ',field_definition);
    PREPARE dcc_application_selection_statement FROM @dcc_application_selection_ddl;
    EXECUTE dcc_application_selection_statement;
    DEALLOCATE PREPARE dcc_application_selection_statement;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME=field_name AND COLUMN_TYPE=expected_type
      AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA=''
      AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC05 nullable selection column definition drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_application_selection_column('effective_mode','VARCHAR(40) NULL','varchar(40)');
CALL ensure_dcc_application_selection_column('effective_time','DATETIME NULL','datetime');
CALL ensure_dcc_application_selection_column('effective_signature_id','BIGINT NULL','bigint');
CALL ensure_dcc_application_selection_column('effective_process_instance_id','VARCHAR(128) NULL','varchar(128)');
CALL ensure_dcc_application_selection_column('distribution_department_ids_json','TEXT NULL','text');
CALL ensure_dcc_application_selection_column('selected_signoff_department_ids_json','TEXT NULL','text');
DROP PROCEDURE IF EXISTS ensure_dcc_application_selection_column;
