-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_process_pool_pqc_process_inspection_aggregation; type=schema; riskLevel=medium
-- AC-M20：PQC 组长复核必须可审计、唯一终态，并闭环正式 PQC 检验任务。

DROP PROCEDURE IF EXISTS add_mes_pp_review_leader_type_if_missing;
DELIMITER $$
CREATE PROCEDURE add_mes_pp_review_leader_type_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_submission_review'
       AND column_name = 'leader_type'
  ) THEN
    ALTER TABLE `mes_pro_process_pool_submission_review`
      ADD COLUMN `leader_type` varchar(32) DEFAULT NULL COMMENT '复核组长类型：PRODUCTION/PQC' AFTER `leader_user_id`;
  END IF;
END$$
DELIMITER ;

CALL add_mes_pp_review_leader_type_if_missing();

DROP PROCEDURE IF EXISTS add_mes_pp_review_leader_type_if_missing;

UPDATE `mes_pro_process_pool_submission_review` `review`
  JOIN `mes_pro_process_pool_event` `event`
    ON `event`.`tenant_id` = `review`.`tenant_id`
   AND `event`.`id` = `review`.`event_id`
   AND `event`.`deleted` = b'0'
   SET `review`.`leader_type` = CASE
     WHEN `event`.`event_type` = 'PQC_INSPECTION' THEN 'PQC'
     ELSE 'PRODUCTION'
   END
 WHERE `review`.`deleted` = b'0'
   AND `review`.`leader_type` IS NULL;

SET @missing_review_leader_type := (
  SELECT COUNT(1)
    FROM `mes_pro_process_pool_submission_review`
   WHERE `deleted` = b'0'
     AND `leader_type` IS NULL
);
SET @review_leader_type_sql := IF(
  @missing_review_leader_type = 0,
  'ALTER TABLE `mes_pro_process_pool_submission_review` MODIFY COLUMN `leader_type` varchar(32) NOT NULL COMMENT ''复核组长类型：PRODUCTION/PQC''',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''mes_pro_process_pool_submission_review requires leader_type backfill before NOT NULL migration'''
);
PREPARE review_leader_type_stmt FROM @review_leader_type_sql;
EXECUTE review_leader_type_stmt;
DEALLOCATE PREPARE review_leader_type_stmt;

SET @duplicate_review_event_count := (
  SELECT COUNT(1)
    FROM (
      SELECT `tenant_id`, `event_id`
        FROM `mes_pro_process_pool_submission_review`
       WHERE `deleted` = b'0'
       GROUP BY `tenant_id`, `event_id`
      HAVING COUNT(1) > 1
    ) duplicate_reviews
);
SET @review_unique_sql := IF(
  @duplicate_review_event_count = 0,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''mes_pro_process_pool_submission_review has duplicate active terminal reviews'''
);
PREPARE review_unique_stmt FROM @review_unique_sql;
EXECUTE review_unique_stmt;
DEALLOCATE PREPARE review_unique_stmt;

SET @review_unique_exists := (
  SELECT COUNT(1)
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pro_process_pool_submission_review'
     AND index_name = 'uk_mes_pp_submission_review_event_terminal'
);
SET @review_unique_sql := IF(
  @review_unique_exists = 0,
  'ALTER TABLE `mes_pro_process_pool_submission_review` ADD UNIQUE KEY `uk_mes_pp_submission_review_event_terminal` (`tenant_id`, `event_id`, `deleted`)',
  'SELECT 1'
);
PREPARE review_unique_stmt FROM @review_unique_sql;
EXECUTE review_unique_stmt;
DEALLOCATE PREPARE review_unique_stmt;

ALTER TABLE `mes_pqc_inspection_task`
  MODIFY COLUMN `task_status` varchar(32) NOT NULL COMMENT '任务状态：PENDING/SUBMITTED/CONFIRMED/CANCELLED';

CREATE TABLE IF NOT EXISTS `mes_pqc_process_inspection_aggregate_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `source_pqc_record_id` bigint NOT NULL COMMENT '来源工序池PQC记录ID',
  `event_id` bigint NOT NULL COMMENT 'PQC提交事件ID',
  `review_id` bigint NOT NULL COMMENT 'PQC组长复核ID',
  `production_submit_event_id` bigint NOT NULL COMMENT '绑定生产提交事件ID',
  `pqc_task_id` bigint NOT NULL COMMENT 'PQC检验任务ID',
  `active_order_id` bigint DEFAULT NULL COMMENT '活跃订单ID',
  `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID',
  `route_version_id` bigint NOT NULL COMMENT '工艺路线版本ID',
  `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
  `process_id` bigint NOT NULL COMMENT '工序ID',
  `regulation_version_id` bigint NOT NULL COMMENT 'QA规程发布版本ID',
  `inspection_type` varchar(32) NOT NULL COMMENT '检验类型',
  `business_date` date NOT NULL COMMENT '业务日期',
  `shift_code` varchar(32) NOT NULL COMMENT '班次编码',
  `round_no` int NOT NULL COMMENT '轮次',
  `source_piece_detail_id` bigint NOT NULL COMMENT '来源逐件检验明细ID',
  `sample_no` int NOT NULL COMMENT '逐件样本序号',
  `item_code` varchar(64) NOT NULL COMMENT '检验项目编码',
  `item_name` varchar(128) NOT NULL COMMENT '检验项目名称',
  `inspection_method` varchar(512) NOT NULL COMMENT '检验方法',
  `standard_text` varchar(1024) NOT NULL COMMENT '合格标准',
  `selected_equipment_id` bigint DEFAULT NULL COMMENT '实际检验设备ID快照',
  `selected_equipment_code` varchar(64) DEFAULT NULL COMMENT '实际检验设备编码快照',
  `selected_equipment_name` varchar(128) DEFAULT NULL COMMENT '实际检验设备名称快照',
  `selected_equipment_number` varchar(64) DEFAULT NULL COMMENT '实际检验设备编号快照',
  `standard_lower_limit` decimal(18,6) DEFAULT NULL COMMENT '接收标准下限快照',
  `standard_upper_limit` decimal(18,6) DEFAULT NULL COMMENT '接收标准上限快照',
  `standard_unit` varchar(32) DEFAULT NULL COMMENT '接收标准单位快照',
  `standard_precision` int DEFAULT NULL COMMENT '接收标准精度快照',
  `result_type` varchar(32) NOT NULL COMMENT '结果类型',
  `item_result` varchar(64) DEFAULT NULL COMMENT '检验结果',
  `measured_value` varchar(128) DEFAULT NULL COMMENT '实测值',
  `judgement` varchar(32) DEFAULT NULL COMMENT '判定',
  `aggregated_at` datetime NOT NULL COMMENT '汇集时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pqc_process_aggregate_piece` (`tenant_id`, `source_piece_detail_id`, `review_id`, `deleted`),
  KEY `idx_mes_pqc_process_aggregate_event` (`tenant_id`, `event_id`, `review_id`),
  KEY `idx_mes_pqc_process_aggregate_task` (`tenant_id`, `pqc_task_id`, `sample_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES PQC 过程检验汇集明细';
