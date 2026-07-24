-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_edhr_work_task_flow,20260612_mes_edhr_final_archive_work_task; type=data; riskLevel=medium
-- eDHR BPM 审批通知改为站内信，避免审批主链路硬依赖用户移动联系方式。
-- Fail fast: 只补正式站内信模板，不增加短信兜底，不自动补用户资料。

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR BPM审批待办通知', 'MES_EDHR_BPM_TASK_ASSIGNED', 2, 'eDHR任务中心',
       '工作到你了：请审批流程{processInstanceName}。入口：{detailUrl}',
       '["processInstanceName","taskName","startUserNickname","detailUrl"]', 0, 'eDHR BPM 通知站内信化',
       'edhr-bpm-notify', NOW(), 'edhr-bpm-notify', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_EDHR_BPM_TASK_ASSIGNED' AND `deleted` = b'0'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR BPM审批通过通知', 'MES_EDHR_BPM_APPROVED', 2, 'eDHR任务中心',
       '你的流程{processInstanceName}已审批通过。入口：{detailUrl}',
       '["processInstanceName","detailUrl"]', 0, 'eDHR BPM 通知站内信化',
       'edhr-bpm-notify', NOW(), 'edhr-bpm-notify', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_EDHR_BPM_APPROVED' AND `deleted` = b'0'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR BPM审批驳回通知', 'MES_EDHR_BPM_REJECTED', 2, 'eDHR任务中心',
       '你的流程{processInstanceName}已被驳回，原因：{reason}。入口：{detailUrl}',
       '["processInstanceName","reason","detailUrl"]', 0, 'eDHR BPM 通知站内信化',
       'edhr-bpm-notify', NOW(), 'edhr-bpm-notify', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_EDHR_BPM_REJECTED' AND `deleted` = b'0'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR BPM审批超时提醒', 'MES_EDHR_BPM_TASK_TIMEOUT', 2, 'eDHR任务中心',
       '你的审批任务{taskName}已超时，请尽快处理流程{processInstanceName}。入口：{detailUrl}',
       '["processInstanceName","taskName","detailUrl"]', 0, 'eDHR BPM 通知站内信化',
       'edhr-bpm-notify', NOW(), 'edhr-bpm-notify', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_EDHR_BPM_TASK_TIMEOUT' AND `deleted` = b'0'
);
