-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_mes_route_process_single_entry_multi_exit; type=schema; riskLevel=medium
-- MES 工艺路线多前置汇合：移除历史目标唯一入边索引，并为排产快照保存完整前置集合。
-- Rollback: 不恢复目标唯一入边索引；该约束与已支持的合法多前置汇合关系冲突。

DROP PROCEDURE IF EXISTS intruoyi_allow_mes_route_process_multi_predecessor;

DELIMITER //

CREATE PROCEDURE intruoyi_allow_mes_route_process_multi_predecessor()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process_flow_edge'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'missing mes_pro_route_process_flow_edge';
  END IF;
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'missing mes_pro_schedule_order_process';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
      AND column_name = 'predecessor_route_process_ids_json'
  ) THEN
    ALTER TABLE `mes_pro_schedule_order_process`
      ADD COLUMN `predecessor_route_process_ids_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL
      COMMENT '完整直接前置路线工序ID集合快照（JSON数组）' AFTER `predecessor_route_process_id`;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process_flow_edge'
      AND index_name = 'uk_mes_route_process_flow_target'
  ) THEN
    ALTER TABLE `mes_pro_route_process_flow_edge`
      DROP INDEX `uk_mes_route_process_flow_target`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process_flow_edge'
      AND index_name = 'idx_mes_route_process_flow_edge_target'
  ) THEN
    ALTER TABLE `mes_pro_route_process_flow_edge`
      ADD INDEX `idx_mes_route_process_flow_edge_target`
      (`tenant_id`, `target_route_process_id`);
  END IF;
END//

DELIMITER ;

CALL intruoyi_allow_mes_route_process_multi_predecessor();

DROP PROCEDURE IF EXISTS intruoyi_allow_mes_route_process_multi_predecessor;
