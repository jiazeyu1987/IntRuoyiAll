-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260701_dcc_batch_recognition_worker_ledger_export; type=schema; riskLevel=medium
-- Add DCC recognition file type levels for rule-based project recognition.

DELIMITER $$

DROP PROCEDURE IF EXISTS ensure_dcc_recognition_file_type_column$$
CREATE PROCEDURE ensure_dcc_recognition_file_type_column(
  IN p_table_name VARCHAR(128),
  IN p_column_name VARCHAR(128),
  IN p_column_statement TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @dcc_recognition_file_type_sql = p_column_statement;
    PREPARE dcc_recognition_file_type_stmt FROM @dcc_recognition_file_type_sql;
    EXECUTE dcc_recognition_file_type_stmt;
    DEALLOCATE PREPARE dcc_recognition_file_type_stmt;
  END IF;
END$$

DROP PROCEDURE IF EXISTS ensure_dcc_recognition_file_type_index$$
CREATE PROCEDURE ensure_dcc_recognition_file_type_index(
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
    SET @dcc_recognition_file_type_sql = p_index_definition;
    PREPARE dcc_recognition_file_type_stmt FROM @dcc_recognition_file_type_sql;
    EXECUTE dcc_recognition_file_type_stmt;
    DEALLOCATE PREPARE dcc_recognition_file_type_stmt;
  END IF;
END$$

DELIMITER ;

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file',
  'file_type_level1',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level1` varchar(64) DEFAULT NULL COMMENT ''File type level 1: QMS or technical document'' AFTER `project_code_recognized_time`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file',
  'file_type_level2',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level2` varchar(128) DEFAULT NULL COMMENT ''File type level 2: DCC category for technical documents'' AFTER `file_type_level1`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file',
  'file_type_level3',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level3` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 3'' AFTER `file_type_level2`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file',
  'file_type_level4',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level4` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 4'' AFTER `file_type_level3`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file',
  'file_type_level5',
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `file_type_level5` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 5'' AFTER `file_type_level4`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level1',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level1` varchar(64) DEFAULT NULL COMMENT ''File type level 1: QMS or technical document'' AFTER `failure_message`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level2',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level2` varchar(128) DEFAULT NULL COMMENT ''File type level 2: DCC category for technical documents'' AFTER `file_type_level1`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level3',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level3` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 3'' AFTER `file_type_level2`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level4',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level4` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 4'' AFTER `file_type_level3`'
);

CALL ensure_dcc_recognition_file_type_column(
  'dcc_controlled_file_recognition_record',
  'file_type_level5',
  'ALTER TABLE `dcc_controlled_file_recognition_record` ADD COLUMN `file_type_level5` varchar(128) DEFAULT NULL COMMENT ''Reserved file type level 5'' AFTER `file_type_level4`'
);

CALL ensure_dcc_recognition_file_type_index(
  'dcc_controlled_file',
  'idx_dcc_controlled_file_type_level',
  'CREATE INDEX `idx_dcc_controlled_file_type_level` ON `dcc_controlled_file` (`tenant_id`, `file_type_level1`, `file_type_level2`)'
);

DROP PROCEDURE IF EXISTS ensure_dcc_recognition_file_type_index;
DROP PROCEDURE IF EXISTS ensure_dcc_recognition_file_type_column;
