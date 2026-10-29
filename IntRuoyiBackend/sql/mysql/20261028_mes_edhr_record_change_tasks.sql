-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_edhr_work_task_flow; type=data; riskLevel=low

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR记录变更审批待办', 'MES_EDHR_RECORD_CHANGE_ASSIGNED', 2, 'eDHR任务中心',
       '请审批记录变更{changeCode}：{reason}。入口：{actionUrl}',
       '["changeCode","reason","actionUrl"]', 0, 'eDHR记录变更待办',
       'edhr-record-change', NOW(), 'edhr-record-change', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code` = 'MES_EDHR_RECORD_CHANGE_ASSIGNED' AND `deleted` = b'0');

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR记录变更审批结果', 'MES_EDHR_RECORD_CHANGE_APPROVED', 2, 'eDHR任务中心',
       '记录变更{changeCode}已批准。入口：{actionUrl}',
       '["changeCode","reason","actionUrl"]', 0, 'eDHR记录变更结果通知',
       'edhr-record-change', NOW(), 'edhr-record-change', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code` = 'MES_EDHR_RECORD_CHANGE_APPROVED' AND `deleted` = b'0');

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR获批记录变更修改待办', 'MES_EDHR_RECORD_CHANGE_REWORK', 2, 'eDHR任务中心',
       '记录变更已获批准，请处理工单{workOrderCode}批次{batchCode}的{processName}记录。原因：{reason}。入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","reason","workTaskId"]', 0, 'eDHR获批变更的原填写责任交接',
       'edhr-record-change', NOW(), 'edhr-record-change', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code` = 'MES_EDHR_RECORD_CHANGE_REWORK' AND `deleted` = b'0');
