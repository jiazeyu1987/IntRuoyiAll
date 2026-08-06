-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260801_mes_process_pool_team_leader_p4_order_completion_backfill; type=schema; riskLevel=medium
-- MES 工序池 M1：统一 activeOrderId 权威身份字段

DROP PROCEDURE IF EXISTS ensure_mes_pp_active_order_authority_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_active_order_authority_column(
    IN p_column_name varchar(128),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'mes_pro_process_pool_active_order'
          AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_pro_process_pool_active_order` ADD COLUMN `',
                          p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_active_order_authority_column(
    'route_id',
    'bigint NULL COMMENT ''正式工艺路线ID'' AFTER `work_order_id`'
);

CALL ensure_mes_pp_active_order_authority_column(
    'route_version_id',
    'bigint NULL COMMENT ''正式工艺路线版本ID'' AFTER `route_id`'
);

CALL ensure_mes_pp_active_order_authority_column(
    'erp_fixed_quantity_snapshot',
    'decimal(24,6) NULL COMMENT ''ERP固定生产数量快照'' AFTER `route_version_id`'
);

CALL ensure_mes_pp_active_order_authority_column(
    'business_status',
    'varchar(32) NULL COMMENT ''跨角色活跃订单业务状态：ACTIVE/REMOVED/TERMINATED/COMPLETED'' AFTER `active_status`'
);

CALL ensure_mes_pp_active_order_authority_column(
    'version',
    'int NOT NULL DEFAULT 0 COMMENT ''乐观锁版本'' AFTER `removed_at`'
);

DROP PROCEDURE IF EXISTS ensure_mes_pp_active_order_authority_column;

DROP PROCEDURE IF EXISTS assert_mes_pp_active_order_authority_backfilled;
DELIMITER $$
CREATE PROCEDURE assert_mes_pp_active_order_authority_backfilled()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM `mes_pro_process_pool_active_order`
        WHERE `deleted` = b'0'
          AND (`route_id` IS NULL
            OR `route_version_id` IS NULL
            OR `erp_fixed_quantity_snapshot` IS NULL
            OR `business_status` IS NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'mes_pro_process_pool_active_order authority fields require formal backfill before NOT NULL migration';
    END IF;
END$$
DELIMITER ;

CALL assert_mes_pp_active_order_authority_backfilled();

DROP PROCEDURE IF EXISTS assert_mes_pp_active_order_authority_backfilled;

ALTER TABLE `mes_pro_process_pool_active_order`
    MODIFY COLUMN `route_id` bigint NOT NULL COMMENT '正式工艺路线ID',
    MODIFY COLUMN `route_version_id` bigint NOT NULL COMMENT '正式工艺路线版本ID',
    MODIFY COLUMN `erp_fixed_quantity_snapshot` decimal(24,6) NOT NULL COMMENT 'ERP固定生产数量快照',
    MODIFY COLUMN `business_status` varchar(32) NOT NULL COMMENT '跨角色活跃订单业务状态：ACTIVE/REMOVED/TERMINATED/COMPLETED',
    MODIFY COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本';

DROP PROCEDURE IF EXISTS ensure_mes_pp_active_order_authority_index;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_active_order_authority_index()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'mes_pro_process_pool_active_order'
          AND index_name = 'uk_mes_pp_active_order'
          AND column_name = 'leader_user_id'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_active_order`
            DROP INDEX `uk_mes_pp_active_order`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'mes_pro_process_pool_active_order'
          AND index_name = 'uk_mes_pp_active_order'
          AND column_name = 'route_version_id'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_active_order`
            ADD UNIQUE KEY `uk_mes_pp_active_order` (`tenant_id`, `work_order_id`, `route_id`, `route_version_id`, `deleted`);
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_active_order_authority_index();

DROP PROCEDURE IF EXISTS ensure_mes_pp_active_order_authority_index;
