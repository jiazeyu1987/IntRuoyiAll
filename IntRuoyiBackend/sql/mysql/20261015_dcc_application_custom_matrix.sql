-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261015_dcc_category_approval_source_mode,20261012_dcc_application_execution_contract; type=schema; riskLevel=medium
-- Application source facts only. Historical rows remain NULL; public baseline and all original history remain unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_application_custom_column;
DELIMITER //
CREATE PROCEDURE ensure_dcc_application_custom_column(IN field_name VARCHAR(64), IN expected_type VARCHAR(32))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='dcc_controlled_file' AND ENGINE='InnoDB') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CUSTOM_APPLICATION_FILE_TABLE_REQUIRED';
    END IF;
    IF field_name NOT IN ('draft_approval_source_mode','draft_application_custom_matrix_json','draft_application_context_key')
        OR expected_type NOT IN ('varchar(32)','longtext','varchar(128)') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CUSTOM_APPLICATION_COLUMN_CONTRACT_INVALID';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME=field_name) THEN
        SET @dcc_custom_application_ddl=CONCAT('ALTER TABLE `dcc_controlled_file` ADD COLUMN `',field_name,'` ',expected_type,' NULL');
        PREPARE dcc_custom_application_statement FROM @dcc_custom_application_ddl;
        EXECUTE dcc_custom_application_statement;
        DEALLOCATE PREPARE dcc_custom_application_statement;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME=field_name AND COLUMN_TYPE=expected_type
        AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA=''
        AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CUSTOM_APPLICATION_COLUMN_DRIFT';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_application_custom_column('draft_approval_source_mode','varchar(32)');
CALL ensure_dcc_application_custom_column('draft_application_custom_matrix_json','longtext');
CALL ensure_dcc_application_custom_column('draft_application_context_key','varchar(128)');
DROP PROCEDURE IF EXISTS ensure_dcc_application_custom_column;

DROP PROCEDURE IF EXISTS ensure_dcc_custom_route_version_nullable;
DELIMITER //
CREATE PROCEDURE ensure_dcc_custom_route_version_nullable()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file_route_snapshot' AND ENGINE='InnoDB') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CUSTOM_APPLICATION_ROUTE_TABLE_REQUIRED';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file_route_snapshot' AND COLUMN_NAME='route_version_no'
        AND DATA_TYPE='int' AND COLUMN_DEFAULT IS NULL AND EXTRA=''
        AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CUSTOM_APPLICATION_ROUTE_VERSION_DRIFT';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file_route_snapshot' AND COLUMN_NAME='route_version_no' AND IS_NULLABLE='NO') THEN
        ALTER TABLE `dcc_controlled_file_route_snapshot` MODIFY COLUMN `route_version_no` INT NULL;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file_route_snapshot' AND COLUMN_NAME='route_version_no'
        AND DATA_TYPE='int' AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA=''
        AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CUSTOM_APPLICATION_ROUTE_VERSION_NOT_NULLABLE';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_custom_route_version_nullable();
DROP PROCEDURE IF EXISTS ensure_dcc_custom_route_version_nullable;
