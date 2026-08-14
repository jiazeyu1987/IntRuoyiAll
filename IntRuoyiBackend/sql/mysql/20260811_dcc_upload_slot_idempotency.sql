-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260528_dcc_controlled_file_protection; type=schema; riskLevel=medium
-- Enforce one active DCC upload ticket per tenant, uploader, session and purpose.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_upload_slot_idempotency;

DELIMITER //
CREATE PROCEDURE ensure_dcc_upload_slot_idempotency()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_temporary_file'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_UPLOAD_SLOT_TEMPORARY_FILE_TABLE_MISSING';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT `tenant_id`, `uploader_id`, `session_id`, `purpose`
      FROM `dcc_controlled_file_temporary_file`
      WHERE `deleted` = 0
        AND `status` = 'AVAILABLE'
        AND `cleanup_status` IN ('ACTIVE', 'CLEANING')
        AND `bound_controlled_file_id` IS NULL
      GROUP BY `tenant_id`, `uploader_id`, `session_id`, `purpose`
      HAVING COUNT(*) > 1
    ) duplicate_active_slot
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_UPLOAD_SLOT_DUPLICATES_REQUIRE_MANUAL_REMEDIATION';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_temporary_file'
      AND COLUMN_NAME = 'active_slot_unique_flag'
  ) THEN
    ALTER TABLE `dcc_controlled_file_temporary_file`
      ADD COLUMN `active_slot_unique_flag` tinyint GENERATED ALWAYS AS (
        CASE
          WHEN `deleted` = 0
            AND `status` = 'AVAILABLE'
            AND `cleanup_status` IN ('ACTIVE', 'CLEANING')
            AND `bound_controlled_file_id` IS NULL
          THEN 1
          ELSE NULL
        END
      ) STORED AFTER `deleted`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_temporary_file'
      AND INDEX_NAME = 'uk_dcc_temp_active_slot'
  ) THEN
    ALTER TABLE `dcc_controlled_file_temporary_file`
      ADD UNIQUE KEY `uk_dcc_temp_active_slot`
        (`tenant_id`, `uploader_id`, `session_id`, `purpose`, `active_slot_unique_flag`);
  END IF;
END//
DELIMITER ;

CALL ensure_dcc_upload_slot_idempotency();
DROP PROCEDURE IF EXISTS ensure_dcc_upload_slot_idempotency;
