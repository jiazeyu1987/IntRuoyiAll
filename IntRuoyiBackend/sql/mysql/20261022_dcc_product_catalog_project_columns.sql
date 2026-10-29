-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_dcc_product_catalog_database; supersedes=20260729_dcc_product_catalog_project_code_columns; type=schema; riskLevel=medium; requiresTargetPreflight=true; applyOrder=10
-- Preserve the generic project columns without replaying fixed Excel-row business mappings.
-- Existing catalog rows, including project values and all media references, remain unchanged.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @dcc_product_catalog_project_name_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dcc_product_catalog'
    AND column_name = 'project_name'
);
SET @dcc_product_catalog_project_name_column_sql := IF(
  @dcc_product_catalog_project_name_column_exists = 0,
  'ALTER TABLE `dcc_product_catalog` ADD COLUMN `project_name` varchar(255) DEFAULT NULL COMMENT ''项目名称'' AFTER `product_code`',
  'SELECT 1'
);
PREPARE dcc_product_catalog_project_name_column_stmt FROM @dcc_product_catalog_project_name_column_sql;
EXECUTE dcc_product_catalog_project_name_column_stmt;
DEALLOCATE PREPARE dcc_product_catalog_project_name_column_stmt;

SET @dcc_product_catalog_project_code_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dcc_product_catalog'
    AND column_name = 'project_code'
);
SET @dcc_product_catalog_project_code_column_sql := IF(
  @dcc_product_catalog_project_code_column_exists = 0,
  'ALTER TABLE `dcc_product_catalog` ADD COLUMN `project_code` varchar(64) DEFAULT NULL COMMENT ''项目代码'' AFTER `project_name`',
  'SELECT 1'
);
PREPARE dcc_product_catalog_project_code_column_stmt FROM @dcc_product_catalog_project_code_column_sql;
EXECUTE dcc_product_catalog_project_code_column_stmt;
DEALLOCATE PREPARE dcc_product_catalog_project_code_column_stmt;

