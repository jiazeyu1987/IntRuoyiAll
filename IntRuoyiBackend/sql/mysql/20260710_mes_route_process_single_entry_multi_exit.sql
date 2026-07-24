-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260709_mes_route_process_flow_graph; type=schema; riskLevel=medium
-- MES 工艺路线单入口多出口：约束目标工序唯一入边，并在排产/eDHR任务中快照直接前置关系。
-- Rollback: DROP INDEX uk_mes_route_process_flow_target ON mes_pro_route_process_flow_edge;
-- Rollback: ALTER TABLE mes_pro_schedule_order_process DROP COLUMN root_process_flag, DROP COLUMN predecessor_route_process_id;
-- Rollback: ALTER TABLE mes_pro_edhr_batch_execution_task DROP COLUMN root_process_flag, DROP COLUMN predecessor_route_process_id;

DROP PROCEDURE IF EXISTS intruoyi_upgrade_mes_route_process_single_entry_multi_exit;

DELIMITER //

CREATE PROCEDURE intruoyi_upgrade_mes_route_process_single_entry_multi_exit()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_process_flow_edge'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing mes_pro_route_process_flow_edge';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_schedule_order_process'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing mes_pro_schedule_order_process';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'mes_pro_edhr_batch_execution_task'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing mes_pro_edhr_batch_execution_task';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM mes_pro_route_process_flow_edge
    WHERE deleted = b'0'
    GROUP BY tenant_id, route_id, target_route_process_id
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'route process flow contains multiple incoming edges';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_route_process_flow_edge'
      AND index_name = 'uk_mes_route_process_flow_target'
  ) THEN
    ALTER TABLE `mes_pro_route_process_flow_edge`
      ADD UNIQUE INDEX `uk_mes_route_process_flow_target`
      (`tenant_id`, `route_id`, `target_route_process_id`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
      AND column_name = 'predecessor_route_process_id'
  ) THEN
    ALTER TABLE `mes_pro_schedule_order_process`
      ADD COLUMN `predecessor_route_process_id` bigint NULL
      COMMENT '直接前置路线工序ID快照' AFTER `route_process_id`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
      AND column_name = 'root_process_flag'
  ) THEN
    ALTER TABLE `mes_pro_schedule_order_process`
      ADD COLUMN `root_process_flag` bit(1) NOT NULL DEFAULT b'0'
      COMMENT '是否根工序快照' AFTER `predecessor_route_process_id`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_schedule_order_process'
      AND index_name = 'idx_mes_schedule_order_process_predecessor'
  ) THEN
    ALTER TABLE `mes_pro_schedule_order_process`
      ADD INDEX `idx_mes_schedule_order_process_predecessor`
      (`tenant_id`, `schedule_order_id`, `predecessor_route_process_id`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution_task'
      AND column_name = 'predecessor_route_process_id'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `predecessor_route_process_id` bigint NULL
      COMMENT '直接前置路线工序ID快照' AFTER `route_process_id`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution_task'
      AND column_name = 'root_process_flag'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `root_process_flag` bit(1) NOT NULL DEFAULT b'0'
      COMMENT '是否根工序快照' AFTER `predecessor_route_process_id`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution_task'
      AND index_name = 'idx_mes_edhr_batch_task_predecessor'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD INDEX `idx_mes_edhr_batch_task_predecessor`
      (`tenant_id`, `batch_execution_id`, `predecessor_route_process_id`);
  END IF;
END//

DELIMITER ;

CALL intruoyi_upgrade_mes_route_process_single_entry_multi_exit();

DROP PROCEDURE IF EXISTS intruoyi_upgrade_mes_route_process_single_entry_multi_exit;
