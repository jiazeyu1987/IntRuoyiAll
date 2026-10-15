-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261011_dcc_application_effective_distribution,20261012_dcc_application_execution_contract,20260930_dcc_a_signature_workflow_round,20260908_system_electronic_signature_t3; type=schema; riskLevel=medium
-- Nullable immutable new confirmation facts only. No historical mode/definition/signature backfill.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_delay_column;
-- C05: widen existing timestamps only; no historical value rewrite and no precision inference.
DROP PROCEDURE IF EXISTS ensure_dcc_signed_micros;
DELIMITER //
CREATE PROCEDURE ensure_dcc_signed_micros(IN table_name_value VARCHAR(64),IN field_name VARCHAR(64),IN null_clause VARCHAR(16))
BEGIN
  IF null_clause NOT IN ('NULL','NOT NULL') OR NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_value AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C05 requires exact InnoDB table and declared timestamp nullability';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_value AND COLUMN_NAME=field_name
      AND DATA_TYPE='datetime' AND COALESCE(DATETIME_PRECISION,0) IN (0,6) AND IS_NULLABLE=IF(null_clause='NULL','YES','NO')
      AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C05 signed timestamp schema drift';
  END IF;
  IF (SELECT COALESCE(DATETIME_PRECISION,0) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_value AND COLUMN_NAME=field_name)=0 THEN
    SET @dcc_micros_ddl=CONCAT('ALTER TABLE ',table_name_value,' MODIFY COLUMN ',field_name,' DATETIME(6) ',null_clause);
    PREPARE dcc_micros_statement FROM @dcc_micros_ddl;EXECUTE dcc_micros_statement;DEALLOCATE PREPARE dcc_micros_statement;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_value AND COLUMN_NAME=field_name
      AND DATA_TYPE='datetime' AND DATETIME_PRECISION=6 AND IS_NULLABLE=IF(null_clause='NULL','YES','NO') AND COLUMN_DEFAULT IS NULL AND EXTRA='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C05 signed timestamp postcondition failed';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_signed_micros('system_electronic_signature','signed_at','NOT NULL');
CALL ensure_dcc_signed_micros('dcc_controlled_file_signature','signed_at','NULL');
CALL ensure_dcc_signed_micros('dcc_controlled_file','controlled_time','NULL');
DROP PROCEDURE IF EXISTS ensure_dcc_signed_micros;
DELIMITER //
CREATE PROCEDURE ensure_dcc_delay_column(IN table_name_value VARCHAR(64),IN field_name VARCHAR(64),IN field_definition VARCHAR(255),IN expected_type VARCHAR(64))
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_value AND ENGINE='InnoDB') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G01 requires exact InnoDB confirmation tables';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_value AND COLUMN_NAME=field_name) THEN
    SET @dcc_delay_ddl=CONCAT('ALTER TABLE ',table_name_value,' ADD COLUMN ',field_name,' ',field_definition);
    PREPARE dcc_delay_statement FROM @dcc_delay_ddl;EXECUTE dcc_delay_statement;DEALLOCATE PREPARE dcc_delay_statement;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_value AND COLUMN_NAME=field_name
      AND COLUMN_TYPE=expected_type AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G01 confirmation nullable column drift';
  END IF;
END//
DELIMITER ;
CALL ensure_dcc_delay_column('dcc_controlled_file','delayed_effective_time','DATETIME(6) NULL','datetime(6)');
CALL ensure_dcc_delay_column('dcc_controlled_file','delayed_effective_confirmed_at','DATETIME(6) NULL','datetime(6)');
CALL ensure_dcc_delay_column('dcc_controlled_file','delayed_effective_signature_id','BIGINT NULL','bigint');
CALL ensure_dcc_delay_column('dcc_controlled_file','delayed_effective_process_instance_id','VARCHAR(128) NULL','varchar(128)');
CALL ensure_dcc_delay_column('dcc_controlled_file_signature','selected_effective_time','DATETIME(6) NULL','datetime(6)');
CALL ensure_dcc_delay_column('dcc_controlled_file_signature','attribute_round','INT NULL','int');
DROP PROCEDURE IF EXISTS ensure_dcc_delay_column;
