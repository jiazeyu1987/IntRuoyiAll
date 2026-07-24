-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260706_dcc_batch_recognition_existing_record_policy; type=schema; riskLevel=medium
-- Extend DCC batch recognition tasks with independent file-category task semantics and counters.
-- Safe to run repeatedly on MySQL runtime schemas.

SET @dcc_batch_recognition_type_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
              AND COLUMN_NAME = 'recognition_type'
        ),
        'SELECT 1',
        'ALTER TABLE `dcc_controlled_file_batch_recognition_task` ADD COLUMN `recognition_type` varchar(32) NOT NULL DEFAULT ''BASIC_INFO'' COMMENT ''BASIC_INFO or FILE_CATEGORY'' AFTER `operator_user_id`'
    )
);
PREPARE stmt FROM @dcc_batch_recognition_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @dcc_batch_unclassified_count_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
              AND COLUMN_NAME = 'unclassified_count'
        ),
        'SELECT 1',
        'ALTER TABLE `dcc_controlled_file_batch_recognition_task` ADD COLUMN `unclassified_count` bigint NOT NULL DEFAULT 0 AFTER `skipped_existing_count`'
    )
);
PREPARE stmt FROM @dcc_batch_unclassified_count_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @dcc_batch_ambiguous_count_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
              AND COLUMN_NAME = 'ambiguous_count'
        ),
        'SELECT 1',
        'ALTER TABLE `dcc_controlled_file_batch_recognition_task` ADD COLUMN `ambiguous_count` bigint NOT NULL DEFAULT 0 AFTER `unclassified_count`'
    )
);
PREPARE stmt FROM @dcc_batch_ambiguous_count_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @dcc_batch_conflict_count_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
              AND COLUMN_NAME = 'conflict_count'
        ),
        'SELECT 1',
        'ALTER TABLE `dcc_controlled_file_batch_recognition_task` ADD COLUMN `conflict_count` bigint NOT NULL DEFAULT 0 AFTER `ambiguous_count`'
    )
);
PREPARE stmt FROM @dcc_batch_conflict_count_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @dcc_batch_type_status_index_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'
              AND INDEX_NAME = 'idx_dcc_batch_recognition_task_type_status'
        ),
        'SELECT 1',
        'CREATE INDEX `idx_dcc_batch_recognition_task_type_status` ON `dcc_controlled_file_batch_recognition_task` (`recognition_type`, `status`, `id`)'
    )
);
PREPARE stmt FROM @dcc_batch_type_status_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
