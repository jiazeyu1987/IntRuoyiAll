-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_pqc_item_equipment_standard_snapshot; type=schema; riskLevel=high
-- QA 规程以 DCC 项目代码为唯一所有权，QA 工序独立于 MES 工艺路线工序。
-- MySQL DDL 会隐式提交；本脚本先执行完整前置校验，再逐项执行可重复的结构变更。
-- 回滚：先停止新写入并备份新表；删除新唯一键/列/工序表，再恢复旧列非空和旧唯一键。

DROP PROCEDURE IF EXISTS migrate_mes_qa_dcc_project_scope;
DELIMITER $$
CREATE PROCEDURE migrate_mes_qa_dcc_project_scope()
BEGIN
  DECLARE v_missing_table_count int DEFAULT 0;

  SELECT COUNT(1)
    INTO v_missing_table_count
    FROM (
      SELECT 'mes_qa_inspection_regulation' AS table_name
      UNION ALL SELECT 'mes_qa_inspection_regulation_version'
      UNION ALL SELECT 'mes_qa_inspection_regulation_item'
      UNION ALL SELECT 'mes_pqc_inspection_task'
    ) required_table
   WHERE NOT EXISTS (
     SELECT 1
       FROM information_schema.tables existing_table
      WHERE existing_table.table_schema = DATABASE()
        AND existing_table.table_name = required_table.table_name
   );
  IF v_missing_table_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'required QA/PQC base tables are missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation'
       AND column_name = 'dcc_project_code_id'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation`
      ADD COLUMN `dcc_project_code_id` bigint DEFAULT NULL COMMENT 'DCC项目代码ID' AFTER `id`;
  END IF;

  ALTER TABLE `mes_qa_inspection_regulation`
    MODIFY COLUMN `product_id` bigint NULL COMMENT '历史产品ID快照',
    MODIFY COLUMN `route_id` bigint NULL COMMENT '历史工艺路线ID快照',
    MODIFY COLUMN `route_version_id` bigint NULL COMMENT '历史工艺路线版本ID快照',
    MODIFY COLUMN `route_process_id` bigint NULL COMMENT '历史工艺路线工序ID快照',
    MODIFY COLUMN `process_id` bigint NULL COMMENT '历史MES工序ID快照';

  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation'
       AND index_name = 'uk_mes_qa_regulation_route_process'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation`
      DROP INDEX `uk_mes_qa_regulation_route_process`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation'
       AND index_name = 'uk_mes_qa_regulation_dcc_project'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation`
      ADD UNIQUE KEY `uk_mes_qa_regulation_dcc_project`
        (`tenant_id`, `dcc_project_code_id`, `deleted`);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation'
       AND index_name = 'uk_mes_qa_regulation_code'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation`
      DROP INDEX `uk_mes_qa_regulation_code`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation'
       AND index_name = 'idx_mes_qa_regulation_code'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation`
      ADD KEY `idx_mes_qa_regulation_code`
        (`tenant_id`, `regulation_code`, `deleted`);
  END IF;

  CREATE TABLE IF NOT EXISTS `mes_qa_inspection_regulation_process` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `regulation_version_id` bigint NOT NULL COMMENT 'QA检验规程版本ID',
    `process_code` varchar(64) NOT NULL COMMENT 'QA工序稳定编码',
    `process_name` varchar(128) NOT NULL COMMENT 'QA工序名称',
    `sort` int NOT NULL COMMENT 'QA工序排序',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_qa_regulation_process_code`
      (`tenant_id`, `regulation_version_id`, `process_code`, `deleted`),
    UNIQUE KEY `uk_mes_qa_regulation_process_sort`
      (`tenant_id`, `regulation_version_id`, `sort`, `deleted`),
    KEY `idx_mes_qa_regulation_process_version`
      (`tenant_id`, `regulation_version_id`, `sort`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES QA规程版本工序';

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_version'
       AND column_name = 'effective_date'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_version`
      ADD COLUMN `effective_date` date DEFAULT NULL COMMENT '生效日期' AFTER `lifecycle_status`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_version'
       AND column_name = 'inspection_type_rules_json'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_version`
      ADD COLUMN `inspection_type_rules_json` longtext COMMENT '检验类型规则JSON' AFTER `effective_date`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'qa_process_id'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `qa_process_id` bigint DEFAULT NULL COMMENT 'QA工序ID' AFTER `regulation_version_id`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'item_sort'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `item_sort` int DEFAULT NULL COMMENT 'QA工序内项目排序' AFTER `qa_process_id`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'critical'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `critical` bit(1) DEFAULT NULL COMMENT '是否关键检验项目' AFTER `patrol_inspection_ratio`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'failure_rule'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `failure_rule` varchar(1024) DEFAULT NULL COMMENT '不合格处理规则' AFTER `critical`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'source_note'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `source_note` varchar(512) DEFAULT NULL COMMENT '来源说明' AFTER `failure_rule`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'source_original_page'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `source_original_page` int DEFAULT NULL COMMENT '来源原页码' AFTER `source_note`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'source_original_item'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `source_original_item` varchar(512) DEFAULT NULL COMMENT '来源原项目' AFTER `source_original_page`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'source_original_excerpt'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `source_original_excerpt` text COMMENT '来源原文摘录' AFTER `source_original_item`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND column_name = 'source_original_method'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD COLUMN `source_original_method` text COMMENT '来源原检验方法' AFTER `source_original_excerpt`;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_qa_inspection_regulation_item'
       AND index_name = 'idx_mes_qa_regulation_item_process'
  ) THEN
    ALTER TABLE `mes_qa_inspection_regulation_item`
      ADD KEY `idx_mes_qa_regulation_item_process`
        (`tenant_id`, `regulation_version_id`, `qa_process_id`, `item_sort`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND column_name = 'qa_process_id'
  ) THEN
    ALTER TABLE `mes_pqc_inspection_task`
      ADD COLUMN `qa_process_id` bigint DEFAULT NULL COMMENT 'QA工序ID' AFTER `process_id`;
  END IF;

  ALTER TABLE `mes_pqc_inspection_task`
    MODIFY COLUMN `route_process_id` bigint NULL COMMENT '生产路线工序追溯快照',
    MODIFY COLUMN `process_id` bigint NULL COMMENT '生产MES工序追溯快照';

  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND index_name = 'uk_mes_pqc_task_identity'
  ) THEN
    ALTER TABLE `mes_pqc_inspection_task`
      DROP INDEX `uk_mes_pqc_task_identity`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pqc_inspection_task'
       AND index_name = 'uk_mes_pqc_task_qa_identity'
  ) THEN
    ALTER TABLE `mes_pqc_inspection_task`
      ADD UNIQUE KEY `uk_mes_pqc_task_qa_identity`
        (`tenant_id`, `active_order_id`, `regulation_version_id`, `qa_process_id`, `inspection_type`, `business_date`, `shift_code`, `round_no`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool'
       AND column_name = 'qa_process_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool`
      ADD COLUMN `qa_process_id` bigint DEFAULT NULL COMMENT 'QA工序ID；仅PQC事件使用' AFTER `process_id`;
  END IF;
  ALTER TABLE `mes_pro_process_pool`
    MODIFY COLUMN `route_process_id` bigint NULL COMMENT '生产路线工序ID；PQC QA工序池为空',
    MODIFY COLUMN `process_id` bigint NULL COMMENT '生产MES工序ID；PQC QA工序池为空';
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool'
       AND index_name = 'uk_mes_pro_process_pool_qa_context'
  ) THEN
    ALTER TABLE `mes_pro_process_pool`
      ADD UNIQUE KEY `uk_mes_pro_process_pool_qa_context`
        (`tenant_id`, `work_order_id`, `route_id`, `qa_process_id`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_event'
       AND column_name = 'qa_process_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event`
      ADD COLUMN `qa_process_id` bigint DEFAULT NULL COMMENT 'QA工序ID；仅PQC事件使用' AFTER `process_id`;
  END IF;
  ALTER TABLE `mes_pro_process_pool_event`
    MODIFY COLUMN `route_process_id` bigint NULL COMMENT '生产路线工序ID；PQC事件为空',
    MODIFY COLUMN `process_id` bigint NULL COMMENT '生产MES工序ID；PQC事件为空';
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_event'
       AND index_name = 'uk_mes_pro_process_pool_event_qa_idem'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_event`
      ADD UNIQUE KEY `uk_mes_pro_process_pool_event_qa_idem`
        (`tenant_id`, `event_type`, `work_order_id`, `qa_process_id`, `actual_employee_id`, `event_idempotency_key`, `deleted`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_pqc_record'
       AND column_name = 'qa_process_id'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_pqc_record`
      ADD COLUMN `qa_process_id` bigint DEFAULT NULL COMMENT 'QA工序ID' AFTER `process_id`;
  END IF;
  ALTER TABLE `mes_pro_process_pool_pqc_record`
    MODIFY COLUMN `route_process_id` bigint NULL COMMENT '生产路线工序追溯字段；QA独立PQC为空',
    MODIFY COLUMN `process_id` bigint NULL COMMENT '生产MES工序追溯字段；QA独立PQC为空';
END$$
DELIMITER ;

CALL migrate_mes_qa_dcc_project_scope();

DROP PROCEDURE IF EXISTS migrate_mes_qa_dcc_project_scope;
