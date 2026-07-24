-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260629_dcc_controlled_file_recognition_record; type=schema; riskLevel=medium
-- Add task worker snapshot and batch task link for DCC controlled-file recognition ledger exports.

DELIMITER $$

DROP PROCEDURE IF EXISTS ensure_dcc_batch_recognition_worker_ledger_column$$
CREATE PROCEDURE ensure_dcc_batch_recognition_worker_ledger_column(
  IN p_table_name VARCHAR(128),
  IN p_column_name VARCHAR(128),
  IN p_column_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @dcc_worker_ledger_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition
    );
    PREPARE dcc_worker_ledger_stmt FROM @dcc_worker_ledger_sql;
    EXECUTE dcc_worker_ledger_stmt;
    DEALLOCATE PREPARE dcc_worker_ledger_stmt;
  END IF;
END$$

DROP PROCEDURE IF EXISTS ensure_dcc_batch_recognition_worker_ledger_index$$
CREATE PROCEDURE ensure_dcc_batch_recognition_worker_ledger_index(
  IN p_table_name VARCHAR(128),
  IN p_index_name VARCHAR(128),
  IN p_index_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @dcc_worker_ledger_sql = p_index_definition;
    PREPARE dcc_worker_ledger_stmt FROM @dcc_worker_ledger_sql;
    EXECUTE dcc_worker_ledger_stmt;
    DEALLOCATE PREPARE dcc_worker_ledger_stmt;
  END IF;
END$$

DELIMITER ;

CALL ensure_dcc_batch_recognition_worker_ledger_column(
  'dcc_controlled_file_batch_recognition_task',
  'worker_count',
  'int NOT NULL DEFAULT 1 COMMENT ''Codex worker count snapshot'''
);

CALL ensure_dcc_batch_recognition_worker_ledger_column(
  'dcc_controlled_file_recognition_record',
  'batch_task_id',
  'bigint DEFAULT NULL COMMENT ''Batch recognition task ID'''
);

CALL ensure_dcc_batch_recognition_worker_ledger_index(
  'dcc_controlled_file_recognition_record',
  'idx_dcc_file_recognition_record_batch',
  'CREATE INDEX `idx_dcc_file_recognition_record_batch` ON `dcc_controlled_file_recognition_record` (`tenant_id`, `batch_task_id`)'
);

DROP PROCEDURE IF EXISTS ensure_dcc_batch_recognition_worker_ledger_index;
DROP PROCEDURE IF EXISTS ensure_dcc_batch_recognition_worker_ledger_column;
