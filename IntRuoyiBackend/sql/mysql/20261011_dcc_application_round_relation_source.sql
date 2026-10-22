-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261001_dcc_b_reserved_draft_round,20261011_dcc_relation_stage_snapshots; type=schema; riskLevel=medium
-- IC14 nullable source facts only. No historical backfill, File BPM or old signed evidence mutation.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_round_relation_source;
DELIMITER //
CREATE PROCEDURE ensure_dcc_round_relation_source()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 requires its actual existing InnoDB table';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_application_type') THEN
    ALTER TABLE dcc_application_round_link ADD COLUMN source_application_type VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_application_type' AND LOWER(COLUMN_TYPE)='varchar(16)' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 nullable source column definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_application_type' AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 source identity collation drift';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_application_id') THEN
    ALTER TABLE dcc_application_round_link ADD COLUMN source_application_id BIGINT NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_application_id' AND LOWER(COLUMN_TYPE)='bigint' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 nullable source column definition drift';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_bpm_round') THEN
    ALTER TABLE dcc_application_round_link ADD COLUMN source_bpm_round VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_bpm_round' AND LOWER(COLUMN_TYPE)='varchar(64)' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 nullable source column definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_bpm_round' AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 source identity collation drift';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_relation_digest') THEN
    ALTER TABLE dcc_application_round_link ADD COLUMN source_relation_digest CHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_relation_digest' AND LOWER(COLUMN_TYPE)='char(64)' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 nullable source column definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_application_round_link' AND COLUMN_NAME='source_relation_digest' AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 source identity collation drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_round_relation_source();
DROP PROCEDURE ensure_dcc_round_relation_source;
