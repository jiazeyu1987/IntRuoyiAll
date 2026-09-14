-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_process_pool_event_idempotency; type=schema; riskLevel=medium
-- P0 生产执行主闭环：数量片段必须持久化生产提交根事件 ID，支撑 FIFO 与批记录追溯

DROP PROCEDURE IF EXISTS ensure_mes_pp_quantity_fragment_submit_root;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_quantity_fragment_submit_root()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_quantity_fragment'
          AND COLUMN_NAME = 'production_submit_event_id'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_quantity_fragment`
            ADD COLUMN `production_submit_event_id` bigint DEFAULT NULL COMMENT '生产提交根事件ID' AFTER `event_id`;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_quantity_fragment_submit_root();

DROP PROCEDURE IF EXISTS ensure_mes_pp_quantity_fragment_submit_root;

UPDATE `mes_pro_process_pool_quantity_fragment` fragment
JOIN `mes_pro_process_pool_event` event
  ON event.`tenant_id` = fragment.`tenant_id`
 AND event.`id` = fragment.`event_id`
 AND event.`deleted` = b'0'
 AND event.`event_type` = 'PRODUCTION_SUBMIT'
SET fragment.`production_submit_event_id` = fragment.`event_id`
WHERE fragment.`deleted` = b'0'
  AND fragment.`production_submit_event_id` IS NULL;

SET @missing_fragment_submit_root := (
    SELECT COUNT(*)
    FROM `mes_pro_process_pool_quantity_fragment` fragment
    LEFT JOIN `mes_pro_process_pool_event` event
      ON event.`tenant_id` = fragment.`tenant_id`
     AND event.`id` = fragment.`event_id`
     AND event.`deleted` = b'0'
    WHERE fragment.`deleted` = b'0'
      AND (
          fragment.`production_submit_event_id` IS NULL
          OR event.`id` IS NULL
          OR event.`event_type` <> 'PRODUCTION_SUBMIT'
          OR fragment.`production_submit_event_id` <> fragment.`event_id`
      )
);
SET @sql := IF(@missing_fragment_submit_root = 0,
    'ALTER TABLE `mes_pro_process_pool_quantity_fragment` MODIFY COLUMN `production_submit_event_id` bigint NOT NULL COMMENT ''生产提交根事件ID''',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''mes_pro_process_pool_quantity_fragment requires formal PRODUCTION_SUBMIT root event backfill before NOT NULL migration'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_process_pool_quantity_fragment'
      AND INDEX_NAME = 'idx_mes_pro_process_pool_fragment_submit_event'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `mes_pro_process_pool_quantity_fragment` ADD KEY `idx_mes_pro_process_pool_fragment_submit_event` (`tenant_id`, `production_submit_event_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
