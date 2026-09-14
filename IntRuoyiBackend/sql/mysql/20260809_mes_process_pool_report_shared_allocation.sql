-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260809_mes_process_pool_active_order_manual_sort; type=schema; riskLevel=medium
-- MES 工序报工共享分配池：当前版本、分配生命周期、数量片段生命周期与调整审计
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_pp_report_shared_allocation_20260809;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_report_shared_allocation_20260809()
BEGIN
    DECLARE v_table_count int DEFAULT 0;
    DECLARE v_column_count int DEFAULT 0;
    DECLARE v_null_count bigint DEFAULT 0;
    DECLARE v_value_count bigint DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        RESIGNAL;
    END;

    SELECT COUNT(*) INTO v_table_count
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_report_allocation';
    IF v_table_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_report_allocation';
    END IF;

    SELECT COUNT(*) INTO v_table_count
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_fifo_allocation_line';
    IF v_table_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_fifo_allocation_line';
    END IF;

    SELECT COUNT(*) INTO v_table_count
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_event';
    IF v_table_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Missing mes_pro_process_pool_event';
    END IF;

    SELECT COUNT(*) INTO v_column_count
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_report_allocation'
       AND column_name IN ('lifecycle_status', 'created_version', 'superseded_version');
    IF v_column_count = 0 THEN
        ALTER TABLE `mes_pro_process_pool_report_allocation`
            ADD COLUMN `lifecycle_status` varchar(32) NULL COMMENT '分配生命周期：CURRENT/SUPERSEDED' AFTER `allocation_mode`,
            ADD COLUMN `created_version` int NULL COMMENT '创建该分配行的报工分配版本' AFTER `lifecycle_status`,
            ADD COLUMN `superseded_version` int DEFAULT NULL COMMENT '替换该分配行的报工分配版本' AFTER `created_version`;
    ELSEIF v_column_count <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Report allocation lifecycle columns are partially migrated';
    END IF;

    SELECT SUM(CASE WHEN `lifecycle_status` IS NULL OR `created_version` IS NULL THEN 1 ELSE 0 END),
           SUM(CASE WHEN `lifecycle_status` IS NOT NULL OR `created_version` IS NOT NULL THEN 1 ELSE 0 END)
      INTO v_null_count, v_value_count
      FROM `mes_pro_process_pool_report_allocation`;
    SET v_null_count = COALESCE(v_null_count, 0);
    SET v_value_count = COALESCE(v_value_count, 0);
    IF v_null_count > 0 AND v_value_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Report allocation lifecycle backfill is partially applied';
    END IF;
    IF v_null_count > 0 THEN
        UPDATE `mes_pro_process_pool_report_allocation`
           SET `lifecycle_status` = 'CURRENT',
               `created_version` = 1,
               `superseded_version` = NULL
         WHERE `lifecycle_status` IS NULL
            OR `created_version` IS NULL;
    END IF;
    IF EXISTS (
        SELECT 1
          FROM `mes_pro_process_pool_report_allocation`
         WHERE `lifecycle_status` NOT IN ('CURRENT', 'SUPERSEDED')
            OR `created_version` < 1
            OR (`lifecycle_status` = 'CURRENT' AND `superseded_version` IS NOT NULL)
            OR (`lifecycle_status` = 'SUPERSEDED' AND `superseded_version` IS NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Report allocation lifecycle data violates the version contract';
    END IF;
    ALTER TABLE `mes_pro_process_pool_report_allocation`
        MODIFY COLUMN `lifecycle_status` varchar(32) NOT NULL COMMENT '分配生命周期：CURRENT/SUPERSEDED',
        MODIFY COLUMN `created_version` int NOT NULL COMMENT '创建该分配行的报工分配版本';

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_report_allocation'
           AND index_name = 'idx_mes_pp_report_alloc_current_event'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_report_allocation`
            ADD KEY `idx_mes_pp_report_alloc_current_event`
                (`tenant_id`, `event_id`, `lifecycle_status`, `created_version`, `id`);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_report_allocation'
           AND index_name = 'idx_mes_pp_report_alloc_current_target'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_report_allocation`
            ADD KEY `idx_mes_pp_report_alloc_current_target`
                (`tenant_id`, `active_order_id`, `process_id`, `lifecycle_status`, `id`);
    END IF;

    SELECT COUNT(*) INTO v_column_count
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_fifo_allocation_line'
       AND column_name IN ('report_allocation_version', 'lifecycle_status', 'superseded_version');
    IF v_column_count = 0 THEN
        ALTER TABLE `mes_pro_process_pool_fifo_allocation_line`
            ADD COLUMN `report_allocation_version` int NULL COMMENT '来源报工当前分配版本' AFTER `source_event_id`,
            ADD COLUMN `lifecycle_status` varchar(32) NULL COMMENT '数量片段分配生命周期：CURRENT/SUPERSEDED' AFTER `allocation_status`,
            ADD COLUMN `superseded_version` int DEFAULT NULL COMMENT '替换该数量片段行的报工分配版本' AFTER `lifecycle_status`;
    ELSEIF v_column_count <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'FIFO allocation lifecycle columns are partially migrated';
    END IF;

    CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_report_allocation_state` (
        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
        `event_id` bigint NOT NULL COMMENT '生产报工事件ID',
        `current_version` int NOT NULL COMMENT '当前分配版本，未保存分配时为0',
        `last_idempotency_key` varchar(128) DEFAULT NULL COMMENT '最近成功保存幂等键',
        `last_request_hash` char(64) DEFAULT NULL COMMENT '最近成功保存请求哈希',
        `last_changed_by` bigint DEFAULT NULL COMMENT '最近变更用户ID',
        `last_changed_at` datetime NOT NULL COMMENT '最近变更时间',
        `creator` varchar(64) DEFAULT '' COMMENT '创建者',
        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        `updater` varchar(64) DEFAULT '' COMMENT '更新者',
        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
        `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_mes_pp_report_alloc_state_event` (`tenant_id`, `event_id`, `deleted`),
        KEY `idx_mes_pp_report_alloc_state_changed` (`tenant_id`, `last_changed_at`, `event_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 报工共享分配池当前版本';

    CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_report_allocation_adjustment_audit` (
        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
        `event_id` bigint NOT NULL COMMENT '生产报工事件ID',
        `allocation_version` int NOT NULL COMMENT '报工分配版本',
        `source_allocation_id` bigint DEFAULT NULL COMMENT '来源分配行ID',
        `active_order_id` bigint NOT NULL COMMENT '活跃订单ID',
        `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
        `route_process_id` bigint NOT NULL COMMENT '目标订单自身工艺路线工序ID',
        `process_id` bigint NOT NULL COMMENT '工序ID',
        `before_quantity` decimal(24,6) NOT NULL COMMENT '变更前当前数量',
        `after_quantity` decimal(24,6) NOT NULL COMMENT '变更后当前数量',
        `delta_quantity` decimal(24,6) NOT NULL COMMENT '数量差额',
        `actor_user_id` bigint NOT NULL COMMENT '操作者用户ID',
        `adjustment_reason` varchar(500) NOT NULL COMMENT '调整原因',
        `allocation_mode` varchar(32) NOT NULL COMMENT '分配模式：FIFO/MANUAL/SYSTEM',
        `change_source` varchar(32) NOT NULL COMMENT '变更来源：INITIAL_BASELINE/FIFO/MANUAL/ORDER_CHANGE',
        `occurred_at` datetime NOT NULL COMMENT '发生时间',
        `creator` varchar(64) DEFAULT '' COMMENT '创建者',
        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        `updater` varchar(64) DEFAULT '' COMMENT '更新者',
        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
        `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_mes_pp_report_alloc_audit_source`
            (`tenant_id`, `source_allocation_id`, `change_source`, `deleted`),
        KEY `idx_mes_pp_report_alloc_audit_event`
            (`tenant_id`, `event_id`, `allocation_version`, `id`),
        KEY `idx_mes_pp_report_alloc_audit_target`
            (`tenant_id`, `active_order_id`, `process_id`, `occurred_at`, `id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 报工共享分配调整审计';

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_report_allocation_state'
           AND column_name = 'current_version'
           AND data_type = 'int'
           AND is_nullable = 'NO'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Report allocation state table contract mismatch';
    END IF;
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_report_allocation_adjustment_audit'
           AND column_name = 'delta_quantity'
           AND data_type = 'decimal'
           AND numeric_precision = 24
           AND numeric_scale = 6
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Report allocation audit table contract mismatch';
    END IF;

    INSERT INTO `mes_pro_process_pool_report_allocation_state` (
        `event_id`, `current_version`, `last_changed_by`, `last_changed_at`,
        `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
    )
    SELECT `event`.`id`,
           COALESCE(MAX(`allocation`.`created_version`), 0),
           MAX(`allocation`.`leader_user_id`),
           COALESCE(MAX(`allocation`.`confirmed_at`), `event`.`create_time`),
           '', CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, b'0', `event`.`tenant_id`
      FROM `mes_pro_process_pool_event` AS `event`
      LEFT JOIN `mes_pro_process_pool_report_allocation` AS `allocation`
        ON `allocation`.`tenant_id` = `event`.`tenant_id`
       AND `allocation`.`event_id` = `event`.`id`
       AND `allocation`.`deleted` = b'0'
     WHERE `event`.`event_type` = 'PRODUCTION_SUBMIT'
       AND `event`.`deleted` = b'0'
       AND NOT EXISTS (
           SELECT 1
             FROM `mes_pro_process_pool_report_allocation_state` AS `existing_state`
            WHERE `existing_state`.`tenant_id` = `event`.`tenant_id`
              AND `existing_state`.`event_id` = `event`.`id`
              AND `existing_state`.`deleted` = b'0'
       )
     GROUP BY `event`.`tenant_id`, `event`.`id`, `event`.`create_time`;

    INSERT INTO `mes_pro_process_pool_report_allocation_adjustment_audit` (
        `event_id`, `allocation_version`, `source_allocation_id`, `active_order_id`,
        `work_order_id`, `route_process_id`, `process_id`, `before_quantity`,
        `after_quantity`, `delta_quantity`, `actor_user_id`, `adjustment_reason`,
        `allocation_mode`, `change_source`, `occurred_at`, `creator`, `create_time`,
        `updater`, `update_time`, `deleted`, `tenant_id`
    )
    SELECT `allocation`.`event_id`, `allocation`.`created_version`, `allocation`.`id`,
           `allocation`.`active_order_id`, `allocation`.`work_order_id`,
           `allocation`.`route_process_id`, `allocation`.`process_id`, 0,
           `allocation`.`allocated_quantity`, `allocation`.`allocated_quantity`,
           `allocation`.`leader_user_id`, '既有有效分配迁移基线',
           `allocation`.`allocation_mode`, 'INITIAL_BASELINE',
           `allocation`.`confirmed_at`, '', CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP,
           b'0', `allocation`.`tenant_id`
      FROM `mes_pro_process_pool_report_allocation` AS `allocation`
     WHERE `allocation`.`deleted` = b'0'
       AND `allocation`.`lifecycle_status` = 'CURRENT'
       AND NOT EXISTS (
           SELECT 1
             FROM `mes_pro_process_pool_report_allocation_adjustment_audit` AS `audit`
            WHERE `audit`.`tenant_id` = `allocation`.`tenant_id`
              AND `audit`.`source_allocation_id` = `allocation`.`id`
              AND `audit`.`change_source` = 'INITIAL_BASELINE'
              AND `audit`.`deleted` = b'0'
       );

    UPDATE `mes_pro_process_pool_fifo_allocation_line` AS `line`
    JOIN `mes_pro_process_pool_report_allocation_state` AS `state`
      ON `state`.`tenant_id` = `line`.`tenant_id`
     AND `state`.`event_id` = `line`.`source_event_id`
     AND `state`.`deleted` = b'0'
       SET `line`.`report_allocation_version` = GREATEST(`state`.`current_version`, 1),
           `line`.`lifecycle_status` = 'CURRENT',
           `line`.`superseded_version` = NULL
     WHERE `line`.`report_allocation_version` IS NULL
        OR `line`.`lifecycle_status` IS NULL;

    SELECT COUNT(*) INTO v_null_count
      FROM `mes_pro_process_pool_fifo_allocation_line`
     WHERE `report_allocation_version` IS NULL
        OR `lifecycle_status` IS NULL;
    IF v_null_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'FIFO allocation lines require a formal production report allocation state';
    END IF;
    IF EXISTS (
        SELECT 1
          FROM `mes_pro_process_pool_fifo_allocation_line`
         WHERE `lifecycle_status` NOT IN ('CURRENT', 'SUPERSEDED')
            OR `report_allocation_version` < 1
            OR (`lifecycle_status` = 'CURRENT' AND `superseded_version` IS NOT NULL)
            OR (`lifecycle_status` = 'SUPERSEDED' AND `superseded_version` IS NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'FIFO allocation lifecycle data violates the version contract';
    END IF;
    ALTER TABLE `mes_pro_process_pool_fifo_allocation_line`
        MODIFY COLUMN `report_allocation_version` int NOT NULL COMMENT '来源报工当前分配版本',
        MODIFY COLUMN `lifecycle_status` varchar(32) NOT NULL COMMENT '数量片段分配生命周期：CURRENT/SUPERSEDED';

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_fifo_allocation_line'
           AND index_name = 'idx_mes_pp_fifo_alloc_current_event'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_fifo_allocation_line`
            ADD KEY `idx_mes_pp_fifo_alloc_current_event`
                (`tenant_id`, `source_event_id`, `lifecycle_status`, `report_allocation_version`, `id`);
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_report_shared_allocation_20260809();

DROP PROCEDURE IF EXISTS ensure_mes_pp_report_shared_allocation_20260809;
