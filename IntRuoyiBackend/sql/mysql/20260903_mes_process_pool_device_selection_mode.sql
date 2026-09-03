DROP PROCEDURE IF EXISTS ensure_mes_device_selection_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_device_selection_column(
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

CALL ensure_mes_device_selection_column(
    'mes_pro_process_pool_team_process_device',
    'device_group_key',
    'varchar(64) DEFAULT NULL COMMENT ''JSON 设备组稳定标识'' AFTER `device_id`'
);
CALL ensure_mes_device_selection_column(
    'mes_pro_process_pool_team_process_device',
    'selection_mode',
    'varchar(16) NOT NULL DEFAULT ''SINGLE'' COMMENT ''设备选择模式：SINGLE/MULTIPLE'' AFTER `device_group_key`'
);

CALL ensure_mes_device_selection_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'device_selection_snapshot_json',
    'longtext DEFAULT NULL COMMENT ''冻结设备组选择规则'' AFTER `parameter_snapshot_json`'
);
CALL ensure_mes_device_selection_column(
    'mes_pro_process_pool_active_order_process_snapshot',
    'device_selection_snapshot_sha256',
    'char(64) DEFAULT NULL COMMENT ''冻结设备组选择规则哈希'' AFTER `device_selection_snapshot_json`'
);

DROP PROCEDURE IF EXISTS ensure_mes_device_selection_column;
