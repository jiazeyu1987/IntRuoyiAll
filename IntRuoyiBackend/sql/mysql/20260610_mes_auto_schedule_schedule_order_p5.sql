-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 排产 P5：自动排程接入排产工单
-- MySQL 8.0 对 ADD COLUMN IF NOT EXISTS 兼容性不稳定，改为 information_schema 防重执行

SET @schedule_order_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_task_schedule_ext'
      AND COLUMN_NAME = 'schedule_order_id'
);
SET @schedule_order_id_sql = IF(
    @schedule_order_id_exists = 0,
    'ALTER TABLE `mes_pro_task_schedule_ext` ADD COLUMN `schedule_order_id` bigint DEFAULT NULL COMMENT ''排产工单ID'' AFTER `task_id`',
    'SELECT 1'
);
PREPARE schedule_order_id_stmt FROM @schedule_order_id_sql;
EXECUTE schedule_order_id_stmt;
DEALLOCATE PREPARE schedule_order_id_stmt;

SET @schedule_order_process_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_task_schedule_ext'
      AND COLUMN_NAME = 'schedule_order_process_id'
);
SET @schedule_order_process_id_sql = IF(
    @schedule_order_process_id_exists = 0,
    'ALTER TABLE `mes_pro_task_schedule_ext` ADD COLUMN `schedule_order_process_id` bigint DEFAULT NULL COMMENT ''排产工单工序ID'' AFTER `schedule_order_id`',
    'SELECT 1'
);
PREPARE schedule_order_process_id_stmt FROM @schedule_order_process_id_sql;
EXECUTE schedule_order_process_id_stmt;
DEALLOCATE PREPARE schedule_order_process_id_stmt;
