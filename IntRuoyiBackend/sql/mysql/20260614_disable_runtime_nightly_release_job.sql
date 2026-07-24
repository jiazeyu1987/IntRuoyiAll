-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Disable the removed runtime-control nightly release job in existing environments.
UPDATE `infra_job`
SET `status` = 2,
    `deleted` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `handler_name` = 'runtimeNightlyReleaseJob';

SET @runtime_nightly_release_sql = (
  SELECT CONCAT(
    'DELETE FROM `', `TABLE_NAME`, '` ',
    'WHERE `TRIGGER_NAME` = ''runtimeNightlyReleaseJob'' ',
    'AND `TRIGGER_GROUP` = ''DEFAULT'''
  )
  FROM `information_schema`.`TABLES`
  WHERE `TABLE_SCHEMA` = DATABASE()
    AND UPPER(`TABLE_NAME`) = 'QRTZ_CRON_TRIGGERS'
  LIMIT 1
);
SET @runtime_nightly_release_sql = COALESCE(@runtime_nightly_release_sql, 'SELECT 1');
PREPARE runtime_nightly_release_stmt FROM @runtime_nightly_release_sql;
EXECUTE runtime_nightly_release_stmt;
DEALLOCATE PREPARE runtime_nightly_release_stmt;

SET @runtime_nightly_release_sql = (
  SELECT CONCAT(
    'DELETE FROM `', `TABLE_NAME`, '` ',
    'WHERE (`JOB_NAME` = ''runtimeNightlyReleaseJob'' AND `JOB_GROUP` = ''DEFAULT'') ',
    'OR (`TRIGGER_NAME` = ''runtimeNightlyReleaseJob'' AND `TRIGGER_GROUP` = ''DEFAULT'')'
  )
  FROM `information_schema`.`TABLES`
  WHERE `TABLE_SCHEMA` = DATABASE()
    AND UPPER(`TABLE_NAME`) = 'QRTZ_TRIGGERS'
  LIMIT 1
);
SET @runtime_nightly_release_sql = COALESCE(@runtime_nightly_release_sql, 'SELECT 1');
PREPARE runtime_nightly_release_stmt FROM @runtime_nightly_release_sql;
EXECUTE runtime_nightly_release_stmt;
DEALLOCATE PREPARE runtime_nightly_release_stmt;

SET @runtime_nightly_release_sql = (
  SELECT CONCAT(
    'DELETE FROM `', `TABLE_NAME`, '` ',
    'WHERE `JOB_NAME` = ''runtimeNightlyReleaseJob'' ',
    'AND `JOB_GROUP` = ''DEFAULT'''
  )
  FROM `information_schema`.`TABLES`
  WHERE `TABLE_SCHEMA` = DATABASE()
    AND UPPER(`TABLE_NAME`) = 'QRTZ_JOB_DETAILS'
  LIMIT 1
);
SET @runtime_nightly_release_sql = COALESCE(@runtime_nightly_release_sql, 'SELECT 1');
PREPARE runtime_nightly_release_stmt FROM @runtime_nightly_release_sql;
EXECUTE runtime_nightly_release_stmt;
DEALLOCATE PREPARE runtime_nightly_release_stmt;

SET @runtime_nightly_release_sql = NULL;
