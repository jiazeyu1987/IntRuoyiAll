-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_process_pool_event_idempotency; type=schema; riskLevel=medium
-- 一线生产不匹配生产工单：工序池生产提交允许 work_order/recordbook 上下文为空，并保留无工单幂等唯一性

DROP PROCEDURE IF EXISTS ensure_mes_pp_frontline_no_work_order;
DELIMITER $$
CREATE PROCEDURE ensure_mes_pp_frontline_no_work_order()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool'
        AND column_name = 'work_order_id'
        AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_process_pool`
      MODIFY COLUMN `work_order_id` bigint DEFAULT NULL COMMENT '生产工单ID；一线生产可为空';
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool'
        AND column_name = 'work_order_context_key'
  ) THEN
    ALTER TABLE `mes_pro_process_pool`
      ADD COLUMN `work_order_context_key` varchar(64)
        GENERATED ALWAYS AS (COALESCE(CAST(`work_order_id` AS CHAR), 'NO_WORK_ORDER')) STORED
        COMMENT '工单上下文唯一键辅助列' AFTER `work_order_id`;
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool'
        AND index_name = 'uk_mes_pro_process_pool_context'
  ) THEN
    DROP INDEX `uk_mes_pro_process_pool_context` ON `mes_pro_process_pool`;
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool'
        AND index_name = 'uk_mes_pro_process_pool_context'
  ) THEN
    ALTER TABLE `mes_pro_process_pool`
      ADD UNIQUE KEY `uk_mes_pro_process_pool_context`
        (`tenant_id`, `work_order_context_key`, `route_id`, `route_process_id`, `process_id`,
         `device_id`, `workstation_id`, `deleted`);
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event'
        AND column_name = 'work_order_id'
        AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event`
      MODIFY COLUMN `work_order_id` bigint DEFAULT NULL COMMENT '生产工单ID；一线生产可为空';
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event'
        AND column_name = 'work_order_context_key'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event`
      ADD COLUMN `work_order_context_key` varchar(64)
        GENERATED ALWAYS AS (COALESCE(CAST(`work_order_id` AS CHAR), 'NO_WORK_ORDER')) STORED
        COMMENT '工单上下文幂等辅助列' AFTER `work_order_id`;
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event'
        AND column_name = 'recordbook_source_type'
        AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event`
      MODIFY COLUMN `recordbook_source_type` varchar(64) DEFAULT NULL COMMENT '记录本来源类型；一线生产无记录本时为空';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event'
        AND column_name = 'recordbook_source_id'
        AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event`
      MODIFY COLUMN `recordbook_source_id` bigint DEFAULT NULL COMMENT '记录本来源ID；一线生产无记录本时为空';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event'
        AND index_name = 'uk_mes_pro_process_pool_event_idem'
  ) THEN
    DROP INDEX `uk_mes_pro_process_pool_event_idem` ON `mes_pro_process_pool_event`;
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_event'
        AND index_name = 'uk_mes_pro_process_pool_event_idem'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event`
      ADD UNIQUE KEY `uk_mes_pro_process_pool_event_idem`
        (`tenant_id`, `event_type`, `work_order_context_key`, `route_process_id`, `process_id`,
         `actual_employee_id`, `event_idempotency_key`, `deleted`);
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_quantity_fragment'
        AND column_name = 'work_order_id'
        AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_quantity_fragment`
      MODIFY COLUMN `work_order_id` bigint DEFAULT NULL COMMENT '生产工单ID；一线生产可为空';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_pp_frontline_no_work_order();

DROP PROCEDURE IF EXISTS ensure_mes_pp_frontline_no_work_order;
