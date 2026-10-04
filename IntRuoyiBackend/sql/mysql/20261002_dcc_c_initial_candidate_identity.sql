-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_c_revision_identity; type=schema; riskLevel=medium
-- CC-2 keeps the original initial-number draft and the selected-body INITIAL candidate as distinct records.
-- Only explicitly recorded INITIAL + selected iteration facts get a candidate role key. No version/name/history DML.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_c_initial_candidate_key;
DELIMITER //
CREATE PROCEDURE ensure_dcc_c_initial_candidate_key()
BEGIN
 DECLARE key_expression LONGTEXT;
 SELECT GENERATION_EXPRESSION INTO key_expression
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='c_version_key';
 IF key_expression IS NULL OR key_expression='' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C revision identity migration is required before INITIAL candidates';
 END IF;
 IF LOCATE('selected_iteration_controlled_file_id',key_expression)=0 THEN
  ALTER TABLE dcc_controlled_file MODIFY COLUMN c_version_key VARBINARY(288)
   GENERATED ALWAYS AS (CASE WHEN source_original_file_name IS NOT NULL
    THEN CONVERT(CASE WHEN revision_change_type='INITIAL' AND selected_iteration_controlled_file_id IS NOT NULL
     THEN CONCAT(version_no,'#INITIAL') ELSE version_no END USING binary) ELSE NULL END) STORED;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
                AND TABLE_NAME='dcc_controlled_file' AND INDEX_NAME='uk_dcc_c_version') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C version uniqueness constraint is required';
 END IF;
END//
DELIMITER ;
CALL ensure_dcc_c_initial_candidate_key();
DROP PROCEDURE IF EXISTS ensure_dcc_c_initial_candidate_key;
