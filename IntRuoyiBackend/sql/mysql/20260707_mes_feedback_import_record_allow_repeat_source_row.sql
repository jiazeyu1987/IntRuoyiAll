-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- 允许直接报工导入同一 Excel 源行多次生成报工，保留源行普通索引用于查询。
SET @schema_name := DATABASE();

SELECT COUNT(1) INTO @source_row_unique_exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME = 'mes_pro_feedback_import_record'
  AND INDEX_NAME = 'uk_mes_pro_feedback_import_record_source_row';

SET @drop_source_row_unique_sql = IF(
    @source_row_unique_exists > 0,
    'ALTER TABLE `mes_pro_feedback_import_record` DROP INDEX `uk_mes_pro_feedback_import_record_source_row`',
    'SELECT 1'
);
PREPARE drop_source_row_unique_stmt FROM @drop_source_row_unique_sql;
EXECUTE drop_source_row_unique_stmt;
DEALLOCATE PREPARE drop_source_row_unique_stmt;

SELECT COUNT(1) INTO @source_row_index_exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME = 'mes_pro_feedback_import_record'
  AND INDEX_NAME = 'idx_mes_pro_feedback_import_record_source_row';

SET @create_source_row_index_sql = IF(
    @source_row_index_exists = 0,
    'CREATE INDEX `idx_mes_pro_feedback_import_record_source_row` ON `mes_pro_feedback_import_record` (`source_file_sha256`, `sheet_name`, `row_no`)',
    'SELECT 1'
);
PREPARE create_source_row_index_stmt FROM @create_source_row_index_sql;
EXECUTE create_source_row_index_stmt;
DEALLOCATE PREPARE create_source_row_index_stmt;
