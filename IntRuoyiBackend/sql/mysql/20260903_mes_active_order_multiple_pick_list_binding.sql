-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260822_mes_active_order_pick_list_binding; type=schema; riskLevel=medium
-- Allow one active order to bind multiple production pick lists while keeping each pick list unique per active order.

DROP PROCEDURE IF EXISTS intruoyi_drop_active_order_pick_index;
DELIMITER $$
CREATE PROCEDURE intruoyi_drop_active_order_pick_index(
  IN p_index_name varchar(64),
  IN p_ddl_sql text
)
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_active_order_pick_list_binding'
      AND index_name = p_index_name
  ) THEN
    SET @ddl = p_ddl_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS intruoyi_add_active_order_pick_index;
DELIMITER $$
CREATE PROCEDURE intruoyi_add_active_order_pick_index(
  IN p_index_name varchar(64),
  IN p_ddl_sql text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_process_pool_active_order_pick_list_binding'
      AND index_name = p_index_name
  ) THEN
    SET @ddl = p_ddl_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL intruoyi_drop_active_order_pick_index(
  'uk_active_order_pick_binding_active',
  'ALTER TABLE `mes_pro_process_pool_active_order_pick_list_binding` DROP INDEX `uk_active_order_pick_binding_active`'
);

CALL intruoyi_add_active_order_pick_index(
  'uk_active_order_pick_binding_pick_list',
  'ALTER TABLE `mes_pro_process_pool_active_order_pick_list_binding` ADD UNIQUE KEY `uk_active_order_pick_binding_pick_list` (`tenant_id`, `active_order_id`, `pick_list_id`, `deleted`)'
);

DROP PROCEDURE IF EXISTS intruoyi_add_active_order_pick_index;
DROP PROCEDURE IF EXISTS intruoyi_drop_active_order_pick_index;
