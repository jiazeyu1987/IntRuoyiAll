-- release-target-preflight: migrationId=20261026_merge_production_tasks_into_workbench; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=900230 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/edhr-work-task/WorkTaskBoardPage') AND BINARY path IN ('/mes/pro/feedback/edhr-work-task')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_merge_production_tasks_into_workbench' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_merge_production_tasks_into_workbench:BASELINE_CONFLICT' END;
