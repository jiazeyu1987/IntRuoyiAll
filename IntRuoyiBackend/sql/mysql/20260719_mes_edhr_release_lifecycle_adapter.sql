-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_system_entitlement_management,20260611_mes_edhr_work_task_flow; type=data; riskLevel=low
-- Thin eDHR release lifecycle adapter seeds. Reuses work tasks, unified approval center, notify templates and dynamic entitlement policy.

INSERT INTO `system_entitlement_policy`
(`policy_code`, `policy_name`, `module_code`, `status`, `description`,
 `allowed_permission_codes_json`, `allowed_menu_refs_json`, `forbidden_permission_codes_json`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  'MES_EDHR_RELEASE_APPROVER_MINIMAL',
  'MES eDHR release approver minimal entitlement',
  'mes',
  0,
  'Sources: EDHR_WORK_TASK_ASSIGNEE taskType=RELEASE_APPROVE',
  JSON_ARRAY(
    'bpm:task:query',
    'bpm:task:update',
    'mes:pro-edhr-work-task:query',
    'mes:pro-edhr-batch-execution:query',
    'mes:pro-edhr-release:query',
    'mes:pro-edhr-release:approve',
    'mes:pro-edhr-release:reject',
    'mes:pro-edhr-release:event-query'
  ),
  JSON_ARRAY(
    JSON_OBJECT('permission', 'bpm:task:query'),
    JSON_OBJECT('permission', 'bpm:task:update'),
    JSON_OBJECT('permission', 'mes:pro-edhr-work-task:query'),
    JSON_OBJECT('permission', 'mes:pro-edhr-batch-execution:query'),
    JSON_OBJECT('permission', 'mes:pro-edhr-release:query'),
    JSON_OBJECT('permission', 'mes:pro-edhr-release:approve'),
    JSON_OBJECT('permission', 'mes:pro-edhr-release:reject'),
    JSON_OBJECT('permission', 'mes:pro-edhr-release:event-query')
  ),
  JSON_ARRAY(
    'mes:pro-edhr-batch-execution:create',
    'mes:pro-edhr-batch-execution:close',
    'mes:pro-edhr-batch-execution-archive:create',
    'mes:pro-edhr-batch-execution:quality-reject',
    'mes:pro-edhr-release:precheck',
    'mes:pro-edhr-release:submit',
    'mes:pro-edhr-release:withdraw',
    'system:permission:assign-role-menu',
    'system:permission:assign-user-role'
  ),
  'edhr-release-lifecycle-adapter',
  NOW(),
  'edhr-release-lifecycle-adapter',
  NOW(),
  b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_entitlement_policy`
  WHERE `policy_code` = 'MES_EDHR_RELEASE_APPROVER_MINIMAL'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  'eDHR最终放行审批任务通知',
  'MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED',
  2,
  'eDHR任务中心',
  '工作到你了：请审批工单{workOrderCode}批次{batchCode}的最终放行。入口：{actionUrl}',
  '["workOrderCode","batchCode","processName","actionUrl","workTaskId","reason"]',
  0,
  'eDHR放行生命周期适配层站内信模板',
  'edhr-release-lifecycle-adapter',
  NOW(),
  'edhr-release-lifecycle-adapter',
  NOW(),
  b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_notify_template`
  WHERE `code` = 'MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED'
    AND `deleted` = b'0'
);
