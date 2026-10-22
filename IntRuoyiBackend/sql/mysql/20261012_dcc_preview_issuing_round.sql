-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260528_dcc_controlled_file_protection; type=schema; riskLevel=medium
-- New preview events may record their verified issuing approval. Historical events remain NULL.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_preview_issuing_round;
DELIMITER //
CREATE PROCEDURE ensure_dcc_preview_issuing_round()
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_access_event' AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC27 requires the formal InnoDB access event table';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_access_event' AND COLUMN_NAME='process_instance_id') THEN
    ALTER TABLE dcc_controlled_file_access_event ADD COLUMN process_instance_id
      VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_controlled_file_access_event' AND COLUMN_NAME='process_instance_id'
      AND COLUMN_TYPE='varchar(128)' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_SET_NAME='ascii' AND COLLATION_NAME='ascii_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC27 issuing round column has schema drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_preview_issuing_round();
DROP PROCEDURE IF EXISTS ensure_dcc_preview_issuing_round;
