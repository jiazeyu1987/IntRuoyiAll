-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_lifecycle,20260930_dcc_a_signature_workflow_round; type=schema; riskLevel=medium
-- Approval owner facts apply to new explicit choices only. Historical revisions/signatures are not rewritten.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_file_owner_column;
DELIMITER //
CREATE PROCEDURE ensure_dcc_file_owner_column(IN column_name_value VARCHAR(64), IN definition_value TEXT)
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
   AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME=column_name_value) THEN
  SET @dcc_file_owner_ddl=CONCAT('ALTER TABLE dcc_controlled_file ADD COLUMN ',column_name_value,' ',definition_value);
  PREPARE dcc_file_owner_statement FROM @dcc_file_owner_ddl;
  EXECUTE dcc_file_owner_statement;
  DEALLOCATE PREPARE dcc_file_owner_statement;
 END IF;
END//
DELIMITER ;
CALL ensure_dcc_file_owner_column('file_owner_user_id','BIGINT NULL');
CALL ensure_dcc_file_owner_column('file_owner_username_snapshot','VARCHAR(128) NULL');
CALL ensure_dcc_file_owner_column('file_owner_nickname_snapshot','VARCHAR(128) NULL');
CALL ensure_dcc_file_owner_column('file_owner_signature_id','BIGINT NULL');
CALL ensure_dcc_file_owner_column('file_owner_approval_task_id','VARCHAR(128) NULL');
CALL ensure_dcc_file_owner_column('file_owner_process_instance_id','VARCHAR(128) NULL');
CALL ensure_dcc_file_owner_column('file_owner_selected_time','DATETIME NULL');
DROP PROCEDURE IF EXISTS ensure_dcc_file_owner_column;
