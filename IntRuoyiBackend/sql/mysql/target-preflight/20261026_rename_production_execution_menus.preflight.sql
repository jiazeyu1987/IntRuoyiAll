-- release-target-preflight: migrationId=20261026_rename_production_execution_menus; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=605071315 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/production-release/PqcProductionReleasePage') AND BINARY path IN ('/mes/production-release/pqc')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=900230 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/edhr-work-task/WorkTaskBoardPage') AND BINARY path IN ('/mes/pro/feedback/edhr-work-task')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=9008300 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/edhr-nonconformance/NonconformanceReviewPage') AND BINARY path IN ('feedback/edhr-nonconformance-review','/mes/pro/feedback/edhr-nonconformance-review')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_rename_production_execution_menus' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_rename_production_execution_menus:BASELINE_CONFLICT' END;
