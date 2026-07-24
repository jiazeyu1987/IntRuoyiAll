-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260701_dcc_batch_recognition_worker_ledger_export; type=schema; riskLevel=medium
-- Expand DCC recognition failure diagnostics so invalid candidates and backend errors remain traceable.

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_dcc_recognition_failure_messages $$
CREATE PROCEDURE upgrade_dcc_recognition_failure_messages()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
           AND COLUMN_NAME = 'last_failure_message'
           AND (DATA_TYPE <> 'varchar' OR CHARACTER_MAXIMUM_LENGTH < 2048)
    ) THEN
        ALTER TABLE `dcc_controlled_file_batch_recognition_task`
            MODIFY COLUMN `last_failure_message` varchar(2048) DEFAULT NULL COMMENT 'Last failure reason';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'dcc_controlled_file_recognition_record'
           AND COLUMN_NAME = 'failure_message'
           AND (DATA_TYPE <> 'varchar' OR CHARACTER_MAXIMUM_LENGTH < 2048)
    ) THEN
        ALTER TABLE `dcc_controlled_file_recognition_record`
            MODIFY COLUMN `failure_message` varchar(2048) DEFAULT NULL COMMENT 'Failure reason';
    END IF;
END $$

CALL upgrade_dcc_recognition_failure_messages() $$
DROP PROCEDURE IF EXISTS upgrade_dcc_recognition_failure_messages $$

DELIMITER ;
