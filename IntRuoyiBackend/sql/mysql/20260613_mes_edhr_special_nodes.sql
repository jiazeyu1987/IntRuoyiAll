-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_edhr_special_node_columns;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_special_node_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND COLUMN_NAME = 'node_type') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `node_type` varchar(64) NULL COMMENT '节点类型：ROUTE_FORM/INCOMING_INSPECTION_REPORT/STERILIZATION_REPORT/FINISHED_PRODUCT_INSPECTION_REPORT/FINISHED_PRODUCT_INSPECTION_RECORD' AFTER `batch_execution_id`;
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND COLUMN_NAME = 'route_process_id' AND IS_NULLABLE = 'NO') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      MODIFY COLUMN `route_process_id` bigint NULL COMMENT '路线工序 ID；特殊无模板节点为空';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND COLUMN_NAME = 'skipped_by') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `skipped_by` bigint NULL COMMENT '跳过人' AFTER `approved_at`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND COLUMN_NAME = 'skipped_at') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `skipped_at` datetime NULL COMMENT '跳过时间' AFTER `skipped_by`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_edhr_batch_execution_task' AND COLUMN_NAME = 'special_payload_json') THEN
    ALTER TABLE `mes_pro_edhr_batch_execution_task`
      ADD COLUMN `special_payload_json` text NULL COMMENT '特殊无模板节点完成载荷 JSON' AFTER `skipped_at`;
  END IF;
END//
DELIMITER ;

CALL ensure_mes_edhr_special_node_columns();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_special_node_columns;
