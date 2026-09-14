-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_dcc_upload_slot_idempotency; type=schema; riskLevel=medium
-- Bind every new DCC upload ticket to the category whose permission and size policy were evaluated.

DELIMITER //

DROP PROCEDURE IF EXISTS `apply_dcc_upload_ticket_category_guard`//
CREATE PROCEDURE `apply_dcc_upload_ticket_category_guard`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_temporary_file'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_UPLOAD_TICKET_CATEGORY_TABLE_MISSING';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_temporary_file'
      AND COLUMN_NAME = 'category_id'
  ) THEN
    ALTER TABLE `dcc_controlled_file_temporary_file`
      ADD COLUMN `category_id` BIGINT NULL AFTER `purpose`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_temporary_file'
      AND INDEX_NAME = 'idx_dcc_temp_category'
  ) THEN
    ALTER TABLE `dcc_controlled_file_temporary_file`
      ADD KEY `idx_dcc_temp_category`
        (`tenant_id`, `category_id`, `uploader_id`, `session_id`, `purpose`);
  END IF;
END//

DELIMITER ;

CALL `apply_dcc_upload_ticket_category_guard`();
DROP PROCEDURE IF EXISTS `apply_dcc_upload_ticket_category_guard`;
