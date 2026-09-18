-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- Persist the required read-only artifact and optional editable artifact for new DCC uploads.

DELIMITER //

DROP PROCEDURE IF EXISTS apply_dcc_dual_version_file_schema//
CREATE PROCEDURE apply_dcc_dual_version_file_schema()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_CONTROLLED_FILE_TABLE_MISSING';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
      AND COLUMN_NAME = 'read_only_file_id'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `read_only_file_id` bigint DEFAULT NULL
      COMMENT 'required non-editable online browsing artifact'
      AFTER `original_file_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcc_controlled_file'
      AND COLUMN_NAME = 'editable_file_id'
  ) THEN
    ALTER TABLE `dcc_controlled_file`
      ADD COLUMN `editable_file_id` bigint DEFAULT NULL
      COMMENT 'optional editable source artifact'
      AFTER `read_only_file_id`;
  END IF;
END//

DELIMITER ;

CALL apply_dcc_dual_version_file_schema();
DROP PROCEDURE IF EXISTS apply_dcc_dual_version_file_schema;
