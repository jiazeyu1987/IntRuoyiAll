-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=seed; riskLevel=low
INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '运行控制台告警通知', 'RUNTIME_OPS_ALERT', 2, '运行控制台',
       '【{environment}】{severity}：{title}。动作：{action}。{content}',
       '["environment","action","severity","title","content"]', 0, 'Runtime control alert notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'RUNTIME_OPS_ALERT');
