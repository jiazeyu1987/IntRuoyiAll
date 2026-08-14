-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260528_dcc_controlled_file_protection; type=seed; riskLevel=low
-- Seed formal tenant-level DCC upload size policies so upload-preview can fail fast on configured limits instead of missing policy data.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS apply_dcc_upload_size_policy_default_seed;

DELIMITER //
CREATE PROCEDURE apply_dcc_upload_size_policy_default_seed()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_controlled_file_upload_policy'
  ) OR NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dcc_file_category'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_UPLOAD_SIZE_POLICY_DEFAULT_SEED_TABLE_MISSING';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_upload_size_policy_default_seed`;
  CREATE TEMPORARY TABLE `tmp_dcc_upload_size_policy_default_seed` (
    `purpose` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `policy_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `max_bytes` BIGINT NOT NULL,
    `policy_version` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `change_reason` VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`purpose`)
  ) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO `tmp_dcc_upload_size_policy_default_seed` (
    `purpose`, `policy_code`, `max_bytes`, `policy_version`, `change_reason`
  )
  VALUES
    ('SOURCE', 'DCC_UPLOAD_DEFAULT_SOURCE_V1', 10485760, 'v1', 'Default DCC SOURCE upload size policy seed'),
    ('DRAWING_PDF', 'DCC_UPLOAD_DEFAULT_DRAWING_PDF_V1', 10485760, 'v1', 'Default DCC DRAWING_PDF upload size policy seed'),
    ('TRAINING_RECORD', 'DCC_UPLOAD_DEFAULT_TRAINING_RECORD_V1', 10485760, 'v1', 'Default DCC TRAINING_RECORD upload size policy seed'),
    ('EXTERNAL_REVIEW_OUTPUT', 'DCC_UPLOAD_DEFAULT_EXTERNAL_REVIEW_OUTPUT_V1', 10485760, 'v1', 'Default DCC EXTERNAL_REVIEW_OUTPUT upload size policy seed');

  INSERT INTO `dcc_controlled_file_upload_policy` (
    `policy_code`, `scope_type`, `category_id`, `purpose`, `max_bytes`, `enabled`, `priority`,
    `policy_version`, `effective_from`, `effective_to`, `change_reason`,
    `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`
  )
  SELECT
    seed.`policy_code`,
    'PURPOSE',
    NULL,
    seed.`purpose`,
    seed.`max_bytes`,
    b'1',
    2,
    seed.`policy_version`,
    NULL,
    NULL,
    seed.`change_reason`,
    tenant_source.`tenant_id`,
    NOW(),
    NOW(),
    'migration',
    'migration',
    0
  FROM (
    SELECT DISTINCT category.`tenant_id`
    FROM `dcc_file_category` category
    WHERE category.`deleted` = 0
      AND category.`active` = 1
  ) tenant_source
  CROSS JOIN `tmp_dcc_upload_size_policy_default_seed` seed
  WHERE NOT EXISTS (
    SELECT 1
    FROM `dcc_controlled_file_upload_policy` existing
    WHERE existing.`tenant_id` = tenant_source.`tenant_id`
      AND existing.`deleted` = 0
      AND existing.`enabled` = b'1'
      AND existing.`max_bytes` > 0
      AND (existing.`effective_from` IS NULL OR existing.`effective_from` <= NOW())
      AND (existing.`effective_to` IS NULL OR existing.`effective_to` > NOW())
      AND (
        existing.`scope_type` = 'GLOBAL'
        OR (existing.`scope_type` = 'PURPOSE' AND existing.`purpose` = seed.`purpose`)
      )
  )
    AND NOT EXISTS (
      SELECT 1
      FROM `dcc_controlled_file_upload_policy` same_code
      WHERE same_code.`tenant_id` = tenant_source.`tenant_id`
        AND same_code.`policy_code` = seed.`policy_code`
        AND same_code.`deleted` = 0
    );

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT DISTINCT category.`tenant_id`
      FROM `dcc_file_category` category
      WHERE category.`deleted` = 0
        AND category.`active` = 1
    ) tenant_source
    CROSS JOIN `tmp_dcc_upload_size_policy_default_seed` seed
    WHERE NOT EXISTS (
      SELECT 1
      FROM `dcc_controlled_file_upload_policy` effective_policy
      WHERE effective_policy.`tenant_id` = tenant_source.`tenant_id`
        AND effective_policy.`deleted` = 0
        AND effective_policy.`enabled` = b'1'
        AND effective_policy.`max_bytes` > 0
        AND (effective_policy.`effective_from` IS NULL OR effective_policy.`effective_from` <= NOW())
        AND (effective_policy.`effective_to` IS NULL OR effective_policy.`effective_to` > NOW())
        AND (
          effective_policy.`scope_type` = 'GLOBAL'
          OR (effective_policy.`scope_type` = 'PURPOSE' AND effective_policy.`purpose` = seed.`purpose`)
        )
    )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_UPLOAD_SIZE_POLICY_DEFAULT_SEED_INSERT_INCOMPLETE';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_upload_size_policy_default_seed`;
END//
DELIMITER ;

CALL apply_dcc_upload_size_policy_default_seed();

DROP PROCEDURE IF EXISTS apply_dcc_upload_size_policy_default_seed;
