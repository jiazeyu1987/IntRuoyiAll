-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261011_dcc_relation_stage_snapshots,20261001_dcc_application_round_link,20260717_bpm_form_center; type=schema; riskLevel=medium
-- IC14 nullable source facts only. No historical backfill, File BPM or old signed evidence mutation.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_stage_app_identity;
DELIMITER //
CREATE PROCEDURE ensure_dcc_stage_app_identity()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 requires its actual existing InnoDB table';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME='application_type') THEN
    ALTER TABLE dcc_relation_stage_snapshot ADD COLUMN application_type VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME='application_type' AND LOWER(COLUMN_TYPE)='varchar(16)' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 nullable source column definition drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME='application_type' AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_bin')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 source identity collation drift';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME='attribute_round') THEN
    ALTER TABLE dcc_relation_stage_snapshot ADD COLUMN attribute_round INT NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME='attribute_round' AND LOWER(COLUMN_TYPE)='int' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 nullable source column definition drift';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME='form_action_instance_id') THEN
    ALTER TABLE dcc_relation_stage_snapshot ADD COLUMN form_action_instance_id BIGINT NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_relation_stage_snapshot' AND COLUMN_NAME='form_action_instance_id' AND LOWER(COLUMN_TYPE)='bigint' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC14 nullable source column definition drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_stage_app_identity();
DROP PROCEDURE ensure_dcc_stage_app_identity;
