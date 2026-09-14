-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260822_mes_active_order_pick_list_binding,20260802_mes_pqc_inspection_task; type=schema; riskLevel=medium
-- Stage1 simulation ownership must be queryable on every persisted production/PQC fact.
-- MySQL 8.0.39 does not support the ADD COLUMN IF NOT EXISTS syntax used by some older scripts in this repo,
-- so this migration uses information_schema guards instead.

DROP PROCEDURE IF EXISTS ensure_mes_stage1_simulation_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_stage1_simulation_column(
    IN target_table VARCHAR(128),
    IN target_column VARCHAR(128),
    IN ddl_statement TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = target_table
          AND COLUMN_NAME = target_column
    ) THEN
        SET @stage1_simulation_column_sql = ddl_statement;
        PREPARE stage1_simulation_column_stmt FROM @stage1_simulation_column_sql;
        EXECUTE stage1_simulation_column_stmt;
        DEALLOCATE PREPARE stage1_simulation_column_stmt;
    END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS ensure_mes_stage1_simulation_index;
DELIMITER $$
CREATE PROCEDURE ensure_mes_stage1_simulation_index(
    IN target_table VARCHAR(128),
    IN target_index VARCHAR(128),
    IN ddl_statement TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = target_table
          AND INDEX_NAME = target_index
    ) THEN
        SET @stage1_simulation_index_sql = ddl_statement;
        PREPARE stage1_simulation_index_stmt FROM @stage1_simulation_index_sql;
        EXECUTE stage1_simulation_index_stmt;
        DEALLOCATE PREPARE stage1_simulation_index_stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_active_order` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `released_at`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_active_order` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_active_order` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');
CALL ensure_mes_stage1_simulation_index('mes_pro_process_pool_active_order', 'idx_mes_pp_active_order_simulation',
    'ALTER TABLE `mes_pro_process_pool_active_order` ADD KEY `idx_mes_pp_active_order_simulation` (`tenant_id`, `simulation_stage`, `simulation_run_id`)');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_process_snapshot', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_active_order_process_snapshot` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `parameter_snapshot_state`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_process_snapshot', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_active_order_process_snapshot` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_process_snapshot', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_active_order_process_snapshot` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_pick_list_binding', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_active_order_pick_list_binding` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `binding_version`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_pick_list_binding', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_active_order_pick_list_binding` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_pick_list_binding', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_active_order_pick_list_binding` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_pick_list_binding_item', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_active_order_pick_list_binding_item` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `item_snapshot_hash`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_pick_list_binding_item', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_active_order_pick_list_binding_item` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_pick_list_binding_item', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_active_order_pick_list_binding_item` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_event', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_event` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `signature_snapshot`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_event', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_event` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_event', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_event` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');
CALL ensure_mes_stage1_simulation_index('mes_pro_process_pool_event', 'idx_mes_pp_event_simulation',
    'ALTER TABLE `mes_pro_process_pool_event` ADD KEY `idx_mes_pp_event_simulation` (`tenant_id`, `simulation_stage`, `simulation_run_id`)');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_pqc_record', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_pqc_record` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `raw_payload`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_pqc_record', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_pqc_record` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_pqc_record', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_pqc_record` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_quantity_fragment', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_quantity_fragment` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `raw_payload`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_quantity_fragment', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_quantity_fragment` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_quantity_fragment', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_quantity_fragment` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_submission_review', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_submission_review` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `review_signature_snapshot_json`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_submission_review', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_submission_review` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_submission_review', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_submission_review` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_report_allocation', 'simulated',
    'ALTER TABLE `mes_pro_process_pool_report_allocation` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `remark`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_report_allocation', 'simulation_stage',
    'ALTER TABLE `mes_pro_process_pool_report_allocation` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_report_allocation', 'simulation_run_id',
    'ALTER TABLE `mes_pro_process_pool_report_allocation` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pqc_inspection_task', 'simulated',
    'ALTER TABLE `mes_pqc_inspection_task` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `submitted_content_hash`');
CALL ensure_mes_stage1_simulation_column('mes_pqc_inspection_task', 'simulation_stage',
    'ALTER TABLE `mes_pqc_inspection_task` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pqc_inspection_task', 'simulation_run_id',
    'ALTER TABLE `mes_pqc_inspection_task` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');
CALL ensure_mes_stage1_simulation_index('mes_pqc_inspection_task', 'idx_mes_pqc_task_simulation',
    'ALTER TABLE `mes_pqc_inspection_task` ADD KEY `idx_mes_pqc_task_simulation` (`tenant_id`, `simulation_stage`, `simulation_run_id`)');

CALL ensure_mes_stage1_simulation_column('mes_pqc_inspection_piece_detail', 'simulated',
    'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `judgement`');
CALL ensure_mes_stage1_simulation_column('mes_pqc_inspection_piece_detail', 'simulation_stage',
    'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pqc_inspection_piece_detail', 'simulation_run_id',
    'ALTER TABLE `mes_pqc_inspection_piece_detail` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pqc_process_inspection_aggregate_detail', 'simulated',
    'ALTER TABLE `mes_pqc_process_inspection_aggregate_detail` ADD COLUMN `simulated` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否Stage模拟数据'' AFTER `judgement`');
CALL ensure_mes_stage1_simulation_column('mes_pqc_process_inspection_aggregate_detail', 'simulation_stage',
    'ALTER TABLE `mes_pqc_process_inspection_aggregate_detail` ADD COLUMN `simulation_stage` varchar(32) DEFAULT NULL COMMENT ''模拟段标识'' AFTER `simulated`');
CALL ensure_mes_stage1_simulation_column('mes_pqc_process_inspection_aggregate_detail', 'simulation_run_id',
    'ALTER TABLE `mes_pqc_process_inspection_aggregate_detail` ADD COLUMN `simulation_run_id` varchar(128) DEFAULT NULL COMMENT ''模拟运行号'' AFTER `simulation_stage`');

CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_completion_receipt', 'completion_status',
    'ALTER TABLE `mes_pro_process_pool_active_order_completion_receipt` ADD COLUMN `completion_status` varchar(32) DEFAULT NULL COMMENT ''完成状态'' AFTER `receipt_status`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_completion_receipt', 'batch_record_id',
    'ALTER TABLE `mes_pro_process_pool_active_order_completion_receipt` ADD COLUMN `batch_record_id` bigint DEFAULT NULL COMMENT ''批记录编号'' AFTER `process_inspection_status`');
CALL ensure_mes_stage1_simulation_column('mes_pro_process_pool_active_order_completion_receipt', 'process_inspection_id',
    'ALTER TABLE `mes_pro_process_pool_active_order_completion_receipt` ADD COLUMN `process_inspection_id` bigint DEFAULT NULL COMMENT ''过程检验编号'' AFTER `batch_record_id`');

DROP PROCEDURE IF EXISTS ensure_mes_stage1_simulation_index;
DROP PROCEDURE IF EXISTS ensure_mes_stage1_simulation_column;
