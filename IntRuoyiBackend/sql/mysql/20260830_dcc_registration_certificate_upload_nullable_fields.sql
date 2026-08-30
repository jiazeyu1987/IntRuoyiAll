-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260817_dcc_registration_certificate_core,20260828_dcc_registration_certificate_minimal_upload_schema; type=schema; riskLevel=medium
-- 2026-08-30: uploads may save only the approved visible fields without legacy product-master or registrant bindings.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS normalize_dcc_reg_cert_upload_nullable_fields_20260830;
DELIMITER $$
CREATE PROCEDURE normalize_dcc_reg_cert_upload_nullable_fields_20260830()
BEGIN
  DECLARE master_table_count int DEFAULT 0;
  DECLARE snapshot_table_count int DEFAULT 0;
  DECLARE product_column_count int DEFAULT 0;
  DECLARE registrant_column_count int DEFAULT 0;

  SELECT COUNT(*)
    INTO master_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_registration_certificate';

  IF master_table_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate master table for product_master_id nullable migration';
  END IF;

  SELECT COUNT(*)
    INTO snapshot_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_registration_certificate_snapshot';

  IF snapshot_table_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate snapshot table for registrant_name nullable migration';
  END IF;

  SELECT COUNT(*)
    INTO product_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_registration_certificate'
     AND COLUMN_NAME = 'product_master_id';

  IF product_column_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate product_master_id column';
  END IF;

  SELECT COUNT(*)
    INTO registrant_column_count
    FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'dcc_registration_certificate_snapshot'
     AND COLUMN_NAME = 'registrant_name';

  IF registrant_column_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing registration certificate registrant_name column';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_registration_certificate'
       AND COLUMN_NAME = 'product_master_id'
       AND IS_NULLABLE = 'NO'
  ) THEN
    ALTER TABLE `dcc_registration_certificate`
      MODIFY COLUMN `product_master_id` bigint DEFAULT NULL COMMENT 'Optional MDM product master id';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_registration_certificate_snapshot'
       AND COLUMN_NAME = 'registrant_name'
       AND IS_NULLABLE = 'NO'
  ) THEN
    ALTER TABLE `dcc_registration_certificate_snapshot`
      MODIFY COLUMN `registrant_name` varchar(255) DEFAULT NULL COMMENT 'Registrant name snapshot';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_registration_certificate'
       AND COLUMN_NAME = 'product_master_id'
       AND (LOWER(COLUMN_TYPE) <> 'bigint' OR IS_NULLABLE <> 'YES')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate product_master_id nullable migration failed';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'dcc_registration_certificate_snapshot'
       AND COLUMN_NAME = 'registrant_name'
       AND (LOWER(COLUMN_TYPE) <> 'varchar(255)' OR IS_NULLABLE <> 'YES')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate registrant_name nullable migration failed';
  END IF;
END$$
DELIMITER ;

CALL normalize_dcc_reg_cert_upload_nullable_fields_20260830();

DROP PROCEDURE IF EXISTS normalize_dcc_reg_cert_upload_nullable_fields_20260830;
