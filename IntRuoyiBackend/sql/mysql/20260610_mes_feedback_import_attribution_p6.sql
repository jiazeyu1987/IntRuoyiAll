-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 排产 P6：第三方报工导入待归属

SET @feedback_import_attribution_status_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'attribution_status'
);
SET @feedback_import_attribution_status_sql = IF(
    @feedback_import_attribution_status_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `attribution_status` varchar(32) NOT NULL DEFAULT ''PENDING'' COMMENT ''归属状态'' AFTER `feedback_id`',
    'SELECT 1'
);
PREPARE feedback_import_attribution_status_stmt FROM @feedback_import_attribution_status_sql;
EXECUTE feedback_import_attribution_status_stmt;
DEALLOCATE PREPARE feedback_import_attribution_status_stmt;

SET @feedback_import_work_order_code_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'work_order_code'
);
SET @feedback_import_work_order_code_sql = IF(
    @feedback_import_work_order_code_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `work_order_code` varchar(64) DEFAULT NULL COMMENT ''来源工单编码'' AFTER `task_code`',
    'SELECT 1'
);
PREPARE feedback_import_work_order_code_stmt FROM @feedback_import_work_order_code_sql;
EXECUTE feedback_import_work_order_code_stmt;
DEALLOCATE PREPARE feedback_import_work_order_code_stmt;

SET @feedback_import_item_code_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'item_code'
);
SET @feedback_import_item_code_sql = IF(
    @feedback_import_item_code_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `item_code` varchar(64) DEFAULT NULL COMMENT ''来源产品编码'' AFTER `work_order_code`',
    'SELECT 1'
);
PREPARE feedback_import_item_code_stmt FROM @feedback_import_item_code_sql;
EXECUTE feedback_import_item_code_stmt;
DEALLOCATE PREPARE feedback_import_item_code_stmt;

SET @feedback_import_process_code_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'process_code'
);
SET @feedback_import_process_code_sql = IF(
    @feedback_import_process_code_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `process_code` varchar(64) DEFAULT NULL COMMENT ''来源工序编码'' AFTER `item_code`',
    'SELECT 1'
);
PREPARE feedback_import_process_code_stmt FROM @feedback_import_process_code_sql;
EXECUTE feedback_import_process_code_stmt;
DEALLOCATE PREPARE feedback_import_process_code_stmt;

SET @feedback_import_source_payload_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'source_payload_json'
);
SET @feedback_import_source_payload_sql = IF(
    @feedback_import_source_payload_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `source_payload_json` text DEFAULT NULL COMMENT ''来源行快照JSON'' AFTER `process_code`',
    'SELECT 1'
);
PREPARE feedback_import_source_payload_stmt FROM @feedback_import_source_payload_sql;
EXECUTE feedback_import_source_payload_stmt;
DEALLOCATE PREPARE feedback_import_source_payload_stmt;

SET @feedback_import_schedule_order_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'schedule_order_id'
);
SET @feedback_import_schedule_order_id_sql = IF(
    @feedback_import_schedule_order_id_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `schedule_order_id` bigint DEFAULT NULL COMMENT ''归属排产工单ID'' AFTER `source_payload_json`',
    'SELECT 1'
);
PREPARE feedback_import_schedule_order_id_stmt FROM @feedback_import_schedule_order_id_sql;
EXECUTE feedback_import_schedule_order_id_stmt;
DEALLOCATE PREPARE feedback_import_schedule_order_id_stmt;

SET @feedback_import_schedule_order_process_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'schedule_order_process_id'
);
SET @feedback_import_schedule_order_process_id_sql = IF(
    @feedback_import_schedule_order_process_id_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `schedule_order_process_id` bigint DEFAULT NULL COMMENT ''归属排产工单工序ID'' AFTER `schedule_order_id`',
    'SELECT 1'
);
PREPARE feedback_import_schedule_order_process_id_stmt FROM @feedback_import_schedule_order_process_id_sql;
EXECUTE feedback_import_schedule_order_process_id_stmt;
DEALLOCATE PREPARE feedback_import_schedule_order_process_id_stmt;

