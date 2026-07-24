-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260608_edhr_batch_execution_schema; type=schema; riskLevel=medium
-- eDHR 批次执行活动上下文唯一键：作废批次释放工单/批号/路线组合，允许同一生产工单重新生成新的有效批次执行。
-- Rollback: DROP INDEX uk_mes_pro_edhr_batch_execution_active_context ON mes_pro_edhr_batch_execution;
-- Rollback: ALTER TABLE mes_pro_edhr_batch_execution ADD UNIQUE INDEX uk_mes_pro_edhr_batch_execution_context (tenant_id, work_order_id, batch_code, route_id, deleted);
-- Rollback: ALTER TABLE mes_pro_edhr_batch_execution DROP COLUMN active_context_key;

DROP PROCEDURE IF EXISTS intruoyi_upgrade_mes_edhr_batch_execution_active_context;

DELIMITER //

CREATE PROCEDURE intruoyi_upgrade_mes_edhr_batch_execution_active_context()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'missing mes_pro_edhr_batch_execution';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
      AND column_name = 'active_context_key'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD COLUMN `active_context_key` varchar(255) DEFAULT NULL
      COMMENT '活动态上下文唯一键' AFTER `batch_code`;
  END IF;

  UPDATE `mes_pro_edhr_batch_execution`
     SET `active_context_key` = CONCAT(`work_order_id`, '|', `route_id`, '|', `batch_code`)
   WHERE `deleted` = b'0'
     AND `status` <> 60
     AND `active_context_key` IS NULL;

  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
      AND index_name = 'uk_mes_pro_edhr_batch_execution_context'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      DROP INDEX `uk_mes_pro_edhr_batch_execution_context`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_edhr_batch_execution'
      AND index_name = 'uk_mes_pro_edhr_batch_execution_active_context'
  ) THEN
    ALTER TABLE `mes_pro_edhr_batch_execution`
      ADD UNIQUE INDEX `uk_mes_pro_edhr_batch_execution_active_context`
      (`tenant_id`, `active_context_key`);
  END IF;
END//

DELIMITER ;

CALL intruoyi_upgrade_mes_edhr_batch_execution_active_context();

DROP PROCEDURE IF EXISTS intruoyi_upgrade_mes_edhr_batch_execution_active_context;
