-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_showroom_award_and_hall_item_schema; type=schema; riskLevel=medium
SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'showroom_hall'
      AND COLUMN_NAME = 'canvas_background_image_url'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE `showroom_hall` ADD COLUMN `canvas_background_image_url` varchar(1024) DEFAULT NULL AFTER `description_en`',
    'SELECT ''showroom_hall_canvas_background_image_url_exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
