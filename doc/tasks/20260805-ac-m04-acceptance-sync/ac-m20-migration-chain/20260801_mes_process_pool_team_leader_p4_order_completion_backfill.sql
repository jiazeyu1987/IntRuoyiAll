-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260801_mes_process_pool_team_leader_p3_report_allocation; type=schema; riskLevel=medium
-- MES 生产组长工作台 P4：订单工序完成状态与正式批记录回填结果

CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_order_process_completion` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
    `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `target_quantity` decimal(24,6) NOT NULL COMMENT '订单工序目标数量',
    `confirmed_quantity` decimal(24,6) NOT NULL COMMENT '当前累计确认分配数量',
    `completion_status` varchar(32) NOT NULL COMMENT '订单工序完成状态：IN_PROGRESS/COMPLETED',
    `completed_at` datetime DEFAULT NULL COMMENT '订单工序完成时间',
    `backfill_status` varchar(32) NOT NULL COMMENT '批记录回填状态：NOT_REQUIRED/SUCCESS',
    `backfill_execution_id` bigint DEFAULT NULL COMMENT '批记录执行实例ID',
    `backfill_error` varchar(1000) DEFAULT NULL COMMENT '批记录回填失败原因',
    `last_event_id` bigint NOT NULL COMMENT '最后一次触发的工序池提交事件ID',
    `last_review_id` bigint NOT NULL COMMENT '最后一次班组长复核记录ID',
    `source_event_ids_json` json NOT NULL COMMENT '本次批记录回填聚合源事件ID集合',
    `source_allocation_ids_json` json NOT NULL COMMENT '本次批记录回填聚合分配ID集合',
    `aggregate_hash` char(64) NOT NULL COMMENT '订单工序完成批记录聚合版本哈希',
    `backfill_idempotency_key` varchar(160) NOT NULL COMMENT '批记录回填聚合版本幂等键',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pp_order_process_completion` (`tenant_id`, `work_order_id`, `route_process_id`, `process_id`, `deleted`),
    KEY `idx_mes_pp_order_process_completion_status` (`tenant_id`, `completion_status`, `backfill_status`),
    KEY `idx_mes_pp_order_process_completion_last_event` (`tenant_id`, `last_event_id`),
    KEY `idx_mes_pp_order_process_completion_execution` (`tenant_id`, `backfill_execution_id`),
    KEY `idx_mes_pp_order_process_completion_aggregate` (`tenant_id`, `aggregate_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工序池生产组长订单工序完成与批记录回填状态';

DROP PROCEDURE IF EXISTS ensure_mes_pp_order_process_completion_aggregate_columns;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_order_process_completion_aggregate_columns()
BEGIN
    DECLARE existing_rows bigint DEFAULT 0;
    DECLARE missing_columns int DEFAULT 0;

    SELECT COUNT(*) INTO missing_columns
    FROM (
        SELECT 'source_event_ids_json' AS column_name
        UNION ALL SELECT 'source_allocation_ids_json'
        UNION ALL SELECT 'aggregate_hash'
        UNION ALL SELECT 'backfill_idempotency_key'
    ) required_columns
    WHERE NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_order_process_completion'
          AND COLUMN_NAME = required_columns.column_name
    );

    IF missing_columns > 0 THEN
        SELECT COUNT(*) INTO existing_rows
        FROM `mes_pro_process_pool_order_process_completion`;
        IF existing_rows > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'mes_pro_process_pool_order_process_completion requires formal aggregate source/idempotency backfill before AC-M19 schema migration';
        END IF;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_order_process_completion'
          AND COLUMN_NAME = 'source_event_ids_json'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_order_process_completion`
            ADD COLUMN `source_event_ids_json` json NOT NULL COMMENT '本次批记录回填聚合源事件ID集合' AFTER `last_review_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_order_process_completion'
          AND COLUMN_NAME = 'source_allocation_ids_json'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_order_process_completion`
            ADD COLUMN `source_allocation_ids_json` json NOT NULL COMMENT '本次批记录回填聚合分配ID集合' AFTER `source_event_ids_json`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_order_process_completion'
          AND COLUMN_NAME = 'aggregate_hash'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_order_process_completion`
            ADD COLUMN `aggregate_hash` char(64) NOT NULL COMMENT '订单工序完成批记录聚合版本哈希' AFTER `source_allocation_ids_json`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_order_process_completion'
          AND COLUMN_NAME = 'backfill_idempotency_key'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_order_process_completion`
            ADD COLUMN `backfill_idempotency_key` varchar(160) NOT NULL COMMENT '批记录回填聚合版本幂等键' AFTER `aggregate_hash`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'mes_pro_process_pool_order_process_completion'
          AND INDEX_NAME = 'idx_mes_pp_order_process_completion_aggregate'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_order_process_completion`
            ADD KEY `idx_mes_pp_order_process_completion_aggregate` (`tenant_id`, `aggregate_hash`);
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_order_process_completion_aggregate_columns();

DROP PROCEDURE IF EXISTS ensure_mes_pp_order_process_completion_aggregate_columns;
