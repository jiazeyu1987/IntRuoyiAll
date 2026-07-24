-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SELECT COUNT(*)
INTO @dcc_product_name_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'dcc_controlled_file'
  AND COLUMN_NAME = 'product_name';

SET @dcc_product_name_sql = IF(
  @dcc_product_name_column_count = 0,
  'ALTER TABLE `dcc_controlled_file` ADD COLUMN `product_name` varchar(255) DEFAULT NULL AFTER `product_code`',
  'SELECT ''dcc_controlled_file.product_name already exists'' AS migration_status'
);

PREPARE dcc_product_name_stmt FROM @dcc_product_name_sql;
EXECUTE dcc_product_name_stmt;
DEALLOCATE PREPARE dcc_product_name_stmt;
