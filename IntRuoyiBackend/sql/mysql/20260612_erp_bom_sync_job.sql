-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- ERP BOM automatic incremental sync job.
-- Default paused; administrators enable it after Kingdee test account validation.

INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5606, '每 10 分钟同步 ERP BOM', 2, 'kingdeeBomSyncJob', '', '0 3/10 * * * ?', 3, 60, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `infra_job`
    WHERE (`id` = 5606 OR `handler_name` = 'kingdeeBomSyncJob')
      AND `deleted` = b'0'
);

UPDATE `infra_job`
SET `name` = '每 10 分钟同步 ERP BOM',
    `status` = 2,
    `handler_name` = 'kingdeeBomSyncJob',
    `handler_param` = '',
    `cron_expression` = '0 3/10 * * * ?',
    `retry_count` = 3,
    `retry_interval` = 60,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 5606
  AND `deleted` = b'0';

UPDATE `infra_job`
SET `name` = '每 10 分钟同步 ERP BOM',
    `status` = 2,
    `handler_param` = '',
    `cron_expression` = '0 3/10 * * * ?',
    `retry_count` = 3,
    `retry_interval` = 60,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW()
WHERE `handler_name` = 'kingdeeBomSyncJob'
  AND `deleted` = b'0';
