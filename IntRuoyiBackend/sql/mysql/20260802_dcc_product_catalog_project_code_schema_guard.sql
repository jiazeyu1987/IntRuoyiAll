-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260710_dcc_product_catalog_database; type=schema; riskLevel=medium
-- DCC product catalog project columns are required by runtime code; keep this schema-only guard in code-only releases.
-- Rollback: ALTER TABLE dcc_product_catalog DROP COLUMN project_name, DROP COLUMN project_code; only after code no longer references these columns.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @dcc_product_catalog_schema_guard_project_name_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dcc_product_catalog'
    AND column_name = 'project_name'
);
SET @dcc_product_catalog_schema_guard_project_name_sql := IF(
  @dcc_product_catalog_schema_guard_project_name_exists = 0,
  'ALTER TABLE `dcc_product_catalog` ADD COLUMN `project_name` varchar(255) DEFAULT NULL COMMENT ''项目名称'' AFTER `product_code`',
  'SELECT ''dcc_product_catalog.project_name already exists'' AS migration_status'
);
PREPARE dcc_product_catalog_schema_guard_project_name_stmt
  FROM @dcc_product_catalog_schema_guard_project_name_sql;
EXECUTE dcc_product_catalog_schema_guard_project_name_stmt;
DEALLOCATE PREPARE dcc_product_catalog_schema_guard_project_name_stmt;

SET @dcc_product_catalog_schema_guard_project_code_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dcc_product_catalog'
    AND column_name = 'project_code'
);
SET @dcc_product_catalog_schema_guard_project_code_sql := IF(
  @dcc_product_catalog_schema_guard_project_code_exists = 0,
  'ALTER TABLE `dcc_product_catalog` ADD COLUMN `project_code` varchar(64) DEFAULT NULL COMMENT ''项目代码'' AFTER `project_name`',
  'SELECT ''dcc_product_catalog.project_code already exists'' AS migration_status'
);
PREPARE dcc_product_catalog_schema_guard_project_code_stmt
  FROM @dcc_product_catalog_schema_guard_project_code_sql;
EXECUTE dcc_product_catalog_schema_guard_project_code_stmt;
DEALLOCATE PREPARE dcc_product_catalog_schema_guard_project_code_stmt;
