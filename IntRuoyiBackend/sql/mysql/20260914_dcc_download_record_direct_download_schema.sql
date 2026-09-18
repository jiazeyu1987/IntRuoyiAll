-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260528_dcc_controlled_file_protection; type=schema; riskLevel=medium
-- Remove DCC encrypted-download record columns after direct controlled download became the only supported path.

DELIMITER //

DROP PROCEDURE IF EXISTS ensure_dcc_direct_download_table//
CREATE PROCEDURE ensure_dcc_direct_download_table()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_download_record'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC direct download migration requires dcc_controlled_file_download_record';
  END IF;
END//

DROP PROCEDURE IF EXISTS rename_dcc_download_status_column//
CREATE PROCEDURE rename_dcc_download_status_column()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_download_record'
      AND COLUMN_NAME = 'encryption_status'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_download_record'
      AND COLUMN_NAME = 'download_status'
  ) THEN
    ALTER TABLE `dcc_controlled_file_download_record`
      CHANGE COLUMN `encryption_status` `download_status` varchar(32) NOT NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_download_record'
      AND COLUMN_NAME = 'download_status'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC direct download migration requires download_status';
  END IF;
END//

DROP PROCEDURE IF EXISTS drop_dcc_download_column_if_exists//
CREATE PROCEDURE drop_dcc_download_column_if_exists(IN target_column VARCHAR(64))
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_download_record'
      AND COLUMN_NAME = target_column
  ) THEN
    SET @dcc_direct_download_ddl = CONCAT(
      'ALTER TABLE `dcc_controlled_file_download_record` DROP COLUMN `',
      target_column,
      '`'
    );
    PREPARE dcc_direct_download_stmt FROM @dcc_direct_download_ddl;
    EXECUTE dcc_direct_download_stmt;
    DEALLOCATE PREPARE dcc_direct_download_stmt;
  END IF;
END//

DELIMITER ;

CALL ensure_dcc_direct_download_table();
CALL rename_dcc_download_status_column();
CALL drop_dcc_download_column_if_exists('encryption_status');
CALL drop_dcc_download_column_if_exists('encryption_policy_version');
CALL drop_dcc_download_column_if_exists('artifact_id');
CALL drop_dcc_download_column_if_exists('cipher_file_ref');
CALL drop_dcc_download_column_if_exists('cipher_sha256');
CALL drop_dcc_download_column_if_exists('encrypted_at');

DROP PROCEDURE IF EXISTS ensure_dcc_direct_download_table;
DROP PROCEDURE IF EXISTS rename_dcc_download_status_column;
DROP PROCEDURE IF EXISTS drop_dcc_download_column_if_exists;
