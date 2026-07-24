-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260623_dcc_browser_batch_recognition_task; type=schema; riskLevel=medium
-- Repair existing DCC batch recognition task tables created before recognition_version_snapshot was introduced.
-- MySQL 8.0 compatibility: use information_schema guards instead of ADD COLUMN IF NOT EXISTS.

SET @dcc_batch_recognition_version_snapshot_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
              AND COLUMN_NAME = 'recognition_version_snapshot'
        ),
        'SELECT 1',
        'ALTER TABLE `dcc_controlled_file_batch_recognition_task` ADD COLUMN `recognition_version_snapshot` varchar(64) NOT NULL DEFAULT ''v1'' COMMENT ''识别版本快照'' AFTER `scope_type`'
    )
);
PREPARE stmt FROM @dcc_batch_recognition_version_snapshot_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
