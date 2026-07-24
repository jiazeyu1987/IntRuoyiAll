-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SELECT COUNT(*)
INTO @showroom_hall_product_layout_x_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'showroom_hall_product'
  AND COLUMN_NAME = 'layout_x';

SET @showroom_hall_product_layout_x_sql = IF(
  @showroom_hall_product_layout_x_column_count = 0,
  'ALTER TABLE `showroom_hall_product` ADD COLUMN `layout_x` decimal(8,6) DEFAULT NULL COMMENT ''Normalized canvas rectangle x'' AFTER `display_order`',
  'SELECT ''showroom_hall_product.layout_x already exists'' AS migration_status'
);

PREPARE showroom_hall_product_layout_x_stmt FROM @showroom_hall_product_layout_x_sql;
EXECUTE showroom_hall_product_layout_x_stmt;
DEALLOCATE PREPARE showroom_hall_product_layout_x_stmt;

SELECT COUNT(*)
INTO @showroom_hall_product_layout_y_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'showroom_hall_product'
  AND COLUMN_NAME = 'layout_y';

SET @showroom_hall_product_layout_y_sql = IF(
  @showroom_hall_product_layout_y_column_count = 0,
  'ALTER TABLE `showroom_hall_product` ADD COLUMN `layout_y` decimal(8,6) DEFAULT NULL COMMENT ''Normalized canvas rectangle y'' AFTER `layout_x`',
  'SELECT ''showroom_hall_product.layout_y already exists'' AS migration_status'
);

PREPARE showroom_hall_product_layout_y_stmt FROM @showroom_hall_product_layout_y_sql;
EXECUTE showroom_hall_product_layout_y_stmt;
DEALLOCATE PREPARE showroom_hall_product_layout_y_stmt;

SELECT COUNT(*)
INTO @showroom_hall_product_layout_width_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'showroom_hall_product'
  AND COLUMN_NAME = 'layout_width';

SET @showroom_hall_product_layout_width_sql = IF(
  @showroom_hall_product_layout_width_column_count = 0,
  'ALTER TABLE `showroom_hall_product` ADD COLUMN `layout_width` decimal(8,6) DEFAULT NULL COMMENT ''Normalized canvas rectangle width'' AFTER `layout_y`',
  'SELECT ''showroom_hall_product.layout_width already exists'' AS migration_status'
);

PREPARE showroom_hall_product_layout_width_stmt FROM @showroom_hall_product_layout_width_sql;
EXECUTE showroom_hall_product_layout_width_stmt;
DEALLOCATE PREPARE showroom_hall_product_layout_width_stmt;

SELECT COUNT(*)
INTO @showroom_hall_product_layout_height_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'showroom_hall_product'
  AND COLUMN_NAME = 'layout_height';

SET @showroom_hall_product_layout_height_sql = IF(
  @showroom_hall_product_layout_height_column_count = 0,
  'ALTER TABLE `showroom_hall_product` ADD COLUMN `layout_height` decimal(8,6) DEFAULT NULL COMMENT ''Normalized canvas rectangle height'' AFTER `layout_width`',
  'SELECT ''showroom_hall_product.layout_height already exists'' AS migration_status'
);

PREPARE showroom_hall_product_layout_height_stmt FROM @showroom_hall_product_layout_height_sql;
EXECUTE showroom_hall_product_layout_height_stmt;
DEALLOCATE PREPARE showroom_hall_product_layout_height_stmt;