SET @feedback_import_candidate_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback_import_record'
      AND COLUMN_NAME = 'candidate_count'
);
SET @feedback_import_candidate_count_sql = IF(
    @feedback_import_candidate_count_exists = 0,
    'ALTER TABLE `mes_pro_feedback_import_record` ADD COLUMN `candidate_count` int DEFAULT NULL COMMENT ''候选数量'' AFTER `schedule_order_process_id`',
    'SELECT 1'
);
PREPARE feedback_import_candidate_count_stmt FROM @feedback_import_candidate_count_sql;
EXECUTE feedback_import_candidate_count_stmt;
DEALLOCATE PREPARE feedback_import_candidate_count_stmt;

SET @feedback_schedule_order_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback'
      AND COLUMN_NAME = 'schedule_order_id'
);
SET @feedback_schedule_order_id_sql = IF(
    @feedback_schedule_order_id_exists = 0,
    'ALTER TABLE `mes_pro_feedback` ADD COLUMN `schedule_order_id` bigint DEFAULT NULL COMMENT ''排产工单ID'' AFTER `task_id`',
    'SELECT 1'
);
PREPARE feedback_schedule_order_id_stmt FROM @feedback_schedule_order_id_sql;
EXECUTE feedback_schedule_order_id_stmt;
DEALLOCATE PREPARE feedback_schedule_order_id_stmt;

SET @feedback_schedule_order_process_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_feedback'
      AND COLUMN_NAME = 'schedule_order_process_id'
);
SET @feedback_schedule_order_process_id_sql = IF(
    @feedback_schedule_order_process_id_exists = 0,
    'ALTER TABLE `mes_pro_feedback` ADD COLUMN `schedule_order_process_id` bigint DEFAULT NULL COMMENT ''排产工单工序ID'' AFTER `schedule_order_id`',
    'SELECT 1'
);
PREPARE feedback_schedule_order_process_id_stmt FROM @feedback_schedule_order_process_id_sql;
EXECUTE feedback_schedule_order_process_id_stmt;
DEALLOCATE PREPARE feedback_schedule_order_process_id_stmt;

UPDATE `mes_pro_feedback_import_record` record
LEFT JOIN `mes_pro_feedback` feedback ON feedback.`id` = record.`feedback_id` AND feedback.`deleted` = b'0'
LEFT JOIN `mes_pro_work_order` work_order ON work_order.`id` = feedback.`work_order_id` AND work_order.`deleted` = b'0'
LEFT JOIN `mes_md_item` item ON item.`id` = feedback.`item_id` AND item.`deleted` = b'0'
LEFT JOIN `mes_pro_process` process ON process.`id` = feedback.`process_id` AND process.`deleted` = b'0'
SET
    record.`attribution_status` = 'ATTRIBUTED',
    record.`work_order_code` = COALESCE(record.`work_order_code`, work_order.`code`),
    record.`item_code` = COALESCE(record.`item_code`, item.`code`),
    record.`process_code` = COALESCE(record.`process_code`, process.`code`),
    record.`source_payload_json` = COALESCE(
        record.`source_payload_json`,
        JSON_OBJECT(
            'sheetName', record.`sheet_name`,
            'rowNo', record.`row_no`,
            'taskCode', record.`task_code`,
            'workOrderCode', work_order.`code`,
            'itemCode', item.`code`,
            'itemName', item.`name`,
            'specification', item.`specification`,
            'processCode', process.`code`,
            'processName', process.`name`,
            'feedbackQuantity', feedback.`feedback_quantity`,
            'feedbackTime', DATE_FORMAT(feedback.`feedback_time`, '%Y-%m-%d %H:%i:%s'),
            'feedbackUserCode', NULL,
            'feedbackUserName', NULL,
            'approverName', NULL
        )
    ),
    record.`schedule_order_id` = COALESCE(record.`schedule_order_id`, feedback.`schedule_order_id`),
    record.`schedule_order_process_id` = COALESCE(record.`schedule_order_process_id`, feedback.`schedule_order_process_id`)
WHERE record.`deleted` = b'0'
  AND record.`feedback_id` IS NOT NULL
  AND record.`feedback_id` > 0
  AND (
      record.`attribution_status` = 'PENDING'
      OR record.`source_payload_json` IS NULL
      OR record.`work_order_code` IS NULL
      OR record.`item_code` IS NULL
      OR record.`process_code` IS NULL
  );
