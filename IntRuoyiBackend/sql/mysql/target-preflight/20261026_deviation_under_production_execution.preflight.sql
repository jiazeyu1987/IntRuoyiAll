-- release-target-preflight: migrationId=20261026_deviation_under_production_execution; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN 1 ELSE JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY' END))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=9008440 AND permission='mes:pro-edhr-deviation:query' AND deleted=b'0'))
  AND ((SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2 AND (component='mes/pro/edhr-deviation/index' OR path='/mes/pro/feedback/edhr-deviation'))<=1)
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991530 AND NOT (deleted=b'0' AND type=2 AND component='mes/pro/edhr-deviation/index')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_deviation_under_production_execution' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_deviation_under_production_execution:BASELINE_CONFLICT' END;
