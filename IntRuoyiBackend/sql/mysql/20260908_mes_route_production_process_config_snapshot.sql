-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260903_mes_process_pool_device_selection_mode; type=schema; riskLevel=medium
-- Purpose: route version production process config projection and active order frozen snapshot envelope.
-- Recovery: additions are idempotent and old applications ignore the new table and nullable columns.
-- Rollback blocker: do not remove snapshot columns after any active order has frozen production configuration evidence.

CREATE TABLE IF NOT EXISTS `mes_pro_route_process_loss_reason` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_version_id` bigint NOT NULL COMMENT '工艺路线版本ID',
    `route_process_id` bigint NOT NULL COMMENT '路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `reason_code` varchar(64) NOT NULL COMMENT '损耗原因编码',
    `reason_name` varchar(255) NOT NULL COMMENT '损耗原因名称',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_route_process_loss_reason` (`tenant_id`, `route_version_id`, `route_process_id`, `reason_code`, `deleted`),
    KEY `idx_mes_route_process_loss_reason_process` (`tenant_id`, `route_process_id`, `process_id`, `enabled`),
    KEY `idx_mes_route_process_loss_reason_version` (`tenant_id`, `route_version_id`, `route_process_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线版本工序损耗原因投影';

DROP PROCEDURE IF EXISTS ensure_mes_route_production_snapshot_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_production_snapshot_column(
    IN p_table_name varchar(128),
    IN p_column_name varchar(128),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_production_snapshot_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'loss_reason_snapshot_json',
    'longtext DEFAULT NULL COMMENT ''冻结损耗原因快照'' AFTER `parameter_snapshot_state`'
);
CALL ensure_mes_route_production_snapshot_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'loss_reason_snapshot_sha256',
    'char(64) DEFAULT NULL COMMENT ''冻结损耗原因快照哈希'' AFTER `loss_reason_snapshot_json`'
);
CALL ensure_mes_route_production_snapshot_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'overage_percent_snapshot',
    'decimal(10,4) DEFAULT NULL COMMENT ''冻结超产比例'' AFTER `loss_reason_snapshot_sha256`'
);
CALL ensure_mes_route_production_snapshot_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'production_config_snapshot_json',
    'longtext DEFAULT NULL COMMENT ''冻结统一生产工序配置快照'' AFTER `overage_percent_snapshot`'
);
CALL ensure_mes_route_production_snapshot_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'production_config_snapshot_sha256',
    'char(64) DEFAULT NULL COMMENT ''冻结统一生产工序配置快照哈希'' AFTER `production_config_snapshot_json`'
);
CALL ensure_mes_route_production_snapshot_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'production_config_migration_source',
    'varchar(32) DEFAULT NULL COMMENT ''生产工序配置迁移来源：ROUTE_VERSION/CUTOVER_CURRENT_CONFIG'' AFTER `production_config_snapshot_sha256`'
);
CALL ensure_mes_route_production_snapshot_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'production_config_migrated_at',
    'datetime DEFAULT NULL COMMENT ''生产工序配置迁移时间'' AFTER `production_config_migration_source`'
);

DROP PROCEDURE IF EXISTS ensure_mes_route_production_snapshot_check;
DELIMITER $$
CREATE PROCEDURE ensure_mes_route_production_snapshot_check()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = DATABASE()
          AND table_name = 'mes_pro_process_pool_active_order_process_snapshot'
          AND constraint_name = 'chk_mes_route_production_config_source'
    ) THEN
        ALTER TABLE `mes_pro_process_pool_active_order_process_snapshot`
            ADD CONSTRAINT `chk_mes_route_production_config_source`
            CHECK (`production_config_migration_source` IN ('ROUTE_VERSION','CUTOVER_CURRENT_CONFIG'));
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_route_production_snapshot_check();

DROP PROCEDURE IF EXISTS ensure_mes_route_production_snapshot_check;
DROP PROCEDURE IF EXISTS ensure_mes_route_production_snapshot_column;
