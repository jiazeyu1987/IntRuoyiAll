-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=low
-- 生产工单补齐 ERP 截图字段；仅新增 ERP 快照字段，不删除本地扩展字段。
-- MySQL 8.0 compatibility: use information_schema guards instead of ADD COLUMN IF NOT EXISTS.

SET @mes_pro_work_order_erp_snapshot_workshop_name_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'workshop_name'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `workshop_name` varchar(128) DEFAULT NULL COMMENT ''ERP生产车间'' AFTER `batch_code`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_workshop_name_stmt FROM @mes_pro_work_order_erp_snapshot_workshop_name_sql;
EXECUTE mes_pro_work_order_erp_snapshot_workshop_name_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_workshop_name_stmt;

SET @mes_pro_work_order_erp_snapshot_bom_version_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'bom_version'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `bom_version` varchar(128) DEFAULT NULL COMMENT ''ERP BOM版本'' AFTER `workshop_name`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_bom_version_stmt FROM @mes_pro_work_order_erp_snapshot_bom_version_sql;
EXECUTE mes_pro_work_order_erp_snapshot_bom_version_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_bom_version_stmt;

SET @mes_pro_work_order_erp_snapshot_pick_mode_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'pick_mode'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `pick_mode` varchar(64) DEFAULT NULL COMMENT ''ERP冲领料'' AFTER `bom_version`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_pick_mode_stmt FROM @mes_pro_work_order_erp_snapshot_pick_mode_sql;
EXECUTE mes_pro_work_order_erp_snapshot_pick_mode_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_pick_mode_stmt;

SET @mes_pro_work_order_erp_snapshot_auxiliary_code_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'auxiliary_code'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `auxiliary_code` varchar(128) DEFAULT NULL COMMENT ''ERP备注1助记码'' AFTER `pick_mode`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_auxiliary_code_stmt FROM @mes_pro_work_order_erp_snapshot_auxiliary_code_sql;
EXECUTE mes_pro_work_order_erp_snapshot_auxiliary_code_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_auxiliary_code_stmt;

SET @mes_pro_work_order_erp_snapshot_business_status_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'business_status'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `business_status` varchar(64) DEFAULT NULL COMMENT ''ERP业务状态'' AFTER `auxiliary_code`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_business_status_stmt FROM @mes_pro_work_order_erp_snapshot_business_status_sql;
EXECUTE mes_pro_work_order_erp_snapshot_business_status_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_business_status_stmt;

SET @mes_pro_work_order_erp_snapshot_drawing_number_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'drawing_number'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `drawing_number` varchar(128) DEFAULT NULL COMMENT ''ERP图号'' AFTER `business_status`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_drawing_number_stmt FROM @mes_pro_work_order_erp_snapshot_drawing_number_sql;
EXECUTE mes_pro_work_order_erp_snapshot_drawing_number_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_drawing_number_stmt;

SET @mes_pro_work_order_erp_snapshot_schedule_status_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'schedule_status'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `schedule_status` varchar(64) DEFAULT NULL COMMENT ''ERP排产状态'' AFTER `drawing_number`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_schedule_status_stmt FROM @mes_pro_work_order_erp_snapshot_schedule_status_sql;
EXECUTE mes_pro_work_order_erp_snapshot_schedule_status_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_schedule_status_stmt;

SET @mes_pro_work_order_erp_snapshot_planned_start_time_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'planned_start_time'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `planned_start_time` datetime DEFAULT NULL COMMENT ''ERP计划开工时间'' AFTER `schedule_status`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_planned_start_time_stmt FROM @mes_pro_work_order_erp_snapshot_planned_start_time_sql;
EXECUTE mes_pro_work_order_erp_snapshot_planned_start_time_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_planned_start_time_stmt;

SET @mes_pro_work_order_erp_snapshot_planned_end_time_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'mes_pro_work_order'
              AND COLUMN_NAME = 'planned_end_time'
        ),
        'SELECT 1',
        'ALTER TABLE `mes_pro_work_order` ADD COLUMN `planned_end_time` datetime DEFAULT NULL COMMENT ''ERP计划完工时间'' AFTER `planned_start_time`'
    )
);
PREPARE mes_pro_work_order_erp_snapshot_planned_end_time_stmt FROM @mes_pro_work_order_erp_snapshot_planned_end_time_sql;
EXECUTE mes_pro_work_order_erp_snapshot_planned_end_time_stmt;
DEALLOCATE PREPARE mes_pro_work_order_erp_snapshot_planned_end_time_stmt;
