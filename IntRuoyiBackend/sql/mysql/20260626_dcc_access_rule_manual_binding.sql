-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260515_dcc_runtime_schema_repair; type=schema; riskLevel=medium
-- Add explicit directory-level source tracking for access-rule directories manually saved from the access-rule page.
-- Safe to run repeatedly: only adds the missing column when absent and does not backfill historical rows.

SELECT COUNT(*) INTO @dcc_file_directory_access_rule_manually_bound_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'dcc_file_directory'
  AND column_name = 'access_rule_manually_bound';

SET @dcc_file_directory_access_rule_manually_bound_sql = IF(
  @dcc_file_directory_access_rule_manually_bound_column_count = 0,
  'ALTER TABLE `dcc_file_directory` ADD COLUMN `access_rule_manually_bound` tinyint NOT NULL DEFAULT 0 AFTER `remark`',
  'SELECT ''dcc_file_directory.access_rule_manually_bound already exists'' AS migration_status'
);

PREPARE dcc_file_directory_access_rule_manually_bound_stmt FROM @dcc_file_directory_access_rule_manually_bound_sql;
EXECUTE dcc_file_directory_access_rule_manually_bound_stmt;
DEALLOCATE PREPARE dcc_file_directory_access_rule_manually_bound_stmt;
