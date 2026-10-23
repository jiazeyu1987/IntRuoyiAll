-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261002_dcc_c_initial_candidate_identity; type=schema; riskLevel=medium
-- Exact failed-attempt rework only. No historical file/application/BPM/body/name/signature DML.
-- Existing rows keep their original generated key because revision_attempt_no is NULL.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_revision_attempt_identity;
DELIMITER //
CREATE PROCEDURE ensure_dcc_revision_attempt_identity()
BEGIN
 DECLARE key_expression LONGTEXT;
 IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='revision_attempt_no') THEN
  ALTER TABLE dcc_controlled_file ADD COLUMN revision_attempt_no INT NULL;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='rework_predecessor_controlled_file_id') THEN
  ALTER TABLE dcc_controlled_file ADD COLUMN rework_predecessor_controlled_file_id BIGINT NULL;
 END IF;
 SELECT GENERATION_EXPRESSION INTO key_expression FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='c_version_key';
 IF key_expression IS NULL OR LOCATE('selected_iteration_controlled_file_id',key_expression)=0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='INITIAL candidate identity migration is required before failed revision attempts';
 END IF;
 IF LOCATE('revision_attempt_no',key_expression)=0 THEN
  ALTER TABLE dcc_controlled_file MODIFY COLUMN c_version_key VARBINARY(320)
   GENERATED ALWAYS AS (CASE WHEN source_original_file_name IS NOT NULL THEN
    CONVERT(CONCAT(CASE WHEN revision_change_type='INITIAL' AND selected_iteration_controlled_file_id IS NOT NULL
       THEN CONCAT(version_no,'#INITIAL') ELSE version_no END,
     CASE WHEN revision_attempt_no>1 THEN CONCAT('#ATTEMPT:',revision_attempt_no) ELSE '' END) USING binary)
    ELSE NULL END) STORED;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND INDEX_NAME='uk_dcc_c_version') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Version uniqueness constraint is required';
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND INDEX_NAME='uk_dcc_c_rework_predecessor') THEN
  ALTER TABLE dcc_controlled_file ADD UNIQUE KEY uk_dcc_c_rework_predecessor(tenant_id,master_id,rework_predecessor_controlled_file_id);
 END IF;
END//
DELIMITER ;
CALL ensure_dcc_revision_attempt_identity();
DROP PROCEDURE IF EXISTS ensure_dcc_revision_attempt_identity;
