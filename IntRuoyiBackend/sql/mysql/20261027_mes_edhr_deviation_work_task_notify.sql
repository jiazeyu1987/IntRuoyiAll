-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260924_mes_edhr_deviation_management,20260611_mes_edhr_work_task_flow; type=data; riskLevel=low

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR偏差接收任务通知', 'MES_EDHR_DEVIATION_RECEIVE_ASSIGNED', 2, 'eDHR任务中心',
       '请接收并开展偏差{reason}。工单{workOrderCode}批次{batchCode}，入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","reason","workTaskId"]', 0,
       'eDHR偏差责任交接', 'edhr-deviation-work-task', NOW(), 'edhr-deviation-work-task', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code` = 'MES_EDHR_DEVIATION_RECEIVE_ASSIGNED' AND `deleted` = b'0');

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR偏差纠正任务通知', 'MES_EDHR_DEVIATION_CORRECT_ASSIGNED', 2, 'eDHR任务中心',
       '请处理偏差纠正措施{reason}。工单{workOrderCode}批次{batchCode}，入口：{actionUrl}',
       '["workOrderCode","batchCode","processName","actionUrl","reason","workTaskId"]', 0,
       'eDHR偏差责任交接', 'edhr-deviation-work-task', NOW(), 'edhr-deviation-work-task', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code` = 'MES_EDHR_DEVIATION_CORRECT_ASSIGNED' AND `deleted` = b'0');
