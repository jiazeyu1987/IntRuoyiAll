-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_dcc_file_category_batch_task; type=schema; riskLevel=medium
-- Enforce one active DCC batch recognition task per tenant and recognition type.
-- Safe to run repeatedly. Existing duplicate active tasks fail fast and require explicit operator resolution.

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_dcc_batch_recognition_active_task_unique_guard $$
CREATE PROCEDURE upgrade_dcc_batch_recognition_active_task_unique_guard()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM `dcc_controlled_file_batch_recognition_task`
         WHERE `deleted` = b'0'
           AND `status` IN ('WAITING', 'RUNNING')
         GROUP BY `tenant_id`, `recognition_type`
        HAVING COUNT(*) > 1
         LIMIT 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'duplicate active DCC batch recognition tasks must be resolved before migration';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
           AND COLUMN_NAME = 'active_recognition_type'
    ) THEN
        ALTER TABLE `dcc_controlled_file_batch_recognition_task`
            ADD COLUMN `active_recognition_type` varchar(32)
                GENERATED ALWAYS AS (
                    CASE
                        WHEN `deleted` = b'0' AND `status` IN ('WAITING', 'RUNNING')
                            THEN `recognition_type`
                        ELSE NULL
                    END
                ) STORED
                COMMENT 'Generated active recognition type for tenant-level unique guard'
                AFTER `tenant_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
           AND INDEX_NAME = 'uk_dcc_batch_recognition_task_active_type'
    ) THEN
        ALTER TABLE `dcc_controlled_file_batch_recognition_task`
            ADD UNIQUE KEY `uk_dcc_batch_recognition_task_active_type`
                (`tenant_id`, `active_recognition_type`);
    END IF;
END $$

CALL upgrade_dcc_batch_recognition_active_task_unique_guard() $$
DROP PROCEDURE IF EXISTS upgrade_dcc_batch_recognition_active_task_unique_guard $$

DELIMITER ;
