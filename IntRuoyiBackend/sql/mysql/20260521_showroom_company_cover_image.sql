-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Add company cover image persistence to showroom company revisions.
-- Safe to run repeatedly on MySQL runtime schemas.

DROP PROCEDURE IF EXISTS ensure_showroom_company_cover_image_column;
DELIMITER $$
CREATE PROCEDURE ensure_showroom_company_cover_image_column()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'showroom_company_revision'
        AND COLUMN_NAME = 'cover_image'
  ) THEN
    ALTER TABLE `showroom_company_revision`
      ADD COLUMN `cover_image` text DEFAULT NULL AFTER `stock_info`;
  END IF;
END$$
DELIMITER ;

CALL ensure_showroom_company_cover_image_column();
DROP PROCEDURE IF EXISTS ensure_showroom_company_cover_image_column;
