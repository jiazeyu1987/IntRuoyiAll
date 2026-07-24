-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=low
-- Phase5: retire duplicate approval-center menu entries.
-- Scope: hide legacy formal menu entries after BPM / DCC / eDHR / Showroom tasks are reachable
-- through /approval-center. Domain detail and processing pages remain available for routed handling.

START TRANSACTION;

UPDATE system_menu
SET `visible` = b'0',
    updater = 'unified-approval-phase5',
    update_time = NOW()
WHERE deleted = b'0'
  AND (
    component IN (
      'bpm/task/todo/index',
      'bpm/task/done/index',
      'bpm/processInstance/index',
      'dcc/controlled-file/approval-tasks/index',
      'mes/pro/edhr/ApprovalPage'
    )
    OR path IN (
      'task/todo',
      'task/done',
      'process-instance/my',
      'controlled-file/approval-tasks',
      'feedback/edhr-approval'
    )
    OR component_name = 'ShowroomAdminApproval'
  );

COMMIT;
