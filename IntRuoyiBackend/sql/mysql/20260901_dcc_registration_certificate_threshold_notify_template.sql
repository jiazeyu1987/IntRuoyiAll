-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_reminder; type=data; riskLevel=low
-- 注册证到期阈值提醒站内信模板；补齐每日提醒任务正式通知依赖。
-- Fail fast: 只补正式站内信模板，不增加短信、静默成功或其它模板兜底。

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '国内注册证到期提醒',
       'DCC_REGISTRATION_CERTIFICATE_THRESHOLD_REMINDER',
       2,
       '国内注册证中心',
       '注册证{certificateId}已进入{thresholdLevel}提醒窗口，到期日：{dueDate}，业务日期：{businessDate}。请及时处理。',
       '["certificateId","thresholdLevel","dueDate","businessDate"]',
       0,
       '国内注册证每日提醒扫描站内信模板',
       'reg-cert-reminder-template',
       NOW(),
       'reg-cert-reminder-template',
       NOW(),
       b'0'
 WHERE NOT EXISTS (
       SELECT 1
         FROM `system_notify_template`
        WHERE `code` = 'DCC_REGISTRATION_CERTIFICATE_THRESHOLD_REMINDER'
          AND `deleted` = b'0'
       );
