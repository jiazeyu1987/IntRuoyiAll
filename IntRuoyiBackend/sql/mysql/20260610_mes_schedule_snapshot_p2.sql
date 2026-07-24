-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 排产工单 P2：排产工序资源与产能快照。
-- 设计边界：快照只在排产工单生成时固化，后续工艺路线或资源调整不回写历史排产工单。

SET @schema_name := DATABASE();

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `hourly_capacity_total` decimal(18, 6) NULL DEFAULT 0 COMMENT ''快照小时总产能'' AFTER `capacity_source`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order_process'
    AND COLUMN_NAME = 'hourly_capacity_total'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `shift_hours` decimal(10, 2) NULL DEFAULT 10.50 COMMENT ''快照班次小时数'' AFTER `hourly_capacity_total`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order_process'
    AND COLUMN_NAME = 'shift_hours'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `shift_capacity_total` decimal(18, 6) NULL DEFAULT 0 COMMENT ''快照班次总产能'' AFTER `shift_hours`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order_process'
    AND COLUMN_NAME = 'shift_capacity_total'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `resource_snapshot_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT ''工序资源快照'' AFTER `shift_capacity_total`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'mes_pro_schedule_order_process'
    AND COLUMN_NAME = 'resource_snapshot_json'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
