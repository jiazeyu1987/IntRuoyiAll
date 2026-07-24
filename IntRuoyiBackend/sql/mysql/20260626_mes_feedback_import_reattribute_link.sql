-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_mes_feedback_surplus_pool; type=schema; riskLevel=medium
-- MES 报工归属：正式报工补充来源导入记录关联

SET @feedback_source_import_record_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback'
      AND COLUMN_NAME = 'source_import_record_id'
);
SET @feedback_source_import_record_id_sql = IF(
    @feedback_source_import_record_id_exists = 0,
    'ALTER TABLE `mes_pro_feedback` ADD COLUMN `source_import_record_id` bigint DEFAULT NULL COMMENT ''来源导入记录ID'' AFTER `schedule_order_process_id`',
    'SELECT 1'
);
PREPARE feedback_source_import_record_id_stmt FROM @feedback_source_import_record_id_sql;
EXECUTE feedback_source_import_record_id_stmt;
DEALLOCATE PREPARE feedback_source_import_record_id_stmt;

SET @feedback_source_import_record_idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback'
      AND INDEX_NAME = 'idx_mes_pro_feedback_source_import_record_id'
);
SET @feedback_source_import_record_idx_sql = IF(
    @feedback_source_import_record_idx_exists = 0,
    'ALTER TABLE `mes_pro_feedback` ADD KEY `idx_mes_pro_feedback_source_import_record_id` (`source_import_record_id`)',
    'SELECT 1'
);
PREPARE feedback_source_import_record_idx_stmt FROM @feedback_source_import_record_idx_sql;
EXECUTE feedback_source_import_record_idx_stmt;
DEALLOCATE PREPARE feedback_source_import_record_idx_stmt;

UPDATE `mes_pro_feedback` feedback
LEFT JOIN `mes_pro_feedback_import_record` record
  ON record.`feedback_id` = feedback.`id`
 AND record.`deleted` = b'0'
SET feedback.`source_import_record_id` = record.`id`
WHERE feedback.`deleted` = b'0'
  AND feedback.`source_import_record_id` IS NULL
  AND record.`id` IS NOT NULL;
