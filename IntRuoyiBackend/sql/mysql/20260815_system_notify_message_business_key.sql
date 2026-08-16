-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_showroom_notify_template_garbled_repair; type=schema; riskLevel=medium
-- Add tenant-scoped station-message business-key idempotency without backfilling historical rows.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_system_notify_message_business_key;

DELIMITER //
CREATE PROCEDURE ensure_system_notify_message_business_key()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'system_notify_message'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'SYSTEM_NOTIFY_MESSAGE_TABLE_MISSING';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'system_notify_message'
      AND COLUMN_NAME = 'business_key'
  ) THEN
    ALTER TABLE `system_notify_message`
      ADD COLUMN `business_key` varchar(255) NULL DEFAULT NULL AFTER `user_type`;
  ELSEIF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'system_notify_message'
      AND COLUMN_NAME = 'business_key'
      AND DATA_TYPE = 'varchar'
      AND CHARACTER_MAXIMUM_LENGTH = 255
      AND IS_NULLABLE = 'YES'
      AND COLUMN_DEFAULT IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'SYSTEM_NOTIFY_MESSAGE_BUSINESS_KEY_COLUMN_CONFLICT';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `system_notify_message`
    WHERE `business_key` IS NOT NULL
    GROUP BY `tenant_id`, `business_key`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'SYSTEM_NOTIFY_MESSAGE_BUSINESS_KEY_DUPLICATES';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'system_notify_message'
      AND INDEX_NAME = 'uk_system_notify_message_tenant_business_key'
  ) AND NOT EXISTS (
    SELECT 1
    FROM (
      SELECT NON_UNIQUE, COUNT(*) AS column_count,
             GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS indexed_columns
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'system_notify_message'
        AND INDEX_NAME = 'uk_system_notify_message_tenant_business_key'
      GROUP BY NON_UNIQUE
      HAVING NON_UNIQUE = 0
        AND column_count = 2
        AND indexed_columns = 'tenant_id,business_key'
    ) expected_index
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'SYSTEM_NOTIFY_MESSAGE_BUSINESS_KEY_INDEX_CONFLICT';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'system_notify_message'
      AND INDEX_NAME = 'uk_system_notify_message_tenant_business_key'
  ) THEN
    ALTER TABLE `system_notify_message`
      ADD UNIQUE KEY `uk_system_notify_message_tenant_business_key` (`tenant_id`, `business_key`);
  END IF;
END//
DELIMITER ;

CALL ensure_system_notify_message_business_key();
DROP PROCEDURE IF EXISTS ensure_system_notify_message_business_key;
