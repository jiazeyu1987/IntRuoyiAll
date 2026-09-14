-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_process_pool_pqc_structured_binding; type=schema; riskLevel=medium
-- P0 生产执行主闭环：工序池事件必须持久化业务提交幂等键和正式记录本条目 ID

DROP PROCEDURE IF EXISTS ensure_mes_pp_event_idempotency_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_event_idempotency_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_event'
          AND COLUMN_NAME = 'event_idempotency_key'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_event`
            ADD COLUMN `event_idempotency_key` varchar(128) DEFAULT NULL COMMENT '业务提交幂等键' AFTER `event_type`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_event'
          AND COLUMN_NAME = 'recordbook_entry_id'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_event`
            ADD COLUMN `recordbook_entry_id` bigint DEFAULT NULL COMMENT '记录本原始条目ID' AFTER `feedback_source_id`;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_event_idempotency_columns();

DROP PROCEDURE IF EXISTS ensure_mes_pp_event_idempotency_columns;

SET @missing_submit_idempotency := (
    SELECT COUNT(*)
    FROM `mes_pro_process_pool_event`
    WHERE `deleted` = b'0'
      AND `event_type` = 'PRODUCTION_SUBMIT'
      AND (`event_idempotency_key` IS NULL OR TRIM(`event_idempotency_key`) = '')
);
SET @sql := IF(@missing_submit_idempotency = 0,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''mes_pro_process_pool_event requires formal event_idempotency_key backfill for PRODUCTION_SUBMIT before P0 idempotency migration'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @missing_recordbook_entry := (
    SELECT COUNT(*)
    FROM `mes_pro_process_pool_event`
    WHERE `deleted` = b'0'
      AND `event_type` = 'PRODUCTION_SUBMIT'
      AND `recordbook_entry_id` IS NULL
);
SET @sql := IF(@missing_recordbook_entry = 0,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''mes_pro_process_pool_event requires formal recordbook_entry_id backfill for PRODUCTION_SUBMIT before P0 idempotency migration'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_process_pool_event'
      AND INDEX_NAME = 'uk_mes_pro_process_pool_event_idem'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `mes_pro_process_pool_event` ADD UNIQUE KEY `uk_mes_pro_process_pool_event_idem` (`tenant_id`, `event_type`, `work_order_id`, `route_process_id`, `process_id`, `actual_employee_id`, `event_idempotency_key`, `deleted`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
