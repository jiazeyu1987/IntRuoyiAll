-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_qa_dcc_project_scope; type=schema; riskLevel=high
-- C00: freeze route-DCC relation, active-order QA snapshots, PQC task rule identity, and one-task-one-formal-PQC-event schema.
-- MySQL DDL implicitly commits. Run C00 preflight and stop active-order/PQC submit writes before applying this schema.

DROP PROCEDURE IF EXISTS migrate_mes_pqc_dcc_qa_c00_schema;
DELIMITER $$
CREATE PROCEDURE migrate_mes_pqc_dcc_qa_c00_schema()
BEGIN
  DECLARE v_missing_table_count int DEFAULT 0;

  SELECT COUNT(1)
    INTO v_missing_table_count
    FROM (
      SELECT 'mes_qa_inspection_regulation' AS table_name
      UNION ALL SELECT 'mes_qa_inspection_regulation_version'
      UNION ALL SELECT 'mes_pro_process_pool_active_order'
      UNION ALL SELECT 'mes_pqc_inspection_task'
      UNION ALL SELECT 'mes_pro_process_pool_event'
    ) required_table
   WHERE NOT EXISTS (
     SELECT 1
       FROM information_schema.tables existing_table
      WHERE existing_table.table_schema = DATABASE()
        AND existing_table.table_name = required_table.table_name
   );
  IF v_missing_table_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C00 requires QA/DCC active-order, PQC task, and process-pool event base tables';
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_pro_route_dcc_project_binding` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `dcc_project_code_id` bigint NOT NULL COMMENT 'DCC项目代码ID',
    `version` bigint NOT NULL COMMENT '同租户同路线单调递增版本',
    `active_route_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `deleted` = b'0' THEN `route_id` ELSE NULL END) STORED COMMENT '未删除当前路线唯一键',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_route_dcc_current` (`tenant_id`, `active_route_id`),
    UNIQUE KEY `uk_mes_pro_route_dcc_history_version` (`tenant_id`, `route_id`, `version`),
    KEY `idx_mes_pro_route_dcc_project` (`tenant_id`, `dcc_project_code_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES工艺路线与DCC项目代码正式关系';

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
       AND column_name = 'dcc_project_code_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order`
      ADD COLUMN `dcc_project_code_id` bigint DEFAULT NULL COMMENT '订单锁定DCC项目代码ID' AFTER `route_version_id`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
       AND column_name = 'qa_regulation_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order`
      ADD COLUMN `qa_regulation_id` bigint DEFAULT NULL COMMENT '订单锁定QA规程ID' AFTER `dcc_project_code_id`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
       AND column_name = 'qa_regulation_version_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order`
      ADD COLUMN `qa_regulation_version_id` bigint DEFAULT NULL COMMENT '订单锁定QA规程发布版本ID' AFTER `qa_regulation_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_active_order'
       AND index_name = 'idx_mes_active_order_qa_snapshot'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_active_order`
      ADD KEY `idx_mes_active_order_qa_snapshot` (`tenant_id`, `dcc_project_code_id`, `qa_regulation_version_id`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND column_name = 'inspection_rule_key'
  ) THEN
    ALTER TABLE `mes_pqc_inspection_task`
      ADD COLUMN `inspection_rule_key` varchar(32) DEFAULT NULL COMMENT '正式检验规则身份' AFTER `inspection_type`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND column_name = 'submitted_content_hash'
  ) THEN
    ALTER TABLE `mes_pqc_inspection_task`
      ADD COLUMN `submitted_content_hash` char(64) DEFAULT NULL COMMENT 'CanonicalPqcSubmissionV1内容哈希' AFTER `task_status`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND column_name = 'submitted_event_id'
  ) THEN
    ALTER TABLE `mes_pqc_inspection_task`
      ADD COLUMN `submitted_event_id` bigint DEFAULT NULL COMMENT '唯一正式PQC提交事件ID' AFTER `submitted_content_hash`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_event'
       AND column_name = 'pqc_submission_task_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event`
      ADD COLUMN `pqc_submission_task_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `deleted` = b'0' AND `event_type` = 'PQC_INSPECTION' AND `feedback_source_type` = 'MES_PQC_INSPECTION_TASK' THEN `feedback_source_id` ELSE NULL END) STORED COMMENT '正式PQC任务提交事件唯一键' AFTER `feedback_source_id`;
  END IF;
END$$
DELIMITER ;

CALL migrate_mes_pqc_dcc_qa_c00_schema();

DROP PROCEDURE IF EXISTS migrate_mes_pqc_dcc_qa_c00_schema;
