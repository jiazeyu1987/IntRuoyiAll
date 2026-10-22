-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261014_dcc_delayed_effective_confirmation,20260930_dcc_d_relations; type=schema; riskLevel=medium
-- Keep controlled event facts at the same microsecond precision as controlled_file.controlled_time.
-- Widen storage only; no historical timestamp, signature, event or application is rewritten.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_controlled_event_micros;
DELIMITER //
CREATE PROCEDURE ensure_dcc_controlled_event_micros(IN table_value VARCHAR(64), IN column_value VARCHAR(64))
BEGIN
    IF NOT ((table_value='dcc_workflow_lifecycle_event' AND column_value='occurred_at')
         OR (table_value='dcc_relation_controlled_event' AND column_value='controlled_at')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_CONTROLLED_EVENT_TIME_SCOPE_INVALID';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME=table_value AND ENGINE='InnoDB')<>1
       OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
           AND TABLE_NAME=table_value AND COLUMN_NAME=column_value AND DATA_TYPE='datetime'
           AND DATETIME_PRECISION IN (0,6) AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
           AND EXTRA='' AND COALESCE(GENERATION_EXPRESSION,'')='')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_CONTROLLED_EVENT_TIME_SCHEMA_DRIFT';
    END IF;
    IF (SELECT DATETIME_PRECISION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME=table_value AND COLUMN_NAME=column_value)=0 THEN
        SET @dcc_event_time_ddl=CONCAT('ALTER TABLE `',table_value,'` MODIFY COLUMN `',column_value,'` DATETIME(6) NOT NULL');
        PREPARE dcc_event_time_statement FROM @dcc_event_time_ddl;
        EXECUTE dcc_event_time_statement;
        DEALLOCATE PREPARE dcc_event_time_statement;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
        AND TABLE_NAME=table_value AND COLUMN_NAME=column_value AND DATA_TYPE='datetime'
        AND DATETIME_PRECISION=6 AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL AND EXTRA='')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_CONTROLLED_EVENT_TIME_POSTCONDITION_FAILED';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_controlled_event_micros('dcc_workflow_lifecycle_event','occurred_at');
CALL ensure_dcc_controlled_event_micros('dcc_relation_controlled_event','controlled_at');
DROP PROCEDURE IF EXISTS ensure_dcc_controlled_event_micros;
