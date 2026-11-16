-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_edhr_work_task_flow; type=data; riskLevel=low
INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR资料补件责任通知', 'MES_EDHR_DOSSIER_COMPLETE_ASSIGNED', 2, 'eDHR任务中心',
       '请补齐工单{workOrderCode}的{processName}。要求：{reason}；入口：{actionUrl}',
       '["workOrderCode","processName","actionUrl","reason","workTaskId"]', 0,
       '活跃订单资料专项责任交接，上传对应类别资料后完成', 'edhr-dossier-completion', NOW(), 'edhr-dossier-completion', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code` = 'MES_EDHR_DOSSIER_COMPLETE_ASSIGNED' AND `deleted` = b'0');
