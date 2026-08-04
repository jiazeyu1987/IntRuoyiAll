-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_foundation; type=schema; riskLevel=medium
-- P0 生产执行主闭环：PQC 到生产提交事件必须持久化正式结构化绑定

DROP PROCEDURE IF EXISTS ensure_mes_pp_pqc_binding_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_pqc_binding_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_pqc_record'
          AND COLUMN_NAME = 'production_submit_event_id'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_pqc_record`
            ADD COLUMN `production_submit_event_id` bigint DEFAULT NULL COMMENT '正式绑定的生产提交事件ID' AFTER `event_id`;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_pqc_binding_column();

DROP PROCEDURE IF EXISTS ensure_mes_pp_pqc_binding_column;

SET @missing_pqc_binding := (
    SELECT COUNT(*)
    FROM `mes_pro_process_pool_pqc_record`
    WHERE `deleted` = b'0'
      AND `production_submit_event_id` IS NULL
);
SET @sql := IF(@missing_pqc_binding = 0,
    'ALTER TABLE `mes_pro_process_pool_pqc_record` MODIFY COLUMN `production_submit_event_id` bigint NOT NULL COMMENT ''正式绑定的生产提交事件ID''',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''mes_pro_process_pool_pqc_record requires formal production_submit_event_id backfill before NOT NULL migration'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_process_pool_pqc_record'
      AND INDEX_NAME = 'idx_mes_pro_process_pool_pqc_submit_event'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `mes_pro_process_pool_pqc_record` ADD KEY `idx_mes_pro_process_pool_pqc_submit_event` (`tenant_id`, `production_submit_event_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
