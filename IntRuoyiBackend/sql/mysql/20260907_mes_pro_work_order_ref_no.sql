-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=low
-- 生产工单增加金蝶物料基础资料 REF.NO. 快照字段。

SET @mes_pro_work_order_table_exists = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_work_order'
);
SET @mes_pro_work_order_table_check_sql = IF(
    @mes_pro_work_order_table_exists = 1,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''missing required table mes_pro_work_order'''
);
PREPARE mes_pro_work_order_table_check_stmt FROM @mes_pro_work_order_table_check_sql;
EXECUTE mes_pro_work_order_table_check_stmt;
DEALLOCATE PREPARE mes_pro_work_order_table_check_stmt;

SET @mes_pro_work_order_ref_no_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_work_order'
      AND COLUMN_NAME = 'ref_no'
);
SET @mes_pro_work_order_ref_no_sql = IF(
    @mes_pro_work_order_ref_no_exists = 0,
    'ALTER TABLE `mes_pro_work_order` ADD COLUMN `ref_no` varchar(128) DEFAULT NULL COMMENT ''ERP REF.NO.'' AFTER `drawing_number`',
    'SELECT 1'
);
PREPARE mes_pro_work_order_ref_no_stmt FROM @mes_pro_work_order_ref_no_sql;
EXECUTE mes_pro_work_order_ref_no_stmt;
DEALLOCATE PREPARE mes_pro_work_order_ref_no_stmt;
