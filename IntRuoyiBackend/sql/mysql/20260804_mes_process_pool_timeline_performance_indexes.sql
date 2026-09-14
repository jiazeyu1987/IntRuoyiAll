-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_process_pool_pqc_process_inspection_aggregation; type=schema; riskLevel=medium
-- 20260804_mes_process_pool_timeline_performance_indexes
-- MES M6 AC-D32: make PQC leader submission timeline pagination indexable and stable.

DROP PROCEDURE IF EXISTS add_mes_pp_timeline_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_mes_pp_timeline_column_if_missing(
  IN p_table_name varchar(128),
  IN p_column_name varchar(128),
  IN p_column_ddl text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table_name
       AND column_name = p_column_name
  ) THEN
    SET @add_column_sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_ddl);
    PREPARE add_column_stmt FROM @add_column_sql;
    EXECUTE add_column_stmt;
    DEALLOCATE PREPARE add_column_stmt;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS add_mes_pp_timeline_index_if_missing;
DELIMITER $$
CREATE PROCEDURE add_mes_pp_timeline_index_if_missing(
  IN p_table_name varchar(128),
  IN p_index_name varchar(128),
  IN p_index_ddl text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = p_table_name
       AND index_name = p_index_name
  ) THEN
    SET @add_index_sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_index_ddl);
    PREPARE add_index_stmt FROM @add_index_sql;
    EXECUTE add_index_stmt;
    DEALLOCATE PREPARE add_index_stmt;
  END IF;
END$$
DELIMITER ;

CALL add_mes_pp_timeline_column_if_missing(
  'mes_pro_process_pool_event',
  'pqc_task_id',
  '`pqc_task_id` bigint GENERATED ALWAYS AS (CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(`raw_payload`, ''$.pqcTaskId'')) REGEXP ''^[0-9]+$'' THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(`raw_payload`, ''$.pqcTaskId'')) AS UNSIGNED) ELSE NULL END) STORED COMMENT ''PQC task id extracted from raw_payload for AC-D32 timeline joins'' AFTER `raw_payload`'
);

CALL add_mes_pp_timeline_index_if_missing(
  'mes_pro_process_pool_event',
  'idx_mes_pp_event_timeline_acd32',
  'KEY `idx_mes_pp_event_timeline_acd32` (`tenant_id`, `deleted`, `template_type`, `actual_employee_id`, `process_id`, `work_order_id`, `server_submit_time`, `id`)'
);

CALL add_mes_pp_timeline_index_if_missing(
  'mes_pqc_inspection_task',
  'idx_mes_pqc_task_timeline_acd32',
  'KEY `idx_mes_pqc_task_timeline_acd32` (`tenant_id`, `deleted`, `inspection_type`, `round_no`, `id`)'
);

CALL add_mes_pp_timeline_index_if_missing(
  'mes_pro_process_pool_submission_review',
  'idx_mes_pp_review_latest_event',
  'KEY `idx_mes_pp_review_latest_event` (`tenant_id`, `deleted`, `event_id`, `reviewed_at`, `id`)'
);

DROP PROCEDURE IF EXISTS add_mes_pp_timeline_column_if_missing;
DROP PROCEDURE IF EXISTS add_mes_pp_timeline_index_if_missing;
