-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_system_entitlement_management,20260526_edhr_approval_archive_schema_contract; type=data; riskLevel=low
-- Dynamic entitlement policy for ordinary eDHR REVIEW/APPROVE work-task assignees.
-- Grants only the approval detail/action permissions needed by the assigned business task.

INSERT INTO `system_entitlement_policy`
(`policy_code`, `policy_name`, `module_code`, `status`, `description`,
 `allowed_permission_codes_json`, `allowed_menu_refs_json`, `forbidden_permission_codes_json`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  'MES_EDHR_APPROVAL_REVIEWER_MINIMAL',
  'MES eDHR approval reviewer minimal entitlement',
  'mes',
  0,
  'Sources: EDHR_WORK_TASK_ASSIGNEE taskType=REVIEW/APPROVE',
  JSON_ARRAY(
    'mes:pro-batch-record-execution:approve',
    'mes:pro-batch-record-execution:track',
    'mes:pro-batch-record-execution:signature-query',
    'mes:pro-batch-record-execution-archive:query'
  ),
  JSON_ARRAY(
    JSON_OBJECT('permission', 'mes:pro-batch-record-execution:approve'),
    JSON_OBJECT('permission', 'mes:pro-batch-record-execution:track'),
    JSON_OBJECT('permission', 'mes:pro-batch-record-execution:signature-query'),
    JSON_OBJECT('permission', 'mes:pro-batch-record-execution-archive:query')
  ),
  JSON_ARRAY(
    'mes:pro-batch-record-execution:create',
    'mes:pro-batch-record-execution:update',
    'mes:pro-batch-record-execution:delete',
    'mes:pro-batch-record-execution-archive:create',
    'mes:pro-batch-record-execution-archive:download',
    'mes:pro-edhr-batch-execution:create',
    'mes:pro-edhr-batch-execution:update',
    'mes:pro-edhr-batch-execution:close',
    'mes:pro-edhr-batch-execution:archive',
    'mes:pro-edhr-batch-execution:quality-reject',
    'mes:pro-edhr-work-task:update',
    'mes:pro-edhr-work-task-rule:update',
    'system:permission:assign-role-menu',
    'system:permission:assign-user-role'
  ),
  'edhr-approval-reviewer-runtime-entitlement',
  NOW(),
  'edhr-approval-reviewer-runtime-entitlement',
  NOW(),
  b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_entitlement_policy`
  WHERE `policy_code` = 'MES_EDHR_APPROVAL_REVIEWER_MINIMAL'
);
