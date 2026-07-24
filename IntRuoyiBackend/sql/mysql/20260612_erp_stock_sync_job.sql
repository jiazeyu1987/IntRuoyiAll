-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- ERP 库存自动增量同步任务配置。
-- 默认暂停：上线或联调前由管理员确认金蝶账套、同步窗口和运行频率后再启用。

INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5603, '每 10 分钟同步 ERP 库存', 2, 'kingdeeStockSyncJob', '', '0 5/10 * * * ?', 3, 60, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
  WHERE (`id` = 5603 OR `handler_name` = 'kingdeeStockSyncJob')
    AND `deleted` = b'0'
);

UPDATE `infra_job`
SET `name` = '每 10 分钟同步 ERP 库存',
    `status` = 2,
    `handler_name` = 'kingdeeStockSyncJob',
    `handler_param` = '',
    `cron_expression` = '0 5/10 * * * ?',
    `retry_count` = 3,
    `retry_interval` = 60,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 5603
   OR `handler_name` = 'kingdeeStockSyncJob';
