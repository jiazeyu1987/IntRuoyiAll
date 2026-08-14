-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_process_pool_active_order_authority; type=schema; riskLevel=medium
-- AC-D04：生产组长按工艺路线“工序开始”授权维护 route_process 共享损耗原因，报工保存原因快照

DROP PROCEDURE IF EXISTS ensure_mes_process_loss_reason_schema;
DELIMITER $$
CREATE PROCEDURE ensure_mes_process_loss_reason_schema()
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_defect_reason'
        AND index_name = 'uk_mes_pp_defect_reason'
  ) THEN
    DROP INDEX `uk_mes_pp_defect_reason` ON `mes_pro_process_pool_defect_reason`;
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_defect_reason'
        AND column_name = 'leader_user_id'
        AND is_nullable = 'YES'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_defect_reason`
      MODIFY COLUMN `leader_user_id` bigint DEFAULT NULL COMMENT '最后维护班组长用户ID，LOSS 原因不作为所有权字段';
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_defect_reason'
        AND index_name = 'uk_mes_pp_loss_reason_route_process'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_defect_reason`
      ADD UNIQUE KEY `uk_mes_pp_loss_reason_route_process` (`tenant_id`, `route_process_id`, `reason_type`, `reason_code`, `deleted`);
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_process_pool_defect_reason'
        AND index_name = 'idx_mes_pp_loss_reason_route_process'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_defect_reason`
      ADD KEY `idx_mes_pp_loss_reason_route_process` (`tenant_id`, `route_process_id`, `reason_type`, `enabled`);
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_feedback'
        AND column_name = 'loss_reason_id'
  ) THEN
    ALTER TABLE `mes_pro_feedback`
      ADD COLUMN `loss_reason_id` bigint DEFAULT NULL COMMENT '损耗原因ID快照来源' AFTER `other_scrap_quantity`;
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_feedback'
        AND column_name = 'loss_reason_code_snapshot'
  ) THEN
    ALTER TABLE `mes_pro_feedback`
      ADD COLUMN `loss_reason_code_snapshot` varchar(64) DEFAULT NULL COMMENT '损耗原因编码快照' AFTER `loss_reason_id`;
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_feedback'
        AND column_name = 'loss_reason_name_snapshot'
  ) THEN
    ALTER TABLE `mes_pro_feedback`
      ADD COLUMN `loss_reason_name_snapshot` varchar(255) DEFAULT NULL COMMENT '损耗原因名称快照' AFTER `loss_reason_code_snapshot`;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_process_loss_reason_schema();

DROP PROCEDURE IF EXISTS ensure_mes_process_loss_reason_schema;
