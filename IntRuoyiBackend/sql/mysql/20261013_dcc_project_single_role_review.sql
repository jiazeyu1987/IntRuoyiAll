-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261003_dcc_project_reviewer_configuration; type=schema; riskLevel=medium
-- New requests explicitly record ROLE_SINGLE_REVIEW_V1 and their real reviewer role identity.
-- Existing requests retain NULL in the new fields and their original frozen two-stage approval facts.
-- Rollback: revert new application entry first; preserve populated contract/role columns for audit.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_project_single_role_review;
DELIMITER //
CREATE PROCEDURE ensure_dcc_project_single_role_review()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_project_product_create_request' AND ENGINE='InnoDB')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC project application table missing or engine drift';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_project_product_create_request' AND COLUMN_NAME='review_contract') THEN
    ALTER TABLE dcc_project_product_create_request ADD COLUMN review_contract VARCHAR(32) NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_project_product_create_request' AND COLUMN_NAME='reviewer_role_id') THEN
    ALTER TABLE dcc_project_product_create_request ADD COLUMN reviewer_role_id BIGINT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_project_product_create_request' AND COLUMN_NAME='reviewer_role_code') THEN
    ALTER TABLE dcc_project_product_create_request ADD COLUMN reviewer_role_code VARCHAR(128) NULL;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_project_product_create_request' AND COLUMN_NAME='review_contract'
      AND DATA_TYPE='varchar' AND CHARACTER_MAXIMUM_LENGTH=32 AND IS_NULLABLE='YES'
      AND COLUMN_DEFAULT IS NULL AND COALESCE(EXTRA,'')='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_project_product_create_request' AND COLUMN_NAME='reviewer_role_id'
      AND LOWER(COLUMN_TYPE)='bigint' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL
      AND COALESCE(EXTRA,'')='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1
     OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_project_product_create_request' AND COLUMN_NAME='reviewer_role_code'
      AND DATA_TYPE='varchar' AND CHARACTER_MAXIMUM_LENGTH=128 AND IS_NULLABLE='YES'
      AND COLUMN_DEFAULT IS NULL AND COALESCE(EXTRA,'')='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC project review contract column definition drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_project_single_role_review();
DROP PROCEDURE ensure_dcc_project_single_role_review;
