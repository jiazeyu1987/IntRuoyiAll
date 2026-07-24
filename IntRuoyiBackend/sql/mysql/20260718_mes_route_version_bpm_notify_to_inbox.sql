-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260717_mes_route_version_approval_bpm_seed; type=data; riskLevel=low
-- 工艺路线版本审批 BPM 通知改为站内信，避免审批主链路硬依赖短信或用户移动联系方式。
-- Process definition key: mes-route-version-approval-v1.
-- Fail fast: 只补正式站内信模板，不增加短信兜底，不自动补审批人或用户资料。

SET NAMES utf8mb4;

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工艺路线版本审批待办通知', 'MES_ROUTE_VERSION_BPM_TASK_ASSIGNED', 2, '工艺路线版本中心',
       '工作到你了：请审批工艺路线版本流程{processInstanceName}。入口：{detailUrl}',
       '["processInstanceName","taskName","startUserNickname","detailUrl"]', 0, '工艺路线版本审批通知站内信化',
       'route-bpm-notify', NOW(), 'route-bpm-notify', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_ROUTE_VERSION_BPM_TASK_ASSIGNED' AND `deleted` = b'0'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工艺路线版本审批通过通知', 'MES_ROUTE_VERSION_BPM_APPROVED', 2, '工艺路线版本中心',
       '你的工艺路线版本流程{processInstanceName}已审批通过并生效。入口：{detailUrl}',
       '["processInstanceName","detailUrl"]', 0, '工艺路线版本审批通知站内信化',
       'route-bpm-notify', NOW(), 'route-bpm-notify', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_ROUTE_VERSION_BPM_APPROVED' AND `deleted` = b'0'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工艺路线版本审批驳回通知', 'MES_ROUTE_VERSION_BPM_REJECTED', 2, '工艺路线版本中心',
       '你的工艺路线版本流程{processInstanceName}已被驳回，原因：{reason}。入口：{detailUrl}',
       '["processInstanceName","reason","detailUrl"]', 0, '工艺路线版本审批通知站内信化',
       'route-bpm-notify', NOW(), 'route-bpm-notify', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_ROUTE_VERSION_BPM_REJECTED' AND `deleted` = b'0'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工艺路线版本审批超时提醒', 'MES_ROUTE_VERSION_BPM_TASK_TIMEOUT', 2, '工艺路线版本中心',
       '你的工艺路线版本审批任务{taskName}已超时，请尽快处理流程{processInstanceName}。入口：{detailUrl}',
       '["processInstanceName","taskName","detailUrl"]', 0, '工艺路线版本审批通知站内信化',
       'route-bpm-notify', NOW(), 'route-bpm-notify', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_ROUTE_VERSION_BPM_TASK_TIMEOUT' AND `deleted` = b'0'
);
