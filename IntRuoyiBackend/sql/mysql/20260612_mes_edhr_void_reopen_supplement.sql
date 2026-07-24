-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR 作废/重开/补录机制数据结构
-- Fail fast: 迁移前必须存在当前执行表与归档表，避免在错误库或旧 schema 上静默降级。

DROP PROCEDURE IF EXISTS ensure_mes_edhr_vrs_table;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_vRS_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_vrs_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_vrs_index;

DELIMITER $$

CREATE PROCEDURE ensure_mes_edhr_vrs_table(IN p_table_name VARCHAR(128))
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing required eDHR table for void/reopen/supplement migration';
    END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_vrs_column(
    IN p_table_name VARCHAR(128),
    IN p_column_name VARCHAR(128),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
           AND COLUMN_NAME = p_column_name
    ) THEN
        SET @edhr_vrs_sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @edhr_vrs_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE ensure_mes_edhr_vrs_index(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
           AND INDEX_NAME = p_index_name
    ) THEN
        SET @edhr_vrs_sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_index_definition);
        PREPARE stmt FROM @edhr_vrs_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL ensure_mes_edhr_vrs_table('mes_pro_batch_record_execution');
CALL ensure_mes_edhr_vrs_table('mes_pro_batch_record_execution_archive');
CALL ensure_mes_edhr_vrs_table('mes_pro_edhr_batch_execution_archive');

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_record_change_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `change_code` varchar(64) NOT NULL COMMENT '变更编号',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型：VOID/REOPEN/SUPPLEMENT',
  `target_scope` varchar(32) NOT NULL COMMENT '对象范围：EXECUTION/BATCH/ARCHIVE',
  `batch_execution_id` bigint DEFAULT NULL COMMENT '批次执行ID',
  `execution_id` bigint DEFAULT NULL COMMENT '执行记录ID',
  `source_execution_id` bigint DEFAULT NULL COMMENT '来源执行记录ID',
  `new_execution_id` bigint DEFAULT NULL COMMENT '新执行记录ID',
  `source_archive_id` bigint DEFAULT NULL COMMENT '来源归档ID',
  `new_archive_id` bigint DEFAULT NULL COMMENT '新归档ID',
  `change_status` varchar(32) NOT NULL COMMENT '变更状态：DRAFT/SUBMITTED/APPROVED/REJECTED/EFFECTIVE',
  `reason_category` varchar(64) NOT NULL COMMENT '原因分类',
  `reason_text` varchar(500) NOT NULL COMMENT '原因说明',
  `requested_by` bigint DEFAULT NULL COMMENT '申请人',
  `requested_at` datetime DEFAULT NULL COMMENT '申请时间',
  `request_signature_id` bigint DEFAULT NULL COMMENT '申请签名ID',
  `approved_by` bigint DEFAULT NULL COMMENT '批准人',
  `approved_at` datetime DEFAULT NULL COMMENT '批准时间',
  `approval_signature_id` bigint DEFAULT NULL COMMENT '批准签名ID',
  `effective_at` datetime DEFAULT NULL COMMENT '生效时间',
  `previous_status` varchar(32) DEFAULT NULL COMMENT '原状态',
  `new_status` varchar(32) DEFAULT NULL COMMENT '新状态',
  `previous_head_hash` char(64) DEFAULT NULL COMMENT '原审计链头Hash',
  `new_head_hash` char(64) DEFAULT NULL COMMENT '新审计链头Hash',
  `previous_archive_hash` char(64) DEFAULT NULL COMMENT '原归档Hash',
  `new_archive_hash` char(64) DEFAULT NULL COMMENT '新归档Hash',
  `bpm_process_instance_id` varchar(64) DEFAULT NULL COMMENT '审批流程实例ID',
  `bpm_task_id` varchar(64) DEFAULT NULL COMMENT '审批任务ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edhr_change_code` (`tenant_id`, `change_code`, `deleted`),
  KEY `idx_edhr_change_execution` (`tenant_id`, `execution_id`, `change_type`, `change_status`),
  KEY `idx_edhr_change_batch` (`tenant_id`, `batch_execution_id`, `change_type`, `change_status`),
  KEY `idx_edhr_change_source_execution` (`tenant_id`, `source_execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR记录作废重开补录变更事件';

CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution', 'voided_by_change_event_id', 'bigint DEFAULT NULL COMMENT ''作废变更事件ID''');
CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution', 'reopened_by_change_event_id', 'bigint DEFAULT NULL COMMENT ''重开变更事件ID''');
CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution', 'supplement_source_execution_id', 'bigint DEFAULT NULL COMMENT ''补录来源执行ID''');
CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution', 'supplement_reason', 'varchar(1000) DEFAULT NULL COMMENT ''补录原因''');
CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution', 'supplement_flag', 'bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否补录记录''');
CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution', 'effective_replaced_by_execution_id', 'bigint DEFAULT NULL COMMENT ''有效记录替代执行ID''');

CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution_archive', 'superseded_by_archive_id', 'bigint DEFAULT NULL COMMENT ''被替代归档ID''');
CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution_archive', 'invalidated_by_change_event_id', 'bigint DEFAULT NULL COMMENT ''失效变更事件ID''');
CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution_archive', 'archive_valid_flag', 'bit(1) NOT NULL DEFAULT b''1'' COMMENT ''归档是否有效''');
CALL ensure_mes_edhr_vrs_column('mes_pro_batch_record_execution_archive', 'archive_valid_status', 'varchar(32) NOT NULL DEFAULT ''VALID'' COMMENT ''归档有效状态''');

CALL ensure_mes_edhr_vrs_column('mes_pro_edhr_batch_execution_archive', 'superseded_by_archive_id', 'bigint DEFAULT NULL COMMENT ''被替代归档ID''');
CALL ensure_mes_edhr_vrs_column('mes_pro_edhr_batch_execution_archive', 'invalidated_by_change_event_id', 'bigint DEFAULT NULL COMMENT ''失效变更事件ID''');
CALL ensure_mes_edhr_vrs_column('mes_pro_edhr_batch_execution_archive', 'archive_valid_flag', 'bit(1) NOT NULL DEFAULT b''1'' COMMENT ''归档是否有效''');
CALL ensure_mes_edhr_vrs_column('mes_pro_edhr_batch_execution_archive', 'archive_valid_status', 'varchar(32) NOT NULL DEFAULT ''VALID'' COMMENT ''归档有效状态''');

CALL ensure_mes_edhr_vrs_index('mes_pro_batch_record_execution', 'idx_mes_pro_bre_voided_change', 'KEY `idx_mes_pro_bre_voided_change` (`voided_by_change_event_id`)');
CALL ensure_mes_edhr_vrs_index('mes_pro_batch_record_execution', 'idx_mes_pro_bre_supplement_source', 'KEY `idx_mes_pro_bre_supplement_source` (`supplement_source_execution_id`)');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_vrs_table;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_vrs_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_vrs_index;
