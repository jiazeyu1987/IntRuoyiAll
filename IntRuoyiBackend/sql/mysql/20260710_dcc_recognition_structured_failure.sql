-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260706_dcc_recognition_traceable_failure_messages; type=schema; riskLevel=medium
-- Add stable failure stage and code fields without guessing historical messages.

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_dcc_recognition_structured_failure $$
CREATE PROCEDURE upgrade_dcc_recognition_structured_failure()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'dcc_controlled_file_recognition_record'
           AND COLUMN_NAME = 'failure_stage'
    ) THEN
        ALTER TABLE `dcc_controlled_file_recognition_record`
            ADD COLUMN `failure_stage` varchar(64) DEFAULT NULL
            COMMENT 'Structured recognition failure stage'
            AFTER `match_text`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'dcc_controlled_file_recognition_record'
           AND COLUMN_NAME = 'failure_code'
    ) THEN
        ALTER TABLE `dcc_controlled_file_recognition_record`
            ADD COLUMN `failure_code` varchar(64) DEFAULT NULL
            COMMENT 'Structured recognition failure code'
            AFTER `failure_stage`;
    END IF;
END $$

CALL upgrade_dcc_recognition_structured_failure() $$
DROP PROCEDURE IF EXISTS upgrade_dcc_recognition_structured_failure $$

DELIMITER ;
