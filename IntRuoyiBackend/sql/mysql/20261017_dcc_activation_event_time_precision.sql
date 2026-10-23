-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261017_dcc_controlled_event_time_precision; type=schema; riskLevel=medium
-- The activation fact must retain the precision used by its lifecycle event and distribution batch.
-- Widen storage only; preserve every historical file, signature and timestamp value.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_activation_micros;
DELIMITER //
CREATE PROCEDURE ensure_dcc_activation_micros()
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file' AND ENGINE='InnoDB')<>1
       OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
           AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='activated_time' AND DATA_TYPE='datetime'
           AND DATETIME_PRECISION IN (0,6) AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL
           AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_ACTIVATION_TIME_SCHEMA_DRIFT';
    END IF;
    IF (SELECT DATETIME_PRECISION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='activated_time')=0 THEN
        ALTER TABLE dcc_controlled_file MODIFY COLUMN activated_time DATETIME(6) NULL;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='activated_time' AND DATA_TYPE='datetime'
        AND DATETIME_PRECISION=6 AND IS_NULLABLE='YES' AND COLUMN_DEFAULT IS NULL AND EXTRA='')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_ACTIVATION_TIME_POSTCONDITION_FAILED';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_activation_micros();
DROP PROCEDURE IF EXISTS ensure_dcc_activation_micros;
