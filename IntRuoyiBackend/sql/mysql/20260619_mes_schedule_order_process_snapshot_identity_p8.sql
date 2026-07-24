-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260610_mes_schedule_order_p1; type=schema; riskLevel=low
-- MES 排产工单池 P8：补齐排产工序快照的工序编码与工序名称快照列，并回填已有数据。

SET @schema_name = DATABASE();

SET @schedule_order_process_code_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order_process'
    AND COLUMN_NAME = 'process_code'
);
SET @schedule_order_process_code_sql = IF(
  @schedule_order_process_code_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `process_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT ''工序编码快照'' AFTER `process_id`',
  'SELECT 1'
);
PREPARE schedule_order_process_code_stmt FROM @schedule_order_process_code_sql;
EXECUTE schedule_order_process_code_stmt;
DEALLOCATE PREPARE schedule_order_process_code_stmt;

SET @schedule_order_process_name_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order_process'
    AND COLUMN_NAME = 'process_name'
);
SET @schedule_order_process_name_sql = IF(
  @schedule_order_process_name_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `process_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT ''工序名称快照'' AFTER `process_code`',
  'SELECT 1'
);
PREPARE schedule_order_process_name_stmt FROM @schedule_order_process_name_sql;
EXECUTE schedule_order_process_name_stmt;
DEALLOCATE PREPARE schedule_order_process_name_stmt;

UPDATE `mes_pro_schedule_order_process` process_snapshot
LEFT JOIN `mes_pro_process` process
       ON process.`id` = process_snapshot.`process_id`
      AND process.`deleted` = b'0'
SET process_snapshot.`process_code` = COALESCE(process_snapshot.`process_code`, process.`code`),
    process_snapshot.`process_name` = COALESCE(process_snapshot.`process_name`, process.`name`),
    process_snapshot.`updater` = '1',
    process_snapshot.`update_time` = NOW()
WHERE process_snapshot.`deleted` = b'0'
  AND (process_snapshot.`process_code` IS NULL OR process_snapshot.`process_name` IS NULL);
