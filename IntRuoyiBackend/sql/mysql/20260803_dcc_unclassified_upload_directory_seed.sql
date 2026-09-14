-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=seed; riskLevel=low
-- Seed the formal DCC unclassified upload directory for categories without an explicit submit-directory binding.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS apply_dcc_unclassified_upload_directory_seed;

DELIMITER //
CREATE PROCEDURE apply_dcc_unclassified_upload_directory_seed()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_file_directory'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_UNCLASSIFIED_UPLOAD_DIRECTORY_SEED_TABLE_MISSING';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `dcc_file_directory` directory_record
    WHERE directory_record.`deleted` = 0
      AND directory_record.`active` = 1
      AND directory_record.`code` = 'UNCLASSIFIED'
    GROUP BY directory_record.`tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_UNCLASSIFIED_UPLOAD_DIRECTORY_SEED_DUPLICATE_ACTIVE';
  END IF;

  INSERT INTO `dcc_file_directory` (
    `parent_id`, `code`, `name`, `active`, `sort`, `remark`, `access_rule_manually_bound`,
    `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`
  )
  SELECT
    NULL,
    'UNCLASSIFIED',
    CONVERT(UNHEX('E69CAAE58886E7B1BB') USING utf8mb4),
    1,
    999999,
    'Formal default upload directory for DCC categories without submit-directory binding',
    0,
    tenant_source.`tenant_id`,
    NOW(),
    NOW(),
    'migration',
    'migration',
    0
  FROM (
    SELECT 0 AS `tenant_id`
    UNION
    SELECT DISTINCT category.`tenant_id`
    FROM `dcc_file_category` category
    WHERE category.`deleted` = 0
  ) tenant_source
  WHERE NOT EXISTS (
    SELECT 1
    FROM `dcc_file_directory` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`deleted` = 0
      AND existing.`code` = 'UNCLASSIFIED'
  );

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT 0 AS `tenant_id`
      UNION
      SELECT DISTINCT category.`tenant_id`
      FROM `dcc_file_category` category
      WHERE category.`deleted` = 0
    ) tenant_source
    WHERE NOT EXISTS (
      SELECT 1
      FROM `dcc_file_directory` directory_record
      WHERE directory_record.`tenant_id` = tenant_source.`tenant_id`
        AND directory_record.`deleted` = 0
        AND directory_record.`active` = 1
        AND directory_record.`code` = 'UNCLASSIFIED'
    )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_UNCLASSIFIED_UPLOAD_DIRECTORY_SEED_INSERT_INCOMPLETE';
  END IF;
END//
DELIMITER ;

CALL apply_dcc_unclassified_upload_directory_seed();

DROP PROCEDURE IF EXISTS apply_dcc_unclassified_upload_directory_seed;
