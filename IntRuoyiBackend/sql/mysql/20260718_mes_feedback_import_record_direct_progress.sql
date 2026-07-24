-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260707_mes_feedback_import_record_allow_repeat_source_row; type=schema; riskLevel=medium
-- purpose: store applied direct work report Excel progress audit rows without creating MES feedback records.

SET @progress_source_type_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_pro_feedback_import_record'
    AND COLUMN_NAME = 'progress_source_type'
);
SET @add_progress_source_type_sql := IF(
  @progress_source_type_exists = 0,
  'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `progress_source_type` varchar(64) DEFAULT NULL COMMENT ''Progress source type''',
  'SELECT 1'
);
PREPARE add_progress_source_type_stmt FROM @add_progress_source_type_sql;
EXECUTE add_progress_source_type_stmt;
DEALLOCATE PREPARE add_progress_source_type_stmt;

SET @progress_quantity_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_pro_feedback_import_record'
    AND COLUMN_NAME = 'progress_quantity'
);
SET @add_progress_quantity_sql := IF(
  @progress_quantity_exists = 0,
  'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `progress_quantity` decimal(18,6) DEFAULT NULL COMMENT ''Applied progress quantity''',
  'SELECT 1'
);
PREPARE add_progress_quantity_stmt FROM @add_progress_quantity_sql;
EXECUTE add_progress_quantity_stmt;
DEALLOCATE PREPARE add_progress_quantity_stmt;

SET @progress_applied_time_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_pro_feedback_import_record'
    AND COLUMN_NAME = 'progress_applied_time'
);
SET @add_progress_applied_time_sql := IF(
  @progress_applied_time_exists = 0,
  'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `progress_applied_time` datetime DEFAULT NULL COMMENT ''Applied progress time''',
  'SELECT 1'
);
PREPARE add_progress_applied_time_stmt FROM @add_progress_applied_time_sql;
EXECUTE add_progress_applied_time_stmt;
DEALLOCATE PREPARE add_progress_applied_time_stmt;

SET @progress_warning_code_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_pro_feedback_import_record'
    AND COLUMN_NAME = 'progress_warning_code'
);
SET @add_progress_warning_code_sql := IF(
  @progress_warning_code_exists = 0,
  'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `progress_warning_code` varchar(64) DEFAULT NULL COMMENT ''Progress warning code''',
  'SELECT 1'
);
PREPARE add_progress_warning_code_stmt FROM @add_progress_warning_code_sql;
EXECUTE add_progress_warning_code_stmt;
DEALLOCATE PREPARE add_progress_warning_code_stmt;

SET @progress_warning_message_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_pro_feedback_import_record'
    AND COLUMN_NAME = 'progress_warning_message'
);
SET @add_progress_warning_message_sql := IF(
  @progress_warning_message_exists = 0,
  'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `progress_warning_message` varchar(500) DEFAULT NULL COMMENT ''Progress warning message''',
  'SELECT 1'
);
PREPARE add_progress_warning_message_stmt FROM @add_progress_warning_message_sql;
EXECUTE add_progress_warning_message_stmt;
DEALLOCATE PREPARE add_progress_warning_message_stmt;

SET @direct_progress_index_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_pro_feedback_import_record'
    AND INDEX_NAME = 'idx_mes_feedback_import_record_direct_progress'
);

SET @create_direct_progress_index_sql := IF(
  @direct_progress_index_exists = 0,
  'CREATE INDEX `idx_mes_feedback_import_record_direct_progress` ON `mes_pro_feedback_import_record` (`tenant_id`, `schedule_order_id`, `progress_source_type`, `attribution_status`, `schedule_order_process_id`)',
  'SELECT 1'
);

PREPARE create_direct_progress_index_stmt FROM @create_direct_progress_index_sql;
EXECUTE create_direct_progress_index_stmt;
DEALLOCATE PREPARE create_direct_progress_index_stmt;
