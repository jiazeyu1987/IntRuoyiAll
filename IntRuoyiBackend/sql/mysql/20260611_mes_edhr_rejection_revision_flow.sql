-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
DROP PROCEDURE IF EXISTS ensure_mes_edhr_rejection_revision_column;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_rejection_revision_column(
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
    SET missing_table_message = CONCAT(target_table, ' is missing; cannot apply eDHR rejection revision migration');
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

DROP PROCEDURE IF EXISTS ensure_mes_edhr_rejection_revision_preconditions;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_rejection_revision_preconditions()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_batch_record_execution'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_batch_record_execution is missing; cannot apply eDHR rejection revision migration';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mes_pro_edhr_work_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'mes_pro_edhr_work_task is missing; cannot apply eDHR rejection revision migration';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_edhr_rejection_revision_preconditions();

CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_batch_record_execution',
  'revision_root_execution_id',
  'ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `revision_root_execution_id` bigint DEFAULT NULL COMMENT ''修订根执行记录ID'' AFTER `active_context_key`'
);
CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_batch_record_execution',
  'revision_no',
  'ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `revision_no` int NOT NULL DEFAULT 1 COMMENT ''修订轮次'' AFTER `revision_root_execution_id`'
);
CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_batch_record_execution',
  'source_rejected_execution_id',
  'ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `source_rejected_execution_id` bigint DEFAULT NULL COMMENT ''来源驳回执行记录ID'' AFTER `revision_no`'
);
CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_batch_record_execution',
  'superseded_by_execution_id',
  'ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `superseded_by_execution_id` bigint DEFAULT NULL COMMENT ''替代本版本的执行记录ID'' AFTER `source_rejected_execution_id`'
);
CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_batch_record_execution',
  'revision_reason',
  'ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `revision_reason` varchar(500) DEFAULT NULL COMMENT ''修订原因'' AFTER `superseded_by_execution_id`'
);
CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_batch_record_execution',
  'revision_parent_hash',
  'ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `revision_parent_hash` char(64) DEFAULT NULL COMMENT ''父版本证据哈希'' AFTER `revision_reason`'
);
CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_batch_record_execution',
  'active_revision_flag',
  -- Applied column definition: `active_revision_flag` bit(1) NOT NULL DEFAULT b'1'
  'ALTER TABLE `mes_pro_batch_record_execution` ADD COLUMN `active_revision_flag` bit(1) NOT NULL DEFAULT b''1'' COMMENT ''是否当前活动修订版本'' AFTER `revision_parent_hash`'
);
CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_edhr_work_task',
  'source_execution_id',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `source_execution_id` bigint DEFAULT NULL COMMENT ''来源执行记录ID'' AFTER `execution_id`'
);
CALL ensure_mes_edhr_rejection_revision_column(
  'mes_pro_edhr_work_task',
  'reason',
  'ALTER TABLE `mes_pro_edhr_work_task` ADD COLUMN `reason` varchar(500) DEFAULT NULL COMMENT ''任务原因'' AFTER `action_url`'
);

DROP PROCEDURE IF EXISTS ensure_mes_edhr_rejection_revision_index;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_rejection_revision_index(
  IN target_table varchar(64),
  IN target_index varchar(64),
  IN ddl_sql text
)
BEGIN
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

CALL ensure_mes_edhr_rejection_revision_index(
  'mes_pro_batch_record_execution',
  'idx_mes_pro_bre_revision_root',
  'CREATE INDEX `idx_mes_pro_bre_revision_root` ON `mes_pro_batch_record_execution` (`tenant_id`, `revision_root_execution_id`, `revision_no`)'
);
CALL ensure_mes_edhr_rejection_revision_index(
  'mes_pro_batch_record_execution',
  'idx_mes_pro_bre_source_rejected',
  'CREATE INDEX `idx_mes_pro_bre_source_rejected` ON `mes_pro_batch_record_execution` (`tenant_id`, `source_rejected_execution_id`)'
);
CALL ensure_mes_edhr_rejection_revision_index(
  'mes_pro_batch_record_execution',
  'idx_mes_pro_bre_superseded',
  'CREATE INDEX `idx_mes_pro_bre_superseded` ON `mes_pro_batch_record_execution` (`tenant_id`, `superseded_by_execution_id`)'
);
CALL ensure_mes_edhr_rejection_revision_index(
  'mes_pro_edhr_work_task',
  'idx_mes_pro_edhr_work_task_source_execution',
  'CREATE INDEX `idx_mes_pro_edhr_work_task_source_execution` ON `mes_pro_edhr_work_task` (`tenant_id`, `source_execution_id`)'
);

UPDATE `mes_pro_batch_record_execution`
SET `revision_root_execution_id` = `id`
WHERE `revision_root_execution_id` IS NULL
  AND `deleted` = b'0';

DROP PROCEDURE IF EXISTS ensure_mes_edhr_rejection_revision_index;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_rejection_revision_column;
DROP PROCEDURE IF EXISTS ensure_mes_edhr_rejection_revision_preconditions;
