-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_mes_schedule_order_freeze_audit; type=schema; riskLevel=low
-- 将 MES 排产工单操作日志 reason 调整为可空，匹配“手动重排 apply 业务原因可选”契约。

SET @schema_name = DATABASE();

SET @table_exists = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order_operation_log'
);

SET @table_guard_sql = IF(
  @table_exists = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''mes replan operation log reason nullable migration missing mes_pro_schedule_order_operation_log'''
);
PREPARE table_guard_stmt FROM @table_guard_sql;
EXECUTE table_guard_stmt;
DEALLOCATE PREPARE table_guard_stmt;

SET @reason_nullable_count = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order_operation_log'
    AND COLUMN_NAME = 'reason'
    AND IS_NULLABLE = 'YES'
);

SET @reason_nullable_sql = IF(
  @reason_nullable_count = 0,
  'ALTER TABLE `mes_pro_schedule_order_operation_log` MODIFY COLUMN `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT ''操作原因''',
  'SELECT 1'
);
PREPARE reason_nullable_stmt FROM @reason_nullable_sql;
EXECUTE reason_nullable_stmt;
DEALLOCATE PREPARE reason_nullable_stmt;
