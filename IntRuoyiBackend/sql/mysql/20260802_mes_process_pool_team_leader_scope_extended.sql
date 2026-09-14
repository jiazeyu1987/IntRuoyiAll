-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_team_leader; type=schema; riskLevel=medium
-- MES 工序池班组长负责范围扩展：生产线、设备、订单 scope

DROP PROCEDURE IF EXISTS intruoyi_add_mes_pp_tl_scope_column;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_mes_pp_tl_scope_column(
    IN p_column_name varchar(64),
    IN p_column_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_team_leader_scope'
           AND column_name = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_pro_process_pool_team_leader_scope` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_mes_pp_tl_scope_column('production_line_id', 'bigint DEFAULT NULL COMMENT ''负责生产线ID'' AFTER `workstation_id`');
CALL intruoyi_add_mes_pp_tl_scope_column('equipment_id', 'bigint DEFAULT NULL COMMENT ''负责设备ID'' AFTER `production_line_id`');
CALL intruoyi_add_mes_pp_tl_scope_column('work_order_id', 'bigint DEFAULT NULL COMMENT ''负责生产订单ID'' AFTER `equipment_id`');

DROP PROCEDURE IF EXISTS intruoyi_add_mes_pp_tl_scope_column;

DROP PROCEDURE IF EXISTS intruoyi_add_mes_pp_tl_scope_index;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_mes_pp_tl_scope_index(
    IN p_index_name varchar(64),
    IN p_index_definition varchar(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'mes_pro_process_pool_team_leader_scope'
           AND index_name = p_index_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `mes_pro_process_pool_team_leader_scope` ADD KEY `', p_index_name, '` ', p_index_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL intruoyi_add_mes_pp_tl_scope_index('idx_mes_pp_tl_scope_line', '(`tenant_id`, `leader_user_id`, `production_line_id`)');
CALL intruoyi_add_mes_pp_tl_scope_index('idx_mes_pp_tl_scope_equipment', '(`tenant_id`, `leader_user_id`, `equipment_id`)');
CALL intruoyi_add_mes_pp_tl_scope_index('idx_mes_pp_tl_scope_order', '(`tenant_id`, `leader_user_id`, `work_order_id`)');

DROP PROCEDURE IF EXISTS intruoyi_add_mes_pp_tl_scope_index;
