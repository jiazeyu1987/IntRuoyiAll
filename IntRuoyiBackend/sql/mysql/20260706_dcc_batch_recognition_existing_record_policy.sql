-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260706_dcc_recognition_traceable_failure_messages; type=schema; riskLevel=medium
-- Add explicit existing-record policy for DCC batch recognition tasks.

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_dcc_batch_recognition_existing_record_policy $$
CREATE PROCEDURE upgrade_dcc_batch_recognition_existing_record_policy()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
           AND COLUMN_NAME = 'existing_record_policy'
    ) THEN
        ALTER TABLE `dcc_controlled_file_batch_recognition_task`
            ADD COLUMN `existing_record_policy` varchar(32) NOT NULL DEFAULT 'SKIP_ALL_EXISTING'
            COMMENT 'SKIP_ALL_EXISTING, RETRY_FAILED, OVERWRITE_ALL'
            AFTER `overwrite_existing`;
    END IF;

    UPDATE `dcc_controlled_file_batch_recognition_task`
       SET `existing_record_policy` = 'OVERWRITE_ALL'
     WHERE `overwrite_existing` = b'1'
       AND (`existing_record_policy` IS NULL OR `existing_record_policy` = 'SKIP_ALL_EXISTING');

    UPDATE `dcc_controlled_file_batch_recognition_task`
       SET `existing_record_policy` = 'SKIP_ALL_EXISTING'
     WHERE `existing_record_policy` IS NULL
        OR `existing_record_policy` NOT IN ('SKIP_ALL_EXISTING', 'RETRY_FAILED', 'OVERWRITE_ALL');
END $$

CALL upgrade_dcc_batch_recognition_existing_record_policy() $$
DROP PROCEDURE IF EXISTS upgrade_dcc_batch_recognition_existing_record_policy $$

DELIMITER ;
