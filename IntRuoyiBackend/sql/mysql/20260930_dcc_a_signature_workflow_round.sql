-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_lifecycle; type=schema; riskLevel=medium
-- Freeze each new v4 signature workflow round and file number; historical evidence rows are untouched.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS dcc_a_signature_workflow_round;
DELIMITER $$
CREATE PROCEDURE dcc_a_signature_workflow_round()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_signature' AND COLUMN_NAME='process_instance_id') THEN
    ALTER TABLE dcc_controlled_file_signature ADD COLUMN process_instance_id VARCHAR(128) NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_signature' AND COLUMN_NAME='file_number_snapshot') THEN
    ALTER TABLE dcc_controlled_file_signature ADD COLUMN file_number_snapshot VARCHAR(128) NULL;
  END IF;
END$$
DELIMITER ;
CALL dcc_a_signature_workflow_round();
DROP PROCEDURE dcc_a_signature_workflow_round;
