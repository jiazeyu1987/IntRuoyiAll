-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260519_showroom_v1_schema; type=data; riskLevel=medium
-- Goal: move product countries-on-sale text from core_selling_points into target_market.
-- Safety: do not overwrite existing target_market / target_market_en values.

UPDATE `showroom_product_revision`
SET
  `target_market` = TRIM(SUBSTRING_INDEX(REPLACE(`core_selling_points`, CHAR(13), ''), CHAR(10), 1)),
  `core_selling_points` = NULLIF(TRIM(
    CASE
      WHEN LOCATE(CHAR(10), REPLACE(`core_selling_points`, CHAR(13), '')) > 0
        THEN SUBSTRING(
          REPLACE(`core_selling_points`, CHAR(13), ''),
          LOCATE(CHAR(10), REPLACE(`core_selling_points`, CHAR(13), '')) + 1
        )
      ELSE ''
    END
  ), '')
WHERE `deleted` = b'0'
  AND (`target_market` IS NULL OR TRIM(`target_market`) = '')
  AND `core_selling_points` IS NOT NULL
  AND TRIM(`core_selling_points`) <> '';

UPDATE `showroom_product_revision`
SET
  `target_market_en` = TRIM(SUBSTRING_INDEX(REPLACE(`core_selling_points_en`, CHAR(13), ''), CHAR(10), 1)),
  `core_selling_points_en` = NULLIF(TRIM(
    CASE
      WHEN LOCATE(CHAR(10), REPLACE(`core_selling_points_en`, CHAR(13), '')) > 0
        THEN SUBSTRING(
          REPLACE(`core_selling_points_en`, CHAR(13), ''),
          LOCATE(CHAR(10), REPLACE(`core_selling_points_en`, CHAR(13), '')) + 1
        )
      ELSE ''
    END
  ), '')
WHERE `deleted` = b'0'
  AND (`target_market_en` IS NULL OR TRIM(`target_market_en`) = '')
  AND `core_selling_points_en` IS NOT NULL
  AND TRIM(`core_selling_points_en`) <> '';
