-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260624_mes_schedule_order_freeze_audit; type=schema; riskLevel=medium
-- 排产工单全状态删除：使用业务撤出标志保留生产、报工、质检、批记录和审计事实。

SET @schema_name = DATABASE();

SET @required_table_count = (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME IN ('mes_pro_schedule_order', 'mes_pro_task_schedule_ext', 'mes_pro_task')
);
SET @required_table_sql = IF(
  @required_table_count = 3,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''schedule order removal migration missing required tables'''
);
PREPARE required_table_stmt FROM @required_table_sql;
EXECUTE required_table_stmt;
DEALLOCATE PREPARE required_table_stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'removed_from_schedule'
);
SET @column_sql = IF(@column_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `removed_from_schedule` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否已撤出排产体系'' AFTER `manual_finished_reason`',
  'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'removed_from_schedule_time'
);
SET @column_sql = IF(@column_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `removed_from_schedule_time` datetime NULL COMMENT ''撤出排产时间'' AFTER `removed_from_schedule`',
  'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'removed_from_schedule_by'
);
SET @column_sql = IF(@column_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `removed_from_schedule_by` bigint NULL COMMENT ''撤出排产操作人'' AFTER `removed_from_schedule_time`',
  'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'removed_from_schedule_reason'
);
SET @column_sql = IF(@column_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `removed_from_schedule_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT ''撤出排产原因'' AFTER `removed_from_schedule_by`',
  'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'removed_from_schedule_status'
);
SET @column_sql = IF(@column_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `removed_from_schedule_status` tinyint NULL COMMENT ''撤出时排产工单状态'' AFTER `removed_from_schedule_reason`',
  'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'reentry_blocked'
);
SET @column_sql = IF(@column_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `reentry_blocked` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''存在生产事实禁止重新入池'' AFTER `removed_from_schedule_status`',
  'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND COLUMN_NAME = 'active_work_order_id'
);
SET @column_sql = IF(@column_exists = 0,
  'ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `active_work_order_id` bigint GENERATED ALWAYS AS (IF(`removed_from_schedule` = b''0'', `work_order_id`, NULL)) STORED COMMENT ''未撤出排产工单来源生产工单唯一键'' AFTER `reentry_blocked`',
  'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;

SET @legacy_unique_exists = (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND INDEX_NAME = 'uk_mes_pro_schedule_order_work_order_tenant'
);
SET @legacy_unique_sql = IF(@legacy_unique_exists > 0,
  'ALTER TABLE `mes_pro_schedule_order` DROP INDEX `uk_mes_pro_schedule_order_work_order_tenant`',
  'SELECT 1');
PREPARE legacy_unique_stmt FROM @legacy_unique_sql;
EXECUTE legacy_unique_stmt;
DEALLOCATE PREPARE legacy_unique_stmt;

SET @active_unique_exists = (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND INDEX_NAME = 'uk_mes_pro_schedule_order_active_work_order'
);
SET @active_unique_sql = IF(@active_unique_exists = 0,
  'CREATE UNIQUE INDEX `uk_mes_pro_schedule_order_active_work_order` ON `mes_pro_schedule_order` (`tenant_id`, `active_work_order_id`, `deleted`)',
  'SELECT 1');
PREPARE active_unique_stmt FROM @active_unique_sql;
EXECUTE active_unique_stmt;
DEALLOCATE PREPARE active_unique_stmt;

SET @index_exists = (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mes_pro_schedule_order'
    AND INDEX_NAME = 'idx_mes_pro_schedule_order_removed_status'
);
SET @index_sql = IF(@index_exists = 0,
  'CREATE INDEX `idx_mes_pro_schedule_order_removed_status` ON `mes_pro_schedule_order` (`tenant_id`, `removed_from_schedule`, `status`)',
  'SELECT 1');
PREPARE index_stmt FROM @index_sql; EXECUTE index_stmt; DEALLOCATE PREPARE index_stmt;
