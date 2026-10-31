-- release-target-preflight: migrationId=20261026_hide_edhr_form_test_menus; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=900272 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/edhr-form/FormPage') AND BINARY path IN ('/mes/pro/feedback/edhr-form')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=900440 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/edhr-batch/BatchRecordTestPage') AND BINARY path IN ('/mes/pro/feedback/edhr-batch-test')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_hide_edhr_form_test_menus' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_hide_edhr_form_test_menus:BASELINE_CONFLICT' END;
