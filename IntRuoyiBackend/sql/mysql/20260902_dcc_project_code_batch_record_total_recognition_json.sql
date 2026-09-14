-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=low
-- Purpose: Store the Word batch-record total recognition JSON on the bound DCC project code for quick review/copy.
-- Recovery: Re-run this idempotent migration after restoring the interrupted database backup.
-- Rollback: Drop dcc_project_code.batch_record_total_recognition_json after confirming no UI or integration reads it.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_pc_rec_json_20260902;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_pc_rec_json_20260902()
BEGIN
  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_project_code'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing dcc_project_code for batch record recognition JSON migration';
  END IF;

  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_project_code'
         AND COLUMN_NAME = 'batch_record_total_recognition_json'
  ) THEN
    ALTER TABLE `dcc_project_code`
      ADD COLUMN `batch_record_total_recognition_json` longtext DEFAULT NULL COMMENT 'Word batch-record total recognition JSON' AFTER `last_import_batch_id`;
  END IF;
END$$
DELIMITER ;

CALL ensure_dcc_pc_rec_json_20260902();
DROP PROCEDURE IF EXISTS ensure_dcc_pc_rec_json_20260902;
