-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- ERP production material list automatic incremental sync job.
INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
                         `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`,
                         `updater`, `update_time`, `deleted`)
SELECT 5607, '每 10 分钟同步 ERP 生产用料清单', 2, 'kingdeeProductionMaterialListSyncJob', '',
       '0 5/10 * * * ?', 3, 60, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `infra_job`
    WHERE `id` = 5607 OR `handler_name` = 'kingdeeProductionMaterialListSyncJob'
);

UPDATE `infra_job`
SET `name` = '每 10 分钟同步 ERP 生产用料清单',
    `handler_name` = 'kingdeeProductionMaterialListSyncJob',
    `cron_expression` = '0 5/10 * * * ?',
    `retry_count` = 3,
    `retry_interval` = 60,
    `update_time` = NOW()
WHERE `id` = 5607;

UPDATE `infra_job`
SET `name` = '每 10 分钟同步 ERP 生产用料清单',
    `cron_expression` = '0 5/10 * * * ?',
    `retry_count` = 3,
    `retry_interval` = 60,
    `update_time` = NOW()
WHERE `handler_name` = 'kingdeeProductionMaterialListSyncJob';

