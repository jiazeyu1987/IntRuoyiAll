-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_edhr_multi_signature_approval.sql; type=schema; riskLevel=low
-- Stage1 模拟签名来源 MES_ACTIVE_ORDER_SIMULATION 长度超过原 varchar(16)，扩展审核来源类型字段长度。

SET @mes_edhr_work_task_exists = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_work_task'
);
SET @mes_edhr_work_task_check_sql = IF(
    @mes_edhr_work_task_exists = 1,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''missing required table mes_pro_edhr_work_task'''
);
PREPARE mes_edhr_work_task_check_stmt FROM @mes_edhr_work_task_check_sql;
EXECUTE mes_edhr_work_task_check_stmt;
DEALLOCATE PREPARE mes_edhr_work_task_check_stmt;

SET @mes_execution_signature_exists = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution_signature'
);
SET @mes_execution_signature_check_sql = IF(
    @mes_execution_signature_exists = 1,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''missing required table mes_pro_batch_record_execution_signature'''
);
PREPARE mes_execution_signature_check_stmt FROM @mes_execution_signature_check_sql;
EXECUTE mes_execution_signature_check_stmt;
DEALLOCATE PREPARE mes_execution_signature_check_stmt;

SET @mes_edhr_work_task_review_source_type_needs_modify = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_work_task'
      AND COLUMN_NAME = 'review_source_type'
      AND CHARACTER_MAXIMUM_LENGTH < 64
);
SET @mes_edhr_work_task_review_source_type_sql = IF(
    @mes_edhr_work_task_review_source_type_needs_modify = 1,
    'ALTER TABLE `mes_pro_edhr_work_task` MODIFY COLUMN `review_source_type` varchar(64) DEFAULT NULL COMMENT ''审核来源类型：POST/ROLE/MES_ACTIVE_ORDER_SIMULATION''',
    'SELECT 1'
);
PREPARE mes_edhr_work_task_review_source_type_stmt FROM @mes_edhr_work_task_review_source_type_sql;
EXECUTE mes_edhr_work_task_review_source_type_stmt;
DEALLOCATE PREPARE mes_edhr_work_task_review_source_type_stmt;

SET @mes_execution_signature_review_source_type_needs_modify = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution_signature'
      AND COLUMN_NAME = 'review_source_type'
      AND CHARACTER_MAXIMUM_LENGTH < 64
);
SET @mes_execution_signature_review_source_type_sql = IF(
    @mes_execution_signature_review_source_type_needs_modify = 1,
    'ALTER TABLE `mes_pro_batch_record_execution_signature` MODIFY COLUMN `review_source_type` varchar(64) DEFAULT NULL COMMENT ''审核来源类型：POST/ROLE/MES_ACTIVE_ORDER_SIMULATION''',
    'SELECT 1'
);
PREPARE mes_execution_signature_review_source_type_stmt FROM @mes_execution_signature_review_source_type_sql;
EXECUTE mes_execution_signature_review_source_type_stmt;
DEALLOCATE PREPARE mes_execution_signature_review_source_type_stmt;
