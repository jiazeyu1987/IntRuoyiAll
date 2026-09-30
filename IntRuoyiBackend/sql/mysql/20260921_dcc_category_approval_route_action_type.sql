-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium
-- DCC three workflow route split: each category owns independent NEW/REVISION/OBSOLETE routes.

SET @table_schema := DATABASE();

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @table_schema
    AND TABLE_NAME = 'dcc_category_approval_route'
    AND COLUMN_NAME = 'action_type'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE `dcc_category_approval_route` ADD COLUMN `action_type` varchar(32) NOT NULL DEFAULT ''LEGACY'' COMMENT ''Action type: LEGACY/NEW/REVISION/OBSOLETE'' AFTER `category_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `dcc_category_approval_route`
SET `action_type` = 'LEGACY'
WHERE `action_type` IS NULL OR `action_type` = '';

SET @old_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @table_schema
    AND TABLE_NAME = 'dcc_category_approval_route'
    AND INDEX_NAME = 'uk_dcc_category_route_version'
);
SET @ddl := IF(@old_index_exists > 0,
  'ALTER TABLE `dcc_category_approval_route` DROP INDEX `uk_dcc_category_route_version`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @new_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @table_schema
    AND TABLE_NAME = 'dcc_category_approval_route'
    AND INDEX_NAME = 'uk_dcc_category_route_action_version'
);
SET @ddl := IF(@new_index_exists = 0,
  'ALTER TABLE `dcc_category_approval_route` ADD UNIQUE KEY `uk_dcc_category_route_action_version` (`category_id`, `action_type`, `version_no`)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
