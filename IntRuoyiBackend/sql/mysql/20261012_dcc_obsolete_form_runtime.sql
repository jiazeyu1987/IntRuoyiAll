-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261012_dcc_obsolete_form_draft_round; type=schema; riskLevel=medium
-- New form-scoped submitted evidence only. No historical rows or source routes are modified.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_obsolete_form_runtime_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  form_instance_id BIGINT NOT NULL,
  controlled_file_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  form_instance_code VARCHAR(128) NOT NULL,
  controlled_file_version_no VARCHAR(128) NOT NULL,
  bpm_process_instance_id VARCHAR(128) NOT NULL,
  attribute_round INT NOT NULL,
  prepared_process_definition_id VARCHAR(128) NOT NULL,
  prepared_definition_digest CHAR(64) NOT NULL,
  execution_contract VARCHAR(64) NOT NULL,
  canonical_preparation_json LONGTEXT NOT NULL,
  preparation_digest CHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT uk_dcc_obsolete_form UNIQUE(tenant_id,form_instance_id),
  CONSTRAINT uk_dcc_obsolete_bpm UNIQUE(tenant_id,bpm_process_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS dcc_obsolete_form_runtime_obligation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  head_id BIGINT NOT NULL,
  form_instance_id BIGINT NOT NULL,
  controlled_file_id BIGINT NOT NULL,
  bpm_process_instance_id VARCHAR(128) NOT NULL,
  obligation_id VARCHAR(256) NOT NULL,
  stage_code VARCHAR(64) NOT NULL,
  node_instance_id VARCHAR(128) NOT NULL,
  stage_no INT NOT NULL,
  stage_order INT NOT NULL,
  department_id BIGINT NULL,
  candidate_source_type VARCHAR(32) NOT NULL,
  candidate_source_ids_json LONGTEXT NOT NULL,
  frozen_user_ids_json LONGTEXT NOT NULL,
  approve_method VARCHAR(16) NOT NULL,
  approve_ratio INT NULL,
  require_all_approvals BOOLEAN NOT NULL,
  created_at DATETIME(3) NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT uk_dcc_obsolete_obligation UNIQUE(tenant_id,bpm_process_instance_id,obligation_id),
  INDEX idx_dcc_obsolete_form(tenant_id,form_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

DROP PROCEDURE IF EXISTS ensure_dcc_obsolete_form_runtime;
DELIMITER //
CREATE PROCEDURE ensure_dcc_obsolete_form_runtime()
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_bin') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 submitted form evidence requires InnoDB';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot')<>16 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 submitted form evidence column count drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='auto_increment' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='tenant_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 tenant_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='form_instance_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 form_instance_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='controlled_file_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 controlled_file_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='project_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 project_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='applicant_user_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 applicant_user_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='form_instance_code'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=128)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 form_instance_code column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='controlled_file_version_no'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=128)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 controlled_file_version_no column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='bpm_process_instance_id'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=128)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 bpm_process_instance_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='attribute_round'
      AND DATA_TYPE='int' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 attribute_round column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='prepared_process_definition_id'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=128)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 prepared_process_definition_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='prepared_definition_digest'
      AND DATA_TYPE='char' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=64)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 prepared_definition_digest column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='execution_contract'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=64)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 execution_contract column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='canonical_preparation_json'
      AND DATA_TYPE='longtext' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 canonical_preparation_json column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='preparation_digest'
      AND DATA_TYPE='char' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=64)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 preparation_digest column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND COLUMN_NAME='created_at'
      AND DATA_TYPE='datetime' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND DATETIME_PRECISION=3)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 created_at column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND INDEX_NAME='PRIMARY')<>1
      OR COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND INDEX_NAME='PRIMARY'
          AND NON_UNIQUE=0 AND SUB_PART IS NULL),'')<>'id' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 PRIMARY index drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND INDEX_NAME='uk_dcc_obsolete_form')<>2
      OR COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND INDEX_NAME='uk_dcc_obsolete_form'
          AND NON_UNIQUE=0 AND SUB_PART IS NULL),'')<>'tenant_id,form_instance_id' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 uk_dcc_obsolete_form index drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND INDEX_NAME='uk_dcc_obsolete_bpm')<>2
      OR COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_obsolete_form_runtime_snapshot' AND INDEX_NAME='uk_dcc_obsolete_bpm'
          AND NON_UNIQUE=0 AND SUB_PART IS NULL),'')<>'tenant_id,bpm_process_instance_id' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 uk_dcc_obsolete_bpm index drift';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND ENGINE='InnoDB' AND TABLE_COLLATION='utf8mb4_bin') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 submitted form evidence requires InnoDB';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation')<>19 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 submitted form evidence column count drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='auto_increment' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='tenant_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 tenant_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='head_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 head_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='form_instance_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 form_instance_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='controlled_file_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 controlled_file_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='bpm_process_instance_id'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=128)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 bpm_process_instance_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='obligation_id'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=256)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 obligation_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='stage_code'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=64)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 stage_code column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='node_instance_id'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=128)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 node_instance_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='stage_no'
      AND DATA_TYPE='int' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 stage_no column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='stage_order'
      AND DATA_TYPE='int' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 stage_order column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='department_id'
      AND DATA_TYPE='bigint' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 department_id column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='candidate_source_type'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=32)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 candidate_source_type column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='candidate_source_ids_json'
      AND DATA_TYPE='longtext' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 candidate_source_ids_json column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='frozen_user_ids_json'
      AND DATA_TYPE='longtext' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 frozen_user_ids_json column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='approve_method'
      AND DATA_TYPE='varchar' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')=''
      AND CHARACTER_MAXIMUM_LENGTH=16)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 approve_method column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='approve_ratio'
      AND DATA_TYPE='int' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE NOT LIKE '%unsigned%')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 approve_ratio column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='require_all_approvals'
      AND DATA_TYPE='tinyint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND COLUMN_TYPE='tinyint(1)')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 require_all_approvals column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND COLUMN_NAME='created_at'
      AND DATA_TYPE='datetime' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
      AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='' AND DATETIME_PRECISION=3)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 created_at column drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND INDEX_NAME='PRIMARY')<>1
      OR COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND INDEX_NAME='PRIMARY'
          AND NON_UNIQUE=0 AND SUB_PART IS NULL),'')<>'id' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 PRIMARY index drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND INDEX_NAME='uk_dcc_obsolete_obligation')<>3
      OR COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND INDEX_NAME='uk_dcc_obsolete_obligation'
          AND NON_UNIQUE=0 AND SUB_PART IS NULL),'')<>'tenant_id,bpm_process_instance_id,obligation_id' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 uk_dcc_obsolete_obligation index drift';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND INDEX_NAME='idx_dcc_obsolete_form')<>2
      OR COALESCE((SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_obsolete_form_runtime_obligation' AND INDEX_NAME='idx_dcc_obsolete_form'
          AND NON_UNIQUE=1 AND SUB_PART IS NULL),'')<>'tenant_id,form_instance_id' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IC29 idx_dcc_obsolete_form index drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_obsolete_form_runtime();
DROP PROCEDURE IF EXISTS ensure_dcc_obsolete_form_runtime;
