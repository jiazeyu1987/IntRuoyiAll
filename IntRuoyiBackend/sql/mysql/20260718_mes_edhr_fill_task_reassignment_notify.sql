-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_edhr_work_task_flow; type=data; riskLevel=low
-- eDHR fill task reassignment notify template.
-- Fail fast: this migration only seeds a missing notify template and does not grant menus, bind roles,
-- create users, or overwrite customized templates.

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR填写任务转派通知', 'MES_EDHR_FILL_TASK_REASSIGNED', 2, 'eDHR任务中心',
       '批记录填写任务已转给你：请填写工单{workOrderCode}批次{batchCode}的{processName}批记录。原因：{reason}。入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","workTaskId","reason"]', 0, 'eDHR填写任务换人转派',
       'edhr-fill-reassignment', NOW(), 'edhr-fill-reassignment', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM `system_notify_template`
     WHERE `code` = 'MES_EDHR_FILL_TASK_REASSIGNED'
       AND `deleted` = b'0'
);
