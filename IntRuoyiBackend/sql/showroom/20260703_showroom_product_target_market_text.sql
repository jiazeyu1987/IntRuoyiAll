-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260519_showroom_v1_schema; type=schema; riskLevel=medium
-- Goal: allow imported product sales-country lists to exceed 255 characters.
-- Safety: widening varchar(255) to text is non-destructive for existing values.
-- Rollback: ALTER TABLE showroom_product_revision MODIFY COLUMN target_market varchar(255) DEFAULT NULL;
--          Only run rollback after confirming all existing values fit in 255 characters.

ALTER TABLE `showroom_product_revision`
    MODIFY COLUMN `target_market` text DEFAULT NULL;
