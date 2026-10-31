-- release-target-preflight: migrationId=20261027_rename_batch_execution_market_release; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=900033 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/edhr-batch/BatchExecutionListPage') AND BINARY path IN ('/mes/pro/feedback/edhr-batch-execution')))
THEN 'TARGET_PREFLIGHT_PASS:20261027_rename_batch_execution_market_release' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261027_rename_batch_execution_market_release:BASELINE_CONFLICT' END;
