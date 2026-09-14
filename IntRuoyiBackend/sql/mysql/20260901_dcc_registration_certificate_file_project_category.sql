-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260817_dcc_registration_certificate_core; type=schema; riskLevel=medium
-- Purpose: Add DCC project-code and file-type taxonomy snapshots to registration-certificate business files.
-- Recovery: Re-run this idempotent migration after restoring the interrupted database backup.
-- Rollback: Drop the added nullable columns and indexes only after confirming no registration-certificate project-code document views depend on them.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_file_project_category_20260901;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_reg_cert_file_project_category_20260901()
BEGIN
  IF NOT EXISTS (
      SELECT 1
        FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing dcc_registration_certificate_file for project category migration';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND COLUMN_NAME = 'dcc_project_code_id'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD COLUMN `dcc_project_code_id` bigint DEFAULT NULL COMMENT 'DCC project code associated with this certificate file' AFTER `bound_by`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND COLUMN_NAME = 'file_type_taxonomy_id'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD COLUMN `file_type_taxonomy_id` bigint DEFAULT NULL COMMENT 'DCC file type taxonomy leaf id' AFTER `dcc_project_code_id`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND COLUMN_NAME = 'file_type_level1'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD COLUMN `file_type_level1` varchar(64) DEFAULT NULL COMMENT 'File type level 1 snapshot' AFTER `file_type_taxonomy_id`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND COLUMN_NAME = 'file_type_level2'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD COLUMN `file_type_level2` varchar(128) DEFAULT NULL COMMENT 'File type level 2 snapshot' AFTER `file_type_level1`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND COLUMN_NAME = 'file_type_level3'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD COLUMN `file_type_level3` varchar(128) DEFAULT NULL COMMENT 'File type level 3 snapshot' AFTER `file_type_level2`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND COLUMN_NAME = 'file_type_level4'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD COLUMN `file_type_level4` varchar(128) DEFAULT NULL COMMENT 'File type level 4 snapshot' AFTER `file_type_level3`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND COLUMN_NAME = 'file_type_level5'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD COLUMN `file_type_level5` varchar(128) DEFAULT NULL COMMENT 'File type level 5 snapshot' AFTER `file_type_level4`;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND INDEX_NAME = 'idx_dcc_reg_cert_file_project_code'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD KEY `idx_dcc_reg_cert_file_project_code` (`tenant_id`, `dcc_project_code_id`);
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND INDEX_NAME = 'idx_dcc_reg_cert_file_taxonomy'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_file`
      ADD KEY `idx_dcc_reg_cert_file_taxonomy` (`tenant_id`, `file_type_taxonomy_id`, `deleted`);
  END IF;

  IF (
      SELECT COUNT(*)
        FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND COLUMN_NAME IN (
           'dcc_project_code_id',
           'file_type_taxonomy_id',
           'file_type_level1',
           'file_type_level2',
           'file_type_level3',
           'file_type_level4',
           'file_type_level5'
         )
  ) <> 7 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate file project category columns incomplete';
  END IF;

  IF (
      SELECT COUNT(DISTINCT INDEX_NAME)
        FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'dcc_registration_certificate_file'
         AND INDEX_NAME IN (
           'idx_dcc_reg_cert_file_project_code',
           'idx_dcc_reg_cert_file_taxonomy'
         )
  ) <> 2 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate file project category indexes incomplete';
  END IF;
END $$
DELIMITER ;

CALL ensure_dcc_reg_cert_file_project_category_20260901();

DROP PROCEDURE IF EXISTS ensure_dcc_reg_cert_file_project_category_20260901;
