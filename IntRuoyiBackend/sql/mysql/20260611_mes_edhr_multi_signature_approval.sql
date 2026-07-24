-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_edhr_multi_signature_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_multi_signature_column(
  IN target_table varchar(64),
  IN target_column varchar(64),
  IN ddl_sql text
)
BEGIN
  DECLARE missing_table_message varchar(255);

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
  ) THEN
    SET missing_table_message = CONCAT(target_table, ' is missing; cannot apply eDHR multi signature approval migration');
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = missing_table_message;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
      AND COLUMN_NAME = target_column
  ) THEN
    SET @ddl = ddl_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_edhr_work_task',
  'signature_cell_key',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `signature_cell_key` varchar(128) NOT NULL DEFAULT '''' COMMENT ''审核签字格稳定键'' AFTER `source_user_id`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_edhr_work_task',
  'signature_row_index',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `signature_row_index` int DEFAULT NULL COMMENT ''审核签字格行号'' AFTER `signature_cell_key`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_edhr_work_task',
  'signature_column_index',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `signature_column_index` int DEFAULT NULL COMMENT ''审核签字格列号'' AFTER `signature_row_index`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_edhr_work_task',
  'review_source_type',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `review_source_type` varchar(16) DEFAULT NULL COMMENT ''审核来源类型：POST/ROLE'' AFTER `signature_column_index`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_edhr_work_task',
  'review_source_id',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `review_source_id` bigint DEFAULT NULL COMMENT ''审核来源ID'' AFTER `review_source_type`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_edhr_work_task',
  'review_source_name',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `review_source_name` varchar(128) DEFAULT NULL COMMENT ''审核来源名称快照'' AFTER `review_source_id`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_edhr_work_task',
  'bpm_task_id',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `bpm_task_id` varchar(64) DEFAULT NULL COMMENT ''BPM审核任务ID'' AFTER `review_source_name`'
);

CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_batch_record_execution_signature',
  'signature_cell_key',
  'ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `signature_cell_key` varchar(128) DEFAULT NULL COMMENT ''签字格稳定键'' AFTER `bpm_task_name`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_batch_record_execution_signature',
  'signature_row_index',
  'ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `signature_row_index` int DEFAULT NULL COMMENT ''签字格行号'' AFTER `signature_cell_key`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_batch_record_execution_signature',
  'signature_column_index',
  'ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `signature_column_index` int DEFAULT NULL COMMENT ''签字格列号'' AFTER `signature_row_index`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_batch_record_execution_signature',
  'review_source_type',
  'ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `review_source_type` varchar(16) DEFAULT NULL COMMENT ''审核来源类型：POST/ROLE'' AFTER `signature_column_index`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_batch_record_execution_signature',
  'review_source_id',
  'ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `review_source_id` bigint DEFAULT NULL COMMENT ''审核来源ID'' AFTER `review_source_type`'
);
CALL ensure_mes_edhr_multi_signature_column(
  'mes_pro_batch_record_execution_signature',
  'review_source_name',
  'ALTER TABLE `mes_pro_batch_record_execution_signature` ADD COLUMN `review_source_name` varchar(128) DEFAULT NULL COMMENT ''审核来源名称快照'' AFTER `review_source_id`'
);

DROP PROCEDURE IF EXISTS ensure_mes_edhr_multi_signature_index;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_multi_signature_index(
  IN target_table varchar(64),
  IN target_index varchar(64),
  IN ddl_sql text
)
BEGIN
  DECLARE missing_table_message varchar(255);

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
  ) THEN
    SET missing_table_message = CONCAT(target_table, ' is missing; cannot apply eDHR multi signature approval index migration');
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = missing_table_message;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
      AND INDEX_NAME = target_index
  ) THEN
    SET @ddl = ddl_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS drop_mes_edhr_multi_signature_index;
DELIMITER $$
CREATE PROCEDURE drop_mes_edhr_multi_signature_index(
  IN target_table varchar(64),
  IN target_index varchar(64),
  IN ddl_sql text
)
BEGIN
  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = target_table
      AND INDEX_NAME = target_index
  ) THEN
    SET @ddl = ddl_sql;
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL drop_mes_edhr_multi_signature_index(
  'mes_pro_edhr_work_task',
  'uk_mes_pro_edhr_work_task_active',
  'ALTER TABLE `mes_pro_edhr_work_task` DROP INDEX `uk_mes_pro_edhr_work_task_active`'
);

CALL ensure_mes_edhr_multi_signature_index(
  'mes_pro_edhr_work_task',
  'uk_mes_pro_edhr_work_task_active_cell',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD UNIQUE KEY `uk_mes_pro_edhr_work_task_active_cell` (`tenant_id`, `batch_task_id`, `task_type`, `status`, `signature_cell_key`, `deleted`)'
);
CALL ensure_mes_edhr_multi_signature_index(
  'mes_pro_edhr_work_task',
  'idx_mes_pro_edhr_work_task_signature_cell',
  'CREATE INDEX `idx_mes_pro_edhr_work_task_signature_cell` ON `mes_pro_edhr_work_task` (`tenant_id`, `execution_id`, `signature_cell_key`)'
);
CALL ensure_mes_edhr_multi_signature_index(
  'mes_pro_edhr_work_task',
  'idx_mes_pro_edhr_work_task_bpm_task',
  'CREATE INDEX `idx_mes_pro_edhr_work_task_bpm_task` ON `mes_pro_edhr_work_task` (`tenant_id`, `bpm_task_id`)'
);

DROP PROCEDURE IF EXISTS drop_mes_edhr_multi_signature_index;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_multi_signature_index;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_multi_signature_column;
