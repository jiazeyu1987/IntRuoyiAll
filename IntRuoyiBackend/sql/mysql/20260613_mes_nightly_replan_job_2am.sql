-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 智能排产每天凌晨 2 点自动重排任务配置。
-- 默认启用：重排服务自身按排产工单池、优先级、承诺交期、最晚开工、产能、班次、休息日、夜班和冻结规则执行。

INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5616, '每天凌晨 2 点重排 MES 排产工单', 1, 'mesProNightlyReplanJob', '', '0 0 2 * * ?', 3, 60, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
  WHERE (`id` = 5616 OR `handler_name` = 'mesProNightlyReplanJob')
    AND `deleted` = b'0'
);

UPDATE `infra_job`
SET `name` = '每天凌晨 2 点重排 MES 排产工单',
    `status` = 1,
    `handler_name` = 'mesProNightlyReplanJob',
    `handler_param` = '',
    `cron_expression` = '0 0 2 * * ?',
    `retry_count` = 3,
    `retry_interval` = 60,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 5616
   OR `handler_name` = 'mesProNightlyReplanJob';
