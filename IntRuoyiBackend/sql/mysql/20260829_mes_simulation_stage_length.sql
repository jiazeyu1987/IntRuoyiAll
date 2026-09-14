-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260825_mes_stage1_simulation_metadata; type=schema; riskLevel=low
-- Stage4 independent input mode stores STAGE4_INDEPENDENT_BATCH_EXECUTION in simulation_stage.
-- The Stage1 metadata migration created these columns as varchar(32), which is too short for explicit mode names.

DROP PROCEDURE IF EXISTS ensure_mes_simulation_stage_length;
DELIMITER $$
CREATE PROCEDURE ensure_mes_simulation_stage_length(
    IN target_table VARCHAR(128)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = target_table
          AND COLUMN_NAME = 'simulation_stage'
          AND CHARACTER_MAXIMUM_LENGTH < 64
    ) THEN
        SET @mes_simulation_stage_length_sql = CONCAT(
            'ALTER TABLE `', target_table, '` MODIFY COLUMN `simulation_stage` varchar(64) DEFAULT NULL COMMENT ''模拟段标识'''
        );
        PREPARE mes_simulation_stage_length_stmt FROM @mes_simulation_stage_length_sql;
        EXECUTE mes_simulation_stage_length_stmt;
        DEALLOCATE PREPARE mes_simulation_stage_length_stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_active_order');
CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_active_order_process_snapshot');
CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_active_order_pick_list_binding');
CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_active_order_pick_list_binding_item');
CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_event');
CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_pqc_record');
CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_quantity_fragment');
CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_submission_review');
CALL ensure_mes_simulation_stage_length('mes_pro_process_pool_report_allocation');
CALL ensure_mes_simulation_stage_length('mes_pqc_inspection_task');
CALL ensure_mes_simulation_stage_length('mes_pqc_inspection_piece_detail');
CALL ensure_mes_simulation_stage_length('mes_pqc_process_inspection_aggregate_detail');

DROP PROCEDURE IF EXISTS ensure_mes_simulation_stage_length;
