-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260623_dcc_view_matrix_independent_source; type=schema; riskLevel=medium
-- DCC review matrix rule editor metadata columns.
-- Idempotent for local/test/prod MySQL 8 deployments.

SET @schema_name := DATABASE();

SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `stage_type` varchar(32) DEFAULT NULL COMMENT ''规则阶段类型：DOC_CONTROL/SIGNOFF/APPROVAL'' AFTER `sort`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'dcc_category_approval_route_node'
    AND COLUMN_NAME = 'stage_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_label` varchar(255) DEFAULT NULL COMMENT ''主体标签'' AFTER `stage_type`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'dcc_category_approval_route_node'
    AND COLUMN_NAME = 'subject_label'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `marker` varchar(32) DEFAULT NULL COMMENT ''标记'' AFTER `subject_label`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'dcc_category_approval_route_node'
    AND COLUMN_NAME = 'marker'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_type` varchar(32) DEFAULT NULL COMMENT ''主体类型'' AFTER `marker`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'dcc_category_approval_route_node'
    AND COLUMN_NAME = 'subject_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_id` bigint DEFAULT NULL COMMENT ''主体ID'' AFTER `subject_type`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'dcc_category_approval_route_node'
    AND COLUMN_NAME = 'subject_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_name` varchar(255) DEFAULT NULL COMMENT ''主体名称'' AFTER `subject_id`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'dcc_category_approval_route_node'
    AND COLUMN_NAME = 'subject_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `subject_department_path` varchar(500) DEFAULT NULL COMMENT ''对应部门路径'' AFTER `subject_name`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'dcc_category_approval_route_node'
    AND COLUMN_NAME = 'subject_department_path'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `dcc_category_approval_route_node` ADD COLUMN `rule_remark` varchar(255) DEFAULT NULL COMMENT ''规则备注'' AFTER `subject_department_path`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'dcc_category_approval_route_node'
    AND COLUMN_NAME = 'rule_remark'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
