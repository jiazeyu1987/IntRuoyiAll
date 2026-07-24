-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260707_mes_route_use_config_enabled; type=schema; riskLevel=low
-- MES 工艺排产路线工序数量系数：用途配置保存系数，排产工序快照固化系数。
-- Rollback: DROP COLUMN production_quantity_factor from the two tables below, or keep columns and restore creation logic to default 1.000000.

DROP PROCEDURE IF EXISTS intruoyi_add_mes_schedule_route_process_quantity_factor;

DELIMITER //

CREATE PROCEDURE intruoyi_add_mes_schedule_route_process_quantity_factor()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_use_process_config'
      AND column_name = 'production_quantity_factor'
  ) THEN
    ALTER TABLE `mes_pro_route_use_process_config`
      ADD COLUMN `production_quantity_factor` decimal(18,6) NOT NULL DEFAULT 1.000000
      COMMENT '生产数量系数，工序计划数量=成品数量*生产数量系数'
      AFTER `execution_mode`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
      AND column_name = 'production_quantity_factor'
  ) THEN
    ALTER TABLE `mes_pro_schedule_order_process`
      ADD COLUMN `production_quantity_factor` decimal(18,6) NOT NULL DEFAULT 1.000000
      COMMENT '生产数量系数快照'
      AFTER `shift_capacity_total`;
  END IF;
END//

DELIMITER ;

CALL intruoyi_add_mes_schedule_route_process_quantity_factor();

DROP PROCEDURE IF EXISTS intruoyi_add_mes_schedule_route_process_quantity_factor;
