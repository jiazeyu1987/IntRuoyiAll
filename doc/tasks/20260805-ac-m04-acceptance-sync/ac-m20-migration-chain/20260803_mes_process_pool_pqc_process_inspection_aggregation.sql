-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_process_pool_pqc_event_source; type=schema; riskLevel=medium
-- MES M6: PQC records only become formal process-inspection evidence after terminal leader approval.

DROP PROCEDURE IF EXISTS add_mes_pp_pqc_record_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_mes_pp_pqc_record_column_if_missing(
  IN p_column_name varchar(128),
  IN p_column_ddl text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_process_pool_pqc_record'
       AND column_name = p_column_name
  ) THEN
    SET @add_column_sql = CONCAT('ALTER TABLE `mes_pro_process_pool_pqc_record` ADD COLUMN ', p_column_ddl);
    PREPARE add_column_stmt FROM @add_column_sql;
    EXECUTE add_column_stmt;
    DEALLOCATE PREPARE add_column_stmt;
  END IF;
END$$
DELIMITER ;

CALL add_mes_pp_pqc_record_column_if_missing(
  'process_inspection_aggregation_status',
  '`process_inspection_aggregation_status` varchar(32) NOT NULL DEFAULT ''PENDING'' COMMENT ''过程检验汇集状态：PENDING/AGGREGATED'' AFTER `raw_payload`'
);
CALL add_mes_pp_pqc_record_column_if_missing(
  'process_inspection_review_id',
  '`process_inspection_review_id` bigint DEFAULT NULL COMMENT ''触发过程检验汇集的PQC组长复核ID'' AFTER `process_inspection_aggregation_status`'
);
CALL add_mes_pp_pqc_record_column_if_missing(
  'process_inspection_aggregated_at',
  '`process_inspection_aggregated_at` datetime DEFAULT NULL COMMENT ''过程检验汇集时间'' AFTER `process_inspection_review_id`'
);

DROP PROCEDURE IF EXISTS add_mes_pp_pqc_record_column_if_missing;

SET @idx_exists = (
  SELECT COUNT(1)
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pro_process_pool_pqc_record'
     AND index_name = 'idx_mes_pp_pqc_process_inspection'
);
SET @add_idx_sql = IF(
  @idx_exists = 0,
  'ALTER TABLE `mes_pro_process_pool_pqc_record` ADD KEY `idx_mes_pp_pqc_process_inspection` (`tenant_id`, `work_order_id`, `route_process_id`, `process_id`, `process_inspection_aggregation_status`)',
  'SELECT 1'
);
PREPARE add_idx_stmt FROM @add_idx_sql;
EXECUTE add_idx_stmt;
DEALLOCATE PREPARE add_idx_stmt;
